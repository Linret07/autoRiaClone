package com.autoriaclone.controller;

import com.autoriaclone.dto.AdStatsResponse;
import com.autoriaclone.entity.Advertisement;
import com.autoriaclone.entity.enums.Currency;
import com.autoriaclone.entity.enums.Region;
import com.autoriaclone.security.CustomUserDetails;
import com.autoriaclone.service.AdvertisementService;
import com.autoriaclone.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AdvertisementController {
    private final AdvertisementService adService;
    private final CurrencyService currencyService;

    @GetMapping("/rates")
    public Map<String, Object> getCurrentRates() {
        return Map.of(
                "source", "PrivatBank API",
                "USD_UAH", currencyService.getUsdToUah(),
                "EUR_UAH", currencyService.getEurToUah(),
                "updatedAt", currencyService.getLastUpdated()
        );
    }

    @GetMapping("/ads")
    public List<Advertisement> getAllActiveAds() {
        return adService.getAllActive();
    }

    @GetMapping("/ads/{id}")
    public Advertisement getAdById(@PathVariable Long id) {
        return adService.getByIdAndRecordView(id);
    }

    @PostMapping(value = "/ads", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('AD_CREATE')")
    public Advertisement createAd(@AuthenticationPrincipal CustomUserDetails principal,
                                  @RequestParam Long brandId,
                                  @RequestParam Long modelId,
                                  @RequestParam Integer year,
                                  @RequestParam Region region,
                                  @RequestParam BigDecimal price,
                                  @RequestParam Currency currency,
                                  @RequestParam(defaultValue = "") String description,
                                  @RequestParam(required = false) MultipartFile image) throws IOException {
        return adService.create(principal.getUser(), brandId, modelId, year, region, price, currency, description, image);
    }

    @PutMapping(value = "/ads/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('AD_EDIT_OWN')")
    public Advertisement editAd(@AuthenticationPrincipal CustomUserDetails principal,
                                @PathVariable Long id,
                                @RequestParam BigDecimal price,
                                @RequestParam Currency currency,
                                @RequestParam String description,
                                @RequestParam(required = false) MultipartFile image) throws IOException {
        return adService.edit(principal.getUser(), id, price, currency, description, image);
    }

    @DeleteMapping("/ads/{id}")
    @PreAuthorize("hasAnyAuthority('AD_DELETE_OWN', 'AD_DELETE_ANY')")
    public Map<String, String> deleteAd(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        adService.delete(principal.getUser(), id);
        return Map.of("message", "Оголошення видалено");
    }

    @GetMapping("/ads/{id}/stats")
    @PreAuthorize("hasAuthority('AD_VIEW_ANALYTICS')")
    public AdStatsResponse getStats(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        return adService.getStatistics(principal.getUser(), id);
    }

    @PostMapping("/ads/{id}/contact")
    public Map<String, Object> contactSeller(@PathVariable Long id,
                                             @RequestBody Map<String, String> payload) {
        String buyerName = payload.getOrDefault("name", "Покупець");
        String buyerPhone = payload.get("phone");
        if (buyerPhone == null || buyerPhone.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Вкажіть номер телефону для зв'язку");
        }
        String message = payload.get("message");
        return adService.contactSeller(id, buyerName, buyerPhone, message);
    }
}
