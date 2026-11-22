package com.autoriaclone.repository;

import com.autoriaclone.entity.Advertisement;
import com.autoriaclone.entity.enums.AdStatus;
import com.autoriaclone.entity.enums.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {
    List<Advertisement> findByStatus(AdStatus status);
    List<Advertisement> findBySellerId(Long sellerId);
    long countBySellerIdAndStatusIn(Long sellerId, List<AdStatus> statuses);

    @Query("SELECT AVG(a.priceUsd) FROM Advertisement a WHERE a.brand.id = :brandId AND a.model.id = :modelId AND a.region = :region AND a.status = 'ACTIVE'")
    BigDecimal findAveragePriceUsdByBrandModelAndRegion(@Param("brandId") Long brandId, @Param("modelId") Long modelId, @Param("region") Region region);

    @Query("SELECT AVG(a.priceUsd) FROM Advertisement a WHERE a.brand.id = :brandId AND a.model.id = :modelId AND a.status = 'ACTIVE'")
    BigDecimal findAveragePriceUsdByBrandAndModel(@Param("brandId") Long brandId, @Param("modelId") Long modelId);
}
