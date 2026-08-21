package com.autorentpro.identity.application;

import java.util.UUID;

public interface IdentityUserLookup {

    boolean existsById(UUID userId);
}