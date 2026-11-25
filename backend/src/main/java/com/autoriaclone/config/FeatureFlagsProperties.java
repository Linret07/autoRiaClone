package com.autoriaclone.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.features")
public class FeatureFlagsProperties {

    private ProfanityFilter profanityFilter = new ProfanityFilter();
    private CurrencySync currencySync = new CurrencySync();
    private Notifications notifications = new Notifications();
    private Storage storage = new Storage();
    private Analytics analytics = new Analytics();
    private Dealerships dealerships = new Dealerships();

    @Data
    public static class ProfanityFilter {
        private boolean enabled = true;
        private int maxEditAttempts = 3;
    }

    @Data
    public static class CurrencySync {
        private boolean enabled = true;
        private boolean useMockFallback = true;
    }

    @Data
    public static class Notifications {
        private String provider = "mock";
        private String managerEmail = "manager@autoria.ua";
    }

    @Data
    public static class Storage {
        private String provider = "s3";
        private String s3Bucket = "autoria-clone-media-prod";
        private String awsRegion = "eu-central-1";
    }

    @Data
    public static class Analytics {
        private boolean enabled = true;
        private boolean premiumOnly = true;
    }

    @Data
    public static class Dealerships {
        private boolean enabled = true;
    }
}
