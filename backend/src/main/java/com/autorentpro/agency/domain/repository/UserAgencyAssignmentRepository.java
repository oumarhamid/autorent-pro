package com.autorentpro.agency.domain.repository;

import com.autorentpro.agency.domain.model.UserAgencyAssignment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserAgencyAssignmentRepository {

    UserAgencyAssignment save(
            UserAgencyAssignment assignment
    );

    UserAgencyAssignment saveAndFlush(
            UserAgencyAssignment assignment
    );

    Optional<UserAgencyAssignment> findByUserIdAndAgencyId(
            UUID userId,
            UUID agencyId
    );

    Optional<UserAgencyAssignment> findForUpdateByUserIdAndAgencyId(
            UUID userId,
            UUID agencyId
    );

    List<UserAgencyAssignment> findAllActiveByAgencyId(
            UUID agencyId
    );

    boolean existsActiveByUserIdAndAgencyId(
            UUID userId,
            UUID agencyId
    );
}