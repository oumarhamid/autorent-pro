package com.autorentpro.identity.infrastructure.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(
        prefix = "autorent.bootstrap.admin"
)
public record InitialAdminBootstrapProperties(
        boolean enabled,
        String email,
        String password
) {
}