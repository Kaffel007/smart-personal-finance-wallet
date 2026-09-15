package com.smartfinance.wallet.transaction.controller;

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
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test") @SpringBootTest @AutoConfigureMockMvc
class FinancialTransactionControllerIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired AppUserRepository userRepository;
    @Autowired CategoryRepository categoryRepository;
    @Autowired FinancialTransactionRepository transactionRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    @BeforeEach void clean() { transactionRepository.deleteAll(); categoryRepository.deleteAll(); userRepository.deleteAll(); }

    @Test void shouldRequireJwtForEveryEndpoint() throws Exception {
        for (MockHttpServletRequestBuilder request : Stream.of(
                post("/api/transactions").contentType(MediaType.APPLICATION_JSON).content("{}"),
                get("/api/transactions"), get("/api/transactions/1"),
                put("/api/transactions/1").contentType(MediaType.APPLICATION_JSON).content("{}"),
                delete("/api/transactions/1")).toList()) {
            mockMvc.perform(request).andExpect(status().isUnauthorized());
        }
    }

    @ParameterizedTest @ValueSource(strings={
            "{\"amount\":1,\"transactionDate\":\"2026-09-01\"}",
            "{\"categoryId\":1,\"transactionDate\":\"2026-09-01\"}",
            "{\"categoryId\":1,\"amount\":0,\"transactionDate\":\"2026-09-01\"}",
            "{\"categoryId\":1,\"amount\":-1,\"transactionDate\":\"2026-09-01\"}",
            "{\"categoryId\":1,\"amount\":1}",
            "{\"categoryId\":1,\"amount\":1,\"transactionDate\":\"2099-01-01\"}"
    }) void shouldRejectInvalidRequests(String json) throws Exception {
        AppUser user=user("validation-tx@example.test",Role.USER);
        mockMvc.perform(auth(post("/api/transactions"),user).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    @Test void shouldRejectDescriptionLongerThan255Characters() throws Exception {
        AppUser user=user("long-description-tx@example.test",Role.USER);
        Category category=category(user,"Transport",CategoryType.EXPENSE);
        mockMvc.perform(auth(post("/api/transactions"),user).contentType(MediaType.APPLICATION_JSON)
                .content(json(category.getId(),"1",LocalDate.now(),"a".repeat(256),null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.description").exists());
    }

    @Test void shouldCreateIncomeAndExpenseWithSafeDerivedType() throws Exception {
        AppUser user=user("create-tx@example.test",Role.USER);
        Category expense=category(user,"Transport",CategoryType.EXPENSE);
        Category income=category(user,"Salaire",CategoryType.INCOME);
        mockMvc.perform(auth(post("/api/transactions"),user).contentType(MediaType.APPLICATION_JSON)
                .content(json(expense.getId(),"45.5000",LocalDate.now(),"  Taxi  ","INCOME")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.description").value("Taxi")).andExpect(jsonPath("$.categoryName").value("Transport"))
                .andExpect(jsonPath("$.userId").doesNotExist()).andExpect(jsonPath("$.user").doesNotExist());
        mockMvc.perform(auth(post("/api/transactions"),user).contentType(MediaType.APPLICATION_JSON)
                .content(json(income.getId(),"1000",LocalDate.now(),null,null)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.type").value("INCOME"));
    }

    @Test void shouldListOnlyOwnerInDateOrderAndApplyFilters() throws Exception {
        AppUser a=user("list-a-tx@example.test",Role.USER), b=user("list-b-tx@example.test",Role.USER);
        Category expense=category(a,"Transport",CategoryType.EXPENSE), income=category(a,"Salaire",CategoryType.INCOME);
        Category secret=category(b,"Secret",CategoryType.EXPENSE);
        tx(a,expense,"10",LocalDate.now().minusDays(1)); tx(a,income,"20",LocalDate.now()); tx(b,secret,"99",LocalDate.now());
        mockMvc.perform(auth(get("/api/transactions"),a)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].type").value("INCOME"));
        mockMvc.perform(auth(get("/api/transactions").queryParam("type","EXPENSE"),a))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].type").value("EXPENSE"));
        mockMvc.perform(auth(get("/api/transactions").queryParam("type","INCOME").queryParam("categoryId",income.getId().toString()),a))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test void shouldEnforceCrossUserIsolationForTransactionAndCategory() throws Exception {
        AppUser a=user("owner-tx@example.test",Role.USER), b=user("other-tx@example.test",Role.ADMIN);
        Category category=category(a,"Private",CategoryType.EXPENSE);
        Category otherCategory=category(b,"Other private",CategoryType.INCOME);
        FinancialTransaction tx=tx(a,category,"5",LocalDate.now());
        mockMvc.perform(auth(post("/api/transactions"),b).contentType(MediaType.APPLICATION_JSON)
                .content(json(category.getId(),"1",LocalDate.now(),null,null)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Catégorie introuvable."));
        mockMvc.perform(auth(get("/api/transactions/"+tx.getId()),b)).andExpect(status().isNotFound());
        mockMvc.perform(auth(put("/api/transactions/"+tx.getId()),b).contentType(MediaType.APPLICATION_JSON)
                .content(json(category.getId(),"1",LocalDate.now(),null,null))).andExpect(status().isNotFound());
        mockMvc.perform(auth(delete("/api/transactions/"+tx.getId()),b)).andExpect(status().isNotFound());
        mockMvc.perform(auth(put("/api/transactions/"+tx.getId()),a).contentType(MediaType.APPLICATION_JSON)
                .content(json(otherCategory.getId(),"1",LocalDate.now(),null,null)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Catégorie introuvable."));
        assertThat(transactionRepository.findById(tx.getId())).isPresent();
    }

    @Test void shouldUpdateRecalculateTypeDeleteAndThenReturnNotFound() throws Exception {
        AppUser user=user("update-tx@example.test",Role.USER);
        Category expense=category(user,"Transport",CategoryType.EXPENSE), income=category(user,"Salaire",CategoryType.INCOME);
        FinancialTransaction tx=tx(user,expense,"5",LocalDate.now());
        mockMvc.perform(auth(put("/api/transactions/"+tx.getId()),user).contentType(MediaType.APPLICATION_JSON)
                .content(json(income.getId(),"25.2500",LocalDate.now()," Prime ",null)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.amount").value(25.25));
        mockMvc.perform(auth(delete("/api/transactions/"+tx.getId()),user)).andExpect(status().isNoContent());
        mockMvc.perform(auth(get("/api/transactions/"+tx.getId()),user)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Transaction introuvable."));
    }

    @Test void shouldProtectUsedCategoryButAllowRename() throws Exception {
        AppUser user=user("category-use@example.test",Role.USER);
        Category category=category(user,"Transport",CategoryType.EXPENSE); tx(user,category,"5",LocalDate.now());
        mockMvc.perform(auth(delete("/api/categories/"+category.getId()),user)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cette catégorie est utilisée par des données financières."));
        mockMvc.perform(auth(put("/api/categories/"+category.getId()),user).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Transport quotidien\",\"type\":\"EXPENSE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Transport quotidien"));
        mockMvc.perform(auth(put("/api/categories/"+category.getId()),user).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Transport quotidien\",\"type\":\"INCOME\"}"))
                .andExpect(status().isConflict());
    }

    private AppUser user(String email,Role role){ AppUser u=new AppUser("Test","User",email,passwordEncoder.encode("fictional-transaction-password"),role);u.setEnabled(true);return userRepository.saveAndFlush(u); }
    private Category category(AppUser u,String name,CategoryType type){ return categoryRepository.saveAndFlush(new Category(u,name,name.toLowerCase(),type)); }
    private FinancialTransaction tx(AppUser u,Category c,String amount,LocalDate date){ return transactionRepository.saveAndFlush(new FinancialTransaction(u,c,c.getType(),new BigDecimal(amount),date,null)); }
    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request,AppUser u){ return request.header(HttpHeaders.AUTHORIZATION,"Bearer "+jwtService.generateToken(u)); }
    private String json(Long categoryId,String amount,LocalDate date,String description,String type){
        return "{\"categoryId\":"+categoryId+",\"amount\":"+amount+",\"transactionDate\":\""+date+"\""
                +(description==null?"":",\"description\":\""+description+"\"")+(type==null?"":",\"type\":\""+type+"\"")+"}";
    }
}
