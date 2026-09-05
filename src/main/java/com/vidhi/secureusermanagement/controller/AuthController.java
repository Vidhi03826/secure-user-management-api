package com.vidhi.secureusermanagement.controller;

import com.vidhi.secureusermanagement.dto.LoginRequest;
import com.vidhi.secureusermanagement.dto.RefreshTokenRequest;
import com.vidhi.secureusermanagement.dto.RegisterRequest;
import com.vidhi.secureusermanagement.dto.TokenResponse;
import com.vidhi.secureusermanagement.dto.UserResponse;
import com.vidhi.secureusermanagement.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // =========================
    // REGISTER
    // =========================

    @PostMapping("/register")
    public UserResponse register(
            @Valid @RequestBody RegisterRequest request
    ) {

        return authService.register(request);
    }

    // =========================
    // LOGIN
    // =========================

    @PostMapping("/login")
    public TokenResponse login(
            @Valid @RequestBody LoginRequest request
    ) {

        return authService.login(request);
    }

    // =========================
    // REFRESH ACCESS TOKEN
    // =========================

    @PostMapping("/refresh")
    public TokenResponse refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {

        return authService.refreshAccessToken(
                request.getRefreshToken()
        );
    }

    // =========================
    // LOGOUT
    // =========================

    @PostMapping("/logout")
    public String logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {

        authService.logout(
                request.getRefreshToken()
        );

        return "Logged out successfully";
    }
}