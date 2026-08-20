package com.autorentpro.identity.infrastructure.persistence;

import com.autorentpro.identity.application.UserRoleAssignmentView;
import com.autorentpro.identity.domain.model.RoleCode;
import com.autorentpro.identity.domain.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository
        extends JpaRepository<UserRole, UUID> {

    @Query("""
            select ur.role.code
            from UserRole ur
            where ur.user.id = :userId
            """)
    List<RoleCode> findRoleCodesByUserId(
            @Param("userId") UUID userId
    );

    @Query("""
            select new com.autorentpro.identity.application.UserRoleAssignmentView(
                ur.user.id,
                ur.role.code
            )
            from UserRole ur
            where ur.user.id in :userIds
            """)
    List<UserRoleAssignmentView> findRoleAssignmentsByUserIds(
            @Param("userIds") Collection<UUID> userIds
    );

    boolean existsByUserIdAndRoleId(
            UUID userId,
            UUID roleId
    );

    Optional<UserRole> findByUserIdAndRoleId(
            UUID userId,
            UUID roleId
    );

    @Query("""
            select count(ur)
            from UserRole ur
            where ur.role.code = :roleCode
            """)
    long countByRoleCode(
            @Param("roleCode") RoleCode roleCode
    );
}