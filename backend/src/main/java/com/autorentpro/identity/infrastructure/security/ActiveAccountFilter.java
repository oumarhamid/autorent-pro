package com.autorentpro.identity.infrastructure.security;

import com.autorentpro.identity.application.ActiveAccountStatusService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class ActiveAccountFilter
        extends OncePerRequestFilter {

    private static final Set<String> EXCLUDED_PATHS =
            Set.of(
                    "/api/auth/login",
                    "/api/auth/csrf",
                    "/api/auth/logout",
                    "/actuator/health"
            );

    private final ActiveAccountStatusService activeAccountStatusService;
    private final LogoutHandler logoutHandler;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public ActiveAccountFilter(
            ActiveAccountStatusService activeAccountStatusService,
            LogoutHandler logoutHandler,
            JsonAuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.activeAccountStatusService =
                activeAccountStatusService;

        this.logoutHandler =
                logoutHandler;

        this.authenticationEntryPoint =
                authenticationEntryPoint;
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {
        return EXCLUDED_PATHS.contains(
                request.getServletPath()
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof AuthenticatedUserPrincipal principal)) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        if (activeAccountStatusService.isActive(
                principal.userId()
        )) {
            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        logoutHandler.logout(
                request,
                response,
                authentication
        );

        authenticationEntryPoint.commence(
                request,
                response,
                new InsufficientAuthenticationException(
                        "The authenticated account is unavailable."
                )
        );
    }
}