package com.autorentpro.agency.api;

import com.autorentpro.agency.application.AgencyAdministrationService;
import com.autorentpro.agency.application.AgencyView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/agencies")
public class AgencyController {

    private final AgencyAdministrationService
            agencyAdministrationService;

    public AgencyController(
            AgencyAdministrationService
                    agencyAdministrationService
    ) {
        this.agencyAdministrationService =
                agencyAdministrationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(
            "@identityAuthorization.canAccessGlobal("
                    + "authentication, 'AGENCY_CREATE')"
    )
    public AgencyResponse createAgency(
            @Valid
            @RequestBody
            CreateAgencyRequest request
    ) {
        AgencyView created =
                agencyAdministrationService
                        .createAgency(
                                request.code(),
                                request.name(),
                                request.addressLine1(),
                                request.addressLine2(),
                                request.city(),
                                request.postalCode(),
                                request.countryCode(),
                                request.phone(),
                                request.email(),
                                request.timeZone()
                        );

        return AgencyResponse.from(
                created
        );
    }

    @GetMapping("/{agencyId}")
    @PreAuthorize(
            "@identityAuthorization.canAccessAgency("
                    + "authentication, "
                    + "'AGENCY_READ', "
                    + "#agencyId)"
    )
    public AgencyResponse getAgency(
            @PathVariable
            UUID agencyId
    ) {
        return AgencyResponse.from(
                agencyAdministrationService
                        .getAgency(
                                agencyId
                        )
        );
    }

    @PutMapping("/{agencyId}")
    @PreAuthorize(
            "@identityAuthorization.canAccessAgency("
                    + "authentication, "
                    + "'AGENCY_UPDATE', "
                    + "#agencyId)"
    )
    public AgencyResponse updateAgency(
            @PathVariable
            UUID agencyId,

            @Valid
            @RequestBody
            UpdateAgencyRequest request
    ) {
        return AgencyResponse.from(
                agencyAdministrationService
                        .updateAgency(
                                agencyId,
                                request.name(),
                                request.addressLine1(),
                                request.addressLine2(),
                                request.city(),
                                request.postalCode(),
                                request.countryCode(),
                                request.phone(),
                                request.email(),
                                request.timeZone()
                        )
        );
    }

    @PostMapping("/{agencyId}/enable")
    @PreAuthorize(
            "@identityAuthorization.canAccessGlobal("
                    + "authentication, 'AGENCY_ENABLE')"
    )
    public AgencyResponse enableAgency(
            @PathVariable
            UUID agencyId
    ) {
        return AgencyResponse.from(
                agencyAdministrationService
                        .activateAgency(
                                agencyId
                        )
        );
    }

    @PostMapping("/{agencyId}/disable")
    @PreAuthorize(
            "@identityAuthorization.canAccessGlobal("
                    + "authentication, 'AGENCY_DISABLE')"
    )
    public AgencyResponse disableAgency(
            @PathVariable
            UUID agencyId
    ) {
        return AgencyResponse.from(
                agencyAdministrationService
                        .deactivateAgency(
                                agencyId
                        )
        );
    }
}