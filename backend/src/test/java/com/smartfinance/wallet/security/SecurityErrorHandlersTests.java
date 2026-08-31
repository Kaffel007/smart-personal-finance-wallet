package com.smartfinance.wallet.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorHandlersTests {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void shouldWriteGenericUnauthorizedApiError() throws Exception {
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(
                request,
                response,
                new AuthenticationCredentialsNotFoundException("internal detail")
        );

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(body.get("message").asText())
                .isEqualTo("Authentification requise ou jeton invalide.");
        assertThat(body.get("path").asText()).isEqualTo("/api/auth/me");
        assertThat(body.has("exception")).isFalse();
    }

    @Test
    void shouldWriteGenericForbiddenApiError() throws Exception {
        RestAccessDeniedHandler handler = new RestAccessDeniedHandler(objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/future-admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("internal detail"));

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(body.get("message").asText()).isEqualTo("Accès refusé.");
        assertThat(body.get("path").asText()).isEqualTo("/api/future-admin");
        assertThat(body.has("exception")).isFalse();
    }
}
