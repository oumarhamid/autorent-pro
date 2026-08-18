package com.autorentpro.identity.infrastructure.persistence;

import com.autorentpro.identity.domain.model.RoleCode;
import com.autorentpro.identity.domain.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    boolean existsByUserIdAndRoleId(
            UUID userId,
            UUID roleId
    );
}