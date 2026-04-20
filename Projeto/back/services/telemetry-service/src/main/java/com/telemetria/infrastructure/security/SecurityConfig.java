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
                // ========================================================
                // ENDPOINTS PÚBLICOS (SEM AUTENTICAÇÃO)
                // ========================================================
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/public/**").permitAll()
                
                // ===== ALERTAS - TOTALMENTE PÚBLICOS =====
                .requestMatchers("/api/v1/alertas/**").permitAll()
                
                // ===== VEÍCULOS E MOTORISTAS (consulta pública) =====
                .requestMatchers("/api/v1/veiculos").permitAll()
                .requestMatchers("/api/v1/veiculos/buscar/**").permitAll()
                .requestMatchers("/api/v1/motoristas").permitAll()
                .requestMatchers("/api/v1/motoristas/buscar/**").permitAll()
                
                // ===== TELEMETRIA - TODOS OS ENDPOINTS LIBERADOS =====
                .requestMatchers("/api/v1/telemetria/**").permitAll()
                
                // ===== PROMETHEUS (métricas) =====
                .requestMatchers("/actuator/prometheus").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                
                // ===== SWAGGER / API DOCS (se tiver) =====
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                
                // ========================================================
                // ENDPOINTS PROTEGIDOS (COM AUTENTICAÇÃO)
                // ========================================================
                
                // Veículos (RN: MOTORISTA: próprio, OPERADOR/GESTOR/ADMIN: todos)
                .requestMatchers("/api/v1/veiculos/meus").hasRole("MOTORISTA")
                .requestMatchers("/api/v1/veiculos/novo").hasAnyRole("ADMIN", "GESTOR", "OPERADOR")
                .requestMatchers("/api/v1/veiculos/editar/**").hasAnyRole("ADMIN", "GESTOR", "OPERADOR")
                .requestMatchers("/api/v1/veiculos/excluir/**").hasAnyRole("ADMIN", "GESTOR")
                
                // Motoristas (RN: MOTORISTA: próprio perfil, OPERADOR/GESTOR/ADMIN: todos)
                .requestMatchers("/api/v1/motoristas/meu-perfil").hasRole("MOTORISTA")
                .requestMatchers("/api/v1/motoristas/novo").hasAnyRole("ADMIN", "GESTOR", "OPERADOR")
                .requestMatchers("/api/v1/motoristas/editar/**").hasAnyRole("ADMIN", "GESTOR", "OPERADOR")
                .requestMatchers("/api/v1/motoristas/excluir/**").hasAnyRole("ADMIN", "GESTOR")
                
                // Financeiro (RN: apenas GESTOR consolidado, ADMIN completo)
                .requestMatchers("/api/v1/financeiro/consolidado").hasAnyRole("GESTOR", "ADMIN")
                .requestMatchers("/api/v1/financeiro/**").hasRole("ADMIN")
                
                // Configurações (RN: ADMIN completo, GESTOR somente leitura)
                .requestMatchers("/api/v1/config/readonly/**").hasAnyRole("GESTOR", "ADMIN")
                .requestMatchers("/api/v1/config/**").hasRole("ADMIN")
                
                // Admin (super admin)
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                
                // Qualquer outra requisição permitida (DEBUG MODE - DESABILITAR AUTENTICAÇÃO)
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Permitir origens do frontend
        configuration.setAllowedOrigins(List.of(
            "http://localhost:5173", 
            "http://localhost:8080",
            "http://localhost:8081",
            "http://127.0.0.1:8080",
            "http://127.0.0.1:5173"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

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
        return new BCryptPasswordEncoder(12);
    }
}