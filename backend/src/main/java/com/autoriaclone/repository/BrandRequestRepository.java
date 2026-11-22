package com.autoriaclone.repository;

import com.autoriaclone.entity.BrandRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BrandRequestRepository extends JpaRepository<BrandRequest, Long> {
    List<BrandRequest> findByResolvedFalse();
}
