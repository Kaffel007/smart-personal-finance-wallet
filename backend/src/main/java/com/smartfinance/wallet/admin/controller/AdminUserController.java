package com.smartfinance.wallet.admin.controller;

import com.smartfinance.wallet.admin.dto.AdminUserResponse;
import com.smartfinance.wallet.admin.service.AdminService;
import com.smartfinance.wallet.security.AuthenticatedUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminService service;

    public AdminUserController(AdminService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminUserResponse> getAll(@RequestParam(required = false) String search) {
        return service.getUsers(search);
    }

    @GetMapping("/{id}")
    public AdminUserResponse getOne(@PathVariable Long id) {
        return service.getUser(id);
    }

    @PutMapping("/{id}/block")
    public AdminUserResponse block(@PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return service.blockUser(id, principal.id());
    }

    @PutMapping("/{id}/unblock")
    public AdminUserResponse unblock(@PathVariable Long id) {
        return service.unblockUser(id);
    }
}
