package com.mfish.erp.securite;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint public de connexion.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter rateLimiter;

    public AuthController(final AuthService authService, final LoginRateLimiter rateLimiter) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody final LoginRequest request) {
        if (!rateLimiter.autorise(request.username())) {
            throw new TropDeTentativesException();
        }
        return authService.login(request);
    }
}
