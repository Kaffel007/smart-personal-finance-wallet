package com.smartfinance.wallet.admin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinance.wallet.budget.entity.Budget;
import com.smartfinance.wallet.budget.repository.BudgetRepository;
import com.smartfinance.wallet.category.entity.Category;
import com.smartfinance.wallet.category.entity.CategoryType;
import com.smartfinance.wallet.category.repository.CategoryRepository;
import com.smartfinance.wallet.savingsgoal.entity.SavingsGoal;
import com.smartfinance.wallet.savingsgoal.repository.SavingsGoalRepository;
import com.smartfinance.wallet.security.jwt.JwtService;
import com.smartfinance.wallet.transaction.entity.FinancialTransaction;
import com.smartfinance.wallet.transaction.repository.FinancialTransactionRepository;
import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.user.entity.Role;
import com.smartfinance.wallet.user.repository.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerIntegrationTests {
    private static final String FICTIONAL_PASSWORD = "fictional-admin-test-passphrase";
    private static final String USERS = "/api/admin/users";
    private static final String STATS = "/api/admin/stats";
    private static final List<String> ACCOUNT_FIELDS = List.of(
            "id", "firstName", "lastName", "email", "role", "enabled", "blocked", "createdAt");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppUserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;
    @Autowired CategoryRepository categoryRepository;
    @Autowired FinancialTransactionRepository transactionRepository;
    @Autowired BudgetRepository budgetRepository;
    @Autowired SavingsGoalRepository savingsGoalRepository;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        budgetRepository.deleteAll();
        transactionRepository.deleteAll();
        savingsGoalRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/admin/users", "/api/admin/users/1", "/api/admin/users/1/block",
            "/api/admin/users/1/unblock", "/api/admin/stats", "/api/admin/future-endpoint"})
    void shouldRejectAnonymousAccessAcrossAdminNamespace(String path) throws Exception {
        mockMvc.perform(endpoint(path)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/admin/users", "/api/admin/users/1", "/api/admin/users/1/block",
            "/api/admin/users/1/unblock", "/api/admin/stats", "/api/admin/future-endpoint"})
    void shouldRejectUserAccessAcrossAdminNamespace(String path) throws Exception {
        AppUser user = user("ordinary@example.test", Role.USER);
        mockMvc.perform(auth(endpoint(path), user)).andExpect(status().isForbidden());
        assertThat(userRepository.findById(user.getId()).orElseThrow().isBlocked()).isFalse();
    }

    @Test
    void shouldListOnlySafeAccountFieldsIncludingAdminsInDescendingIdOrder() throws Exception {
        AppUser admin = user("admin@example.test", Role.ADMIN);
        AppUser user = user("member@example.test", Role.USER);
        createPrivateFinances(user);

        JsonNode response = json(mockMvc.perform(auth(get(USERS), admin))
                .andExpect(status().isOk()).andReturn());
        assertThat(response.size()).isEqualTo(2);
        assertSafeAccount(response.get(0));
        assertSafeAccount(response.get(1));
        assertThat(response.get(0).get("id").asLong()).isEqualTo(user.getId());
        assertThat(response.get(1).get("role").asText()).isEqualTo("ADMIN");
        assertThat(response.toString()).doesNotContain("PRIVATE_FINANCE", "password", "passwordHash", "accessToken");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Sara", "MARTIN", " sara martin ", "MEMBER@", "example.test"})
    void shouldSearchNameAndEmailIgnoringCaseAndSurroundingWhitespace(String search) throws Exception {
        AppUser admin = user("admin@platform.test", Role.ADMIN);
        AppUser member = userRepository.saveAndFlush(new AppUser("Sara", "Martin", "member@example.test",
                passwordEncoder.encode(FICTIONAL_PASSWORD), Role.USER));
        JsonNode response = json(mockMvc.perform(auth(get(USERS).queryParam("search", search), admin))
                .andExpect(status().isOk()).andReturn());
        assertThat(response.size()).isOne();
        assertThat(response.get(0).get("id").asLong()).isEqualTo(member.getId());
        assertSafeAccount(response.get(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "%", "_", "' or 1=1 --"})
    void shouldReturnEmptySearchWithoutTreatingInputAsWildcardsOrSql(String search) throws Exception {
        AppUser admin = user("admin@example.test", Role.ADMIN);
        mockMvc.perform(auth(get(USERS).queryParam("search", search), admin))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void shouldTreatBlankSearchAsUnfilteredList() throws Exception {
        AppUser admin = user("admin@example.test", Role.ADMIN);
        mockMvc.perform(auth(get(USERS).queryParam("search", "  "), admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldReturnAccountDetailWithoutPrivateData() throws Exception {
        AppUser admin = user("admin@example.test", Role.ADMIN);
        AppUser member = user("member@example.test", Role.USER);
        member.setEnabled(false);
        member.setBlocked(true);
        userRepository.saveAndFlush(member);
        createPrivateFinances(member);
        JsonNode response = json(mockMvc.perform(auth(get(USERS + "/" + member.getId()), admin))
                .andExpect(status().isOk()).andReturn());
        assertSafeAccount(response);
        assertThat(response.get("id").asLong()).isEqualTo(member.getId());
        assertThat(response.get("email").asText()).isEqualTo(member.getEmail());
        assertThat(response.get("enabled").asBoolean()).isFalse();
        assertThat(response.get("blocked").asBoolean()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/block", "/unblock"})
    void shouldReturn404ForMissingAccount(String suffix) throws Exception {
        AppUser admin = user("admin@example.test", Role.ADMIN);
        String path = USERS + "/9223372036854775807" + suffix;
        mockMvc.perform(auth(suffix.isEmpty() ? get(path) : put(path), admin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.message").value("Utilisateur introuvable."));
    }

    @Test
    void shouldBlockUserInvalidateExistingJwtRejectLoginAndUnblockWithoutChangingFinances() throws Exception {
        AppUser admin = user("admin@example.test", Role.ADMIN);
        AppUser member = user("member@example.test", Role.USER);
        createPrivateFinances(member);
        String token = loginToken(member);
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        JsonNode blocked = json(mockMvc.perform(auth(put(USERS + "/" + member.getId() + "/block"), admin))
                .andExpect(status().isOk()).andReturn());
        assertSafeAccount(blocked);
        assertThat(blocked.get("blocked").asBoolean()).isTrue();
        assertThat(userRepository.findById(member.getId()).orElseThrow().isBlocked()).isTrue();
        for (String path : List.of("/api/auth/me", "/api/categories", "/api/transactions", "/api/budgets",
                "/api/savings-goals", "/api/dashboard/summary?year=2026&month=9")) {
            mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(login(member)).andExpect(status().isUnauthorized());

        JsonNode unblocked = json(mockMvc.perform(auth(put(USERS + "/" + member.getId() + "/unblock"), admin))
                .andExpect(status().isOk()).andReturn());
        assertSafeAccount(unblocked);
        assertThat(unblocked.get("blocked").asBoolean()).isFalse();
        assertThat(userRepository.findById(member.getId()).orElseThrow().isBlocked()).isFalse();
        String newToken = loginToken(member);
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + newToken))
                .andExpect(status().isOk());
        assertThat(categoryRepository.count()).isOne();
        assertThat(transactionRepository.count()).isOne();
        assertThat(budgetRepository.count()).isOne();
        assertThat(savingsGoalRepository.count()).isOne();
        assertThat(transactionRepository.findAll().get(0).getAmount()).isEqualByComparingTo("125");
        AppUser unchanged = userRepository.findById(member.getId()).orElseThrow();
        assertThat(unchanged.getRole()).isEqualTo(Role.USER);
        assertThat(unchanged.getPasswordHash()).isEqualTo(member.getPasswordHash());
    }

    @Test
    void shouldAllowRepeatedUserBlockAndUnblockWithoutReenablingDisabledAccounts() throws Exception {
        AppUser admin = user("admin@example.test", Role.ADMIN);
        AppUser member = user("disabled@example.test", Role.USER);
        member.setEnabled(false);
        userRepository.saveAndFlush(member);
        for (String action : List.of("block", "block", "unblock", "unblock")) {
            mockMvc.perform(auth(put(USERS + "/" + member.getId() + "/" + action), admin))
                    .andExpect(status().isOk());
        }
        AppUser result = userRepository.findById(member.getId()).orElseThrow();
        assertThat(result.isEnabled()).isFalse();
        assertThat(result.isBlocked()).isFalse();
        mockMvc.perform(login(member)).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldPreventAdminSelfBlockWithClearConflict() throws Exception {
        AppUser admin = user("admin@example.test", Role.ADMIN);
        mockMvc.perform(auth(put(USERS + "/" + admin.getId() + "/block"), admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Un administrateur ne peut pas bloquer son propre compte."));
        assertThat(userRepository.findById(admin.getId()).orElseThrow().isBlocked()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"block", "unblock"})
    void shouldPreventActionsOnOtherAdminAccounts(String action) throws Exception {
        AppUser admin = user("admin@example.test", Role.ADMIN);
        AppUser other = user("other-admin@example.test", Role.ADMIN);
        other.setBlocked(action.equals("unblock"));
        userRepository.saveAndFlush(other);
        mockMvc.perform(auth(put(USERS + "/" + other.getId() + "/" + action), admin))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Le blocage et le déblocage d'un compte administrateur ne sont pas autorisés."));
        assertThat(userRepository.findById(other.getId()).orElseThrow().isBlocked()).isEqualTo(other.isBlocked());
    }

    @Test
    void shouldPreventAdminSelfUnblock() throws Exception {
        AppUser admin = user("admin@example.test", Role.ADMIN);
        mockMvc.perform(auth(put(USERS + "/" + admin.getId() + "/unblock"), admin))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturnOnlyAccountCountsWithCorrectEnabledAndBlockedSemantics() throws Exception {
        AppUser admin = user("admin@example.test", Role.ADMIN);
        user("active@example.test", Role.USER);
        AppUser blocked = user("blocked@example.test", Role.USER);
        blocked.setBlocked(true);
        userRepository.saveAndFlush(blocked);
        AppUser disabled = user("disabled@example.test", Role.USER);
        disabled.setEnabled(false);
        userRepository.saveAndFlush(disabled);
        AppUser both = user("disabled-blocked@example.test", Role.USER);
        both.setEnabled(false);
        both.setBlocked(true);
        userRepository.saveAndFlush(both);
        createPrivateFinances(blocked);
        JsonNode stats = json(mockMvc.perform(auth(get(STATS), admin)).andExpect(status().isOk()).andReturn());
        assertThat(fields(stats)).containsExactlyInAnyOrder("totalUsers", "activeUsers", "blockedUsers", "adminUsers");
        assertThat(stats.get("totalUsers").asLong()).isEqualTo(5);
        assertThat(stats.get("activeUsers").asLong()).isEqualTo(2);
        assertThat(stats.get("blockedUsers").asLong()).isEqualTo(2);
        assertThat(stats.get("adminUsers").asLong()).isEqualTo(1);
    }

    @Test
    void shouldUpdateStatsAfterBlockAndUnblock() throws Exception {
        AppUser admin = user("admin@example.test", Role.ADMIN);
        AppUser member = user("member@example.test", Role.USER);
        mockMvc.perform(auth(put(USERS + "/" + member.getId() + "/block"), admin)).andExpect(status().isOk());
        mockMvc.perform(auth(get(STATS), admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(2)).andExpect(jsonPath("$.activeUsers").value(1))
                .andExpect(jsonPath("$.blockedUsers").value(1)).andExpect(jsonPath("$.adminUsers").value(1));
        mockMvc.perform(auth(put(USERS + "/" + member.getId() + "/unblock"), admin)).andExpect(status().isOk());
        mockMvc.perform(auth(get(STATS), admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.activeUsers").value(2)).andExpect(jsonPath("$.blockedUsers").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"categories", "transactions", "budgets", "savings-goals"})
    void shouldKeepAdminRestrictedToOwnFinancesOnExistingUserEndpoints(String resource) throws Exception {
        AppUser admin = user("admin@example.test", Role.ADMIN);
        AppUser member = user("member@example.test", Role.USER);
        Map<String, Long> privateIds = createPrivateFinances(member);
        mockMvc.perform(auth(get("/api/" + resource).queryParam("userId", member.getId().toString()), admin))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mockMvc.perform(auth(get("/api/" + resource + "/" + privateIds.get(resource)), admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAlwaysRegisterUserEvenWithInjectedAdminFieldsAndDenyAdminAccess() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("firstName", "Test", "lastName", "Member",
                "email", "register@example.test", "password", FICTIONAL_PASSWORD,
                "role", "ADMIN", "roles", List.of("ADMIN"), "authorities", List.of("ROLE_ADMIN")));
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.role").value("USER"));
        AppUser member = userRepository.findByEmail("register@example.test").orElseThrow();
        assertThat(member.getRole()).isEqualTo(Role.USER);
        String token = loginToken(member);
        mockMvc.perform(get(USERS).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectDisabledOrBlockedAdminJwt() throws Exception {
        AppUser admin = user("admin@example.test", Role.ADMIN);
        String token = loginToken(admin);
        admin.setBlocked(true);
        userRepository.saveAndFlush(admin);
        mockMvc.perform(get(USERS).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andExpect(status().isUnauthorized());
        admin.setBlocked(false);
        admin.setEnabled(false);
        userRepository.saveAndFlush(admin);
        mockMvc.perform(get(STATS).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andExpect(status().isUnauthorized());
    }

    private AppUser user(String email, Role role) {
        return userRepository.saveAndFlush(new AppUser("Test", "Account", email,
                passwordEncoder.encode(FICTIONAL_PASSWORD), role));
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, AppUser user) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user));
    }

    private MockHttpServletRequestBuilder endpoint(String path) {
        return path.endsWith("/block") || path.endsWith("/unblock") ? put(path) : get(path);
    }

    private MockHttpServletRequestBuilder login(AppUser user) throws Exception {
        return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", user.getEmail(), "password", FICTIONAL_PASSWORD)));
    }

    private String loginToken(AppUser user) throws Exception {
        return json(mockMvc.perform(login(user)).andExpect(status().isOk()).andReturn()).get("accessToken").asText();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private List<String> fields(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private void assertSafeAccount(JsonNode node) {
        assertThat(fields(node)).containsExactlyInAnyOrderElementsOf(ACCOUNT_FIELDS);
        assertThat(node.get("createdAt").isNull()).isFalse();
    }

    private Map<String, Long> createPrivateFinances(AppUser user) {
        Category category = categoryRepository.saveAndFlush(new Category(user, "PRIVATE_FINANCE", "private_finance", CategoryType.EXPENSE));
        FinancialTransaction transaction = transactionRepository.saveAndFlush(new FinancialTransaction(user, category,
                CategoryType.EXPENSE, new BigDecimal("125"), LocalDate.of(2026, 9, 1), "PRIVATE_FINANCE"));
        Budget budget = budgetRepository.saveAndFlush(new Budget(user, category, 2026, 9, new BigDecimal("300")));
        SavingsGoal goal = savingsGoalRepository.saveAndFlush(new SavingsGoal(user, "PRIVATE_FINANCE",
                new BigDecimal("1000"), new BigDecimal("100"), null));
        return Map.of("categories", category.getId(), "transactions", transaction.getId(),
                "budgets", budget.getId(), "savings-goals", goal.getId());
    }
}
