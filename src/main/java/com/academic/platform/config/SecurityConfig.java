package com.academic.platform.config;

import com.academic.platform.security.FirebaseTokenFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.context.annotation.Lazy;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final List<String> ALLOWED_ORIGINS = List.of(
            "https://adadamic-sync-frontend-ufaq.vercel.app",
            "http://localhost:5173",
            "http://localhost:5174",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:5174",
            "http://10.10.188.128:5173",
            "http://10.10.188.128:5174"
    );

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
            @Lazy com.academic.platform.service.UserService userService,
            @Lazy com.academic.platform.service.SystemSettingService systemSettingService)
            throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
        .requestMatchers("/").permitAll()   // ✅ Allow root
        .requestMatchers("/api/public/**").permitAll()
        .requestMatchers("/api/users/register").permitAll()
        .requestMatchers("/api/users/**").permitAll()
        .requestMatchers("/api/seed/**").permitAll()
        .requestMatchers("/api/admin/settings/public/features").permitAll()
        .requestMatchers("/api/leaves/parent-view/**").permitAll()
        .requestMatchers("/api/leaves/parent-action/**").permitAll()
        .requestMatchers("/error").permitAll()
        .anyRequest().authenticated()
)
                .addFilterBefore(new FirebaseTokenFilter(userService, systemSettingService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(ALLOWED_ORIGINS);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Collections.singletonList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Collections.singletonList("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
