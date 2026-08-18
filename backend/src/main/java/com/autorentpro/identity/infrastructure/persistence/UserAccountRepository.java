package com.autorentpro.identity.infrastructure.persistence;

import com.autorentpro.identity.domain.model.UserAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository
        extends JpaRepository<UserAccount, UUID> {

    Optional<UserAccount> findByEmail(String email);

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select user
            from UserAccount user
            where user.email = :email
            """)
    Optional<UserAccount> findForAuthenticationByEmail(
            @Param("email") String email
    );
}