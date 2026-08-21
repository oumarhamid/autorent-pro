package com.autorentpro.agency.infrastructure.persistence;

import com.autorentpro.agency.domain.model.Agency;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataAgencyRepository
        extends JpaRepository<Agency, UUID> {

    Optional<Agency> findByCode(String code);

    boolean existsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select agency
            from Agency agency
            where agency.id = :agencyId
            """)
    Optional<Agency> findForUpdateById(
            @Param("agencyId") UUID agencyId
    );
}