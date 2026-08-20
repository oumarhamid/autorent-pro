package com.autorentpro;

import com.autorentpro.identity.infrastructure.persistence.RolePermissionRepository;
import com.autorentpro.identity.infrastructure.persistence.RoleRepository;
import com.autorentpro.identity.infrastructure.persistence.UserAccountRepository;
import com.autorentpro.identity.infrastructure.persistence.UserRoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@MockitoBean(types = {
        UserAccountRepository.class,
        UserRoleRepository.class,
        RolePermissionRepository.class,
        RoleRepository.class
})
class AutorentBackendApplicationTests {

    @Test
    void contextLoads() {
    }
}