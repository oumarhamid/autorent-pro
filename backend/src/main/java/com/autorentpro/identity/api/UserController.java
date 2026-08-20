package com.autorentpro.identity.api;

import com.autorentpro.identity.application.UserAdministrationService;
import com.autorentpro.identity.application.UserView;
import com.autorentpro.identity.domain.model.RoleCode;
import com.autorentpro.identity.infrastructure.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserAdministrationService userAdministrationService;

    public UserController(
            UserAdministrationService userAdministrationService
    ) {
        this.userAdministrationService =
                userAdministrationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(
            "@identityAuthorization.canAccessGlobal("
                    + "authentication, 'USER_CREATE') "
                    + "and "
                    + "@identityAuthorization.canAccessGlobal("
                    + "authentication, 'USER_ROLE_ASSIGN')"
    )
    public UserResponse createUser(
            @Valid
            @RequestBody
            CreateUserRequest request
    ) {
        UserView created =
                userAdministrationService
                        .createUser(
                                request.email(),
                                request.temporaryPassword(),
                                request.initialRole()
                        );

        return UserResponse.from(
                created
        );
    }

    @GetMapping("/{userId}")
    @PreAuthorize(
            "@identityAuthorization.canAccessGlobal("
                    + "authentication, 'USER_READ')"
    )
    public UserResponse getUser(
            @PathVariable
            UUID userId
    ) {
        return UserResponse.from(
                userAdministrationService
                        .getUser(userId)
        );
    }

    @GetMapping
    @PreAuthorize(
            "@identityAuthorization.canAccessGlobal("
                    + "authentication, 'USER_READ')"
    )
    public UserPageResponse listUsers(
            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size
    ) {
        return UserPageResponse.from(
                userAdministrationService
                        .listUsers(
                                page,
                                size
                        )
        );
    }

    @PostMapping("/{userId}/disable")
    @PreAuthorize(
            "@identityAuthorization.canAccessGlobal("
                    + "authentication, 'USER_DISABLE')"
    )
    public UserResponse disableUser(
            @PathVariable
            UUID userId,

            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal
    ) {
        return UserResponse.from(
                userAdministrationService
                        .disableUser(
                                principal.userId(),
                                userId
                        )
        );
    }

    @PostMapping("/{userId}/enable")
    @PreAuthorize(
            "@identityAuthorization.canAccessGlobal("
                    + "authentication, 'USER_ENABLE')"
    )
    public UserResponse enableUser(
            @PathVariable
            UUID userId
    ) {
        return UserResponse.from(
                userAdministrationService
                        .enableUser(userId)
        );
    }

    @PutMapping("/{userId}/roles/{role}")
    @PreAuthorize(
            "@identityAuthorization.canAccessGlobal("
                    + "authentication, 'USER_ROLE_ASSIGN')"
    )
    public UserResponse assignRole(
            @PathVariable
            UUID userId,

            @PathVariable
            RoleCode role
    ) {
        return UserResponse.from(
                userAdministrationService
                        .assignRole(
                                userId,
                                role
                        )
        );
    }

    @DeleteMapping("/{userId}/roles/{role}")
    @PreAuthorize(
            "@identityAuthorization.canAccessGlobal("
                    + "authentication, 'USER_ROLE_ASSIGN')"
    )
    public UserResponse removeRole(
            @PathVariable
            UUID userId,

            @PathVariable
            RoleCode role,

            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal
    ) {
        return UserResponse.from(
                userAdministrationService
                        .removeRole(
                                principal.userId(),
                                userId,
                                role
                        )
        );
    }
}