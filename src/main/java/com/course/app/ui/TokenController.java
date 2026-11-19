package com.course.app.ui;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TokenController {

    @GetMapping("/ui/token")
    public Map<String, String> token(@RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient client) {
        return Map.of("access_token", client.getAccessToken().getTokenValue());
    }

    @GetMapping("/ui/csrf")
    public Map<String, String> csrfToken(CsrfToken csrfToken) {
        return Map.of("headerName", csrfToken.getHeaderName(), "token", csrfToken.getToken());
    }
}
