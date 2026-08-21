package com.autorentpro.agency.application;

import com.autorentpro.agency.domain.model.Agency;
import com.autorentpro.agency.domain.repository.AgencyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AgencyAdministrationService {

    private final AgencyRepository agencyRepository;

    public AgencyAdministrationService(
            AgencyRepository agencyRepository
    ) {
        this.agencyRepository =
                agencyRepository;
    }

    @Transactional
    public AgencyView createAgency(
            String code,
            String name,
            String addressLine1,
            String addressLine2,
            String city,
            String postalCode,
            String countryCode,
            String phone,
            String email,
            String timeZone
    ) {
        Agency agency =
                Agency.create(
                        code,
                        name,
                        addressLine1,
                        addressLine2,
                        city,
                        postalCode,
                        countryCode,
                        phone,
                        email,
                        timeZone
                );

        if (agencyRepository.existsByCode(
                agency.getCode()
        )) {
            throw duplicateCode();
        }

        try {
            Agency saved =
                    agencyRepository
                            .saveAndFlush(
                                    agency
                            );

            return toView(saved);
        } catch (DataIntegrityViolationException exception) {
            /*
             * The domain has already validated the other
             * structural invariants before persistence.
             *
             * The remaining expected concurrent conflict
             * during creation is the unique business code.
             */
            throw duplicateCode();
        }
    }

    @Transactional(readOnly = true)
    public AgencyView getAgency(
            UUID agencyId
    ) {
        if (agencyId == null) {
            throw agencyNotFound();
        }

        Agency agency =
                agencyRepository
                        .findById(agencyId)
                        .orElseThrow(
                                this::agencyNotFound
                        );

        return toView(agency);
    }

    @Transactional
    public AgencyView updateAgency(
            UUID agencyId,
            String name,
            String addressLine1,
            String addressLine2,
            String city,
            String postalCode,
            String countryCode,
            String phone,
            String email,
            String timeZone
    ) {
        Agency agency =
                findForUpdate(
                        agencyId
                );

        agency.updateDetails(
                name,
                addressLine1,
                addressLine2,
                city,
                postalCode,
                countryCode,
                phone,
                email,
                timeZone
        );

        Agency saved =
                agencyRepository
                        .saveAndFlush(
                                agency
                        );

        return toView(saved);
    }

    @Transactional
    public AgencyView activateAgency(
            UUID agencyId
    ) {
        Agency agency =
                findForUpdate(
                        agencyId
                );

        agency.activate();

        Agency saved =
                agencyRepository
                        .saveAndFlush(
                                agency
                        );

        return toView(saved);
    }

    @Transactional
    public AgencyView deactivateAgency(
            UUID agencyId
    ) {
        Agency agency =
                findForUpdate(
                        agencyId
                );

        /*
         * Guards involving active staff assignments,
         * operational vehicles, reservations and rentals
         * will be introduced by the modules that own
         * those concepts.
         */
        agency.deactivate();

        Agency saved =
                agencyRepository
                        .saveAndFlush(
                                agency
                        );

        return toView(saved);
    }

    private Agency findForUpdate(
            UUID agencyId
    ) {
        if (agencyId == null) {
            throw agencyNotFound();
        }

        return agencyRepository
                .findForUpdateById(
                        agencyId
                )
                .orElseThrow(
                        this::agencyNotFound
                );
    }

    private AgencyView toView(
            Agency agency
    ) {
        return new AgencyView(
                agency.getId(),
                agency.getCode(),
                agency.getName(),
                agency.getAddressLine1(),
                agency.getAddressLine2(),
                agency.getCity(),
                agency.getPostalCode(),
                agency.getCountryCode(),
                agency.getPhone(),
                agency.getEmail(),
                agency.getTimeZone(),
                agency.getStatus(),
                agency.getCreatedAt(),
                agency.getUpdatedAt()
        );
    }

    private AgencyManagementException duplicateCode() {
        return new AgencyManagementException(
                "AGENCY_CODE_ALREADY_IN_USE",
                "An agency already exists with this code."
        );
    }

    private AgencyManagementException agencyNotFound() {
        return new AgencyManagementException(
                "AGENCY_NOT_FOUND",
                "The requested agency was not found."
        );
    }
}