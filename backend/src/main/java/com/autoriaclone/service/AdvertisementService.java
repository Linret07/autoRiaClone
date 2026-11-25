package com.autoriaclone.service;

import com.autoriaclone.config.FeatureFlagsProperties;
import com.autoriaclone.dto.AdStatsResponse;
import com.autoriaclone.entity.*;
import com.autoriaclone.entity.enums.*;
import com.autoriaclone.repository.*;
import com.autoriaclone.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
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
    private final FileStorageService fileStorageService;
    private final FeatureFlagsProperties features;

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
            validateImageExtension(image);
            ad.setImageUrl(fileStorageService.save(image));
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
        int maxAttempts = features.getProfanityFilter().getMaxEditAttempts();
        if (ad.getEditAttempts() >= maxAttempts) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Вичерпано ліміт редагувань (" + maxAttempts + " спроби). Оголошення передано менеджеру.");
        }

        ad.setDescription(description);
        ad.setOriginalPrice(price);
        ad.setOriginalCurrency(currency);
        ad.setEditAttempts(ad.getEditAttempts() + 1);

        currencyService.calculatePrices(ad);
        applyModeration(ad);
        if (image != null && !image.isEmpty()) {
            validateImageExtension(image);
            ad.setImageUrl(fileStorageService.save(image));
        }
        return adRepository.save(ad);
    }

    private void validateImageExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf('.')).toLowerCase() : "";
        if (!List.of(".jpg", ".jpeg", ".png", ".webp").contains(ext)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Підтримуються лише jpg, jpeg, png, webp");
        }
    }

    private void applyModeration(Advertisement ad) {
        if (!features.getProfanityFilter().isEnabled()) {
            ad.setStatus(AdStatus.ACTIVE);
            return;
        }
        int maxAttempts = features.getProfanityFilter().getMaxEditAttempts();
        if (profanityFilter.containsProfanity(ad.getDescription())) {
            if (ad.getEditAttempts() >= maxAttempts) {
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
        if (!features.getAnalytics().isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Модуль аналітики тимчасово вимкнено");
        }
        if (features.getAnalytics().isPremiumOnly() && user.getAccountType() != AccountType.PREMIUM) {
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

    public Map<String, Object> contactSeller(Long adId, String buyerName, String buyerPhone, String message) {
        Advertisement ad = requireAd(adId);
        User seller = ad.getSeller();

        String text = (message != null && !message.isBlank())
                ? message
                : "Запит на огляд автомобіля та тест-драйв";

        log.info("[CONTACT REQUEST] Ad #{} ({} {} {}): buyer='{}', phone='{}', sellerEmail='{}', msg='{}'",
                ad.getId(), ad.getBrand().getName(), ad.getModel().getName(), ad.getYear(),
                buyerName, buyerPhone, seller.getEmail(), text);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SENT");
        response.put("advertisementId", ad.getId());
        response.put("car", ad.getBrand().getName() + " " + ad.getModel().getName() + " (" + ad.getYear() + ")");
        response.put("sellerName", seller.getFullName());
        response.put("sellerEmail", seller.getEmail());
        response.put("buyerName", buyerName);
        response.put("buyerPhone", buyerPhone);
        response.put("message", text);
        return response;
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
}
