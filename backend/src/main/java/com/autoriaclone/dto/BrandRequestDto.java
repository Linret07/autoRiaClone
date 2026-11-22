package com.autoriaclone.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BrandRequestDto {
    @NotBlank
    private String brandName;
    private String modelName;
}
