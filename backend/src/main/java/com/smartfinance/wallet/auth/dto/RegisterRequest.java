package com.smartfinance.wallet.auth.dto;

import com.smartfinance.wallet.common.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Le prénom est obligatoire.")
        @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères.")
        String firstName,

        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères.")
        String lastName,

        @NotBlank(message = "L'adresse e-mail est obligatoire.")
        @Email(message = "L'adresse e-mail doit être valide.")
        @Size(max = 254, message = "L'adresse e-mail ne doit pas dépasser 254 caractères.")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @ValidPassword
        String password
) {

    public RegisterRequest {
        if (email != null) {
            email = email.trim();
        }
    }
}
