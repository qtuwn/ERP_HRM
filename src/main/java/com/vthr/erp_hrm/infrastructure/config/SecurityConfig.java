package com.vthr.erp_hrm.infrastructure.config;

import com.vthr.erp_hrm.infrastructure.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/api/auth/**", "/api/files/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/jobs", "/api/jobs/**")
                        .permitAll()
                        .requestMatchers(
                                "/", "/login", "/register",
                                "/forbidden",
                                "/verify-email", "/verify-otp",
                                "/forgot-password", "/forgot-password/**",
                                "/jobs", "/jobs/**",
                                "/dashboard", "/jobs/*/kanban",
                                "/admin/users",
                                "/company/staff",
                                "/candidate/applications", "/profile",
                                "/index.html",
                                "/assets/**", "/favicon.svg", "/favicon.ico",
                                "/css/**", "/js/**", "/images/**", "/error")
                        .permitAll()
                        .requestMatchers("/api/dashboard/**").hasAnyRole("ADMIN", "HR", "COMPANY")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/applications/*/status")
                        .hasAnyRole("ADMIN", "HR", "COMPANY")
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/applications/bulk-reject")
                        .hasAnyRole("ADMIN", "HR", "COMPANY")
                        .requestMatchers("/api/applications/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter()
                                    .write("{\"success\":false,\"message\":\"Unauthorized: Token expired or invalid\"}");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"success\":false,\"message\":\"Forbidden\"}");
                        }));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
