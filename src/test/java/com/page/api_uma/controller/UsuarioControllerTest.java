package com.page.api_uma.controller;

import com.page.api_uma.config.JwtService;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private JwtService jwtService;

    @Mock
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;

    @InjectMocks
    private UsuarioController usuarioController;

    private Usuario usuarioPrueba;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(usuarioController).build();

        usuarioPrueba = new Usuario();
        usuarioPrueba.setId(1);
        usuarioPrueba.setNombre("Test User");
        usuarioPrueba.setEmail("test@test.com");
        usuarioPrueba.setPermiso("ADMIN");

        lenient().when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioPrueba);
    }

    @Test
    @DisplayName("GET /api/usuarios/me - Obtener mi perfil")
    void getMe_Ok() throws Exception {
        when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioPrueba);

        mockMvc.perform(get("/api/usuarios/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@test.com"))
                .andExpect(jsonPath("$.nombre").value("Test User"));
    }

    @Test
    @DisplayName("PUT /api/usuarios/me - Actualizar nombre y email")
    void updateMe_Ok() throws Exception {
        when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioPrueba);
        when(usuarioService.save(any(Usuario.class))).thenReturn(usuarioPrueba);

        String json = """
            {
                "nombre": "Nuevo Nombre",
                "email": "nuevo@test.com"
            }
        """;

        mockMvc.perform(put("/api/usuarios/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Nuevo Nombre")); // Devuelve el guardado
    }

    @Test
    @DisplayName("PUT /api/usuarios/me - Error por campos vacíos")
    void updateMe_BadRequest() throws Exception {
        when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioPrueba);

        String json = "{\"nombre\": \"\", \"email\": \"\"}";

        mockMvc.perform(put("/api/usuarios/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/usuarios/login - Éxito")
    void login_Ok() throws Exception {
        when(usuarioService.buscarPorEmail("test@test.com")).thenReturn(usuarioPrueba);
        when(jwtService.generateToken(any())).thenReturn("token-falso-123");

        String json = """
            {
                "email": "test@test.com",
                "password": "password123"
            }
        """;

        mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-falso-123"));
    }

    @Test
    @DisplayName("DELETE /api/usuarios/{id} - Permiso denegado")
    void delete_Forbidden() throws Exception {
        // Simulamos un usuario que no es ADMIN ni el mismo que el target
        Usuario normalUser = new Usuario();
        normalUser.setId(2);
        normalUser.setPermiso("USER");

        when(usuarioService.getUsuarioAutenticado()).thenReturn(normalUser);

        mockMvc.perform(delete("/api/usuarios/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/usuarios - Listar todos")
    void findAll_Ok() throws Exception {
        when(usuarioService.getUsuarios()).thenReturn(List.of(usuarioPrueba));

        mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/usuarios/{id} - OK (Es Admin)")
    void findById_Admin_Ok() throws Exception {

        when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioPrueba);

        Usuario otroUsuario = new Usuario();
        otroUsuario.setId(2);
        otroUsuario.setNombre("Otro");
        otroUsuario.setEmail("otro@test.com");
        otroUsuario.setPermiso("USER");

        when(usuarioService.findById(2)).thenReturn(otroUsuario);

        mockMvc.perform(get("/api/usuarios/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    @DisplayName("GET /api/usuarios/{id} - Forbidden (No es el mismo ni Admin)")
    void findById_Forbidden() throws Exception {

        Usuario userNormal = new Usuario();
        userNormal.setId(2);
        userNormal.setNombre("User");
        userNormal.setPermiso("USER");

        when(usuarioService.getUsuarioAutenticado()).thenReturn(userNormal);
        when(usuarioService.findById(1)).thenReturn(usuarioPrueba);

        mockMvc.perform(get("/api/usuarios/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/usuarios - Crear nuevo")
    void create_Ok() throws Exception {
        when(usuarioService.save(any(Usuario.class))).thenReturn(usuarioPrueba);

        String json = """
            {
                "nombre": "Test User",
                "email": "test@test.com",
                "contrasenia": "password123",
                "permiso": "ADMIN"
            }
        """;

        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    @Test
    @DisplayName("PUT /api/usuarios/{id} - Actualizar por Admin")
    void update_Ok() throws Exception {
        when(usuarioService.findById(1)).thenReturn(usuarioPrueba);
        when(usuarioService.save(any(Usuario.class))).thenReturn(usuarioPrueba);

        String json = "{\"nombre\": \"Nombre Actualizado\"}";

        mockMvc.perform(put("/api/usuarios/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/usuarios/buscar - Búsqueda por query")
    void buscar_Usuarios_Ok() throws Exception {
        when(usuarioService.buscarUsuarios("test")).thenReturn(List.of(usuarioPrueba));

        mockMvc.perform(get("/api/usuarios/buscar")
                        .param("q", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Test User"));
    }
}