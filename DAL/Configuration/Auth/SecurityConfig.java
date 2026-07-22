package com.group130.laundryapp.laundry2_0.DAL.Configuration.Auth;

import com.group130.laundryapp.laundry2_0.DAL.Configuration.Google.GoogleOAuth.GoogleOAuthFailureHandler;
import com.group130.laundryapp.laundry2_0.DAL.Configuration.Google.GoogleOAuth.GoogleOAuthSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter             jwtAuthFilter;
    private final AuthenticationProvider    authenticationProvider;
    private final GoogleOAuthSuccessHandler googleSuccessHandler;
    private final GoogleOAuthFailureHandler googleFailureHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())

                // ── Stateless except for Google OAuth state round-trip ──────────
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                // ── Disable the default form login page ─────────────────────────
                // This is the key fix — without this, Spring redirects any
                // unauthenticated request (including your JSON login POST)
                // to the OAuth2 login page
                .formLogin(form -> form.disable())

                .authorizeHttpRequests(auth -> auth

                        // ── Public auth endpoints ────────────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register/user").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register/business").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register/rider").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()

                        // ── Google OAuth ─────────────────────────────────────────────
                        .requestMatchers("/api/v1/auth/google/**").permitAll()

                        // ── Other public routes ──────────────────────────────────────
                        .requestMatchers("/api/v1/businesses/public/**").permitAll()
                        .requestMatchers("/api/v1/webhooks/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // ── Role-protected routes ────────────────────────────────────
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
                                "/api/v1/notifications/**",
                                "/api/v1/conversations/**"
                        ).authenticated()

                        .anyRequest().authenticated()
                )

                // ── Google OAuth2 — scoped ONLY to /api/v1/auth/google/** ────────
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(endpoint ->
                                endpoint.baseUri("/api/v1/auth/google/login"))
                        .redirectionEndpoint(endpoint ->
                                endpoint.baseUri("/api/v1/auth/google/callback/*"))
                        .successHandler(googleSuccessHandler)
                        .failureHandler(googleFailureHandler)
                )

                // ── Return 401 (not 403) for unauthenticated API requests ────────
                // Without this, Spring's default Http403ForbiddenEntryPoint fires
                // for any anonymous request, masking "token expired" as "forbidden".
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")))

                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}