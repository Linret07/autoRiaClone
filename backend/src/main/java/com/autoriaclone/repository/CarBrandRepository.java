package com.autoriaclone.repository;

import com.autoriaclone.entity.CarBrand;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CarBrandRepository extends JpaRepository<CarBrand, Long> {
    Optional<CarBrand> findByNameIgnoreCase(String name);
}
