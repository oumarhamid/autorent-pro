package com.autorentpro.identity.api;

import com.autorentpro.identity.application.PasswordChangeService;
import com.autorentpro.identity.infrastructure.security.AuthenticatedUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final PasswordChangeService passwordChangeService;
    private final LogoutHandler logoutHandler;

    public AccountController(
            PasswordChangeService passwordChangeService,
            LogoutHandler logoutHandler
    ) {
        this.passwordChangeService =
                passwordChangeService;

        this.logoutHandler =
                logoutHandler;
    }

    @PostMapping("/change-password")
    @PreAuthorize(
            "@identityAuthorization.canAccessSelf("
                    + "authentication, "
                    + "'ACCOUNT_CHANGE_PASSWORD', "
                    + "authentication.principal.userId)"
    )
    public ResponseEntity<Void> changePassword(
            @Valid
            @RequestBody
            ChangePasswordRequest requestBody,

            @AuthenticationPrincipal
            AuthenticatedUserPrincipal principal,

            HttpServletRequest request,

            HttpServletResponse response,

            Authentication authentication
    ) {
        passwordChangeService.changePassword(
                principal.userId(),
                requestBody.currentPassword(),
                requestBody.newPassword()
        );

        logoutHandler.logout(
                request,
                response,
                authentication
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}