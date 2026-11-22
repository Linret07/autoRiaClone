package com.autoriaclone.controller;

import com.autoriaclone.dto.BrandRequestDto;
import com.autoriaclone.entity.BrandRequest;
import com.autoriaclone.entity.CarBrand;
import com.autoriaclone.entity.CarModel;
import com.autoriaclone.repository.BrandRequestRepository;
import com.autoriaclone.repository.CarBrandRepository;
import com.autoriaclone.repository.CarModelRepository;
import com.autoriaclone.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {
    private final CarBrandRepository brandRepository;
    private final CarModelRepository modelRepository;
    private final BrandRequestRepository brandRequestRepository;

    @GetMapping
    public List<CarBrand> getAllBrands() {
        return brandRepository.findAll();
    }

    @GetMapping("/{brandId}/models")
    public List<CarModel> getModelsByBrand(@PathVariable Long brandId) {
        return modelRepository.findByBrandId(brandId);
    }

    @PostMapping("/request")
    @PreAuthorize("hasAuthority('BRAND_REQUEST_CREATE')")
    public BrandRequest requestMissingBrand(@AuthenticationPrincipal CustomUserDetails principal,
                                            @Valid @RequestBody BrandRequestDto dto) {
        return brandRequestRepository.save(BrandRequest.builder()
                .brandName(dto.getBrandName())
                .modelName(dto.getModelName())
                .requestedBy(principal.getUser())
                .build());
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAuthority('BRAND_MANAGE')")
    public List<BrandRequest> getPendingRequests() {
        return brandRequestRepository.findByResolvedFalse();
    }

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('BRAND_MANAGE')")
    public Map<String, Object> addBrandAndModel(@RequestParam String brandName, @RequestParam String modelName) {
        CarBrand brand = brandRepository.findByNameIgnoreCase(brandName)
                .orElseGet(() -> brandRepository.save(CarBrand.builder().name(brandName).build()));
        CarModel model = modelRepository.findByBrandIdAndNameIgnoreCase(brand.getId(), modelName)
                .orElseGet(() -> modelRepository.save(CarModel.builder().name(modelName).brand(brand).build()));
        return Map.of("brand", brand, "model", model);
    }
}
