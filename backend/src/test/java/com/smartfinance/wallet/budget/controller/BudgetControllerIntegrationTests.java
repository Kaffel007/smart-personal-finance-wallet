package com.smartfinance.wallet.budget.controller;

import com.smartfinance.wallet.budget.entity.Budget;
import com.smartfinance.wallet.budget.repository.BudgetRepository;
import com.smartfinance.wallet.category.entity.*;
import com.smartfinance.wallet.category.repository.CategoryRepository;
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
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test") @SpringBootTest @AutoConfigureMockMvc
class BudgetControllerIntegrationTests {
    @Autowired MockMvc mockMvc; @Autowired BudgetRepository budgetRepository;
    @Autowired FinancialTransactionRepository transactionRepository; @Autowired CategoryRepository categoryRepository;
    @Autowired AppUserRepository userRepository; @Autowired PasswordEncoder passwordEncoder; @Autowired JwtService jwtService;

    @BeforeEach @AfterEach void clean(){budgetRepository.deleteAll();transactionRepository.deleteAll();categoryRepository.deleteAll();userRepository.deleteAll();}

    @Test void shouldRequireJwtForEveryEndpoint() throws Exception {
        for(MockHttpServletRequestBuilder request:Stream.of(post("/api/budgets").contentType(MediaType.APPLICATION_JSON).content("{}"),
                get("/api/budgets"),get("/api/budgets/1"),put("/api/budgets/1").contentType(MediaType.APPLICATION_JSON).content("{}"),delete("/api/budgets/1")).toList())
            mockMvc.perform(request).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest @ValueSource(strings={
            "{\"year\":2026,\"month\":9,\"amount\":100}",
            "{\"categoryId\":1,\"year\":1999,\"month\":9,\"amount\":100}",
            "{\"categoryId\":1,\"year\":2101,\"month\":9,\"amount\":100}",
            "{\"categoryId\":1,\"year\":2026,\"month\":0,\"amount\":100}",
            "{\"categoryId\":1,\"year\":2026,\"month\":13,\"amount\":100}",
            "{\"categoryId\":1,\"year\":2026,\"month\":9}",
            "{\"categoryId\":1,\"year\":2026,\"month\":9,\"amount\":0}",
            "{\"categoryId\":1,\"year\":2026,\"month\":9,\"amount\":-1}"})
    void shouldRejectInvalidRequest(String json) throws Exception {
        AppUser user=user("budget-validation@example.test",Role.USER);
        mockMvc.perform(auth(post("/api/budgets"),user).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isBadRequest());
    }

    @Test void shouldCreateExpenseBudgetRejectIncomeAndDuplicateWithSafeResponse() throws Exception {
        AppUser user=user("budget-create@example.test",Role.USER); Category expense=category(user,"Transport",CategoryType.EXPENSE),income=category(user,"Salaire",CategoryType.INCOME);
        mockMvc.perform(auth(post("/api/budgets"),user).contentType(MediaType.APPLICATION_JSON).content(json(expense.getId(),2026,9,"200")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.spent").value(0)).andExpect(jsonPath("$.remaining").value(200))
                .andExpect(jsonPath("$.usagePercent").value(0)).andExpect(jsonPath("$.userId").doesNotExist()).andExpect(jsonPath("$.user").doesNotExist());
        mockMvc.perform(auth(post("/api/budgets"),user).contentType(MediaType.APPLICATION_JSON).content(json(income.getId(),2026,9,"200")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Un budget doit utiliser une catégorie de dépense."));
        mockMvc.perform(auth(post("/api/budgets"),user).contentType(MediaType.APPLICATION_JSON).content(json(expense.getId(),2026,9,"300")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Un budget existe déjà pour cette catégorie et cette période."));
    }

    @Test void shouldRecalculateProgressFromCurrentMonthExpensesAndAllowOverspend() throws Exception {
        AppUser user=user("budget-progress@example.test",Role.USER); Category category=category(user,"Transport",CategoryType.EXPENSE);
        Budget budget=budget(user,category,2026,9,"200");
        assertProgress(user,budget,0,200,0);
        tx(user,category,"50",LocalDate.of(2026,9,5),CategoryType.EXPENSE); assertProgress(user,budget,50,150,25);
        tx(user,category,"200",LocalDate.of(2026,9,20),CategoryType.EXPENSE);
        tx(user,category,"999",LocalDate.of(2026,10,1),CategoryType.EXPENSE);
        tx(user,category,"999",LocalDate.of(2026,9,1),CategoryType.INCOME);
        assertProgress(user,budget,250,-50,125);
    }

    @Test void shouldListOnlyOwnerAndFilterByYearMonthCategory() throws Exception {
        AppUser a=user("budget-list-a@example.test",Role.USER),b=user("budget-list-b@example.test",Role.ADMIN);
        Category ca=category(a,"Transport",CategoryType.EXPENSE), cb=category(b,"Secret",CategoryType.EXPENSE);
        budget(a,ca,2026,9,"200");budget(a,ca,2026,10,"300");budget(b,cb,2026,9,"999");
        mockMvc.perform(auth(get("/api/budgets"),a)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mockMvc.perform(auth(get("/api/budgets").queryParam("year","2026").queryParam("month","9").queryParam("categoryId",ca.getId().toString()),a))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].month").value(9));
        mockMvc.perform(auth(get("/api/budgets"),b)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test void shouldHideBudgetAndCategoryAcrossUsers() throws Exception {
        AppUser a=user("budget-owner@example.test",Role.USER),b=user("budget-other@example.test",Role.USER);
        Category category=category(a,"Private",CategoryType.EXPENSE); Budget budget=budget(a,category,2026,9,"100");
        mockMvc.perform(auth(get("/api/budgets/"+budget.getId()),b)).andExpect(status().isNotFound());
        mockMvc.perform(auth(put("/api/budgets/"+budget.getId()),b).contentType(MediaType.APPLICATION_JSON).content(json(category.getId(),2026,9,"200"))).andExpect(status().isNotFound());
        mockMvc.perform(auth(delete("/api/budgets/"+budget.getId()),b)).andExpect(status().isNotFound());
        mockMvc.perform(auth(post("/api/budgets"),b).contentType(MediaType.APPLICATION_JSON).content(json(category.getId(),2026,9,"200")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Catégorie introuvable."));
    }

    @Test void shouldUpdateDeleteAndThenReturnNotFound() throws Exception {
        AppUser user=user("budget-update@example.test",Role.USER);Category first=category(user,"Transport",CategoryType.EXPENSE),second=category(user,"Food",CategoryType.EXPENSE);
        Budget budget=budget(user,first,2026,9,"100");tx(user,second,"50",LocalDate.of(2026,10,2),CategoryType.EXPENSE);
        mockMvc.perform(auth(put("/api/budgets/"+budget.getId()),user).contentType(MediaType.APPLICATION_JSON).content(json(second.getId(),2026,10,"200")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.categoryId").value(second.getId())).andExpect(jsonPath("$.spent").value(50)).andExpect(jsonPath("$.usagePercent").value(25));
        mockMvc.perform(auth(delete("/api/budgets/"+budget.getId()),user)).andExpect(status().isNoContent());
        mockMvc.perform(auth(get("/api/budgets/"+budget.getId()),user)).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Budget introuvable."));
    }

    @Test void shouldProtectCategoryUsedByBudgetAndAllowRename() throws Exception {
        AppUser user=user("budget-category-use@example.test",Role.USER);Category category=category(user,"Transport",CategoryType.EXPENSE);budget(user,category,2026,9,"100");
        mockMvc.perform(auth(delete("/api/categories/"+category.getId()),user)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cette catégorie est utilisée par des données financières."));
        mockMvc.perform(auth(put("/api/categories/"+category.getId()),user).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Transport quotidien\",\"type\":\"EXPENSE\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(auth(put("/api/categories/"+category.getId()),user).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Transport quotidien\",\"type\":\"INCOME\"}"))
                .andExpect(status().isConflict());
    }

    private void assertProgress(AppUser u,Budget b,double spent,double remaining,double usage)throws Exception{mockMvc.perform(auth(get("/api/budgets/"+b.getId()),u)).andExpect(status().isOk()).andExpect(jsonPath("$.spent").value(spent)).andExpect(jsonPath("$.remaining").value(remaining)).andExpect(jsonPath("$.usagePercent").value(usage));}
    private AppUser user(String email,Role role){AppUser u=new AppUser("Test","User",email,passwordEncoder.encode("fictional-budget-password"),role);u.setEnabled(true);return userRepository.saveAndFlush(u);}
    private Category category(AppUser u,String name,CategoryType type){return categoryRepository.saveAndFlush(new Category(u,name,name.toLowerCase(),type));}
    private Budget budget(AppUser u,Category c,int year,int month,String amount){return budgetRepository.saveAndFlush(new Budget(u,c,year,month,new BigDecimal(amount)));}
    private void tx(AppUser u,Category c,String amount,LocalDate date,CategoryType type){transactionRepository.saveAndFlush(new FinancialTransaction(u,c,type,new BigDecimal(amount),date,null));}
    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder r,AppUser u){return r.header(HttpHeaders.AUTHORIZATION,"Bearer "+jwtService.generateToken(u));}
    private String json(Long categoryId,int year,int month,String amount){return "{\"categoryId\":"+categoryId+",\"year\":"+year+",\"month\":"+month+",\"amount\":"+amount+"}";}
}
