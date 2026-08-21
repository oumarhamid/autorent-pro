package com.autorentpro.agency.infrastructure.persistence;

import com.autorentpro.agency.domain.model.UserAgencyAssignment;
import com.autorentpro.agency.domain.repository.UserAgencyAssignmentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaUserAgencyAssignmentRepositoryAdapter
        implements UserAgencyAssignmentRepository {

    private final SpringDataUserAgencyAssignmentRepository repository;

    public JpaUserAgencyAssignmentRepositoryAdapter(
            SpringDataUserAgencyAssignmentRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public UserAgencyAssignment save(
            UserAgencyAssignment assignment
    ) {
        Objects.requireNonNull(
                assignment,
                "assignment must not be null"
        );

        return repository.save(assignment);
    }

    @Override
    public UserAgencyAssignment saveAndFlush(
            UserAgencyAssignment assignment
    ) {
        Objects.requireNonNull(
                assignment,
                "assignment must not be null"
        );

        return repository.saveAndFlush(assignment);
    }

    @Override
    public Optional<UserAgencyAssignment>
    findByUserIdAndAgencyId(
            UUID userId,
            UUID agencyId
    ) {
        requireIds(userId, agencyId);

        return repository.findByUserIdAndAgencyId(
                userId,
                agencyId
        );
    }

    @Override
    public Optional<UserAgencyAssignment>
    findForUpdateByUserIdAndAgencyId(
            UUID userId,
            UUID agencyId
    ) {
        requireIds(userId, agencyId);

        return repository.findForUpdateByUserIdAndAgencyId(
                userId,
                agencyId
        );
    }

    @Override
    public List<UserAgencyAssignment>
    findAllActiveByAgencyId(
            UUID agencyId
    ) {
        Objects.requireNonNull(
                agencyId,
                "agencyId must not be null"
        );

        return repository
                .findAllByAgencyIdAndActiveTrueOrderByAssignedAtAsc(
                        agencyId
                );
    }

    @Override
    public boolean existsActiveByUserIdAndAgencyId(
            UUID userId,
            UUID agencyId
    ) {
        requireIds(userId, agencyId);

        return repository
                .existsByUserIdAndAgencyIdAndActiveTrue(
                        userId,
                        agencyId
                );
    }

    private static void requireIds(
            UUID userId,
            UUID agencyId
    ) {
        Objects.requireNonNull(
                userId,
                "userId must not be null"
        );

        Objects.requireNonNull(
                agencyId,
                "agencyId must not be null"
        );
    }
}