package com.autoriaclone.repository;

import com.autoriaclone.entity.CarModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CarModelRepository extends JpaRepository<CarModel, Long> {
    List<CarModel> findByBrandId(Long brandId);
    Optional<CarModel> findByBrandIdAndNameIgnoreCase(Long brandId, String name);
}
