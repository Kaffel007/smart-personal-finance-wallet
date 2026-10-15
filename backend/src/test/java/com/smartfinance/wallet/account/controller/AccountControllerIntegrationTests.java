package com.smartfinance.wallet.account.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinance.wallet.account.dto.AccountResponse;
import com.smartfinance.wallet.account.dto.ChangePasswordRequest;
import com.smartfinance.wallet.account.dto.UpdateEmailRequest;
import com.smartfinance.wallet.account.dto.UpdateProfileRequest;
import com.smartfinance.wallet.security.jwt.JwtService;
import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.user.entity.Role;
import com.smartfinance.wallet.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AccountControllerIntegrationTests {
    private static final String BASE = "/api/account";
    private static final String FICTIONAL_PASSWORD = "fictional-account-test-passphrase";
    private static final String FICTIONAL_NEW_PASSWORD = "fictional-updated-test-passphrase";
    private static final String OTHER_PASSWORD = "fictional-other-test-passphrase";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppUserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired RequestMappingHandlerMapping requestMappingHandlerMapping;

    @BeforeEach
    void cleanUsers() {
        // Each test is rolled back; earlier integration suites clean their financial fixtures.
        userRepository.deleteAll();
    }

    @ParameterizedTest
    @ValueSource(strings = {"me", "profile", "email", "password"})
    void shouldRequireAuthenticationForEveryAccountEndpoint(String endpoint) throws Exception {
        mockMvc.perform(endpoint.equals("me") ? get(BASE + "/me") : put(BASE + "/" + endpoint)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void shouldReturnOnlyOwnSafeAccountForUserAndAdmin(Role role) throws Exception {
        AppUser actor = user("actor@example.test", role, FICTIONAL_PASSWORD);
        AppUser other = user("other@example.test", Role.USER, OTHER_PASSWORD);
        JsonNode response = json(mockMvc.perform(auth(get(BASE + "/me")
                        .queryParam("userId", other.getId().toString()), actor))
                .andExpect(status().isOk()).andReturn());
        assertSafeAccount(response);
        assertThat(response.get("id").asLong()).isEqualTo(actor.getId());
        assertThat(response.get("role").asText()).isEqualTo(role.name());
        assertThat(response.get("email").asText()).isEqualTo(actor.getEmail());
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void shouldUpdateOnlyOwnNamesAndPreserveSecurityFields(Role role) throws Exception {
        AppUser actor = user("actor@example.test", role, FICTIONAL_PASSWORD);
        String originalHash = actor.getPasswordHash();
        JsonNode response = json(mockMvc.perform(auth(put(BASE + "/profile"), actor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("firstName", "  Sara  ", "lastName", "  Martin  "))))
                .andExpect(status().isOk()).andReturn());
        assertSafeAccount(response);
        assertThat(response.get("firstName").asText()).isEqualTo("Sara");
        assertThat(response.get("lastName").asText()).isEqualTo("Martin");
        userRepository.flush();
        AppUser saved = userRepository.findById(actor.getId()).orElseThrow();
        assertThat(saved.getFirstName()).isEqualTo("Sara");
        assertThat(saved.getLastName()).isEqualTo("Martin");
        assertThat(saved.getEmail()).isEqualTo("actor@example.test");
        assertThat(saved.getPasswordHash()).isEqualTo(originalHash);
        assertSecurityFields(saved, role);
        mockMvc.perform(auth(get("/api/auth/me"), actor)).andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Sara"));
    }

    static Stream<Arguments> invalidProfileRequests() {
        return Stream.of(
                Arguments.of(Map.of("firstName", "", "lastName", "Valid"), "firstName"),
                Arguments.of(Map.of("firstName", "   ", "lastName", "Valid"), "firstName"),
                Arguments.of(Map.of("firstName", "a".repeat(101), "lastName", "Valid"), "firstName"),
                Arguments.of(Map.of("lastName", "Valid"), "firstName"),
                Arguments.of(Map.of("firstName", "Valid", "lastName", ""), "lastName"),
                Arguments.of(Map.of("firstName", "Valid", "lastName", "  "), "lastName"),
                Arguments.of(Map.of("firstName", "Valid", "lastName", "a".repeat(101)), "lastName"),
                Arguments.of(Map.of("firstName", "Valid"), "lastName"));
    }

    @ParameterizedTest
    @MethodSource("invalidProfileRequests")
    void shouldApplyExistingNameValidation(Map<String, String> payload, String field) throws Exception {
        AppUser actor = user("actor@example.test", Role.USER, FICTIONAL_PASSWORD);
        mockMvc.perform(auth(put(BASE + "/profile"), actor).contentType(MediaType.APPLICATION_JSON).content(body(payload)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors." + field).exists());
        assertThat(userRepository.findById(actor.getId()).orElseThrow().getFirstName()).isEqualTo("Test");
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void shouldNormalizeEmailPreserveJwtAndAllowLoginOnlyWithNewEmail(Role role) throws Exception {
        AppUser actor = user("actor@example.test", role, FICTIONAL_PASSWORD);
        String token = jwtService.generateToken(actor);
        JsonNode response = json(mockMvc.perform(put(BASE + "/email").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("newEmail", "  NEW.EMAIL@EXAMPLE.TEST  ", "currentPassword", FICTIONAL_PASSWORD))))
                .andExpect(status().isOk()).andReturn());
        assertSafeAccount(response);
        assertThat(response.get("email").asText()).isEqualTo("new.email@example.test");
        assertSecurityFields(userRepository.findById(actor.getId()).orElseThrow(), role);
        mockMvc.perform(get(BASE + "/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("new.email@example.test"));
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("new.email@example.test"));
        mockMvc.perform(login("actor@example.test", FICTIONAL_PASSWORD)).andExpect(status().isUnauthorized());
        mockMvc.perform(login("new.email@example.test", FICTIONAL_PASSWORD)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void shouldRejectEmailOwnedByAnotherAccount(Role role) throws Exception {
        AppUser actor = user("actor@example.test", role, FICTIONAL_PASSWORD);
        user("taken@example.test", Role.USER, OTHER_PASSWORD);
        mockMvc.perform(auth(put(BASE + "/email"), actor).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("newEmail", " TAKEN@EXAMPLE.TEST ", "currentPassword", FICTIONAL_PASSWORD))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cette adresse e-mail ne peut pas être utilisée."));
        assertThat(userRepository.findById(actor.getId()).orElseThrow().getEmail()).isEqualTo("actor@example.test");
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void shouldAllowOwnUnchangedNormalizedEmail(Role role) throws Exception {
        AppUser actor = user("actor@example.test", role, FICTIONAL_PASSWORD);
        mockMvc.perform(auth(put(BASE + "/email"), actor).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("newEmail", " ACTOR@EXAMPLE.TEST ", "currentPassword", FICTIONAL_PASSWORD))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value("actor@example.test"));
    }

    static Stream<String> invalidEmails() {
        return Stream.of("", "  ", "invalid", "a".repeat(243) + "@example.test");
    }

    @ParameterizedTest
    @MethodSource("invalidEmails")
    void shouldRejectInvalidEmail(String newEmail) throws Exception {
        AppUser actor = user("actor@example.test", Role.USER, FICTIONAL_PASSWORD);
        mockMvc.perform(auth(put(BASE + "/email"), actor).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("newEmail", newEmail, "currentPassword", FICTIONAL_PASSWORD))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.newEmail").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "password"})
    void shouldRequireCurrentPassword(String endpoint) throws Exception {
        AppUser actor = user("actor@example.test", Role.USER, FICTIONAL_PASSWORD);
        Map<String, String> payload = endpoint.equals("email") ? Map.of("newEmail", "new@example.test")
                : Map.of("newPassword", FICTIONAL_NEW_PASSWORD);
        mockMvc.perform(auth(put(BASE + "/" + endpoint), actor).contentType(MediaType.APPLICATION_JSON).content(body(payload)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.currentPassword").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "password"})
    void shouldRejectWrongCurrentPasswordWithoutChangingAccountOrExposingCredentials(String endpoint) throws Exception {
        AppUser actor = user("actor@example.test", Role.USER, FICTIONAL_PASSWORD);
        String originalHash = actor.getPasswordHash();
        Map<String, String> payload = endpoint.equals("email")
                ? Map.of("newEmail", "new@example.test", "currentPassword", OTHER_PASSWORD)
                : Map.of("newPassword", FICTIONAL_NEW_PASSWORD, "currentPassword", OTHER_PASSWORD);
        MvcResult result = mockMvc.perform(auth(put(BASE + "/" + endpoint), actor).contentType(MediaType.APPLICATION_JSON).content(body(payload)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.currentPassword").exists())
                .andExpect(jsonPath("$.message").value("Le mot de passe actuel est incorrect.")).andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain(FICTIONAL_PASSWORD, OTHER_PASSWORD,
                FICTIONAL_NEW_PASSWORD, originalHash);
        AppUser unchanged = userRepository.findById(actor.getId()).orElseThrow();
        assertThat(unchanged.getEmail()).isEqualTo("actor@example.test");
        assertThat(unchanged.getPasswordHash()).isEqualTo(originalHash);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void shouldChangePasswordWithBcryptRejectOldLoginAndKeepExistingJwtValid(Role role) throws Exception {
        AppUser actor = user("actor@example.test", role, FICTIONAL_PASSWORD);
        String token = jwtService.generateToken(actor);
        String oldHash = actor.getPasswordHash();
        mockMvc.perform(put(BASE + "/password").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("currentPassword", FICTIONAL_PASSWORD, "newPassword", FICTIONAL_NEW_PASSWORD))))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        AppUser updated = userRepository.findById(actor.getId()).orElseThrow();
        assertThat(updated.getPasswordHash()).startsWith("$2").isNotEqualTo(oldHash).isNotEqualTo(FICTIONAL_NEW_PASSWORD);
        assertThat(passwordEncoder.matches(FICTIONAL_NEW_PASSWORD, updated.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches(FICTIONAL_PASSWORD, updated.getPasswordHash())).isFalse();
        assertSecurityFields(updated, role);
        mockMvc.perform(login(actor.getEmail(), FICTIONAL_PASSWORD)).andExpect(status().isUnauthorized());
        mockMvc.perform(login(actor.getEmail(), FICTIONAL_NEW_PASSWORD)).andExpect(status().isOk());
        JsonNode account = json(mockMvc.perform(get(BASE + "/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andReturn());
        assertSafeAccount(account);
    }

    static Stream<Arguments> invalidNewPasswords() {
        return Stream.of(Arguments.of(""), Arguments.of("           "), Arguments.of("a".repeat(11)),
                Arguments.of("😀".repeat(6)), Arguments.of("é".repeat(37)), Arguments.of("a".repeat(73)), Arguments.of((String) null));
    }

    @ParameterizedTest
    @MethodSource("invalidNewPasswords")
    void shouldApplyExistingUnicodeAndBcryptPasswordPolicy(String newPassword) throws Exception {
        AppUser actor = user("actor@example.test", Role.USER, FICTIONAL_PASSWORD);
        String originalHash = actor.getPasswordHash();
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("currentPassword", FICTIONAL_PASSWORD); payload.put("newPassword", newPassword);
        MvcResult result = mockMvc.perform(auth(put(BASE + "/password"), actor).contentType(MediaType.APPLICATION_JSON).content(body(payload)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.newPassword").exists()).andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain(FICTIONAL_PASSWORD, originalHash);
        assertThat(userRepository.findById(actor.getId()).orElseThrow().getPasswordHash()).isEqualTo(originalHash);
    }

    static Stream<String> validNewPasswords() {
        return Stream.of("a".repeat(12), "a".repeat(72), "😀".repeat(12), "é".repeat(36));
    }

    @ParameterizedTest
    @MethodSource("validNewPasswords")
    void shouldAcceptExistingPasswordPolicyBoundaries(String newPassword) throws Exception {
        AppUser actor = user("actor@example.test", Role.USER, FICTIONAL_PASSWORD);
        mockMvc.perform(auth(put(BASE + "/password"), actor).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("currentPassword", FICTIONAL_PASSWORD, "newPassword", newPassword))))
                .andExpect(status().isNoContent());
        assertThat(passwordEncoder.matches(newPassword, userRepository.findById(actor.getId()).orElseThrow().getPasswordHash())).isTrue();
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void shouldRejectPasswordReuse(Role role) throws Exception {
        AppUser actor = user("actor@example.test", role, FICTIONAL_PASSWORD);
        mockMvc.perform(auth(put(BASE + "/password"), actor).contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("currentPassword", FICTIONAL_PASSWORD, "newPassword", FICTIONAL_PASSWORD))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.newPassword").exists());
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void shouldIgnoreInjectedTargetAndSecurityFieldsAcrossAllMutationsAndLeaveOtherUserUntouched(Role role) throws Exception {
        AppUser actor = user("actor@example.test", role, FICTIONAL_PASSWORD);
        AppUser target = user("target@example.test", Role.USER, OTHER_PASSWORD);
        String targetHash = target.getPasswordHash();
        Map<String, Object> injected = Map.of("userId", target.getId(), "id", target.getId(),
                "role", role == Role.USER ? "ADMIN" : "USER", "enabled", false, "blocked", true);
        Map<String, Object> profile = new LinkedHashMap<>(injected);
        profile.put("firstName", "Own"); profile.put("lastName", "Profile");
        Map<String, Object> email = new LinkedHashMap<>(injected);
        email.put("newEmail", "own@example.test"); email.put("currentPassword", FICTIONAL_PASSWORD);
        Map<String, Object> password = new LinkedHashMap<>(injected);
        password.put("newPassword", FICTIONAL_NEW_PASSWORD); password.put("currentPassword", FICTIONAL_PASSWORD);
        Map<String, Map<String, Object>> operations = new LinkedHashMap<>();
        operations.put("profile", profile); operations.put("email", email); operations.put("password", password);
        for (var operation : operations.entrySet()) {
            int expected = operation.getKey().equals("password") ? 204 : 200;
            mockMvc.perform(auth(put(BASE + "/" + operation.getKey()).queryParam("userId", target.getId().toString()), actor)
                            .contentType(MediaType.APPLICATION_JSON).content(body(operation.getValue())))
                    .andExpect(status().is(expected));
            assertSecurityFields(userRepository.findById(actor.getId()).orElseThrow(), role);
            AppUser unchanged = userRepository.findById(target.getId()).orElseThrow();
            assertThat(unchanged.getFirstName()).isEqualTo("Test"); assertThat(unchanged.getLastName()).isEqualTo("Account");
            assertThat(unchanged.getEmail()).isEqualTo("target@example.test"); assertThat(unchanged.getPasswordHash()).isEqualTo(targetHash);
            assertSecurityFields(unchanged, Role.USER);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"me", "profile", "email", "password"})
    void shouldRejectExistingJwtOfBlockedOrDisabledAccount(String endpoint) throws Exception {
        AppUser actor = user("actor@example.test", Role.USER, FICTIONAL_PASSWORD);
        String token = jwtService.generateToken(actor);
        for (boolean blocked : List.of(true, false)) {
            actor.setBlocked(blocked); actor.setEnabled(blocked); userRepository.saveAndFlush(actor);
            var request = endpoint.equals("me") ? get(BASE + "/me") : put(BASE + "/" + endpoint)
                    .contentType(MediaType.APPLICATION_JSON).content("{}");
            mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void shouldExposeOnlyFourSelfServicePathsAndNoAdminProfileEditMappingOrTargetDto() {
        List<String> accountPaths = requestMappingHandlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getValue().getBeanType() == AccountController.class)
                .flatMap(entry -> entry.getKey().getPatternValues().stream()).toList();
        assertThat(accountPaths).containsExactlyInAnyOrder(BASE + "/me", BASE + "/profile", BASE + "/email", BASE + "/password");
        List<String> adminPaths = requestMappingHandlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream()).filter(path -> path.startsWith("/api/admin/")).toList();
        assertThat(adminPaths).containsExactlyInAnyOrder("/api/admin/users", "/api/admin/users/{id}",
                "/api/admin/users/{id}/block", "/api/admin/users/{id}/unblock", "/api/admin/stats");
        for (Class<?> type : List.of(UpdateProfileRequest.class, UpdateEmailRequest.class, ChangePasswordRequest.class)) {
            assertThat(Arrays.stream(type.getRecordComponents()).map(component -> component.getName()))
                    .doesNotContain("userId", "id", "role", "enabled", "blocked", "passwordHash");
        }
    }

    @Test
    void shouldRedactCredentialDtoStringRepresentations() {
        assertThat(new UpdateEmailRequest("new@example.test", FICTIONAL_PASSWORD).toString()).doesNotContain(FICTIONAL_PASSWORD);
        assertThat(new ChangePasswordRequest(FICTIONAL_PASSWORD, FICTIONAL_NEW_PASSWORD).toString())
                .doesNotContain(FICTIONAL_PASSWORD, FICTIONAL_NEW_PASSWORD);
    }

    private AppUser user(String email, Role role, String password) {
        return userRepository.saveAndFlush(new AppUser("Test", "Account", email, passwordEncoder.encode(password), role));
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, AppUser user) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user));
    }

    private MockHttpServletRequestBuilder login(String email, String password) throws Exception {
        return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body(Map.of("email", email, "password", password)));
    }

    private String body(Object payload) throws Exception { return objectMapper.writeValueAsString(payload); }
    private JsonNode json(MvcResult result) throws Exception { return objectMapper.readTree(result.getResponse().getContentAsString()); }

    private void assertSafeAccount(JsonNode node) {
        List<String> fields = new ArrayList<>();
        node.fieldNames().forEachRemaining(fields::add);
        assertThat(fields).containsExactlyInAnyOrder(
                "id", "firstName", "lastName", "email", "role", "enabled", "blocked", "createdAt");
        assertThat(node.get("createdAt").isNull()).isFalse();
        assertThat(Arrays.stream(AccountResponse.class.getRecordComponents()).map(component -> component.getName()))
                .doesNotContain("password", "passwordHash", "accessToken");
    }

    private void assertSecurityFields(AppUser user, Role role) {
        assertThat(user.getRole()).isEqualTo(role); assertThat(user.isEnabled()).isTrue(); assertThat(user.isBlocked()).isFalse();
    }
}
