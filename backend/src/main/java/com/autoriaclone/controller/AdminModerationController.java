package com.autoriaclone.controller;

import com.autoriaclone.dto.RegisterRequest;
import com.autoriaclone.entity.Advertisement;
import com.autoriaclone.entity.Role;
import com.autoriaclone.entity.User;
import com.autoriaclone.entity.enums.AccountType;
import com.autoriaclone.entity.enums.RoleName;
import com.autoriaclone.repository.RoleRepository;
import com.autoriaclone.repository.UserRepository;
import com.autoriaclone.service.AdvertisementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminModerationController {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdvertisementService adService;

    @PostMapping("/managers")
    @PreAuthorize("hasAuthority('MANAGER_CREATE')")
    public Map<String, Object> createManager(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email вже існує");
        }
        Role managerRole = roleRepository.findByName(RoleName.MANAGER).orElseThrow();
        User manager = userRepository.save(User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .role(managerRole)
                .accountType(AccountType.PREMIUM)
                .build());
        return Map.of("id", manager.getId(), "email", manager.getEmail(), "role", "MANAGER");
    }

    @PostMapping("/users/{userId}/ban")
    @PreAuthorize("hasAuthority('USER_BAN')")
    public Map<String, Object> banUser(@PathVariable Long userId, @RequestParam(defaultValue = "true") boolean banned) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Користувача не знайдено"));
        user.setBanned(banned);
        userRepository.save(user);
        return Map.of("userId", user.getId(), "email", user.getEmail(), "banned", user.isBanned());
    }

    @GetMapping("/moderation/pending")
    @PreAuthorize("hasAuthority('AD_MODERATE')")
    public List<Advertisement> getPendingAds() {
        return adService.getPendingReview();
    }

    @PostMapping("/moderation/{adId}/approve")
    @PreAuthorize("hasAuthority('AD_MODERATE')")
    public Advertisement approveAd(@PathVariable Long adId) {
        return adService.approveByManager(adId);
    }
}
