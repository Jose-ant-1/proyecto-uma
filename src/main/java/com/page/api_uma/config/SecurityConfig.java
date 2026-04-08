package com.page.api_uma.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    private static final String ADMIN = "ADMIN";

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter, AuthenticationProvider authenticationProvider) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.authenticationProvider = authenticationProvider;
    }

    @SuppressWarnings("squid:S4502")
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. LOGIN PÚBLICO
                        .requestMatchers("/api/usuarios/login").permitAll()

                        // 2. PERFIL PROPIO (Cualquier autenticado)
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/me").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/me").authenticated()

                        // 3. ADMINISTRACIÓN DE USUARIOS (Lectura para todos)
                        .requestMatchers(HttpMethod.GET, "/api/usuarios").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/{id}").authenticated()

                        // 4. RESTRICCIONES DE ADMIN (Solo ADMIN puede crear, editar otros o borrar)
                        // Importante: .hasAuthority("ADMIN") debe coincidir con lo que devuelve tu Usuario.getAuthorities()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").hasAuthority(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/**").hasAuthority(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/usuarios/**").hasAuthority(ADMIN)

                        // 5. RESTO DE MÓDULOS (Acceso general autenticado)
                        .requestMatchers("/api/monitoreos/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/paginas/**").authenticated()
                        .requestMatchers("/api/plantillaPagina/**").authenticated()
                        .requestMatchers("/api/plantillaUsuario/**").authenticated()

                        // 6. CUALQUIER OTRA PETICIÓN
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // MODIFICACIÓN AQUÍ: Añadimos x-xsrf-token y X-Requested-With
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "x-xsrf-token",
                "X-Requested-With"
        ));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }



}