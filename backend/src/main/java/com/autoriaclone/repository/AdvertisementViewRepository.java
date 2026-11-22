package com.autoriaclone.repository;

import com.autoriaclone.entity.AdvertisementView;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;

public interface AdvertisementViewRepository extends JpaRepository<AdvertisementView, Long> {
    long countByAdvertisementId(Long advertisementId);
    long countByAdvertisementIdAndViewedAtAfter(Long advertisementId, LocalDateTime after);
}
