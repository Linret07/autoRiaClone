package com.autoriaclone.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class AdStatsResponse {
    private Long advertisementId;
    private long totalViews;
    private long viewsToday;
    private long viewsWeek;
    private long viewsMonth;
    private String region;
    private BigDecimal averagePriceInRegionUsd;
    private BigDecimal averagePriceInUkraineUsd;
}
