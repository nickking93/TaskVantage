package com.taskvantage.backend.controller;

import com.taskvantage.backend.model.User;
import com.taskvantage.backend.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oauth2")
public class GoogleOAuth2Controller {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    @GetMapping("/callback/google")
    public String googleCallback(OAuth2AuthenticationToken authentication) {
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(),
                authentication.getName()
        );

        OAuth2AccessToken accessToken = client.getAccessToken();
        OAuth2RefreshToken refreshToken = client.getRefreshToken();

        // Save tokens to the database for the authenticated user
        User user = userDetailsService.findUserByUsername(authentication.getName());
        user.setGoogleAccessToken(accessToken.getTokenValue());
        user.setGoogleRefreshToken(refreshToken.getTokenValue());
        userDetailsService.saveUser(user);

        return "Successfully connected Google Calendar!";
    }
}