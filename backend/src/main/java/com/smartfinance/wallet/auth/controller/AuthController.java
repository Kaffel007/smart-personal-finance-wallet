package com.smartfinance.wallet.auth.controller;

import com.smartfinance.wallet.auth.dto.LoginRequest;
import com.smartfinance.wallet.auth.dto.LoginResponse;
import com.smartfinance.wallet.auth.dto.RegisterRequest;
import com.smartfinance.wallet.auth.dto.UserSummaryResponse;
import com.smartfinance.wallet.auth.service.AuthService;
import com.smartfinance.wallet.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserSummaryResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserSummaryResponse> me(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal
    ) {
        return ResponseEntity.ok(new UserSummaryResponse(
                principal.id(),
                principal.firstName(),
                principal.lastName(),
                principal.email(),
                principal.role()
        ));
    }
}
