package com.autorentpro.identity.application;

import com.autorentpro.identity.domain.model.UserAccount;
import com.autorentpro.identity.infrastructure.persistence.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ActiveAccountStatusService {

    private final UserAccountRepository userAccountRepository;

    public ActiveAccountStatusService(
            UserAccountRepository userAccountRepository
    ) {
        this.userAccountRepository =
                userAccountRepository;
    }

    @Transactional(readOnly = true)
    public boolean isActive(
            UUID userId
    ) {
        if (userId == null) {
            return false;
        }

        return userAccountRepository
                .findById(userId)
                .map(UserAccount::isActive)
                .orElse(false);
    }
}