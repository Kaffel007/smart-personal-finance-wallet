package com.smartfinance.wallet.account.controller;

import com.smartfinance.wallet.account.dto.AccountResponse;
import com.smartfinance.wallet.account.dto.ChangePasswordRequest;
import com.smartfinance.wallet.account.dto.UpdateEmailRequest;
import com.smartfinance.wallet.account.dto.UpdateProfileRequest;
import com.smartfinance.wallet.account.service.AccountService;
import com.smartfinance.wallet.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public AccountResponse getAccount(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return service.getAccount(principal.id());
    }

    @PutMapping("/profile")
    public AccountResponse updateProfile(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        return service.updateProfile(principal.id(), request);
    }

    @PutMapping("/email")
    public AccountResponse updateEmail(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody UpdateEmailRequest request) {
        return service.updateEmail(principal.id(), request);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(principal.id(), request);
        return ResponseEntity.noContent().build();
    }
}
