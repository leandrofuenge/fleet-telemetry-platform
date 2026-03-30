package com.telemetria.infrastructure.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/public/**").permitAll()
                .requestMatchers("/api/v1/veiculos").permitAll()
                .requestMatchers("/api/v1/veiculos/buscar/**").permitAll()
                .requestMatchers("/api/v1/motoristas").permitAll()
                .requestMatchers("/api/v1/motoristas/buscar/**").permitAll()
                
                // ===== MATRIZ DE VISIBILIDADE =====
                // Veículos (RN: MOTORISTA: próprio, OPERADOR/GESTOR/ADMIN: todos)
                .requestMatchers("/api/v1/veiculos/meus").hasRole("MOTORISTA")
                .requestMatchers("/api/v1/veiculos/**").hasAnyRole("ADMIN", "GESTOR", "OPERADOR")
                
                // Motoristas (RN: MOTORISTA: próprio perfil, OPERADOR/GESTOR/ADMIN: todos)
                .requestMatchers("/api/v1/motoristas/meu-perfil").hasRole("MOTORISTA")
                .requestMatchers("/api/v1/motoristas/**").hasAnyRole("ADMIN", "GESTOR", "OPERADOR")
                
                // Telemetria (RN: MOTORISTA: própria viagem, OPERADOR: leitura completa, GESTOR: leitura+export)
                .requestMatchers("/api/v1/telemetria/viagem/**").hasAnyRole("MOTORISTA", "OPERADOR", "GESTOR", "ADMIN")
                .requestMatchers("/api/v1/telemetria/exportar/**").hasAnyRole("GESTOR", "ADMIN")
                .requestMatchers("/api/v1/telemetria/**").hasAnyRole("OPERADOR", "GESTOR", "ADMIN")
                
                // Alertas (RN: MOTORISTA: próprios, OPERADOR: ver/resolver, GESTOR: criar regras)
                .requestMatchers("/api/v1/alertas/meus").hasRole("MOTORISTA")
                .requestMatchers("/api/v1/alertas/resolver/**").hasAnyRole("OPERADOR", "GESTOR", "ADMIN")
                .requestMatchers("/api/v1/alertas/regras/**").hasAnyRole("GESTOR", "ADMIN")
                .requestMatchers("/api/v1/alertas/**").hasAnyRole("OPERADOR", "GESTOR", "ADMIN")
                
                // Financeiro (RN: apenas GESTOR consolidado, ADMIN completo)
                .requestMatchers("/api/v1/financeiro/consolidado").hasAnyRole("GESTOR", "ADMIN")
                .requestMatchers("/api/v1/financeiro/**").hasRole("ADMIN")
                
                // Configurações (RN: ADMIN completo, GESTOR somente leitura)
                .requestMatchers("/api/v1/config/readonly/**").hasAnyRole("GESTOR", "ADMIN")
                .requestMatchers("/api/v1/config/**").hasRole("ADMIN")
                
                // Admin (super admin)
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // RN-USR-001: BCrypt rounds = 12
        return new BCryptPasswordEncoder(12);
    }
}