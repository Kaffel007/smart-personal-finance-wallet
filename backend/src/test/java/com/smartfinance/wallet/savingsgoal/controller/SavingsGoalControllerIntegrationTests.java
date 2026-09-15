package com.smartfinance.wallet.savingsgoal.controller;

import com.smartfinance.wallet.budget.repository.BudgetRepository;
import com.smartfinance.wallet.category.repository.CategoryRepository;
import com.smartfinance.wallet.savingsgoal.entity.SavingsGoal;
import com.smartfinance.wallet.savingsgoal.repository.SavingsGoalRepository;
import com.smartfinance.wallet.security.jwt.JwtService;
import com.smartfinance.wallet.transaction.repository.FinancialTransactionRepository;
import com.smartfinance.wallet.user.entity.*;
import com.smartfinance.wallet.user.repository.AppUserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class SavingsGoalControllerIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired SavingsGoalRepository repository;
    @Autowired BudgetRepository budgetRepository;
    @Autowired FinancialTransactionRepository transactionRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired AppUserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    @BeforeEach @AfterEach
    void clean() {
        budgetRepository.deleteAll();
        transactionRepository.deleteAll();
        categoryRepository.deleteAll();
        repository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRequireJwtForEveryEndpoint() throws Exception {
        for (MockHttpServletRequestBuilder request : Stream.of(
                post("/api/savings-goals").contentType(MediaType.APPLICATION_JSON).content("{}"),
                get("/api/savings-goals"), get("/api/savings-goals/1"),
                put("/api/savings-goals/1").contentType(MediaType.APPLICATION_JSON).content("{}"),
                delete("/api/savings-goals/1")).toList()) {
            mockMvc.perform(request).andExpect(status().isUnauthorized());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"name\":\"\",\"targetAmount\":100,\"savedAmount\":0}",
            "{\"name\":\"Goal\",\"savedAmount\":0}",
            "{\"name\":\"Goal\",\"targetAmount\":0,\"savedAmount\":0}",
            "{\"name\":\"Goal\",\"targetAmount\":-1,\"savedAmount\":0}",
            "{\"name\":\"Goal\",\"targetAmount\":100,\"savedAmount\":-1}"
    })
    void shouldRejectInvalidCreateRequest(String json) throws Exception {
        AppUser user = user("goal-validation@example.test", Role.USER);
        mockMvc.perform(auth(post("/api/savings-goals"), user)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreateWithSafeDynamicResponseAndDefaultSavedAmount() throws Exception {
        AppUser user = user("goal-create@example.test", Role.USER);
        mockMvc.perform(auth(post("/api/savings-goals"), user).contentType(MediaType.APPLICATION_JSON)
                        .content(json("  Acheter une voiture  ", "30000.0000", "8000.0000", "2027-06-30")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Acheter une voiture"))
                .andExpect(jsonPath("$.remainingAmount").value(22000))
                .andExpect(jsonPath("$.progressPercent").value(26.67))
                .andExpect(jsonPath("$.targetDate").value("2027-06-30"))
                .andExpect(jsonPath("$.userId").doesNotExist()).andExpect(jsonPath("$.user").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist()).andExpect(jsonPath("$.updatedAt").doesNotExist());

        mockMvc.perform(auth(post("/api/savings-goals"), user).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Urgences\",\"targetAmount\":1000}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.savedAmount").value(0))
                .andExpect(jsonPath("$.progressPercent").value(0));
    }

    @Test
    void shouldReturnEmptyListThenOnlyOwnersGoalsIncludingAdminOwnGoals() throws Exception {
        AppUser a = user("goal-list-a@example.test", Role.USER);
        AppUser b = user("goal-list-b@example.test", Role.ADMIN);
        mockMvc.perform(auth(get("/api/savings-goals"), a)).andExpect(status().isOk())
                .andExpect(content().json("[]"));
        goal(a, "A", "100", "10", null);
        goal(b, "B", "999", "999", null);
        mockMvc.perform(auth(get("/api/savings-goals"), a)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].name").value("A"));
        mockMvc.perform(auth(get("/api/savings-goals"), b)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].name").value("B"));
    }

    @Test
    void shouldHideDetailUpdateAndDeleteAcrossUsers() throws Exception {
        AppUser owner = user("goal-owner@example.test", Role.USER);
        AppUser other = user("goal-other@example.test", Role.USER);
        SavingsGoal goal = goal(owner, "Privé", "100", "10", null);
        String update = json("Volé", "200", "20", null);

        mockMvc.perform(auth(get("/api/savings-goals/" + goal.getId()), other))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message")
                        .value("Objectif d'épargne introuvable."));
        mockMvc.perform(auth(put("/api/savings-goals/" + goal.getId()), other)
                        .contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().isNotFound());
        mockMvc.perform(auth(delete("/api/savings-goals/" + goal.getId()), other))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetUpdateRecalculateAllowOverTargetDeleteAndThenReturnNotFound() throws Exception {
        AppUser user = user("goal-update@example.test", Role.USER);
        SavingsGoal goal = goal(user, "Voyage", "1000", "250", null);
        mockMvc.perform(auth(get("/api/savings-goals/" + goal.getId()), user))
                .andExpect(status().isOk()).andExpect(jsonPath("$.progressPercent").value(25));
        mockMvc.perform(auth(put("/api/savings-goals/" + goal.getId()), user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Voiture", "1000", "1200", "2028-12-31")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.remainingAmount").value(-200))
                .andExpect(jsonPath("$.progressPercent").value(120))
                .andExpect(jsonPath("$.targetDate").value("2028-12-31"));
        mockMvc.perform(auth(delete("/api/savings-goals/" + goal.getId()), user))
                .andExpect(status().isNoContent());
        mockMvc.perform(auth(get("/api/savings-goals/" + goal.getId()), user))
                .andExpect(status().isNotFound());
    }

    private AppUser user(String email, Role role) {
        AppUser user = new AppUser("Test", "User", email,
                passwordEncoder.encode("fictional-savings-goal-password"), role);
        user.setEnabled(true);
        return userRepository.saveAndFlush(user);
    }

    private SavingsGoal goal(AppUser user, String name, String target, String saved, LocalDate date) {
        return repository.saveAndFlush(new SavingsGoal(user, name, new BigDecimal(target),
                new BigDecimal(saved), date));
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, AppUser user) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user));
    }

    private String json(String name, String target, String saved, String targetDate) {
        String date = targetDate == null ? "null" : "\"" + targetDate + "\"";
        return "{\"name\":\"" + name + "\",\"targetAmount\":" + target
                + ",\"savedAmount\":" + saved + ",\"targetDate\":" + date + "}";
    }
}
