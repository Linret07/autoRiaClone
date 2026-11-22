package com.autoriaclone.entity;

import com.autoriaclone.entity.enums.AdStatus;
import com.autoriaclone.entity.enums.Currency;
import com.autoriaclone.entity.enums.Region;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "advertisements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Advertisement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "brand_id", nullable = false)
    private CarBrand brand;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "model_id", nullable = false)
    private CarModel model;

    @Column(nullable = false)
    private Integer year;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Region region;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency originalCurrency;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal priceUah;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal priceUsd;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal priceEur;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal exchangeRateUsd;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal exchangeRateEur;

    private LocalDateTime rateUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AdStatus status = AdStatus.ACTIVE;

    @Builder.Default
    private int editAttempts = 0;

    private String imageUrl;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
