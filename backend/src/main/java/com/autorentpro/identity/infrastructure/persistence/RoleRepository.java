package com.autorentpro.identity.infrastructure.persistence;

import com.autorentpro.identity.domain.model.Role;
import com.autorentpro.identity.domain.model.RoleCode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository
        extends JpaRepository<Role, UUID> {

    Optional<Role> findByCode(
            RoleCode code
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select role
            from Role role
            where role.code = :code
            """)
    Optional<Role> findForUpdateByCode(
            @Param("code") RoleCode code
    );
}