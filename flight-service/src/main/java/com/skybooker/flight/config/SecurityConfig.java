package com.skybooker.flight.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skybooker.flight.security.JwtFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String FLIGHTS_PATTERN = "/flights/**";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
//                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .cors(cors -> cors.disable())
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            Map<String, Object> error = new HashMap<>();
                            error.put("status", 401);
                            error.put("error", "Unauthorized");
                            error.put("message", "Please login first. Token missing or invalid.");
                            response.getWriter().write(new ObjectMapper().writeValueAsString(error));
                        })
                        .accessDeniedHandler((request, response, e) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            Map<String, Object> error = new HashMap<>();
                            error.put("status", 403);
                            error.put("error", "Access Denied");
                            error.put("message", "You do not have permission to perform this action");
                            response.getWriter().write(new ObjectMapper().writeValueAsString(error));
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs"
                        ).permitAll()

                        // ✅ YAHI FIX HAI — search bina login ke bhi chalega
                        // ZAROORI: ye line /flights/** se PEHLE aani chahiye
                        .requestMatchers(HttpMethod.GET, "/flights/search").permitAll()

                        // Baaki GET requests (getAllFlights etc.) login ke baad
                        .requestMatchers(HttpMethod.GET, FLIGHTS_PATTERN).authenticated()

                        // Flight add karna sirf STAFF/ADMIN
                        .requestMatchers(HttpMethod.POST, "/flights").hasAnyRole("AIRLINE_STAFF", ROLE_ADMIN)

                        // Seat reduce — booking service call karta hai (token hoga)
                        .requestMatchers(HttpMethod.PUT, "/flights/*/reduce-seats").authenticated()

                        // Baaki PUT (flight update) — STAFF/ADMIN
                        .requestMatchers(HttpMethod.PUT, FLIGHTS_PATTERN).hasAnyRole("AIRLINE_STAFF", ROLE_ADMIN)

                        // Delete — sirf ADMIN
                        .requestMatchers(HttpMethod.DELETE, FLIGHTS_PATTERN).hasRole(ROLE_ADMIN)

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

}
