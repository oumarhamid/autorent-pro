package com.autorentpro.agency.infrastructure.persistence;

import com.autorentpro.agency.domain.model.Agency;
import com.autorentpro.agency.domain.repository.AgencyRepository;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaAgencyRepositoryAdapter
        implements AgencyRepository {

    private final SpringDataAgencyRepository repository;

    public JpaAgencyRepositoryAdapter(
            SpringDataAgencyRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public Agency save(Agency agency) {
        Objects.requireNonNull(
                agency,
                "agency must not be null"
        );

        return repository.save(agency);
    }

    @Override
    public Agency saveAndFlush(Agency agency) {
        Objects.requireNonNull(
                agency,
                "agency must not be null"
        );

        return repository.saveAndFlush(agency);
    }

    @Override
    public Optional<Agency> findById(
            UUID agencyId
    ) {
        Objects.requireNonNull(
                agencyId,
                "agencyId must not be null"
        );

        return repository.findById(agencyId);
    }

    @Override
    public Optional<Agency> findForUpdateById(
            UUID agencyId
    ) {
        Objects.requireNonNull(
                agencyId,
                "agencyId must not be null"
        );

        return repository.findForUpdateById(
                agencyId
        );
    }

    @Override
    public Optional<Agency> findByCode(
            String code
    ) {
        Objects.requireNonNull(
                code,
                "code must not be null"
        );

        return repository.findByCode(code);
    }

    @Override
    public boolean existsByCode(
            String code
    ) {
        Objects.requireNonNull(
                code,
                "code must not be null"
        );

        return repository.existsByCode(code);
    }
}