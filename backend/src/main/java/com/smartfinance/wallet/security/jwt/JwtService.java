package com.smartfinance.wallet.security.jwt;

import com.smartfinance.wallet.user.entity.AppUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private static final int MINIMUM_KEY_LENGTH_BYTES = 32;

    private final SecretKey signingKey;
    private final Duration expiration;

    public JwtService(
            @Value("${app.jwt.secret-base64}") String secretBase64,
            @Value("${app.jwt.expiration-minutes:60}") long expirationMinutes
    ) {
        this.signingKey = createSigningKey(secretBase64);
        if (expirationMinutes <= 0) {
            throw invalidConfiguration();
        }
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    public String generateToken(AppUser user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expiration);

        return Jwts.builder()
                .subject(user.getId().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public long getExpirationSeconds() {
        return expiration.toSeconds();
    }

    public long getUserIdFromToken(String token) {
        String subject = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();

        if (subject == null || subject.isBlank()) {
            throw new MalformedJwtException("Subject JWT invalide.");
        }

        try {
            long userId = Long.parseLong(subject);
            if (userId <= 0) {
                throw new MalformedJwtException("Subject JWT invalide.");
            }
            return userId;
        } catch (NumberFormatException exception) {
            throw new MalformedJwtException("Subject JWT invalide.", exception);
        }
    }

    private SecretKey createSigningKey(String secretBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(secretBase64);
            if (keyBytes.length < MINIMUM_KEY_LENGTH_BYTES) {
                throw invalidConfiguration();
            }
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException exception) {
            throw invalidConfiguration();
        }
    }

    private IllegalStateException invalidConfiguration() {
        return new IllegalStateException("Configuration JWT invalide.");
    }
}
