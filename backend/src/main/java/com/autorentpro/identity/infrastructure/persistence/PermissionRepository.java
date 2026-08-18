package com.autorentpro.identity.infrastructure.persistence;

import com.autorentpro.identity.domain.model.Permission;
import com.autorentpro.identity.domain.model.PermissionCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository
        extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByCode(PermissionCode code);
}