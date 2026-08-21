package com.autorentpro.agency.infrastructure.persistence;

import com.autorentpro.agency.domain.model.Agency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataAgencyRepository
        extends JpaRepository<Agency, UUID> {

    Optional<Agency> findByCode(String code);

    boolean existsByCode(String code);
}