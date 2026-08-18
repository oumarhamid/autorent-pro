package com.autorentpro.identity.infrastructure.persistence;

import com.autorentpro.identity.application.PermissionGrant;
import com.autorentpro.identity.domain.model.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RolePermissionRepository
        extends JpaRepository<RolePermission, UUID> {

    @Query("""
            select new com.autorentpro.identity.application.PermissionGrant(
                rp.permission.code,
                rp.scope
            )
            from RolePermission rp
            where rp.role.id in (
                select ur.role.id
                from UserRole ur
                where ur.user.id = :userId
            )
            """)
    List<PermissionGrant> findPermissionGrantsByUserId(
            @Param("userId") UUID userId
    );
}