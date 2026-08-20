package com.autorentpro.identity.application;

import com.autorentpro.identity.domain.model.RoleCode;
import com.autorentpro.identity.infrastructure.persistence.RolePermissionRepository;
import com.autorentpro.identity.infrastructure.persistence.UserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class IdentityAccessService {

    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public IdentityAccessService(
            UserRoleRepository userRoleRepository,
            RolePermissionRepository rolePermissionRepository
    ) {
        this.userRoleRepository = userRoleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Transactional(readOnly = true)
    public ResolvedIdentityAccess resolveForUser(UUID userId) {
        Set<RoleCode> roles = new HashSet<>(
                userRoleRepository.findRoleCodesByUserId(userId)
        );

        Set<PermissionGrant> permissions = new HashSet<>(
                rolePermissionRepository.findPermissionGrantsByUserId(userId)
        );

        return new ResolvedIdentityAccess(
                roles,
                permissions
        );
    }
}