package com.autorentpro.agency.infrastructure.authorization;

import com.autorentpro.agency.domain.repository.UserAgencyAssignmentRepository;
import com.autorentpro.identity.application.AgencyScopeMembershipResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class JpaAgencyScopeMembershipResolver
        implements AgencyScopeMembershipResolver {

    private final UserAgencyAssignmentRepository
            userAgencyAssignmentRepository;

    public JpaAgencyScopeMembershipResolver(
            UserAgencyAssignmentRepository
                    userAgencyAssignmentRepository
    ) {
        this.userAgencyAssignmentRepository =
                userAgencyAssignmentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveMembership(
            UUID userId,
            UUID agencyId
    ) {
        if (userId == null
                || agencyId == null) {
            return false;
        }

        return userAgencyAssignmentRepository
                .existsActiveByUserIdAndAgencyId(
                        userId,
                        agencyId
                );
    }
}