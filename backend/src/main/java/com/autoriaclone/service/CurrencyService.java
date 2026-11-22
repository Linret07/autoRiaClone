package com.autoriaclone.service;

import com.autoriaclone.entity.Advertisement;
import com.autoriaclone.entity.enums.AdStatus;
import com.autoriaclone.entity.enums.Currency;
import com.autoriaclone.repository.AdvertisementRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
@Getter
public class CurrencyService {
    private final AdvertisementRepository adRepository;

    private BigDecimal usdToUah = BigDecimal.valueOf(41.20);
    private BigDecimal eurToUah = BigDecimal.valueOf(44.50);
    private LocalDateTime lastUpdated = LocalDateTime.now();

    @PostConstruct
    public void init() {
        fetchRatesFromPrivatBank();
    }

    // Оновлення раз на день о 08:00
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void dailyRateUpdate() {
        fetchRatesFromPrivatBank();
        List<Advertisement> activeAds = adRepository.findByStatus(AdStatus.ACTIVE);
        for (Advertisement ad : activeAds) {
            calculatePrices(ad);
        }
        adRepository.saveAll(activeAds);
        log.info("Щоденне оновлення цін виконано для {} оголошень", activeAds.size());
    }

    public void fetchRatesFromPrivatBank() {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.privatbank.ua/p24api/pubinfo?json&exchange&coursid=5"))
                    .GET().build();
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                usdToUah = parseSaleRate(resp.body(), "USD", usdToUah);
                eurToUah = parseSaleRate(resp.body(), "EUR", eurToUah);
                lastUpdated = LocalDateTime.now();
                log.info("Оновлено курс ПриватБанку: USD={}, EUR={}", usdToUah, eurToUah);
            }
        } catch (Exception e) {
            log.warn("Не вдалося отримати курс ПриватБанку, використовується резервний: {}", e.getMessage());
        }
    }

    private BigDecimal parseSaleRate(String json, String ccy, BigDecimal fallback) {
        Matcher m = Pattern.compile("\"ccy\"\\s*:\\s*\"" + ccy + "\".*?\"sale\"\\s*:\\s*\"([0-9.]+)\"").matcher(json);
        return m.find() ? new BigDecimal(m.group(1)) : fallback;
    }

    public void calculatePrices(Advertisement ad) {
        BigDecimal price = ad.getOriginalPrice();
        Currency curr = ad.getOriginalCurrency();

        BigDecimal uah = switch (curr) {
            case UAH -> price;
            case USD -> price.multiply(usdToUah);
            case EUR -> price.multiply(eurToUah);
        };

        ad.setPriceUah(uah.setScale(2, RoundingMode.HALF_UP));
        ad.setPriceUsd(uah.divide(usdToUah, 2, RoundingMode.HALF_UP));
        ad.setPriceEur(uah.divide(eurToUah, 2, RoundingMode.HALF_UP));
        ad.setExchangeRateUsd(usdToUah);
        ad.setExchangeRateEur(eurToUah);
        ad.setRateUpdatedAt(lastUpdated);
    }
}
