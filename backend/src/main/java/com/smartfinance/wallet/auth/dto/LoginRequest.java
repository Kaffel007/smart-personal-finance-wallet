package com.smartfinance.wallet.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "L'adresse e-mail est obligatoire.")
        @Email(message = "L'adresse e-mail doit être valide.")
        @Size(max = 254, message = "L'adresse e-mail ne doit pas dépasser 254 caractères.")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        String password
) {

    public LoginRequest {
        if (email != null) {
            email = email.trim();
        }
    }
}
