package com.autorentpro.identity.api;

import com.autorentpro.identity.application.UserAdministrationService;
import com.autorentpro.identity.application.UserView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
}