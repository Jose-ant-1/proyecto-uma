package com.page.api_uma.config;

import com.page.api_uma.model.Usuario;
import com.page.api_uma.repository.UsuarioRepository;
import com.page.api_uma.service.UsuarioService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!test")
public class DataLoader implements CommandLineRunner {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    public DataLoader(UsuarioService usuarioService, UsuarioRepository usuarioRepository) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void run(String... args) {

        if (usuarioRepository.findByEmail("admin@uma.es") != null) {
            log.info("Base de datos ya inicializada. Saltando carga de datos."); // 3. Usamos log.info
            return;
        }

        Usuario admin = Usuario.builder()
                .nombre("Admin Global")
                .email("admin@uma.es")
                .contrasenia("admin1234") // Se cifrará en el service[cite: 2]
                .permiso("ADMIN") // Requerido para el control de acceso en el Controller[cite: 3]
                .build();

        usuarioService.save(admin);

        log.info("DataLoader: Usuario ADMIN creado con éxito (admin@uma.es)"); // 4. Log informativo
    }
}