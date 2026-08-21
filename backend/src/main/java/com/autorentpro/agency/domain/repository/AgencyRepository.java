package com.autorentpro.agency.domain.repository;

import com.autorentpro.agency.domain.model.Agency;

import java.util.Optional;
import java.util.UUID;

public interface AgencyRepository {

    Agency save(Agency agency);

    Agency saveAndFlush(Agency agency);

    Optional<Agency> findById(UUID agencyId);

    Optional<Agency> findForUpdateById(UUID agencyId);

    Optional<Agency> findByCode(String code);

    boolean existsByCode(String code);
}