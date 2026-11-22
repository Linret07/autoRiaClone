package com.autoriaclone.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "car_models", uniqueConstraints = @UniqueConstraint(columnNames = {"brand_id", "name"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CarModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "brand_id", nullable = false)
    private CarBrand brand;
}
