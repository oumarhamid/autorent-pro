package com.autorentpro.identity.infrastructure.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.CompositeLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfLogoutHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

import java.time.Clock;
import java.util.List;

@Configuration
@EnableConfigurationProperties(
        LoginSecurityProperties.class
)
public class SecurityConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories
                .createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AutoRentAuthenticationProvider authenticationProvider
    ) {
        return new ProviderManager(
                authenticationProvider
        );
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy(
            CsrfTokenRepository csrfTokenRepository
    ) {
        ChangeSessionIdAuthenticationStrategy
                sessionFixationStrategy =
                new ChangeSessionIdAuthenticationStrategy();

        CsrfAuthenticationStrategy csrfStrategy =
                new CsrfAuthenticationStrategy(
                        csrfTokenRepository
                );

        return new CompositeSessionAuthenticationStrategy(
                List.of(
                        sessionFixationStrategy,
                        csrfStrategy
                )
        );
    }

    @Bean
    public LogoutHandler logoutHandler(
            CsrfTokenRepository csrfTokenRepository
    ) {
        CsrfLogoutHandler csrfLogoutHandler =
                new CsrfLogoutHandler(
                        csrfTokenRepository
                );

        SecurityContextLogoutHandler
                securityContextLogoutHandler =
                new SecurityContextLogoutHandler();

        securityContextLogoutHandler
                .setInvalidateHttpSession(true);

        securityContextLogoutHandler
                .setClearAuthentication(true);

        return new CompositeLogoutHandler(
                csrfLogoutHandler,
                securityContextLogoutHandler
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JsonAuthenticationEntryPoint authenticationEntryPoint,
            JsonAccessDeniedHandler accessDeniedHandler,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository
    ) throws Exception {

        http
                .csrf(csrf ->
                        csrf.csrfTokenRepository(
                                csrfTokenRepository
                        )
                )

                .securityContext(securityContext ->
                        securityContext
                                .securityContextRepository(
                                        securityContextRepository
                                )
                                .requireExplicitSave(true)
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )

                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers(
                                        "/actuator/health",
                                        "/api/auth/csrf",
                                        "/api/auth/login"
                                )
                                .permitAll()

                                .anyRequest()
                                .authenticated()
                )

                .exceptionHandling(exceptions ->
                        exceptions
                                .authenticationEntryPoint(
                                        authenticationEntryPoint
                                )
                                .accessDeniedHandler(
                                        accessDeniedHandler
                                )
                )

                .requestCache(cache ->
                        cache.disable()
                )

                .formLogin(
                        AbstractHttpConfigurer::disable
                )

                .httpBasic(
                        AbstractHttpConfigurer::disable
                )

                .logout(
                        AbstractHttpConfigurer::disable
                );

        return http.build();
    }
}