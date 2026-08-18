package com.autorentpro.identity.application;

import java.util.Objects;
import java.util.UUID;

public sealed interface AuthorizationScope
        permits AuthorizationScope.Self,
                AuthorizationScope.Agency,
                AuthorizationScope.Global {

    record Self(UUID userId)
            implements AuthorizationScope {

        public Self {
            Objects.requireNonNull(
                    userId,
                    "userId must not be null"
            );
        }
    }

    record Agency(UUID agencyId)
            implements AuthorizationScope {

        public Agency {
            Objects.requireNonNull(
                    agencyId,
                    "agencyId must not be null"
            );
        }
    }

    record Global()
            implements AuthorizationScope {
    }

    static Self self(UUID userId) {
        return new Self(userId);
    }

    static Agency agency(UUID agencyId) {
        return new Agency(agencyId);
    }

    static Global global() {
        return new Global();
    }
}