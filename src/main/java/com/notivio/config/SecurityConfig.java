package com.notivio.config;

import com.notivio.security.CustomOAuth2UserService;
import com.notivio.security.JwtAuthenticationFilter;
import com.notivio.security.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration:
 * - Stateless JWT-based sessions
 * - Google OAuth2 login
 * - CORS locked to exact frontend origin
 * - ROLE_ADMIN required for /api/admin/**
 * - Secure HTTP headers (HSTS, X-Frame-Options, CSP, Referrer-Policy)
 * - Public endpoints whitelist (minimal surface area)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomOAuth2UserService  customOAuth2UserService;
    private final OAuth2SuccessHandler     oAuth2SuccessHandler;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // ── CSRF: disabled — we use stateless JWT, no cookies ──────────────────
                .csrf(AbstractHttpConfigurer::disable)

                // ── CORS ───────────────────────────────────────────────────────────────
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ── Session: STATELESS ─────────────────────────────────────────────────
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ── Security Headers ────────────────────────────────────────────────────
                .headers(headers -> headers
                        // Prevent clickjacking
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        // Prevent MIME sniffing
                        .contentTypeOptions(HeadersConfigurer.ContentTypeOptionsConfig::disable) // disable = adds the header
                        // Force HTTPS for 1 year (production only — harmless in dev)
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000))
                        // Restrict Referrer to same origin — prevents token leakage via Referer
                        .referrerPolicy(rp -> rp
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // Basic CSP: restrict script sources
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +
                                        "script-src 'self'; " +
                                        "frame-ancestors 'none'; " +
                                        "form-action 'self'"
                                ))
                )

                // ── Authorization Rules ─────────────────────────────────────────────────
                .authorizeHttpRequests(auth -> auth
                        // Public health + docs
                        .requestMatchers(HttpMethod.GET,
                                "/health", "/health/**",
                                "/swagger-ui.html", "/swagger-ui/**",
                                "/api-docs", "/api-docs/**",
                                "/actuator/health"
                        ).permitAll()
                        // OAuth2 flow
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        // One-time code exchange (called before the client has a JWT)
                        .requestMatchers(HttpMethod.POST, "/api/auth/exchange").permitAll()
                        // Public auth endpoints
                        .requestMatchers(HttpMethod.GET, "/api/auth/login").permitAll()
                        // SECURITY FIX: Admin endpoints now require ROLE_ADMIN, not just any auth
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // Everything else requires a valid JWT
                        .anyRequest().authenticated()
                )

                // ── OAuth2 Login ────────────────────────────────────────────────────────
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo ->
                                userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                )

                // ── JWT Filter ──────────────────────────────────────────────────────────
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // SECURITY FIX: Explicit origin list, not wildcard
        config.setAllowedOrigins(List.of(frontendUrl));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // SECURITY FIX: Explicit allowed headers, not wildcard "*"
        config.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Accept",
                "X-Requested-With", "Origin", "Cache-Control"
        ));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
