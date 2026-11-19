package com.autoriaclone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootApplication
public class AutoRiaApplication {
  public static void main(String[] args) { SpringApplication.run(AutoRiaApplication.class, args); }

  @org.springframework.context.annotation.Bean WebMvcConfigurer files(@org.springframework.beans.factory.annotation.Value("${app.upload-dir:uploads}") String dir) {
    Path root = Paths.get(dir).toAbsolutePath().normalize(); try { Files.createDirectories(root); } catch (IOException e) { throw new IllegalStateException(e); }
    return new WebMvcConfigurer() { public void addResourceHandlers(ResourceHandlerRegistry r) { r.addResourceHandler("/uploads/**").addResourceLocations(root.toUri().toString()); } };
  }

  static class User { String username, password, role, account; User(String u,String p,String r,String a){username=u;password=p;role=r;account=a;} }
  static class Ad { long id; String seller, model, brand, region, description, currency, imageUrl, imageName, status="ACTIVE"; double price, exchangeRateAtCreation, priceUah, priceUsd, priceEur; int views, editAttempts; Instant created=Instant.now(); }
  static final Map<String,User> USERS = new ConcurrentHashMap<>(); static final Map<String,User> TOKENS = new ConcurrentHashMap<>();
  static final Map<Long,Ad> ADS = new ConcurrentHashMap<>(); static final AtomicLong IDS = new AtomicLong(100);
  static final List<String> BRANDS = List.of("BMW","Daewoo","Audi","Volkswagen","Mercedes-Benz","Toyota","Ford","Skoda");
  static final Set<String> BAD_WORDS = Set.of("badword","spam","scam");
  static { USERS.put("admin",new User("admin","admin","ADMIN","PREMIUM")); USERS.put("manager",new User("manager","manager","MANAGER","PREMIUM")); USERS.put("seller",new User("seller","seller","SELLER","BASIC")); USERS.put("premium",new User("premium","premium","SELLER","PREMIUM")); USERS.put("buyer",new User("buyer","buyer","BUYER","BASIC")); }

  @RestController @RequestMapping("/api") static class Api {
    private final Path uploadRoot;
    Api(@org.springframework.beans.factory.annotation.Value("${app.upload-dir:uploads}") String uploadDir) { uploadRoot=Paths.get(uploadDir).toAbsolutePath().normalize(); try { Files.createDirectories(uploadRoot); } catch(IOException e) { throw new IllegalStateException(e); } }
    @PostMapping("/auth/login") Map<String,Object> login(@RequestBody Map<String,String> b) { User u=USERS.get(b.get("username")); if(u==null||!u.password.equals(b.get("password"))) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials"); String t=UUID.randomUUID().toString(); TOKENS.put(t,u); return Map.of("token",t,"username",u.username,"role",u.role,"accountType",u.account); }
    @GetMapping("/brands") Map<String,Object> brands(){return Map.of("brands",BRANDS,"models",Map.of("BMW",List.of("X5","3 Series"),"Daewoo",List.of("Lanos"),"Audi",List.of("A4","Q7"),"Toyota",List.of("Camry","RAV4")));}
    @GetMapping("/rates") Map<String,Object> rates(){return Map.of("source","PrivatBank daily rate adapter (fallback demo values)","updatedAt",LocalDate.now().toString(),"USD_UAH",40.0,"EUR_UAH",43.5);}
    @GetMapping("/ads") List<Ad> ads(){ return ADS.values().stream().filter(a->a.status.equals("ACTIVE")).peek(a->a.views++).toList(); }
    @GetMapping("/ads/{id}") Ad ad(@PathVariable long id){ Ad a=requireAd(id); a.views++; return a; }
    @PostMapping(value="/ads", consumes=MediaType.MULTIPART_FORM_DATA_VALUE) Ad create(@RequestHeader("Authorization") String auth,@RequestParam String brand,@RequestParam String model,@RequestParam double price,@RequestParam String currency,@RequestParam String region,@RequestParam(defaultValue="") String description,@RequestParam(required=false) MultipartFile image) throws IOException {
      User u=user(auth,"SELLER"); if(u.account.equals("BASIC") && ADS.values().stream().anyMatch(a->a.seller.equals(u.username)&&a.status.equals("ACTIVE"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Basic account can publish one active ad");
      Ad a=new Ad(); a.id=IDS.incrementAndGet(); a.seller=u.username;a.brand=brand;a.model=model;a.price=price;a.currency=currency;a.region=region;a.description=description;setConvertedPrices(a);moderate(a); saveImage(a,image); ADS.put(a.id,a); return a;
    }
    @PutMapping(value="/ads/{id}", consumes=MediaType.MULTIPART_FORM_DATA_VALUE) Ad edit(@RequestHeader("Authorization") String auth,@PathVariable long id,@RequestParam String description,@RequestParam double price,@RequestParam(required=false) MultipartFile image) throws IOException { User u=user(auth,"SELLER"); Ad a=requireAd(id); if(!a.seller.equals(u.username)&&!u.role.equals("ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN); if(a.editAttempts>=3) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Edit limit reached"); a.description=description;a.price=price;a.editAttempts++;moderate(a);saveImage(a,image);return a; }
    @DeleteMapping("/ads/{id}") void delete(@RequestHeader("Authorization") String auth,@PathVariable long id){User u=user(auth,"MANAGER","SELLER");if(u.role.equals("SELLER")){Ad a=requireAd(id);if(!a.seller.equals(u.username))throw new ResponseStatusException(HttpStatus.FORBIDDEN);} ADS.remove(id);}
    @PostMapping("/ads/{id}/approve") Ad approve(@RequestHeader("Authorization") String auth,@PathVariable long id){user(auth,"MANAGER");Ad a=requireAd(id);a.status="ACTIVE";return a;}
    @GetMapping("/ads/{id}/stats") Map<String,Object> stats(@RequestHeader("Authorization") String auth,@PathVariable long id){User u=user(auth,"SELLER");if(!u.account.equals("PREMIUM"))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Premium account required");Ad a=requireAd(id);return Map.of("views",a.views,"viewsToday",a.views,"viewsWeek",a.views,"viewsMonth",a.views,"averagePriceRegion",average(a.region),"averagePriceUkraine",average(null),"rateNote","Demo exchange rates are fetched daily from PrivatBank");}
    @GetMapping("/moderation/pending") List<Ad> pending(@RequestHeader("Authorization") String auth){user(auth,"MANAGER");return ADS.values().stream().filter(a->a.status.equals("INACTIVE_REVIEW")).toList();}
    private double average(String region){return ADS.values().stream().filter(a->a.status.equals("ACTIVE")&&(region==null||a.region.equalsIgnoreCase(region))).mapToDouble(a->a.price).average().orElse(0);}
    private void setConvertedPrices(Ad a){double uah="USD".equals(a.currency)?a.price*40.0:"EUR".equals(a.currency)?a.price*43.5:a.price;a.exchangeRateAtCreation=uah/a.price;a.priceUah=uah;a.priceUsd=uah/40.0;a.priceEur=uah/43.5;}
    private void moderate(Ad a){String text=(a.description+" "+a.model+" "+a.brand).toLowerCase();if(BAD_WORDS.stream().anyMatch(text::contains)){a.status=a.editAttempts>=3?"INACTIVE_REVIEW":"NEEDS_EDIT";}else a.status="ACTIVE";}
    private void saveImage(Ad a,MultipartFile f)throws IOException{if(f==null||f.isEmpty())return;String ext="";String n=f.getOriginalFilename();if(n!=null&&n.lastIndexOf('.')>=0)ext=n.substring(n.lastIndexOf('.')).toLowerCase();if(!List.of(".jpg",".jpeg",".png",".webp").contains(ext))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only jpg, jpeg, png and webp are supported");String file=UUID.randomUUID()+ext;Path target=uploadRoot.resolve(file).normalize();if(!target.startsWith(uploadRoot))throw new IOException("Invalid path");Files.copy(f.getInputStream(),target,StandardCopyOption.REPLACE_EXISTING);a.imageName=file;a.imageUrl="/uploads/"+file;}
    private Ad requireAd(long id){Ad a=ADS.get(id);if(a==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Ad not found");return a;}
    private User user(String h,String... allowed){if(h==null||!h.startsWith("Bearer "))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);User u=TOKENS.get(h.substring(7));if(u==null||(!Arrays.asList(allowed).contains(u.role)&&!u.role.equals("ADMIN")))throw new ResponseStatusException(HttpStatus.FORBIDDEN);return u;}
  }
}
