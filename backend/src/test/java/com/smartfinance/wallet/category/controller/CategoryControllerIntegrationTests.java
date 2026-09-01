package com.smartfinance.wallet.category.controller;

import com.smartfinance.wallet.category.entity.Category;
import com.smartfinance.wallet.category.entity.CategoryType;
import com.smartfinance.wallet.category.repository.CategoryRepository;
import com.smartfinance.wallet.security.jwt.JwtService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class CategoryControllerIntegrationTests {

    private static final String FICTIONAL_PASSWORD = "fictional-category-passphrase";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        categoryRepository.deleteAll();
        appUserRepository.deleteAll();
    }

    @Test
    void shouldRequireJwtForEveryCategoryEndpoint() throws Exception {
        for (MockHttpServletRequestBuilder request : Stream.of(
                post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("Transport", "EXPENSE")),
                get("/api/categories"),
                get("/api/categories/1"),
                put("/api/categories/1").contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("Transport", "EXPENSE")),
                delete("/api/categories/1")
        ).toList()) {
            mockMvc.perform(request)
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }
    }

    @Test
    void shouldCreateSafeCategoryForJwtOwnerAndIgnoreMaliciousUserId() throws Exception {
        AppUser userA = saveUser("category-a@example.com", Role.USER);
        AppUser userB = saveUser("category-b@example.com", Role.USER);

        mockMvc.perform(withToken(post("/api/categories"), userA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "  Transport  ",
                                  "type": "EXPENSE",
                                  "userId": %d
                                }
                                """.formatted(userB.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Transport"))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.normalizedName").doesNotExist())
                .andExpect(jsonPath("$.user").doesNotExist());

        Category saved = categoryRepository.findAll().getFirst();
        assertThat(saved.getUser().getId()).isEqualTo(userA.getId());
        assertThat(saved.getUser().getId()).isNotEqualTo(userB.getId());
        assertThat(saved.getNormalizedName()).isEqualTo("transport");
    }

    @ParameterizedTest
    @MethodSource("invalidCategoryRequests")
    void shouldRejectInvalidCreateRequest(String json, String field) throws Exception {
        AppUser user = saveUser("validation@example.com", Role.USER);

        mockMvc.perform(withToken(post("/api/categories"), user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/api/categories"))
                .andExpect(field == null
                        ? jsonPath("$.message").exists()
                        : jsonPath("$.fieldErrors." + field).exists());

        assertThat(categoryRepository.count()).isZero();
    }

    @Test
    void shouldReturnConflictForNormalizedDuplicate() throws Exception {
        AppUser user = saveUser("duplicate-category@example.com", Role.USER);
        createCategory(user, "Transport", CategoryType.EXPENSE);

        mockMvc.perform(withToken(post("/api/categories"), user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("  TRANSPORT  ", "EXPENSE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Cette catégorie existe déjà."))
                .andExpect(jsonPath("$.constraint").doesNotExist());
    }

    @Test
    void shouldListOnlyOwnedCategoriesInNameOrderAndFilterByType() throws Exception {
        AppUser userA = saveUser("list-a@example.com", Role.USER);
        AppUser userB = saveUser("list-b@example.com", Role.USER);
        createCategory(userA, "Transport", CategoryType.EXPENSE);
        createCategory(userA, "Alimentation", CategoryType.EXPENSE);
        createCategory(userA, "Salaire", CategoryType.INCOME);
        createCategory(userB, "Secret B", CategoryType.EXPENSE);

        mockMvc.perform(withToken(get("/api/categories"), userA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[0].name").value("Alimentation"))
                .andExpect(jsonPath("$.[1].name").value("Salaire"))
                .andExpect(jsonPath("$.[2].name").value("Transport"))
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(withToken(get("/api/categories").queryParam("type", "EXPENSE"), userA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.[0].type").value("EXPENSE"))
                .andExpect(jsonPath("$.[1].type").value("EXPENSE"));

        mockMvc.perform(withToken(get("/api/categories").queryParam("type", "INCOME"), userA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$.[0].name").value("Salaire"));
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoCategory() throws Exception {
        AppUser user = saveUser("empty-categories@example.com", Role.USER);

        mockMvc.perform(withToken(get("/api/categories"), user))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void shouldHideOtherUsersCategoryForDetailUpdateAndDelete() throws Exception {
        AppUser owner = saveUser("owner@example.com", Role.USER);
        AppUser other = saveUser("other@example.com", Role.USER);
        Category category = createCategory(owner, "Privée", CategoryType.EXPENSE);

        mockMvc.perform(withToken(get("/api/categories/" + category.getId()), owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Privée"));

        mockMvc.perform(withToken(get("/api/categories/" + category.getId()), other))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Catégorie introuvable."));
        mockMvc.perform(withToken(put("/api/categories/" + category.getId()), other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("Volée", "INCOME")))
                .andExpect(status().isNotFound());
        mockMvc.perform(withToken(delete("/api/categories/" + category.getId()), other))
                .andExpect(status().isNotFound());

        assertThat(categoryRepository.findById(category.getId())).isPresent();
    }

    @Test
    void shouldUpdateOwnedCategoryAndRejectDuplicateUpdate() throws Exception {
        AppUser user = saveUser("update-category@example.com", Role.USER);
        Category updated = createCategory(user, "Divers", CategoryType.INCOME);
        createCategory(user, "Transport", CategoryType.EXPENSE);

        mockMvc.perform(withToken(put("/api/categories/" + updated.getId()), user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("  Loisirs  ", "EXPENSE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Loisirs"))
                .andExpect(jsonPath("$.type").value("EXPENSE"));

        Category persisted = categoryRepository.findById(updated.getId()).orElseThrow();
        assertThat(persisted.getNormalizedName()).isEqualTo("loisirs");
        assertThat(persisted.getUser().getId()).isEqualTo(user.getId());

        mockMvc.perform(withToken(put("/api/categories/" + updated.getId()), user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson(" transport ", "EXPENSE")))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldDeleteOwnedCategoryAndThenReturnNotFound() throws Exception {
        AppUser user = saveUser("delete-category@example.com", Role.USER);
        Category category = createCategory(user, "Temporary", CategoryType.EXPENSE);

        mockMvc.perform(withToken(delete("/api/categories/" + category.getId()), user))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mockMvc.perform(withToken(get("/api/categories/" + category.getId()), user))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldAllowSameNameForDifferentUsersAndKeepAdminIsolated() throws Exception {
        AppUser user = saveUser("regular-category@example.com", Role.USER);
        AppUser admin = saveUser("admin-category@example.com", Role.ADMIN);

        mockMvc.perform(withToken(post("/api/categories"), user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("Transport", "EXPENSE")))
                .andExpect(status().isCreated());
        mockMvc.perform(withToken(post("/api/categories"), admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryJson("Transport", "EXPENSE")))
                .andExpect(status().isCreated());

        mockMvc.perform(withToken(get("/api/categories"), admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$.[0].name").value("Transport"));
        assertThat(categoryRepository.count()).isEqualTo(2);
    }

    private AppUser saveUser(String email, Role role) {
        AppUser user = new AppUser(
                "Category", "User", email,
                passwordEncoder.encode(FICTIONAL_PASSWORD), role
        );
        user.setEnabled(true);
        user.setBlocked(false);
        return appUserRepository.saveAndFlush(user);
    }

    private Category createCategory(AppUser user, String name, CategoryType type) {
        return categoryRepository.saveAndFlush(new Category(
                user, name, name.trim().toLowerCase(), type
        ));
    }

    private MockHttpServletRequestBuilder withToken(
            MockHttpServletRequestBuilder request,
            AppUser user
    ) {
        return request.header(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + jwtService.generateToken(user)
        );
    }

    private static Stream<Arguments> invalidCategoryRequests() {
        return Stream.of(
                Arguments.of(categoryJson("   ", "EXPENSE"), "name"),
                Arguments.of("{\"name\":\"Transport\"}", "type"),
                Arguments.of(categoryJson("Transport", "INVALID"), null),
                Arguments.of(categoryJson("a".repeat(101), "EXPENSE"), "name")
        );
    }

    private static String categoryJson(String name, String type) {
        return """
                {
                  "name": "%s",
                  "type": "%s"
                }
                """.formatted(name, type);
    }
}
