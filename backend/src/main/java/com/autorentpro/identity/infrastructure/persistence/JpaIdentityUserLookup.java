package com.autorentpro.identity.infrastructure.persistence;

import com.autorentpro.identity.application.IdentityUserLookup;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Component
public class JpaIdentityUserLookup
        implements IdentityUserLookup {

    private final UserAccountRepository userAccountRepository;

    public JpaIdentityUserLookup(
            UserAccountRepository userAccountRepository
    ) {
        this.userAccountRepository =
                userAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(
            UUID userId
    ) {
        Objects.requireNonNull(
                userId,
                "userId must not be null"
        );

        return userAccountRepository.existsById(
                userId
        );
    }
}