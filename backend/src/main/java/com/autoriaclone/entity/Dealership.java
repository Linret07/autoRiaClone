package com.autoriaclone.entity;

import com.autoriaclone.entity.enums.Region;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "dealerships")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Dealership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    private Region region;

    @Builder.Default
    private boolean active = true;
}
