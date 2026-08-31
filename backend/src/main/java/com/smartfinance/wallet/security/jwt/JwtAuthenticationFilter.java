package com.smartfinance.wallet.security.jwt;

import com.smartfinance.wallet.security.AuthenticatedUserPrincipal;
import com.smartfinance.wallet.security.RestAuthenticationEntryPoint;
import com.smartfinance.wallet.user.entity.AppUser;
import com.smartfinance.wallet.user.repository.AppUserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            AppUserRepository appUserRepository,
            RestAuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.jwtService = jwtService;
        this.appUserRepository = appUserRepository;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(BEARER_PREFIX.length());
        try {
            if (token.isBlank()) {
                throw new AuthenticationServiceException("Jeton invalide.");
            }

            long userId = jwtService.getUserIdFromToken(token);
            AppUser user = appUserRepository.findById(userId)
                    .filter(AppUser::isEnabled)
                    .filter(candidate -> !candidate.isBlocked())
                    .orElseThrow(() -> new AuthenticationServiceException("Utilisateur indisponible."));

            AuthenticatedUserPrincipal principal = AuthenticatedUserPrincipal.from(user);
            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            principal,
                            null,
                            principal.authorities()
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException | AuthenticationServiceException exception) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new AuthenticationServiceException("Authentification JWT invalide.")
            );
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String method = request.getMethod();
        return (HttpMethod.POST.matches(method)
                && (path.equals("/api/auth/register") || path.equals("/api/auth/login")))
                || (HttpMethod.GET.matches(method)
                && (path.equals("/actuator/health") || path.equals("/actuator/info")));
    }
}
