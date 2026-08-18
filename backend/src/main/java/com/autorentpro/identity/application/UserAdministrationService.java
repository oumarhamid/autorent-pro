package com.autorentpro.identity.application;

import com.autorentpro.identity.domain.model.Role;
import com.autorentpro.identity.domain.model.RoleCode;
import com.autorentpro.identity.domain.model.UserAccount;
import com.autorentpro.identity.domain.model.UserRole;
import com.autorentpro.identity.infrastructure.persistence.RoleRepository;
import com.autorentpro.identity.infrastructure.persistence.UserAccountRepository;
import com.autorentpro.identity.infrastructure.persistence.UserRoleRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class UserAdministrationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserAccountRepository userAccountRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy;
    private final Clock clock;

    public UserAdministrationService(
            UserAccountRepository userAccountRepository,
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicy passwordPolicy,
            Clock clock
    ) {
        this.userAccountRepository =
                userAccountRepository;

        this.userRoleRepository =
                userRoleRepository;

        this.roleRepository =
                roleRepository;

        this.passwordEncoder =
                passwordEncoder;

        this.passwordPolicy =
                passwordPolicy;

        this.clock =
                clock;
    }

    @Transactional
    public UserView createUser(
            String email,
            String temporaryPassword,
            RoleCode initialRole
    ) {
        String normalizedEmail =
                normalizeEmail(email);

        passwordPolicy.validate(
                temporaryPassword
        );

        if (userAccountRepository.existsByEmail(
                normalizedEmail
        )) {
            throw duplicateEmail();
        }

        Role role =
                roleRepository
                        .findByCode(initialRole)
                        .orElseThrow(
                                () ->
                                        new UserManagementException(
                                                "ROLE_NOT_AVAILABLE",
                                                "The requested role is not available."
                                        )
                        );

        UserAccount user =
                UserAccount.create(
                        normalizedEmail,
                        passwordEncoder.encode(
                                temporaryPassword
                        ),
                        true,
                        Instant.now(clock)
                );

        try {
            userAccountRepository
                    .saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw duplicateEmail();
        }

        userRoleRepository.saveAndFlush(
                UserRole.assign(
                        user,
                        role
                )
        );

        return toView(
                user,
                Set.of(initialRole)
        );
    }

    @Transactional(readOnly = true)
    public UserView getUser(
            UUID userId
    ) {
        UserAccount user =
                userAccountRepository
                        .findById(userId)
                        .orElseThrow(
                                this::userNotFound
                        );

        Set<RoleCode> roles =
                Set.copyOf(
                        userRoleRepository
                                .findRoleCodesByUserId(
                                        userId
                                )
                );

        return toView(
                user,
                roles
        );
    }

    @Transactional(readOnly = true)
    public UserPage listUsers(
            int page,
            int size
    ) {
        if (page < 0) {
            throw new UserManagementException(
                    "INVALID_PAGE",
                    "Page must be greater than or equal to zero."
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new UserManagementException(
                    "INVALID_PAGE_SIZE",
                    "Page size must be between 1 and "
                            + MAX_PAGE_SIZE
                            + "."
            );
        }

        Page<UserAccount> users =
                userAccountRepository.findAll(
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(
                                        Sort.Direction.DESC,
                                        "createdAt"
                                )
                        )
                );

        List<UUID> userIds =
                users.getContent()
                        .stream()
                        .map(UserAccount::getId)
                        .toList();

        Map<UUID, Set<RoleCode>> rolesByUser =
                loadRoles(userIds);

        List<UserView> items =
                users.getContent()
                        .stream()
                        .map(
                                user ->
                                        toView(
                                                user,
                                                rolesByUser.getOrDefault(
                                                        user.getId(),
                                                        Set.of()
                                                )
                                        )
                        )
                        .toList();

        return new UserPage(
                items,
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages()
        );
    }

    @Transactional
    public UserView disableUser(
            UUID authenticatedUserId,
            UUID targetUserId
    ) {
        if (authenticatedUserId == null) {
            throw new UserManagementException(
                    "AUTHENTICATED_USER_REQUIRED",
                    "The authenticated user is required."
            );
        }

        if (targetUserId == null) {
            throw userNotFound();
        }

        if (authenticatedUserId.equals(targetUserId)) {
            throw new UserManagementException(
                    "SELF_DISABLE_NOT_ALLOWED",
                    "You cannot disable your own account."
            );
        }

        UserAccount user =
                userAccountRepository
                        .findForUpdateById(targetUserId)
                        .orElseThrow(
                                this::userNotFound
                        );

        user.disable();

        userAccountRepository
                .saveAndFlush(user);

        return toView(
                user,
                loadUserRoles(targetUserId)
        );
    }

    @Transactional
    public UserView enableUser(
            UUID targetUserId
    ) {
        if (targetUserId == null) {
            throw userNotFound();
        }

        UserAccount user =
                userAccountRepository
                        .findForUpdateById(targetUserId)
                        .orElseThrow(
                                this::userNotFound
                        );

        user.enable();

        userAccountRepository
                .saveAndFlush(user);

        return toView(
                user,
                loadUserRoles(targetUserId)
        );
    }

    private Set<RoleCode> loadUserRoles(
            UUID userId
    ) {
        return Set.copyOf(
                userRoleRepository
                        .findRoleCodesByUserId(
                                userId
                        )
        );
    }

    private Map<UUID, Set<RoleCode>> loadRoles(
            Collection<UUID> userIds
    ) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<UUID, Set<RoleCode>> result =
                new HashMap<>();

        for (UserRoleAssignmentView assignment
                : userRoleRepository
                        .findRoleAssignmentsByUserIds(
                                userIds
                        )) {

            result.computeIfAbsent(
                    assignment.userId(),
                    ignored -> new HashSet<>()
            ).add(
                    assignment.role()
            );
        }

        return result;
    }

    private UserView toView(
            UserAccount user,
            Set<RoleCode> roles
    ) {
        return new UserView(
                user.getId(),
                user.getEmail(),
                user.getStatus(),
                user.isMustChangePassword(),
                roles,
                user.getLockedUntil(),
                user.getLastLoginAt(),
                user.getPasswordChangedAt(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    private String normalizeEmail(
            String email
    ) {
        if (email == null) {
            return "";
        }

        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private UserManagementException duplicateEmail() {
        return new UserManagementException(
                "EMAIL_ALREADY_IN_USE",
                "An account already exists for this email."
        );
    }

    private UserManagementException userNotFound() {
        return new UserManagementException(
                "USER_NOT_FOUND",
                "The requested user was not found."
        );
    }

    public record UserPage(
            List<UserView> items,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
        public UserPage {
            items = List.copyOf(items);
        }
    }
}