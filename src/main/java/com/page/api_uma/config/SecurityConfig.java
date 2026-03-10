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
                        // 1. PERFIL PROPIO (Prioridad máxima)
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/me").authenticated()

                        // 2. ADMINISTRACIÓN DE USUARIOS (Acceso para Pedro y otros)
                        // Permitimos ver la lista y ver un perfil concreto a cualquier logueado
                        .requestMatchers(HttpMethod.GET, "/api/usuarios").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/{id}").authenticated()

                        // RESTRICCIONES DE ADMIN (Solo administradores pueden crear, borrar o editar otros usuarios)
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/usuarios/**").hasAuthority("ADMIN")

                        // 3. MONITOREOS Y OTROS (Acceso general autenticado)
                        .requestMatchers("/api/monitoreos/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/paginas/**").authenticated()
                        .requestMatchers("/api/plantillaPagina/**").authenticated()
                        .requestMatchers("/api/plantillaUsuario/**").authenticated()

                        // 4. RESTO DE PETICIONES
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Permitimos el origen de tu frontend Angular
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        // Es vital incluir OPTIONS para las peticiones de pre-vuelo del navegador
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Permitimos las cabeceras necesarias para Basic Auth y JSON
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept"));
        // Permitimos el envío de credenciales
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}