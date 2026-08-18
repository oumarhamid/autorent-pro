package com.autorentpro.identity.application;

import com.autorentpro.identity.domain.model.RoleCode;
import com.autorentpro.identity.infrastructure.persistence.RoleRepository;
import com.autorentpro.identity.infrastructure.persistence.UserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InitialAdminBootstrapService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserAdministrationService userAdministrationService;

    public InitialAdminBootstrapService(
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository,
            UserAdministrationService userAdministrationService
    ) {
        this.roleRepository =
                roleRepository;

        this.userRoleRepository =
                userRoleRepository;

        this.userAdministrationService =
                userAdministrationService;
    }

    @Transactional
    public BootstrapResult bootstrap(
            String email,
            String temporaryPassword
    ) {
        roleRepository
                .findForUpdateByCode(
                        RoleCode.ADMIN
                )
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "The ADMIN role is not available."
                                )
                );

        if (userRoleRepository
                .countByRoleCode(
                        RoleCode.ADMIN
                ) > 0) {

            return BootstrapResult.ALREADY_EXISTS;
        }

        userAdministrationService
                .createUser(
                        email,
                        temporaryPassword,
                        RoleCode.ADMIN
                );

        return BootstrapResult.CREATED;
    }

    public enum BootstrapResult {
        CREATED,
        ALREADY_EXISTS
    }
}