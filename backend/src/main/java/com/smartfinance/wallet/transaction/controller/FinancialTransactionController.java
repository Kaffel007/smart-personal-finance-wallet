package com.smartfinance.wallet.transaction.controller;

import com.smartfinance.wallet.category.entity.CategoryType;
import com.smartfinance.wallet.security.AuthenticatedUserPrincipal;
import com.smartfinance.wallet.transaction.dto.*;
import com.smartfinance.wallet.transaction.service.FinancialTransactionService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class FinancialTransactionController {
    private final FinancialTransactionService service;
    public FinancialTransactionController(FinancialTransactionService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<FinancialTransactionResponse> create(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody CreateFinancialTransactionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(principal.id(), request));
    }
    @GetMapping
    public List<FinancialTransactionResponse> getAll(@AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam(required = false) CategoryType type, @RequestParam(required = false) Long categoryId) {
        return service.getAll(principal.id(), type, categoryId);
    }
    @GetMapping("/{id}")
    public FinancialTransactionResponse getOne(@AuthenticationPrincipal AuthenticatedUserPrincipal principal, @PathVariable Long id) {
        return service.getOne(principal.id(), id);
    }
    @PutMapping("/{id}")
    public FinancialTransactionResponse update(@AuthenticationPrincipal AuthenticatedUserPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody UpdateFinancialTransactionRequest request) { return service.update(principal.id(), id, request); }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUserPrincipal principal, @PathVariable Long id) {
        service.delete(principal.id(), id); return ResponseEntity.noContent().build();
    }
}
