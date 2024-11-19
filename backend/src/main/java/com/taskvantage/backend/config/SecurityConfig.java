package com.taskvantage.backend.config;

import com.taskvantage.backend.Security.GoogleOAuth2SuccessHandler;
import com.taskvantage.backend.Security.JwtFilter;
import com.taskvantage.backend.Security.OAuth2CookieFilter;
import com.taskvantage.backend.Security.SimpleLoggingFilter;
import com.taskvantage.backend.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.beans.factory.annotation.Autowired;
import com.taskvantage.backend.Security.HttpCookieOAuth2AuthorizationRequestRepository;
import java.net.URI;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Autowired
    private JwtFilter jwtFilter;

    @Autowired
    private SimpleLoggingFilter simpleLoggingFilter;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Value("${GOOGLE_CAL_SECRET}")
    private String googleClientSecret;

    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(
            ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }

    @Bean
    public GoogleOAuth2SuccessHandler oAuth2SuccessHandler(
            OAuth2AuthorizedClientService authorizedClientService,
            CustomUserDetailsService userDetailsService) {
        return new GoogleOAuth2SuccessHandler(authorizedClientService, userDetailsService);
    }

    @Bean
    public OAuth2AuthorizationRequestRedirectFilter oauth2AuthorizationRequestRedirectFilter(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizationRequestResolver authorizationRequestResolver) {
        return new OAuth2AuthorizationRequestRedirectFilter(authorizationRequestResolver);
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(googleClientRegistration());
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            GoogleOAuth2SuccessHandler oAuth2SuccessHandler,
            OAuth2AuthorizationRequestResolver authorizationRequestResolver,
            OAuth2CookieFilter oAuth2CookieFilter) throws Exception {
        logger.info("Initializing security configuration");

        DefaultAuthorizationCodeTokenResponseClient tokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient();
        tokenResponseClient.setRequestEntityConverter(converter -> {
            OAuth2AuthorizationCodeGrantRequest grantRequest = converter;
            ClientRegistration clientRegistration = grantRequest.getClientRegistration();

            MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
            parameters.add(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.AUTHORIZATION_CODE.getValue());
            parameters.add(OAuth2ParameterNames.CODE, grantRequest.getAuthorizationExchange().getAuthorizationResponse().getCode());
            parameters.add(OAuth2ParameterNames.REDIRECT_URI, clientRegistration.getRedirectUri());
            parameters.add(OAuth2ParameterNames.CLIENT_ID, clientRegistration.getClientId());
            parameters.add(OAuth2ParameterNames.CLIENT_SECRET, googleClientSecret);

            return RequestEntity
                    .post(URI.create("https://oauth2.googleapis.com/token"))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(parameters);
        });

        return http
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration corsConfig = new CorsConfiguration();
                    corsConfig.addAllowedOrigin("http://localhost:4200");
                    corsConfig.addAllowedOrigin("https://taskvantage-frontend-cbaab3e2bxcpbyb8.eastus-01.azurewebsites.net");
                    corsConfig.addAllowedMethod("*");
                    corsConfig.addAllowedHeader("*");
                    corsConfig.setAllowCredentials(true);
                    return corsConfig;
                }))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/oauth2/**", "/login/oauth2/code/*", "/oauth2/authorization/**",
                                "/oauth2/google/status", "/api/oauth2/google/status",
                                "/api/set-user-id-cookie").permitAll()
                        .requestMatchers("/api/login", "/api/register", "/api/verify-email", "/api/forgot-password", "/api/reset-password").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.getWriter().write("{\"error\":\"Unauthorized\"}");
                            } else if (request.getRequestURI().contains("/oauth2/authorization/google")) {
                                // Let OAuth2 requests go through without redirect
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            } else {
                                // Redirect non-API unauthorized requests to login
                                response.sendRedirect("http://localhost:4200/login");
                            }
                        })
                )
                .oauth2Login(oauth2 -> {
                    logger.info("Setting up OAuth2 login");
                    oauth2
                            .authorizationEndpoint(authorization -> {
                                authorization.authorizationRequestResolver(authorizationRequestResolver);
                                authorization.authorizationRequestRepository(cookieAuthorizationRequestRepository());
                            })
                            .loginPage("/oauth2/authorization/google")
                            .successHandler(oAuth2SuccessHandler)
                            .failureHandler((request, response, exception) -> {
                                logger.error("OAuth2 login failed: " + exception.getMessage(), exception);
                                response.sendRedirect("http://localhost:4200/settings?auth=error&message=" + exception.getMessage());
                            })
                            .tokenEndpoint(token ->
                                    token.accessTokenResponseClient(tokenResponseClient));
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(simpleLoggingFilter, OAuth2AuthorizationRequestRedirectFilter.class)
                .addFilterBefore(oAuth2CookieFilter, OAuth2AuthorizationRequestRedirectFilter.class)
                .addFilterBefore(jwtFilter, OAuth2AuthorizationRequestRedirectFilter.class)
                .build();
    }

    @Bean
    public HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository() {
        return new HttpCookieOAuth2AuthorizationRequestRepository();
    }

    @Value("${app.backend-url}")
    private String backendUrl;

    @Bean
    public ClientRegistration googleClientRegistration() {
        return CommonOAuth2Provider.GOOGLE
                .getBuilder("google")
                .clientId("872741914932-asspmr6jois4ovvr3bvjm4p44csq9qjs.apps.googleusercontent.com")
                .clientSecret(googleClientSecret)
                .scope("openid",
                        "profile",
                        "email",
                        "https://www.googleapis.com/auth/calendar",
                        "https://www.googleapis.com/auth/tasks")
                .redirectUri(backendUrl + "/login/oauth2/code/google")
                .build();
    }

    @Bean
    public OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, "/oauth2/authorization");

        resolver.setAuthorizationRequestCustomizer(customizer -> {
            customizer.additionalParameters(params -> {
                if (params.containsKey("state")) {
                    String state = params.get("state").toString();
                    params.put("state", state);
                }
            });
        });

        return resolver;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public OAuth2CookieFilter oAuth2CookieFilter() {
        return new OAuth2CookieFilter();
    }
}