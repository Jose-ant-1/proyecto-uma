package com.page.api_uma.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 1. Aquí defines el Bean para hashear contraseñas
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 1. EL ORDEN IMPORTA: /me debe ir antes que la restricción de /api/usuarios/**
                        // Permitimos que CUALQUIER usuario logueado vea su propio perfil
                        .requestMatchers("/api/usuarios/me").authenticated()

                        // 2. RESTRICCIONES DE ESCRITURA PARA PÁGINAS
                        // Usamos hasAuthority porque en tu DB pone "ADMIN" a secas
                        .requestMatchers(HttpMethod.POST, "/api/paginas/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/paginas/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/paginas/**").hasAuthority("ADMIN")

                        // 3. ADMINISTRACIÓN DE USUARIOS Y PLANTILLAS
                        // Bloqueamos el resto de /api/usuarios/ (como /{id} o el listado general)
                        .requestMatchers("/api/usuarios/**").hasAuthority("ADMIN")
                        .requestMatchers("/api/plantillaPagina/**").hasAuthority("ADMIN")
                        .requestMatchers("/api/plantillaUsuario/**").hasAuthority("ADMIN")

                        // 4. EL RESTO (como ver mis páginas GET /api/paginas)
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET","POST", "PUT", "DELETE", "OPTIONS"));
        // Añadimos "Accept" por si acaso
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));

        // FUNDAMENTAL: Permitir que el navegador envíe credenciales (cookies/auth headers)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}