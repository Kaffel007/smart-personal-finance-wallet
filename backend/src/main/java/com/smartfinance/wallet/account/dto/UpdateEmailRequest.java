package com.smartfinance.wallet.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateEmailRequest(
        @NotBlank(message = "L'adresse e-mail est obligatoire.")
        @Email(message = "L'adresse e-mail doit être valide.")
        @Size(max = 254, message = "L'adresse e-mail ne doit pas dépasser 254 caractères.") String newEmail,
        @NotBlank(message = "Le mot de passe actuel est obligatoire.") String currentPassword
) {
    public UpdateEmailRequest {
        if (newEmail != null) newEmail = newEmail.trim();
    }

    @Override
    public String toString() {
        return "UpdateEmailRequest[credentials=REDACTED]";
    }
}
