package com.page.api_uma.controller;

import com.page.api_uma.dto.PlantillaUsuarioDTO;
import com.page.api_uma.mapper.PlantillaUsuarioMapper;
import com.page.api_uma.model.PlantillaUsuario;
import com.page.api_uma.service.PlantillaUsuarioService;
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

import java.security.Principal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PlantillaUsuarioControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PlantillaUsuarioService service;

    @Mock
    private PlantillaUsuarioMapper mapper;

    @InjectMocks
    private PlantillaUsuarioController controller;

    private Principal mockPrincipal;
    private PlantillaUsuarioDTO dtoPrueba;
    private PlantillaUsuario entidadPrueba;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // Mock del Principal (quien hace la petición)
        mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn("usuario@test.com");

        dtoPrueba = new PlantillaUsuarioDTO();
        dtoPrueba.setId(1);
        dtoPrueba.setNombre("Plantilla Usuario Test");

        entidadPrueba = new PlantillaUsuario();
        entidadPrueba.setId(1);
        entidadPrueba.setNombre("Plantilla Usuario Test");
    }

    @Test
    @DisplayName("GET /api/plantillaUsuario - Listar")
    void findAll_Ok() throws Exception {
        when(service.findByPropietario(anyString())).thenReturn(List.of(entidadPrueba));
        when(mapper.toDTOList(anyList())).thenReturn(List.of(dtoPrueba));

        mockMvc.perform(get("/api/plantillaUsuario")
                        .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Plantilla Usuario Test"));
    }

    @Test
    @DisplayName("GET /api/plantillaUsuario/{id} - Propietario Ok")
    void findById_Ok() throws Exception {
        when(service.esPropietario(eq(1), anyString())).thenReturn(true);
        when(service.findById(1)).thenReturn(entidadPrueba);

        mockMvc.perform(get("/api/plantillaUsuario/1")
                        .principal(mockPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/plantillaUsuario/{id} - Prohibido")
    void findById_Forbidden() throws Exception {
        when(service.esPropietario(eq(1), anyString())).thenReturn(false);

        mockMvc.perform(get("/api/plantillaUsuario/1")
                        .principal(mockPrincipal))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/plantillaUsuario - Crear")
    void create_Ok() throws Exception {
        when(service.save(any(PlantillaUsuarioDTO.class), anyString())).thenReturn(dtoPrueba);

        String json = "{\"nombre\": \"Plantilla Usuario Test\"}";

        mockMvc.perform(post("/api/plantillaUsuario")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Plantilla Usuario Test"));
    }

    @Test
    @DisplayName("PUT /api/plantillaUsuario/{id} - Actualización exitosa")
    void update_Ok() throws Exception {
        // 1. Mockeamos que SÍ es el propietario
        when(service.esPropietario(eq(1), anyString())).thenReturn(true);

        // 2. Mockeamos el guardado (el service recibe el DTO y el email del principal)
        when(service.save(any(PlantillaUsuarioDTO.class), anyString())).thenReturn(dtoPrueba);

        String json = """
            {
                "nombre": "Plantilla Actualizada",
                "descripcion": "Nueva descripción"
            }
        """;

        mockMvc.perform(put("/api/plantillaUsuario/1")
                        .principal(mockPrincipal) // El mockPrincipal que ya tenemos en el setUp
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Plantilla Usuario Test"));
    }

    @Test
    @DisplayName("PUT /api/plantillaUsuario/{id} - Prohibido (No es el dueño)")
    void update_Forbidden() throws Exception {
        // 1. Mockeamos que NO es el propietario
        when(service.esPropietario(eq(1), anyString())).thenReturn(false);

        String json = "{\"nombre\": \"Intento de hackeo\"}";

        mockMvc.perform(put("/api/plantillaUsuario/1")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isForbidden()); // Esperamos un 403
    }

    @Test
    @DisplayName("DELETE /api/plantillaUsuario/{id} - Éxito")
    void delete_Ok() throws Exception {
        when(service.esPropietario(eq(1), anyString())).thenReturn(true);

        mockMvc.perform(delete("/api/plantillaUsuario/1")
                        .principal(mockPrincipal))
                .andExpect(status().isNoContent());
    }
}