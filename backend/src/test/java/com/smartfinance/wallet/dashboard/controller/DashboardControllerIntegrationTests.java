package com.smartfinance.wallet.dashboard.controller;

import com.smartfinance.wallet.budget.entity.Budget;
import com.smartfinance.wallet.budget.repository.BudgetRepository;
import com.smartfinance.wallet.category.entity.*;
import com.smartfinance.wallet.category.repository.CategoryRepository;
import com.smartfinance.wallet.savingsgoal.entity.SavingsGoal;
import com.smartfinance.wallet.savingsgoal.repository.SavingsGoalRepository;
import com.smartfinance.wallet.security.jwt.JwtService;
import com.smartfinance.wallet.transaction.entity.FinancialTransaction;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class DashboardControllerIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired BudgetRepository budgetRepository;
    @Autowired FinancialTransactionRepository transactionRepository;
    @Autowired SavingsGoalRepository savingsGoalRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired AppUserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    @BeforeEach @AfterEach
    void clean() {
        budgetRepository.deleteAll();
        transactionRepository.deleteAll();
        savingsGoalRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRequireJwt() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary").queryParam("year", "2026")
                        .queryParam("month", "9"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"?month=9", "?year=1999&month=9", "?year=2101&month=9",
            "?year=2026", "?year=2026&month=0", "?year=2026&month=13"})
    void shouldRejectMissingOrInvalidPeriod(String query) throws Exception {
        AppUser user = user("dashboard-validation@example.test", Role.USER);
        mockMvc.perform(auth(get("/api/dashboard/summary" + query), user))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnCompleteMonthlySummaryWithoutDoubleCounting() throws Exception {
        AppUser user = user("dashboard-summary@example.test", Role.USER);
        Category salary = category(user, "Salaire", CategoryType.INCOME);
        Category food = category(user, "Alimentation", CategoryType.EXPENSE);
        Category transport = category(user, "Transport", CategoryType.EXPENSE);
        tx(user, salary, "3000", LocalDate.of(2026, 9, 1));
        tx(user, food, "600", LocalDate.of(2026, 9, 5));
        tx(user, food, "500", LocalDate.of(2026, 9, 20));
        tx(user, transport, "200", LocalDate.of(2026, 9, 10));
        tx(user, food, "999", LocalDate.of(2026, 10, 1));
        budgetRepository.saveAndFlush(new Budget(user, food, 2026, 9, new BigDecimal("1000")));
        savingsGoalRepository.saveAndFlush(new SavingsGoal(user, "Voiture",
                new BigDecimal("2000"), new BigDecimal("2400"), null));

        mockMvc.perform(auth(get("/api/dashboard/summary").queryParam("year", "2026")
                        .queryParam("month", "9"), user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026)).andExpect(jsonPath("$.month").value(9))
                .andExpect(jsonPath("$.totalIncome").value(3000))
                .andExpect(jsonPath("$.totalExpense").value(1300))
                .andExpect(jsonPath("$.balance").value(1700))
                .andExpect(jsonPath("$.transactionCount").value(4))
                .andExpect(jsonPath("$.totalBudget").value(1000))
                .andExpect(jsonPath("$.budgetSpent").value(1100))
                .andExpect(jsonPath("$.budgetRemaining").value(-100))
                .andExpect(jsonPath("$.budgetUsagePercent").value(110))
                .andExpect(jsonPath("$.budgetCount").value(1))
                .andExpect(jsonPath("$.totalSavingsTarget").value(2000))
                .andExpect(jsonPath("$.totalSavingsSaved").value(2400))
                .andExpect(jsonPath("$.savingsRemaining").value(-400))
                .andExpect(jsonPath("$.savingsProgressPercent").value(120))
                .andExpect(jsonPath("$.savingsGoalCount").value(1))
                .andExpect(jsonPath("$.userId").doesNotExist());
    }

    @Test
    void shouldStrictlyIsolateUsersIncludingAdmin() throws Exception {
        AppUser a = user("dashboard-a@example.test", Role.USER);
        AppUser b = user("dashboard-b@example.test", Role.ADMIN);
        Category incomeA = category(a, "A income", CategoryType.INCOME);
        Category expenseA = category(a, "A expense", CategoryType.EXPENSE);
        Category incomeB = category(b, "B income", CategoryType.INCOME);
        tx(a, incomeA, "1000", LocalDate.of(2026, 9, 1));
        tx(a, expenseA, "200", LocalDate.of(2026, 9, 2));
        tx(b, incomeB, "9999", LocalDate.of(2026, 9, 1));
        savingsGoalRepository.saveAndFlush(new SavingsGoal(a, "A goal",
                new BigDecimal("500"), new BigDecimal("100"), null));
        savingsGoalRepository.saveAndFlush(new SavingsGoal(b, "B goal",
                new BigDecimal("9000"), new BigDecimal("9000"), null));

        mockMvc.perform(auth(get("/api/dashboard/summary").queryParam("year", "2026")
                        .queryParam("month", "9"), a))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalIncome").value(1000))
                .andExpect(jsonPath("$.totalExpense").value(200))
                .andExpect(jsonPath("$.totalSavingsTarget").value(500));
        mockMvc.perform(auth(get("/api/dashboard/summary").queryParam("year", "2026")
                        .queryParam("month", "9"), b))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalIncome").value(9999))
                .andExpect(jsonPath("$.totalExpense").value(0))
                .andExpect(jsonPath("$.totalSavingsTarget").value(9000));
    }

    private AppUser user(String email, Role role) {
        AppUser user = new AppUser("Test", "User", email,
                passwordEncoder.encode("fictional-dashboard-password"), role);
        user.setEnabled(true);
        return userRepository.saveAndFlush(user);
    }

    private Category category(AppUser user, String name, CategoryType type) {
        return categoryRepository.saveAndFlush(new Category(user, name, name.toLowerCase(), type));
    }

    private void tx(AppUser user, Category category, String amount, LocalDate date) {
        transactionRepository.saveAndFlush(new FinancialTransaction(user, category, category.getType(),
                new BigDecimal(amount), date, null));
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, AppUser user) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.generateToken(user));
    }
}
