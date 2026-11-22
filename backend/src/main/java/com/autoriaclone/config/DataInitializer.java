package com.autoriaclone.config;

import com.autoriaclone.entity.*;
import com.autoriaclone.entity.enums.*;
import com.autoriaclone.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CarBrandRepository brandRepository;
    private final CarModelRepository modelRepository;
    private final AdvertisementRepository adRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (roleRepository.count() > 0) return;

        Role buyerRole = roleRepository.save(Role.builder()
                .name(RoleName.BUYER)
                .permissions(Set.of())
                .build());

        Role sellerRole = roleRepository.save(Role.builder()
                .name(RoleName.SELLER)
                .permissions(Set.of(
                        Permission.AD_CREATE,
                        Permission.AD_EDIT_OWN,
                        Permission.AD_DELETE_OWN,
                        Permission.AD_VIEW_ANALYTICS,
                        Permission.BRAND_REQUEST_CREATE
                ))
                .build());

        Role managerRole = roleRepository.save(Role.builder()
                .name(RoleName.MANAGER)
                .permissions(Set.of(
                        Permission.AD_DELETE_ANY,
                        Permission.AD_MODERATE,
                        Permission.USER_BAN,
                        Permission.BRAND_MANAGE
                ))
                .build());

        Role adminRole = roleRepository.save(Role.builder()
                .name(RoleName.ADMIN)
                .permissions(Set.of(Permission.values()))
                .build());

        userRepository.save(User.builder()
                .email("admin@autoria.ua")
                .password(passwordEncoder.encode("admin123"))
                .fullName("Головний Адміністратор")
                .phone("+380500000001")
                .role(adminRole)
                .accountType(AccountType.PREMIUM)
                .build());

        userRepository.save(User.builder()
                .email("manager@autoria.ua")
                .password(passwordEncoder.encode("manager123"))
                .fullName("Менеджер Платформи")
                .phone("+380500000002")
                .role(managerRole)
                .accountType(AccountType.PREMIUM)
                .build());

        User basicSeller = userRepository.save(User.builder()
                .email("seller@autoria.ua")
                .password(passwordEncoder.encode("seller123"))
                .fullName("Іван Продавець (Basic)")
                .phone("+380500000003")
                .role(sellerRole)
                .accountType(AccountType.BASIC)
                .build());

        User premiumSeller = userRepository.save(User.builder()
                .email("premium@autoria.ua")
                .password(passwordEncoder.encode("premium123"))
                .fullName("Олена Автобізнес (Premium)")
                .phone("+380500000004")
                .role(sellerRole)
                .accountType(AccountType.PREMIUM)
                .build());

        userRepository.save(User.builder()
                .email("buyer@autoria.ua")
                .password(passwordEncoder.encode("buyer123"))
                .fullName("Петро Покупець")
                .phone("+380500000005")
                .role(buyerRole)
                .accountType(AccountType.BASIC)
                .build());

        CarBrand bmw = brandRepository.save(CarBrand.builder().name("BMW").build());
        CarBrand daewoo = brandRepository.save(CarBrand.builder().name("Daewoo").build());
        CarBrand toyota = brandRepository.save(CarBrand.builder().name("Toyota").build());

        CarModel x5 = modelRepository.save(CarModel.builder().name("X5").brand(bmw).build());
        modelRepository.save(CarModel.builder().name("3 Series").brand(bmw).build());
        CarModel lanos = modelRepository.save(CarModel.builder().name("Lanos").brand(daewoo).build());
        CarModel camry = modelRepository.save(CarModel.builder().name("Camry").brand(toyota).build());

        adRepository.saveAll(List.of(
                Advertisement.builder()
                        .seller(premiumSeller).brand(bmw).model(x5).year(2020).region(Region.LVIV)
                        .description("Ідеальний стан, офіційна історія обслуговування.")
                        .originalPrice(BigDecimal.valueOf(45000)).originalCurrency(Currency.USD)
                        .priceUsd(BigDecimal.valueOf(45000)).priceUah(BigDecimal.valueOf(1845000)).priceEur(BigDecimal.valueOf(41500))
                        .exchangeRateUsd(BigDecimal.valueOf(41.00)).exchangeRateEur(BigDecimal.valueOf(44.45))
                        .rateUpdatedAt(LocalDateTime.now()).status(AdStatus.ACTIVE)
                        .build(),
                Advertisement.builder()
                        .seller(premiumSeller).brand(bmw).model(x5).year(2019).region(Region.KYIV)
                        .description("Гарна комплектація, один власник.")
                        .originalPrice(BigDecimal.valueOf(41000)).originalCurrency(Currency.USD)
                        .priceUsd(BigDecimal.valueOf(41000)).priceUah(BigDecimal.valueOf(1681000)).priceEur(BigDecimal.valueOf(37815))
                        .exchangeRateUsd(BigDecimal.valueOf(41.00)).exchangeRateEur(BigDecimal.valueOf(44.45))
                        .rateUpdatedAt(LocalDateTime.now()).status(AdStatus.ACTIVE)
                        .build(),
                Advertisement.builder()
                        .seller(basicSeller).brand(daewoo).model(lanos).year(2008).region(Region.LVIV)
                        .description("Надійне авто на кожен день, газ/бензин.")
                        .originalPrice(BigDecimal.valueOf(3200)).originalCurrency(Currency.USD)
                        .priceUsd(BigDecimal.valueOf(3200)).priceUah(BigDecimal.valueOf(131200)).priceEur(BigDecimal.valueOf(2951))
                        .exchangeRateUsd(BigDecimal.valueOf(41.00)).exchangeRateEur(BigDecimal.valueOf(44.45))
                        .rateUpdatedAt(LocalDateTime.now()).status(AdStatus.ACTIVE)
                        .build()
        ));
    }
}
