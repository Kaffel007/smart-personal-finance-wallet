package com.smartfinance.wallet.auth.controller;

import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.user.entity.Role;
import com.smartfinance.wallet.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTests {

    private static final String RAW_PASSWORD = "phrase-secrete-fictive";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        appUserRepository.deleteAll();
    }

    @Test
    void shouldRegisterNormalizedUserAndReturnSafeResponse() throws Exception {
        String request = """
                {
                  "firstName": "  Sara  ",
                  "lastName": "  Martin  ",
                  "email": "SARA.EXAMPLE@EXAMPLE.COM",
                  "password": "phrase-secrete-fictive"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.firstName").value("Sara"))
                .andExpect(jsonPath("$.lastName").value("Martin"))
                .andExpect(jsonPath("$.email").value("sara.example@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.enabled").doesNotExist())
                .andExpect(jsonPath("$.blocked").doesNotExist());

        AppUser savedUser = appUserRepository.findByEmail("sara.example@example.com")
                .orElseThrow();
        assertThat(savedUser.getFirstName()).isEqualTo("Sara");
        assertThat(savedUser.getLastName()).isEqualTo("Martin");
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
        assertThat(savedUser.isEnabled()).isTrue();
        assertThat(savedUser.isBlocked()).isFalse();
        assertThat(savedUser.getPasswordHash()).isNotEqualTo(RAW_PASSWORD);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, savedUser.getPasswordHash())).isTrue();
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void shouldRejectInvalidRegistration(String request, String invalidField) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/auth/register"))
                .andExpect(jsonPath("$.fieldErrors." + invalidField).exists());

        assertThat(appUserRepository.count()).isZero();
    }

    @Test
    void shouldRejectDuplicateNormalizedEmailWithoutSqlDetails() throws Exception {
        String firstRequest = validRequest("duplicate@example.com");
        String duplicateRequest = validRequest("  DUPLICATE@EXAMPLE.COM  ");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstRequest))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateRequest))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message")
                        .value("Cette adresse e-mail ne peut pas être utilisée."))
                .andExpect(jsonPath("$.path").value("/api/auth/register"))
                .andExpect(jsonPath("$.table").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());

        assertThat(appUserRepository.count()).isOne();
    }

    @Test
    void shouldIgnoreAttemptToAssignAdminRole() throws Exception {
        String request = """
                {
                  "firstName": "Sara",
                  "lastName": "Martin",
                  "email": "role-attempt@example.com",
                  "password": "phrase-secrete-fictive",
                  "role": "ADMIN"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));

        AppUser savedUser = appUserRepository.findByEmail("role-attempt@example.com")
                .orElseThrow();
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void shouldReturnBadRequestForUnreadableJson() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Le corps de la requête est invalide."))
                .andExpect(jsonPath("$.path").value("/api/auth/register"));
    }

    private static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of(requestWith("firstName", "\"   \""), "firstName"),
                Arguments.of(requestWith("lastName", "\"   \""), "lastName"),
                Arguments.of(requestWith("email", "\"invalid-email\""), "email"),
                Arguments.of(requestWith("password", "\"short\""), "password"),
                Arguments.of(requestWith("password", "\"" + "😀".repeat(19) + "\""), "password")
        );
    }

    private static String validRequest(String email) {
        return """
                {
                  "firstName": "Sara",
                  "lastName": "Martin",
                  "email": "%s",
                  "password": "phrase-secrete-fictive"
                }
                """.formatted(email);
    }

    private static String requestWith(String field, String value) {
        String firstName = field.equals("firstName") ? value : "\"Sara\"";
        String lastName = field.equals("lastName") ? value : "\"Martin\"";
        String email = field.equals("email") ? value : "\"validation@example.com\"";
        String password = field.equals("password") ? value : "\"phrase-secrete-fictive\"";

        return """
                {
                  "firstName": %s,
                  "lastName": %s,
                  "email": %s,
                  "password": %s
                }
                """.formatted(firstName, lastName, email, password);
    }
}
