package com.smartfinance.wallet.security.jwt;

import com.smartfinance.wallet.user.entity.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTests {

    private static final byte[] TEST_KEY_BYTES =
            "unit-test-signing-key-at-least-32-bytes".getBytes(StandardCharsets.UTF_8);
    private static final String TEST_KEY_BASE64 =
            Base64.getEncoder().encodeToString(TEST_KEY_BYTES);

    @Test
    void shouldGenerateMinimalHs256TokenWithExpectedLifetime() {
        JwtService jwtService = new JwtService(TEST_KEY_BASE64, 60);
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(42L);

        String token = jwtService.generateToken(user);
        Jws<Claims> parsedToken = parse(token, TEST_KEY_BYTES);
        Claims claims = parsedToken.getPayload();

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
        assertThat(parsedToken.getHeader().getAlgorithm()).isEqualTo("HS256");
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(Duration.between(
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant()
        )).isEqualTo(Duration.ofMinutes(60));
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(3600);
        assertThat(claims.keySet()).doesNotContainAnyElementsOf(Set.of(
                "userId", "email", "role", "password", "passwordHash", "enabled", "blocked"
        ));
    }

    @Test
    void shouldAcceptCorrectKeyAndRejectDifferentValidKey() {
        JwtService jwtService = new JwtService(TEST_KEY_BASE64, 60);
        AppUser user = mock(AppUser.class);
        when(user.getId()).thenReturn(7L);
        String token = jwtService.generateToken(user);
        byte[] differentKey =
                "different-test-signing-key-32-bytes-minimum".getBytes(StandardCharsets.UTF_8);

        assertThat(parse(token, TEST_KEY_BYTES).getPayload().getSubject()).isEqualTo("7");
        assertThatThrownBy(() -> parse(token, differentKey))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void shouldRejectInvalidBase64WithoutExposingIt() {
        String invalidValue = "not-valid-base64-value";

        assertThatThrownBy(() -> new JwtService(invalidValue, 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Configuration JWT invalide.")
                .hasMessageNotContaining(invalidValue);
    }

    @Test
    void shouldRejectMissingSecretWithGenericConfigurationError() {
        assertThatThrownBy(() -> new JwtService("", 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Configuration JWT invalide.");
    }

    @Test
    void shouldRejectDecodedKeyShorterThan32BytesWithoutExposingIt() {
        String shortValue = Base64.getEncoder().encodeToString(
                "short-test-key".getBytes(StandardCharsets.UTF_8)
        );

        assertThatThrownBy(() -> new JwtService(shortValue, 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Configuration JWT invalide.")
                .hasMessageNotContaining(shortValue);
    }

    @Test
    void shouldRejectNonPositiveExpiration() {
        assertThatThrownBy(() -> new JwtService(TEST_KEY_BASE64, 0))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Configuration JWT invalide.")
                .hasMessageNotContaining(TEST_KEY_BASE64);
    }

    private Jws<Claims> parse(String token, byte[] keyBytes) {
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }
}
