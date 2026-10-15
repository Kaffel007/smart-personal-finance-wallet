package com.smartfinance.wallet.admin.controller;

import com.smartfinance.wallet.admin.dto.AdminStatsResponse;
import com.smartfinance.wallet.admin.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {
    private final AdminService service;

    public AdminStatsController(AdminService service) {
        this.service = service;
    }

    @GetMapping
    public AdminStatsResponse getStats() {
        return service.getStats();
    }
}
