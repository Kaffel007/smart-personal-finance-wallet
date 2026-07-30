package com.smartfinance.wallet.auth.dto;

import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.user.entity.Role;

public record UserSummaryResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role
) {

    public static UserSummaryResponse from(AppUser user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole()
        );
    }
}
