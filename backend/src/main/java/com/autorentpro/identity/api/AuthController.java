package com.autorentpro.identity.api;

import com.autorentpro.identity.infrastructure.security.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final LogoutHandler logoutHandler;

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            LogoutHandler logoutHandler
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy =
                sessionAuthenticationStrategy;
        this.logoutHandler = logoutHandler;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            UsernamePasswordAuthenticationToken
                                    .unauthenticated(
                                            loginRequest.email(),
                                            loginRequest.password()
                                    )
                    );

            sessionAuthenticationStrategy.onAuthentication(
                    authentication,
                    request,
                    response
            );

            SecurityContext securityContext =
                    SecurityContextHolder
                            .createEmptyContext();

            securityContext.setAuthentication(
                    authentication
            );

            SecurityContextHolder.setContext(
                    securityContext
            );

            securityContextRepository.saveContext(
                    securityContext,
                    request,
                    response
            );

            AuthenticatedUserPrincipal principal =
                    (AuthenticatedUserPrincipal)
                            authentication.getPrincipal();

            return ResponseEntity.ok(
                    CurrentUserResponse.from(principal)
            );

        } catch (AuthenticationException exception) {
            SecurityContextHolder.clearContext();

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(
                            SecurityErrorResponse.of(
                                    HttpServletResponse
                                            .SC_UNAUTHORIZED,
                                    "AUTHENTICATION_FAILED",
                                    "Authentication failed.",
                                    request.getRequestURI()
                            )
                    );
        }
    }

    @GetMapping("/me")
    public CurrentUserResponse me(
            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal
    ) {
        return CurrentUserResponse.from(principal);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        logoutHandler.logout(
                request,
                response,
                authentication
        );

        return ResponseEntity.noContent().build();
    }
}