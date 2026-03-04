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
                        // 1. PERMITIR SIEMPRE EL LOGIN (Endpoint /me)
                        // Esta debe ser la primerísima regla para que no la pise nadie
                        .requestMatchers("/api/usuarios/me").authenticated()

                        // 2. MONITOREOS Y PAGINAS (Lectura para todos, el resto filtrado en Service)
                        .requestMatchers("/api/monitoreos/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/paginas/**").authenticated()

                        // 3. PLANTILLAS (Nuevas políticas: Propietarios y Admins pueden entrar)
                        .requestMatchers("/api/plantillaPagina/**").authenticated()
                        .requestMatchers("/api/plantillaUsuario/**").authenticated()

                        // 4. RESTRICCIONES DE ADMIN (Escritura en páginas y gestión de usuarios)
                        .requestMatchers(HttpMethod.POST, "/api/paginas/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/paginas/**").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/paginas/**").hasAuthority("ADMIN")
                        .requestMatchers("/api/usuarios/**").hasAuthority("ADMIN")

                        // 5. CUALQUIER OTRA PETICIÓN
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