package com.smartfinance.wallet.savingsgoal.controller;

import com.smartfinance.wallet.savingsgoal.dto.*;
import com.smartfinance.wallet.savingsgoal.service.SavingsGoalService;
import com.smartfinance.wallet.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/savings-goals")
public class SavingsGoalController {
    private final SavingsGoalService service;

    public SavingsGoalController(SavingsGoalService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SavingsGoalResponse> create(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody CreateSavingsGoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(principal.id(), request));
    }

    @GetMapping
    public List<SavingsGoalResponse> getAll(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return service.getAll(principal.id());
    }

    @GetMapping("/{id}")
    public SavingsGoalResponse getOne(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable Long id) {
        return service.getOne(principal.id(), id);
    }

    @PutMapping("/{id}")
    public SavingsGoalResponse update(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable Long id, @Valid @RequestBody UpdateSavingsGoalRequest request) {
        return service.update(principal.id(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable Long id) {
        service.delete(principal.id(), id);
        return ResponseEntity.noContent().build();
    }
}
