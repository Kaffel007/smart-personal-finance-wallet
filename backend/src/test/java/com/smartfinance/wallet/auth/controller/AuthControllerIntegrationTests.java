package com.smartfinance.wallet.auth.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.security.jwt.JwtService;
import com.smartfinance.wallet.user.entity.Role;
import com.smartfinance.wallet.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Base64;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.HttpHeaders;

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

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Value("${app.jwt.secret-base64}")
    private String jwtSecretBase64;

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
    @ParameterizedTest
    @MethodSource("validLoginRequests")
    void shouldLoginUserOrAdminWithRealBcryptAndReturnOnlyPublicData(
            Role role,
            String requestEmail
    ) throws Exception {
        AppUser user = saveLoginUser("login@example.com", role, true, false);
        Long id = user.getId();
        String passwordHash = user.getPasswordHash();
        Instant updatedAt = user.getUpdatedAt();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest(requestEmail, RAW_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.user.id").value(id))
                .andExpect(jsonPath("$.user.firstName").value("Sara"))
                .andExpect(jsonPath("$.user.lastName").value("Martin"))
                .andExpect(jsonPath("$.user.email").value("login@example.com"))
                .andExpect(jsonPath("$.user.role").value(role.name()))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.user.enabled").doesNotExist())
                .andExpect(jsonPath("$.user.blocked").doesNotExist())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        String accessToken = response.get("accessToken").asText();
        Jws<Claims> parsedToken = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecretBase64)))
                .build()
                .parseSignedClaims(accessToken);
        Claims claims = parsedToken.getPayload();
        assertThat(parsedToken.getHeader().getAlgorithm()).isEqualTo("HS256");
        assertThat(claims.getSubject()).isEqualTo(id.toString());
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.keySet()).doesNotContainAnyElementsOf(Set.of(
                "userId", "email", "role", "password", "passwordHash", "enabled", "blocked"
        ));

        AppUser unchangedUser = appUserRepository.findById(id).orElseThrow();
        assertThat(passwordEncoder.matches(RAW_PASSWORD, passwordHash)).isTrue();
        assertThat(unchangedUser.getPasswordHash()).isEqualTo(passwordHash);
        assertThat(unchangedUser.getUpdatedAt())
                .isCloseTo(updatedAt, within(1, java.time.temporal.ChronoUnit.MICROS));
        assertThat(unchangedUser.getRole()).isEqualTo(role);
        assertThat(unchangedUser.isEnabled()).isTrue();
        assertThat(unchangedUser.isBlocked()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("rejectedLoginScenarios")
    void shouldReturnSameGenericUnauthorizedErrorForEveryAuthenticationFailure(
            String scenario,
            String email,
            String password
    ) throws Exception {
        if (!scenario.equals("missing")) {
            AppUser user = saveLoginUser(
                    "login@example.com",
                    Role.USER,
                    !scenario.equals("disabled"),
                    scenario.equals("blocked")
            );
            assertThat(user.getId()).isNotNull();
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest(email, password)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message")
                        .value("Identifiants incorrects ou compte indisponible."))
                .andExpect(jsonPath("$.path").value("/api/auth/login"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.table").doesNotExist());
    }

    @ParameterizedTest
    @MethodSource("invalidLoginRequests")
    void shouldRejectInvalidLoginRequest(String request, String invalidField) throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/auth/login"))
                .andExpect(jsonPath("$.fieldErrors." + invalidField).exists());
    }

    @Test
    void shouldReturnBadRequestForUnreadableLoginJson() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Le corps de la requête est invalide."))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    @Test
    void shouldReturnJsonUnauthorizedForMeWithoutTokenAndCreateNoSession() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message")
                        .value("Authentification requise ou jeton invalide."))
                .andExpect(jsonPath("$.path").value("/api/auth/me"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @ParameterizedTest
    @MethodSource("authenticatedRoles")
    void shouldReturnAuthenticatedUserForValidBearerToken(Role role) throws Exception {
        AppUser user = saveLoginUser("me-" + role.name().toLowerCase() + "@example.com",
                role, true, false);
        String token = jwtService.generateToken(user);

        MvcResult result = mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.firstName").value("Sara"))
                .andExpect(jsonPath("$.lastName").value("Martin"))
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.role").value(role.name()))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.enabled").doesNotExist())
                .andExpect(jsonPath("$.blocked").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andReturn();

        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    void shouldUseIdentityFromTokenEvenWhenClientSuppliesAnotherId() throws Exception {
        AppUser userA = saveLoginUser("user-a@example.com", Role.USER, true, false);
        AppUser userB = saveLoginUser("user-b@example.com", Role.USER, true, false);
        String tokenA = jwtService.generateToken(userA);

        mockMvc.perform(get("/api/auth/me")
                        .queryParam("id", userB.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userA.getId()))
                .andExpect(jsonPath("$.email").value("user-a@example.com"));
    }

    @Test
    void shouldRejectMalformedWrongSignatureExpiredAndInvalidSubjectTokens() throws Exception {
        byte[] otherKey = "integration-wrong-signing-key-at-least-32-bytes"
                .getBytes(StandardCharsets.UTF_8);
        String wrongSignature = signedToken("1", otherKey, Instant.now().plusSeconds(60));
        String expired = signedToken("1", testSigningKey(), Instant.now().minusSeconds(1));
        String invalidSubject = signedToken("invalid", testSigningKey(), Instant.now().plusSeconds(60));

        for (String token : Set.of("malformed-token", wrongSignature, expired, invalidSubject)) {
            assertUnauthorizedBearer(token);
        }
        assertUnauthorizedBearer("");
    }

    @ParameterizedTest
    @MethodSource("unavailableAccountStates")
    void shouldRejectOldTokenWhenCurrentUserBecomesUnavailable(String state) throws Exception {
        AppUser user = saveLoginUser("state@example.com", Role.USER, true, false);
        String token = jwtService.generateToken(user);

        if (state.equals("blocked")) {
            user.setBlocked(true);
            appUserRepository.saveAndFlush(user);
        } else if (state.equals("disabled")) {
            user.setEnabled(false);
            appUserRepository.saveAndFlush(user);
        } else {
            appUserRepository.deleteById(user.getId());
            appUserRepository.flush();
        }

        assertUnauthorizedBearer(token);
    }

    @Test
    void shouldRemainStatelessBetweenAuthenticatedAndUnauthenticatedRequests() throws Exception {
        AppUser user = saveLoginUser("stateless@example.com", Role.USER, true, false);
        String token = jwtService.generateToken(user);

        MvcResult authenticatedResult = mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(authenticatedResult.getRequest().getSession(false)).isNull();

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReloadChangedRoleFromDatabaseWithSameToken() throws Exception {
        AppUser user = saveLoginUser("dynamic-role@example.com", Role.ADMIN, true, false);
        String token = jwtService.generateToken(user);

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        user.setRole(Role.USER);
        appUserRepository.saveAndFlush(user);

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void shouldNeverExposeUnknownApiRouteWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/future-resource"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    private AppUser saveLoginUser(
            String email,
            Role role,
            boolean enabled,
            boolean blocked
    ) {
        AppUser user = new AppUser(
                "Sara",
                "Martin",
                email,
                passwordEncoder.encode(RAW_PASSWORD),
                role
        );
        user.setEnabled(enabled);
        user.setBlocked(blocked);
        return appUserRepository.saveAndFlush(user);
    }

    private static Stream<Arguments> validLoginRequests() {
        return Stream.of(
                Arguments.of(Role.USER, "LOGIN@EXAMPLE.COM"),
                Arguments.of(Role.ADMIN, "  LOGIN@EXAMPLE.COM  ")
        );
    }

    private static Stream<Role> authenticatedRoles() {
        return Stream.of(Role.USER, Role.ADMIN);
    }

    private static Stream<String> unavailableAccountStates() {
        return Stream.of("blocked", "disabled", "deleted");
    }

    private static Stream<Arguments> rejectedLoginScenarios() {
        return Stream.of(
                Arguments.of("missing", "missing@example.com", RAW_PASSWORD),
                Arguments.of("wrong-password", "login@example.com", "mot-de-passe-incorrect"),
                Arguments.of("blocked", "login@example.com", RAW_PASSWORD),
                Arguments.of("disabled", "login@example.com", RAW_PASSWORD)
        );
    }

    private static Stream<Arguments> invalidLoginRequests() {
        return Stream.of(
                Arguments.of(loginRequest("invalid-email", RAW_PASSWORD), "email"),
                Arguments.of(loginRequest("   ", RAW_PASSWORD), "email"),
                Arguments.of(loginRequest("login@example.com", "   "), "password")
        );
    }

    private static String loginRequest(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }

    private byte[] testSigningKey() {
        return Base64.getDecoder().decode(jwtSecretBase64);
    }

    private String signedToken(String subject, byte[] keyBytes, Instant expiration) {
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date())
                .expiration(Date.from(expiration))
                .signWith(Keys.hmacShaKeyFor(keyBytes), Jwts.SIG.HS256)
                .compact();
    }

    private void assertUnauthorizedBearer(String token) throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message")
                        .value("Authentification requise ou jeton invalide."))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }
}
