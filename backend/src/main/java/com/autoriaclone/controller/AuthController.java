package com.autoriaclone.controller;

import com.autoriaclone.dto.*;
import com.autoriaclone.entity.Role;
import com.autoriaclone.entity.User;
import com.autoriaclone.entity.enums.AccountType;
import com.autoriaclone.entity.enums.RoleName;
import com.autoriaclone.repository.RoleRepository;
import com.autoriaclone.repository.UserRepository;
import com.autoriaclone.security.CustomUserDetails;
import com.autoriaclone.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (user.isBanned()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ваш акаунт заблоковано менеджером");
        }
        String token = jwtService.generateToken(new CustomUserDetails(user));
        return new AuthResponse(token, user.getEmail(), user.getRole().getName().name(), user.getAccountType().name());
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest req,
                                 @RequestParam(defaultValue = "SELLER") RoleName roleName) {
        if (roleName == RoleName.ADMIN || roleName == RoleName.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Самостійна реєстрація доступна лише для BUYER або SELLER");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email вже використовується");
        }
        Role role = roleRepository.findByName(roleName).orElseThrow();
        User user = userRepository.save(User.builder()
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .role(role)
                .accountType(AccountType.BASIC)
                .build());
        String token = jwtService.generateToken(new CustomUserDetails(user));
        return new AuthResponse(token, user.getEmail(), user.getRole().getName().name(), user.getAccountType().name());
    }

    @PostMapping("/upgrade-premium")
    public Map<String, Object> upgradeToPremium(@AuthenticationPrincipal CustomUserDetails principal) {
        User user = principal.getUser();
        user.setAccountType(AccountType.PREMIUM);
        userRepository.save(user);
        return Map.of("message", "Акаунт успішно оновлено до PREMIUM", "accountType", user.getAccountType());
    }
}
