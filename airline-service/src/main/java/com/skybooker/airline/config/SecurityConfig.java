package com.skybooker.airline.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skybooker.airline.security.JwtFilter;
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

    private static final String AIRLINES_PATH = "/airlines/**";
    private static final String AIRPORTS_PATH = "/airports/**";
    private static final String ROLE_ADMIN = "ADMIN";

    private final JwtFilter jwtFilter;

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
                        .requestMatchers(HttpMethod.GET, AIRLINES_PATH).authenticated()
                        .requestMatchers(HttpMethod.GET, AIRPORTS_PATH).authenticated()
                        .requestMatchers(HttpMethod.POST, AIRLINES_PATH).hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PUT, AIRLINES_PATH).hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.POST, AIRPORTS_PATH).hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PUT, AIRPORTS_PATH).hasRole(ROLE_ADMIN)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

}