package com.smartfinance.wallet.account.dto;

import com.smartfinance.wallet.common.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "Le mot de passe actuel est obligatoire.") String currentPassword,
        @NotBlank(message = "Le nouveau mot de passe est obligatoire.") @ValidPassword String newPassword
) {
    @Override
    public String toString() {
        return "ChangePasswordRequest[credentials=REDACTED]";
    }
}
