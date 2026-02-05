package com.cbrviewer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SessionAuthenticationFilter sessionAuthenticationFilter() {
        return new SessionAuthenticationFilter();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .addFilterBefore(sessionAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // Static resources - allow all (SPA assets)
                .requestMatchers("/", "/index.html", "/assets/**", "/icon-*.png", "/manifest.webmanifest", "/sw.js", "/workbox-*.js", "/registerSW.js").permitAll()
                .requestMatchers("/*.js", "/*.css", "/*.ico", "/*.png", "/*.svg", "/*.json").permitAll()
                // SPA routes - allow access (auth checked by frontend)
                .requestMatchers("/login", "/register", "/library", "/viewer/**", "/admin/**").permitAll()
                // Auth endpoints
                .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                // Actuator endpoints
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Admin API requires admin role
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // All other API requests require authentication
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                .sessionFixation(fixation -> fixation.none())  // Disable for SPA - concurrent requests need stable session ID
            )
            .csrf(AbstractHttpConfigurer::disable)  // Disabled for SPA API
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
