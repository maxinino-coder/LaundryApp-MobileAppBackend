package com.group130.laundryapp.DAL.Configuration.Auth;

import com.group130.laundryapp.DAL.Configuration.Google.GoogleOAuth.GoogleOAuthFailureHandler;
import com.group130.laundryapp.DAL.Configuration.Google.GoogleOAuth.GoogleOAuthSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter             jwtAuthFilter;
    private final AuthenticationProvider    authenticationProvider;
    private final GoogleOAuthSuccessHandler googleSuccessHandler;
    private final GoogleOAuthFailureHandler googleFailureHandler;

    private static final String[] PUBLIC_ROUTES = {
            "/api/v1/auth/**",
            "/api/v1/businesses/public/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/actuator/health"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ROUTES).permitAll()
                        .requestMatchers(
                                "/api/v1/users/**",
                                "/api/v1/orders/place",
                                "/api/v1/orders/user/**",
                                "/api/v1/addresses/**",
                                "/api/v1/reviews/**"
                        ).hasRole("USER")
                        .requestMatchers(
                                "/api/v1/business/**",
                                "/api/v1/service-items/**",
                                "/api/v1/orders/business/**",
                                "/api/v1/payouts/business/**"
                        ).hasRole("BUSINESS")
                        .requestMatchers(
                                "/api/v1/riders/**",
                                "/api/v1/orders/rider/**",
                                "/api/v1/earnings/**"
                        ).hasRole("RIDER")
                        .requestMatchers(
                                "/api/v1/profile/**",
                                "/api/v1/notifications/**"
                        ).authenticated()
                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth2 -> oauth2


                        .authorizationEndpoint(endpoint ->
                                endpoint.baseUri("/api/v1/auth/google/login"))

                        /*
                         * FIX — redirectionEndpoint baseUri
                         *
                         * This must match EXACTLY what you put in Google Console redirect URIs.
                         * Spring appends "/{registrationId}" here too, so the actual callback
                         * URL is:
                         *   http://localhost:8080/api/v1/auth/google/callback/google
                         *
                         * Add this exact URL to Google Console → Authorised redirect URIs.
                         *
                         * In application.yml also align:
                         *   redirect-uri: "{baseUrl}/api/v1/auth/google/callback/{registrationId}"
                         */
                        .redirectionEndpoint(endpoint ->
                                endpoint.baseUri("/api/v1/auth/google/callback/*"))

                        .successHandler(googleSuccessHandler)
                        .failureHandler(googleFailureHandler)
                )

                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}