package com.autoriaclone.controller;

import com.autoriaclone.config.FeatureFlagsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class FeatureController {

    private final FeatureFlagsProperties features;

    @GetMapping("/features")
    public Map<String, Object> getSystemFeatures() {
        return Map.of(
                "profanityFilter", features.getProfanityFilter(),
                "currencySync", features.getCurrencySync(),
                "notifications", features.getNotifications(),
                "storage", features.getStorage(),
                "analytics", features.getAnalytics(),
                "dealerships", features.getDealerships()
        );
    }
}
