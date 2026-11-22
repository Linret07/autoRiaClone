package com.autoriaclone.service;

import com.autoriaclone.dto.AdStatsResponse;
import com.autoriaclone.entity.*;
import com.autoriaclone.entity.enums.*;
import com.autoriaclone.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdvertisementService {
    private final AdvertisementRepository adRepository;
    private final AdvertisementViewRepository viewRepository;
    private final CarBrandRepository brandRepository;
    private final CarModelRepository modelRepository;
    private final CurrencyService currencyService;
    private final ProfanityFilterService profanityFilter;
    private final EmailService emailService;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public List<Advertisement> getAllActive() {
        return adRepository.findByStatus(AdStatus.ACTIVE);
    }

    @Transactional
    public Advertisement getByIdAndRecordView(Long id) {
        Advertisement ad = requireAd(id);
        viewRepository.save(AdvertisementView.builder().advertisement(ad).build());
        return ad;
    }

    @Transactional
    public Advertisement create(User seller, Long brandId, Long modelId, Integer year, Region region,
                                BigDecimal price, Currency currency, String description, MultipartFile image) throws IOException {
        if (seller.getAccountType() == AccountType.BASIC) {
            long existing = adRepository.countBySellerIdAndStatusIn(
                    seller.getId(), List.of(AdStatus.ACTIVE, AdStatus.NEEDS_EDIT));
            if (existing >= 1) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Акаунт BASIC дозволяє мати лише 1 оголошення. Оновіть підписку до PREMIUM.");
            }
        }

        CarBrand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Марку авто не знайдено"));
        CarModel model = modelRepository.findById(modelId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Модель авто не знайдено"));
        if (!model.getBrand().getId().equals(brand.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Обрана модель не належить цій марці авто");
        }

        Advertisement ad = Advertisement.builder()
                .seller(seller)
                .brand(brand)
                .model(model)
                .year(year)
                .region(region)
                .description(description)
                .originalPrice(price)
                .originalCurrency(currency)
                .build();

        currencyService.calculatePrices(ad);
        applyModeration(ad);
        if (image != null && !image.isEmpty()) {
            ad.setImageUrl(saveImage(image));
        }
        return adRepository.save(ad);
    }

    @Transactional
    public Advertisement edit(User seller, Long adId, BigDecimal price, Currency currency,
                              String description, MultipartFile image) throws IOException {
        Advertisement ad = requireAd(adId);
        if (!ad.getSeller().getId().equals(seller.getId()) && seller.getRole().getName() != RoleName.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ви можете редагувати лише власні оголошення");
        }
        if (ad.getEditAttempts() >= 3) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Вичерпано ліміт редагувань (3 спроби). Оголошення передано менеджеру.");
        }

        ad.setDescription(description);
        ad.setOriginalPrice(price);
        ad.setOriginalCurrency(currency);
        ad.setEditAttempts(ad.getEditAttempts() + 1);

        currencyService.calculatePrices(ad);
        applyModeration(ad);
        if (image != null && !image.isEmpty()) {
            ad.setImageUrl(saveImage(image));
        }
        return adRepository.save(ad);
    }

    private void applyModeration(Advertisement ad) {
        if (profanityFilter.containsProfanity(ad.getDescription())) {
            if (ad.getEditAttempts() >= 3) {
                ad.setStatus(AdStatus.INACTIVE_REVIEW);
                emailService.notifyManagerAboutFailedAd(ad);
            } else {
                ad.setStatus(AdStatus.NEEDS_EDIT);
            }
        } else {
            ad.setStatus(AdStatus.ACTIVE);
        }
    }

    public AdStatsResponse getStatistics(User user, Long adId) {
        if (user.getAccountType() != AccountType.PREMIUM) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Статистика доступна лише для користувачів з акаунтом PREMIUM");
        }
        Advertisement ad = requireAd(adId);
        if (!ad.getSeller().getId().equals(user.getId()) && user.getRole().getName() != RoleName.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Це не ваше оголошення");
        }

        LocalDateTime now = LocalDateTime.now();
        long total = viewRepository.countByAdvertisementId(adId);
        long today = viewRepository.countByAdvertisementIdAndViewedAtAfter(adId, now.minusDays(1));
        long week = viewRepository.countByAdvertisementIdAndViewedAtAfter(adId, now.minusWeeks(1));
        long month = viewRepository.countByAdvertisementIdAndViewedAtAfter(adId, now.minusMonths(1));

        BigDecimal avgRegion = adRepository.findAveragePriceUsdByBrandModelAndRegion(
                ad.getBrand().getId(), ad.getModel().getId(), ad.getRegion());
        BigDecimal avgUkraine = adRepository.findAveragePriceUsdByBrandAndModel(
                ad.getBrand().getId(), ad.getModel().getId());

        return AdStatsResponse.builder()
                .advertisementId(adId)
                .totalViews(total)
                .viewsToday(today)
                .viewsWeek(week)
                .viewsMonth(month)
                .region(ad.getRegion().name())
                .averagePriceInRegionUsd(avgRegion != null ? avgRegion.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .averagePriceInUkraineUsd(avgUkraine != null ? avgUkraine.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .build();
    }

    @Transactional
    public void delete(User actor, Long adId) {
        Advertisement ad = requireAd(adId);
        boolean canDeleteAny = actor.getRole().getPermissions().contains(Permission.AD_DELETE_ANY);
        if (!canDeleteAny && !ad.getSeller().getId().equals(actor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Немає прав на видалення цього оголошення");
        }
        adRepository.delete(ad);
    }

    @Transactional
    public Advertisement approveByManager(Long adId) {
        Advertisement ad = requireAd(adId);
        ad.setStatus(AdStatus.ACTIVE);
        return adRepository.save(ad);
    }

    public List<Advertisement> getPendingReview() {
        return adRepository.findByStatus(AdStatus.INACTIVE_REVIEW);
    }

    private Advertisement requireAd(Long id) {
        return adRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Оголошення не знайдено"));
    }

    private String saveImage(MultipartFile file) throws IOException {
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf('.')).toLowerCase() : "";
        if (!List.of(".jpg", ".jpeg", ".png", ".webp").contains(ext)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Підтримуються лише jpg, jpeg, png, webp");
        }
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
        String filename = UUID.randomUUID() + ext;
        Files.copy(file.getInputStream(), root.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/" + filename;
    }
}
