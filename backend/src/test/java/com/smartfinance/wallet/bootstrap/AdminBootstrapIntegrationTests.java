package com.smartfinance.wallet.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.user.entity.Role;
import com.smartfinance.wallet.user.repository.AppUserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "app.admin-bootstrap.enabled=true",
        "app.admin-bootstrap.email=bootstrap.admin@example.com",
        "app.admin-bootstrap.password=fictional-bootstrap-passphrase",
        "app.admin-bootstrap.first-name=Bootstrap",
        "app.admin-bootstrap.last-name=Administrator"
})
@AutoConfigureMockMvc
class AdminBootstrapIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.jwt.secret-base64}")
    private String jwtSecretBase64;

    @Test
    void shouldLoginBootstrappedAdminAndAccessMeWithoutSensitiveData() throws Exception {
        AppUser admin = appUserRepository.findByEmail("bootstrap.admin@example.com")
                .orElseThrow();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.isEnabled()).isTrue();
        assertThat(admin.isBlocked()).isFalse();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "bootstrap.admin@example.com",
                                  "password": "fictional-bootstrap-passphrase"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        JsonNode loginResponse = objectMapper.readTree(
                loginResult.getResponse().getContentAsString()
        );
        String token = loginResponse.get("accessToken").asText();
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecretBase64)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        assertThat(claims.getSubject()).isEqualTo(admin.getId().toString());
        assertThat(claims.keySet()).doesNotContain("role", "email", "password", "passwordHash");

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(admin.getId()))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.enabled").doesNotExist())
                .andExpect(jsonPath("$.blocked").doesNotExist());
    }
}
