package com.autorentpro.identity.infrastructure.bootstrap;

import com.autorentpro.identity.application.InitialAdminBootstrapService;
import com.autorentpro.identity.application.InitialAdminBootstrapService.BootstrapResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
        InitialAdminBootstrapProperties.class
)
public class InitialAdminBootstrapConfiguration {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    InitialAdminBootstrapConfiguration.class
            );

    @Bean
    ApplicationRunner initialAdminBootstrapRunner(
            InitialAdminBootstrapProperties properties,
            InitialAdminBootstrapService bootstrapService
    ) {
        return new ApplicationRunner() {

            @Override
            public void run(
                    ApplicationArguments arguments
            ) {
                if (!properties.enabled()) {
                    return;
                }

                String email =
                        requireConfiguration(
                                properties.email(),
                                "AUTORENT_BOOTSTRAP_ADMIN_EMAIL"
                        );

                String password =
                        requireConfiguration(
                                properties.password(),
                                "AUTORENT_BOOTSTRAP_ADMIN_PASSWORD"
                        );

                BootstrapResult result =
                        bootstrapService.bootstrap(
                                email,
                                password
                        );

                if (result == BootstrapResult.CREATED) {
                    LOGGER.info(
                            "Initial administrator account created."
                    );
                } else {
                    LOGGER.info(
                            "Initial administrator bootstrap skipped "
                                    + "because an administrator already exists."
                    );
                }
            }
        };
    }

    private static String requireConfiguration(
            String value,
            String environmentVariable
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    environmentVariable
                            + " must be configured when "
                            + "AUTORENT_BOOTSTRAP_ADMIN_ENABLED=true."
            );
        }

        return value;
    }
}