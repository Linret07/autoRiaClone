package com.autoriaclone.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "advertisement_views")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdvertisementView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advertisement_id", nullable = false)
    private Advertisement advertisement;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime viewedAt = LocalDateTime.now();
}
