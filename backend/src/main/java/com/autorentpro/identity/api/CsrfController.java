package com.autorentpro.identity.api;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class CsrfController {

    @GetMapping("/csrf")
    public CsrfResponse csrf(
            CsrfToken csrfToken
    ) {
        return new CsrfResponse(
                csrfToken.getToken(),
                csrfToken.getHeaderName(),
                csrfToken.getParameterName()
        );
    }

    public record CsrfResponse(
            String token,
            String headerName,
            String parameterName
    ) {
    }
}