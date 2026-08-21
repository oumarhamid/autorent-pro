package com.autorentpro.agency.infrastructure.authorization;

import com.autorentpro.agency.domain.repository.UserAgencyAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaAgencyScopeMembershipResolverTest {

    @Mock
    UserAgencyAssignmentRepository
            userAgencyAssignmentRepository;

    JpaAgencyScopeMembershipResolver resolver;

    @BeforeEach
    void setUp() {
        resolver =
                new JpaAgencyScopeMembershipResolver(
                        userAgencyAssignmentRepository
                );
    }

    @Test
    void returnsTrueForActiveMembership() {
        UUID userId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();

        when(
                userAgencyAssignmentRepository
                        .existsActiveByUserIdAndAgencyId(
                                userId,
                                agencyId
                        )
        ).thenReturn(true);

        assertThat(
                resolver.hasActiveMembership(
                        userId,
                        agencyId
                )
        ).isTrue();

        verify(
                userAgencyAssignmentRepository
        ).existsActiveByUserIdAndAgencyId(
                userId,
                agencyId
        );
    }

    @Test
    void returnsFalseWithoutActiveMembership() {
        UUID userId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();

        when(
                userAgencyAssignmentRepository
                        .existsActiveByUserIdAndAgencyId(
                                userId,
                                agencyId
                        )
        ).thenReturn(false);

        assertThat(
                resolver.hasActiveMembership(
                        userId,
                        agencyId
                )
        ).isFalse();
    }

    @Test
    void nullIdentifiersFailClosed() {
        UUID userId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();

        assertThat(
                resolver.hasActiveMembership(
                        null,
                        agencyId
                )
        ).isFalse();

        assertThat(
                resolver.hasActiveMembership(
                        userId,
                        null
                )
        ).isFalse();

        verify(
                userAgencyAssignmentRepository,
                never()
        ).existsActiveByUserIdAndAgencyId(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }
}