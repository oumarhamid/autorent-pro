package com.autorentpro.identity.infrastructure.security;

import com.autorentpro.identity.application.IdentityAccessService;
import com.autorentpro.identity.application.PermissionGrant;
import com.autorentpro.identity.application.ResolvedIdentityAccess;
import com.autorentpro.identity.domain.model.RoleCode;
import com.autorentpro.identity.domain.model.UserAccount;
import com.autorentpro.identity.infrastructure.persistence.UserAccountRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Component
public class AutoRentAuthenticationProvider
        implements AuthenticationProvider {

    private final UserAccountRepository userAccountRepository;
    private final IdentityAccessService identityAccessService;
    private final PasswordEncoder passwordEncoder;
    private final LoginSecurityProperties securityProperties;
    private final Clock clock;
    private final String dummyPasswordHash;

    public AutoRentAuthenticationProvider(
            UserAccountRepository userAccountRepository,
            IdentityAccessService identityAccessService,
            PasswordEncoder passwordEncoder,
            LoginSecurityProperties securityProperties,
            Clock clock
    ) {
        this.userAccountRepository = userAccountRepository;
        this.identityAccessService = identityAccessService;
        this.passwordEncoder = passwordEncoder;
        this.securityProperties = securityProperties;
        this.clock = clock;

        this.dummyPasswordHash = passwordEncoder.encode(
                "AutoRent-Pro-Dummy-Password-Never-Used"
        );
    }

    @Override
    @Transactional(
            noRollbackFor = AuthenticationException.class
    )
    public Authentication authenticate(
            Authentication authentication
    ) throws AuthenticationException {

        String email = normalizeEmail(
                authentication.getName()
        );

        Object credentials = authentication.getCredentials();

        if (!(credentials instanceof String rawPassword)
                || rawPassword.isBlank()) {
            throw authenticationFailed();
        }

        Optional<UserAccount> optionalUser =
                userAccountRepository
                        .findForAuthenticationByEmail(email);

        if (optionalUser.isEmpty()) {
            performDummyPasswordCheck(rawPassword);
            throw authenticationFailed();
        }

        UserAccount user = optionalUser.orElseThrow();

        Instant now = Instant.now(clock);

        if (!user.isActive()) {
            performDummyPasswordCheck(rawPassword);

            throw new DisabledException(
                    "User account is disabled"
            );
        }

        if (user.isTemporarilyLocked(now)) {
            performDummyPasswordCheck(rawPassword);

            throw new LockedException(
                    "User account is temporarily locked"
            );
        }

        if (!passwordEncoder.matches(
                rawPassword,
                user.getPasswordHash()
        )) {
            user.registerFailedLogin(
                    securityProperties.maxFailedAttempts(),
                    securityProperties.lockDuration(),
                    now
            );

            throw authenticationFailed();
        }

        user.recordSuccessfulLogin(now);

        ResolvedIdentityAccess access =
                identityAccessService.resolveForUser(
                        user.getId()
                );

        AuthenticatedUserPrincipal principal =
                new AuthenticatedUserPrincipal(
                        user.getId(),
                        user.getEmail(),
                        access.roles(),
                        access.permissions(),
                        user.isMustChangePassword()
                );

        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                buildAuthorities(access)
        );
    }

    @Override
    public boolean supports(
            Class<?> authentication
    ) {
        return UsernamePasswordAuthenticationToken.class
                .isAssignableFrom(authentication);
    }

    private Set<GrantedAuthority> buildAuthorities(
            ResolvedIdentityAccess access
    ) {
        Set<GrantedAuthority> authorities =
                new LinkedHashSet<>();

        for (RoleCode role : access.roles()) {
            authorities.add(
                    new SimpleGrantedAuthority(
                            "ROLE_" + role.name()
                    )
            );
        }

        for (PermissionGrant grant : access.permissions()) {
            authorities.add(
                    new SimpleGrantedAuthority(
                            "PERMISSION_"
                                    + grant.permission().name()
                                    + "_"
                                    + grant.scope().name()
                    )
            );
        }

        return authorities;
    }

    private void performDummyPasswordCheck(
            String rawPassword
    ) {
        passwordEncoder.matches(
                rawPassword,
                dummyPasswordHash
        );
    }

    private BadCredentialsException authenticationFailed() {
        return new BadCredentialsException(
                "Authentication failed"
        );
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return "";
        }

        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}