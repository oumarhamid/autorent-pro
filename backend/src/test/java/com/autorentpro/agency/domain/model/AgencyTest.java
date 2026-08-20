package com.autorentpro.agency.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgencyTest {

    @Test
    void createsAgencyWithNormalizedValues() {
        Agency agency = Agency.create(
                " casa-ain-diab ",
                " Agence Aïn Diab ",
                " 10 Boulevard de la Corniche ",
                "   ",
                " Casablanca ",
                " 20000 ",
                " ma ",
                " +212500000000 ",
                " CONTACT@AUTORENT.MA ",
                "Africa/Casablanca"
        );

        assertNotNull(agency.getId());
        assertEquals("CASA-AIN-DIAB", agency.getCode());
        assertEquals("Agence Aïn Diab", agency.getName());
        assertEquals("10 Boulevard de la Corniche", agency.getAddressLine1());
        assertNull(agency.getAddressLine2());
        assertEquals("Casablanca", agency.getCity());
        assertEquals("20000", agency.getPostalCode());
        assertEquals("MA", agency.getCountryCode());
        assertEquals("+212500000000", agency.getPhone());
        assertEquals("contact@autorent.ma", agency.getEmail());
        assertEquals("Africa/Casablanca", agency.getTimeZone());
        assertEquals(AgencyStatus.ACTIVE, agency.getStatus());
    }

    @Test
    void rejectsInvalidCountryCode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Agency.create(
                        "CASA-CENTRE",
                        "Casablanca Centre",
                        "1 Avenue Hassan II",
                        null,
                        "Casablanca",
                        null,
                        "MAR",
                        null,
                        null,
                        "Africa/Casablanca"
                )
        );
    }

    @Test
    void rejectsInvalidTimeZone() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Agency.create(
                        "CASA-CENTRE",
                        "Casablanca Centre",
                        "1 Avenue Hassan II",
                        null,
                        "Casablanca",
                        null,
                        "MA",
                        null,
                        null,
                        "Invalid/Timezone"
                )
        );
    }

    @Test
    void rejectsBlankRequiredField() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Agency.create(
                        "CASA-CENTRE",
                        "   ",
                        "1 Avenue Hassan II",
                        null,
                        "Casablanca",
                        null,
                        "MA",
                        null,
                        null,
                        "Africa/Casablanca"
                )
        );
    }

    @Test
    void rejectsOverlongCode() {
        String code = "A".repeat(65);

        assertThrows(
                IllegalArgumentException.class,
                () -> Agency.create(
                        code,
                        "Casablanca Centre",
                        "1 Avenue Hassan II",
                        null,
                        "Casablanca",
                        null,
                        "MA",
                        null,
                        null,
                        "Africa/Casablanca"
                )
        );
    }

    @Test
    void updatesDetailsWithoutChangingIdentityOrCode() {
        Agency agency = createAgency();

        UUID id = agency.getId();
        String code = agency.getCode();

        agency.updateDetails(
                "Casablanca Centre Updated",
                "25 Avenue Hassan II",
                "Étage 2",
                "Casablanca",
                "20250",
                "MA",
                "+212511111111",
                "updated@autorent.ma",
                "Africa/Casablanca"
        );

        assertEquals(id, agency.getId());
        assertEquals(code, agency.getCode());
        assertEquals("Casablanca Centre Updated", agency.getName());
        assertEquals("25 Avenue Hassan II", agency.getAddressLine1());
        assertEquals("Étage 2", agency.getAddressLine2());
        assertEquals("20250", agency.getPostalCode());
        assertEquals("updated@autorent.ma", agency.getEmail());
    }

    @Test
    void deactivatesAndReactivatesAgency() {
        Agency agency = createAgency();

        assertEquals(AgencyStatus.ACTIVE, agency.getStatus());

        agency.deactivate();
        assertEquals(AgencyStatus.INACTIVE, agency.getStatus());

        agency.activate();
        assertEquals(AgencyStatus.ACTIVE, agency.getStatus());
    }

    @Test
    void generatesDifferentIdentifiersForDifferentAgencies() {
        Agency first = createAgency();

        Agency second = Agency.create(
                "RABAT-AGDAL",
                "Rabat Agdal",
                "10 Avenue de France",
                null,
                "Rabat",
                null,
                "MA",
                null,
                null,
                "Africa/Casablanca"
        );

        assertNotEquals(first.getId(), second.getId());
    }

    private static Agency createAgency() {
        return Agency.create(
                "CASA-CENTRE",
                "Casablanca Centre",
                "1 Avenue Hassan II",
                null,
                "Casablanca",
                "20000",
                "MA",
                "+212500000000",
                "centre@autorent.ma",
                "Africa/Casablanca"
        );
    }
}