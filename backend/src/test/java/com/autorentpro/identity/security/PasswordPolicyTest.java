package com.autorentpro.identity.security;

import com.autorentpro.identity.application.PasswordChangeException;
import com.autorentpro.identity.application.PasswordPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyTest {

    private final PasswordPolicy passwordPolicy =
            new PasswordPolicy();

    @Test
    void acceptsLongPassphrase() {
        assertThatCode(
                () ->
                        passwordPolicy.validate(
                                "a strong rental passphrase"
                        )
        ).doesNotThrowAnyException();
    }

    @Test
    void acceptsSpacesWithoutCompositionRules() {
        assertThatCode(
                () ->
                        passwordPolicy.validate(
                                "this is long enough"
                        )
        ).doesNotThrowAnyException();
    }

    @Test
    void rejectsPasswordShorterThanFifteenCharacters() {
        assertThatThrownBy(
                () ->
                        passwordPolicy.validate(
                                "too-short"
                        )
        )
                .isInstanceOf(
                        PasswordChangeException.class
                )
                .extracting("code")
                .isEqualTo(
                        "PASSWORD_TOO_SHORT"
                );
    }

    @Test
    void rejectsPasswordLongerThanMaximum() {
        String password =
                "a".repeat(
                        PasswordPolicy.MAX_LENGTH + 1
                );

        assertThatThrownBy(
                () ->
                        passwordPolicy.validate(
                                password
                        )
        )
                .isInstanceOf(
                        PasswordChangeException.class
                )
                .extracting("code")
                .isEqualTo(
                        "PASSWORD_TOO_LONG"
                );
    }

    @Test
    void rejectsKnownCommonPassword() {
        assertThatThrownBy(
                () ->
                        passwordPolicy.validate(
                                "passwordpassword"
                        )
        )
                .isInstanceOf(
                        PasswordChangeException.class
                )
                .extracting("code")
                .isEqualTo(
                        "PASSWORD_TOO_COMMON"
                );
    }
}