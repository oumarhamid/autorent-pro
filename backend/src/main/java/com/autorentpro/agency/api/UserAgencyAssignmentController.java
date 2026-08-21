package com.autorentpro.agency.api;

import com.autorentpro.agency.application.UserAgencyAssignmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/agencies/{agencyId}/users")
public class UserAgencyAssignmentController {

    private final UserAgencyAssignmentService
            userAgencyAssignmentService;

    public UserAgencyAssignmentController(
            UserAgencyAssignmentService
                    userAgencyAssignmentService
    ) {
        this.userAgencyAssignmentService =
                userAgencyAssignmentService;
    }

    @PostMapping("/{userId}")
    @PreAuthorize(
            "@identityAuthorization.canAccessGlobal("
                    + "authentication, "
                    + "'USER_AGENCY_ASSIGN')"
    )
    public UserAgencyAssignmentResponse assignUser(
            @PathVariable
            UUID agencyId,
            @PathVariable
            UUID userId
    ) {
        return UserAgencyAssignmentResponse.from(
                userAgencyAssignmentService
                        .assignUserToAgency(
                                userId,
                                agencyId
                        )
        );
    }

    @PostMapping("/{userId}/remove")
    @PreAuthorize(
            "@identityAuthorization.canAccessGlobal("
                    + "authentication, "
                    + "'USER_AGENCY_ASSIGN')"
    )
    public UserAgencyAssignmentResponse removeUser(
            @PathVariable
            UUID agencyId,
            @PathVariable
            UUID userId
    ) {
        return UserAgencyAssignmentResponse.from(
                userAgencyAssignmentService
                        .removeUserFromAgency(
                                userId,
                                agencyId
                        )
        );
    }

    @GetMapping
    @PreAuthorize(
            "@identityAuthorization.canAccessGlobal("
                    + "authentication, "
                    + "'USER_AGENCY_ASSIGN')"
    )
    public List<UserAgencyAssignmentResponse>
    listActiveAssignments(
            @PathVariable
            UUID agencyId
    ) {
        return userAgencyAssignmentService
                .listActiveAssignments(
                        agencyId
                )
                .stream()
                .map(
                        UserAgencyAssignmentResponse::from
                )
                .toList();
    }
}