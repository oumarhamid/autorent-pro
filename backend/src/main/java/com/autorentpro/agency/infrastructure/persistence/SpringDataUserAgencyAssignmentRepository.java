package com.autorentpro.agency.infrastructure.persistence;

import com.autorentpro.agency.domain.model.UserAgencyAssignment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataUserAgencyAssignmentRepository
        extends JpaRepository<UserAgencyAssignment, UUID> {

    Optional<UserAgencyAssignment> findByUserIdAndAgencyId(
            UUID userId,
            UUID agencyId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select assignment
            from UserAgencyAssignment assignment
            where assignment.userId = :userId
              and assignment.agencyId = :agencyId
            """)
    Optional<UserAgencyAssignment> findForUpdateByUserIdAndAgencyId(
            @Param("userId") UUID userId,
            @Param("agencyId") UUID agencyId
    );

    List<UserAgencyAssignment>
    findAllByAgencyIdAndActiveTrueOrderByAssignedAtAsc(
            UUID agencyId
    );

    boolean existsByUserIdAndAgencyIdAndActiveTrue(
            UUID userId,
            UUID agencyId
    );
}