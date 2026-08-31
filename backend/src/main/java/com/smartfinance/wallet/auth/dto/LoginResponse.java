package com.smartfinance.wallet.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserSummaryResponse user
) {
}
