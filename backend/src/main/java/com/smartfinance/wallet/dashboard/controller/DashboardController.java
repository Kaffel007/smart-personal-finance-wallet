package com.smartfinance.wallet.dashboard.controller;

import com.smartfinance.wallet.dashboard.dto.DashboardSummaryResponse;
import com.smartfinance.wallet.dashboard.service.DashboardService;
import com.smartfinance.wallet.security.AuthenticatedUserPrincipal;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam @NotNull @Min(2000) @Max(2100) Integer year,
            @RequestParam @NotNull @Min(1) @Max(12) Integer month) {
        return service.getSummary(principal.id(), year, month);
    }
}
