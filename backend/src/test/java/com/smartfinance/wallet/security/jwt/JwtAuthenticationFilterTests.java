package com.smartfinance.wallet.security.jwt;

import com.smartfinance.wallet.security.AuthenticatedUserPrincipal;
import com.smartfinance.wallet.security.RestAuthenticationEntryPoint;
import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.user.entity.Role;
import com.smartfinance.wallet.user.repository.AppUserRepository;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTests {

    @Mock
    private JwtService jwtService;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private RestAuthenticationEntryPoint authenticationEntryPoint;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueWithoutAuthenticationWhenHeaderIsAbsent() throws Exception {
        JwtAuthenticationFilter filter = createFilter();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(meRequest(), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).getUserIdFromToken(any());
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void shouldAuthenticateActiveUserWithDatabaseRole(Role role) throws Exception {
        JwtAuthenticationFilter filter = createFilter();
        AppUser user = user(role, true, false);
        when(jwtService.getUserIdFromToken("valid-token")).thenReturn(12L);
        when(appUserRepository.findById(12L)).thenReturn(Optional.of(user));
        MockHttpServletRequest request = meRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getCredentials()).isNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(AuthenticatedUserPrincipal.class);
        assertThat(authentication.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactly("ROLE_" + role.name());
        assertThat(Arrays.stream(AuthenticatedUserPrincipal.class.getRecordComponents())
                .map(component -> component.getName()))
                .doesNotContain("password", "passwordHash");
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "blocked", "disabled"})
    void shouldRejectUnavailableCurrentUser(String state) throws Exception {
        JwtAuthenticationFilter filter = createFilter();
        when(jwtService.getUserIdFromToken("valid-token")).thenReturn(12L);
        if (state.equals("missing")) {
            when(appUserRepository.findById(12L)).thenReturn(Optional.empty());
        } else {
            when(appUserRepository.findById(12L)).thenReturn(Optional.of(user(
                    Role.USER,
                    !state.equals("disabled"),
                    state.equals("blocked")
            )));
        }
        MockHttpServletRequest request = meRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(authenticationEntryPoint).commence(any(), any(), any());
    }

    @Test
    void shouldRejectMalformedTokenWithoutAuthenticating() throws Exception {
        JwtAuthenticationFilter filter = createFilter();
        when(jwtService.getUserIdFromToken("bad-token"))
                .thenThrow(new MalformedJwtException("invalid"));
        MockHttpServletRequest request = meRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bad-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(authenticationEntryPoint).commence(any(), any(), any());
        verify(appUserRepository, never()).findById(any());
    }

    private JwtAuthenticationFilter createFilter() {
        return new JwtAuthenticationFilter(jwtService, appUserRepository, authenticationEntryPoint);
    }

    private MockHttpServletRequest meRequest() {
        return new MockHttpServletRequest("GET", "/api/auth/me");
    }

    private AppUser user(Role role, boolean enabled, boolean blocked) {
        AppUser user = new AppUser(
                "Sara", "Martin", "sara@example.com", "internal-test-hash", role
        );
        user.setEnabled(enabled);
        user.setBlocked(blocked);
        return user;
    }
}
