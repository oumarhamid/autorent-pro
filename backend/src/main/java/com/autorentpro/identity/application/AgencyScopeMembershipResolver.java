package com.autorentpro.identity.application;

import java.util.UUID;

public interface AgencyScopeMembershipResolver {

    boolean hasActiveMembership(
            UUID userId,
            UUID agencyId
    );
}