package com.autorentpro.identity.application;

import com.autorentpro.identity.domain.model.PermissionCode;
import com.autorentpro.identity.domain.model.PermissionScope;

import java.util.Objects;

public record PermissionGrant(
        PermissionCode permission,
        PermissionScope scope
) {

    public PermissionGrant {
        Objects.requireNonNull(
                permission,
                "permission must not be null"
        );

        Objects.requireNonNull(
                scope,
                "scope must not be null"
        );
    }
}