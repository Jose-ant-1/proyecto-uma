package com.page.api_uma.config;

import com.page.api_uma.model.Usuario;
import com.page.api_uma.repository.UsuarioRepository;
import com.page.api_uma.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataLoaderTest {

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private DataLoader dataLoader;

    @Test
    @DisplayName("Debería crear el usuario admin si no existe en la base de datos")
    void run_DeberiaCrearAdminSiNoExiste() {
        // GIVEN: El repositorio devuelve null (usuario no encontrado)
        when(usuarioRepository.findByEmail("admin@uma.es")).thenReturn(null);

        // WHEN
        dataLoader.run();

        // THEN: Se debe llamar al service para guardar el nuevo usuario
        verify(usuarioRepository, times(1)).findByEmail("admin@uma.es");
        verify(usuarioService, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("No debería crear nada si el usuario admin ya existe")
    void run_NoDeberiaCrearNadaSiYaExiste() {
        // GIVEN: El repositorio ya tiene al admin
        Usuario adminExistente = Usuario.builder().email("admin@uma.es").build();
        when(usuarioRepository.findByEmail("admin@uma.es")).thenReturn(adminExistente);

        // WHEN
        dataLoader.run();

        // THEN: Se comprueba la existencia pero NO se llama al método save
        verify(usuarioRepository, times(1)).findByEmail("admin@uma.es");
        verify(usuarioService, never()).save(any(Usuario.class));
    }
}