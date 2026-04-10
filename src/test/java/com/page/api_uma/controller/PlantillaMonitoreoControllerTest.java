package com.page.api_uma.controller;

import com.page.api_uma.dto.PlantillaMonitoreoDTO;
import com.page.api_uma.model.PlantillaMonitoreo;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.service.PlantillaMonitoreoService;
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

import java.security.Principal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PlantillaMonitoreoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PlantillaMonitoreoService plantillaService;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private PlantillaMonitoreoController plantillaController;

    private PlantillaMonitoreo plantillaPrueba;
    private PlantillaMonitoreoDTO plantillaDTO;
    private Usuario usuarioPrueba;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(plantillaController).build();

        usuarioPrueba = new Usuario();
        usuarioPrueba.setId(1);
        usuarioPrueba.setEmail("test@test.com");

        plantillaPrueba = new PlantillaMonitoreo();
        plantillaPrueba.setId(1);
        plantillaPrueba.setNombre("Plantilla Base");

        plantillaDTO = new PlantillaMonitoreoDTO();
        plantillaDTO.setId(1);
        plantillaDTO.setNombre("Plantilla Base");
    }

    @Test
    @DisplayName("GET /api/plantillaMonitoreo - Buscar por Principal")
    void findAll_Ok() throws Exception {
        // Mockeamos el Principal que inyecta Spring
        Principal mockPrincipal = mock(Principal.class);
        when(mockPrincipal.getName()).thenReturn("test@test.com");
        when(plantillaService.findByPropietario("test@test.com")).thenReturn(List.of(plantillaPrueba));

        mockMvc.perform(get("/api/plantillaMonitoreo")
                        .principal(mockPrincipal)) // Inyectamos el principal aquí
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Plantilla Base"));
    }

    @Test
    @DisplayName("GET /api/plantillaMonitoreo/{id} - Encontrada")
    void findById_Ok() throws Exception {
        when(plantillaService.findById(1)).thenReturn(plantillaPrueba);

        mockMvc.perform(get("/api/plantillaMonitoreo/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Plantilla Base"));
    }

    @Test
    @DisplayName("GET /api/plantillaMonitoreo/{id} - No encontrada")
    void findById_NotFound() throws Exception {
        when(plantillaService.findById(99)).thenReturn(null);

        mockMvc.perform(get("/api/plantillaMonitoreo/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/plantillaMonitoreo/propietario/{id} - Listar por ID de usuario")
    void findByPropietarioId_Ok() throws Exception {
        when(plantillaService.findByUsuario(1)).thenReturn(List.of(plantillaPrueba));

        mockMvc.perform(get("/api/plantillaMonitoreo/propietario/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Plantilla Base"));
    }

    @Test
    @DisplayName("POST /api/plantillaMonitoreo - Crear")
    void create_Ok() throws Exception {
        when(plantillaService.save(any(PlantillaMonitoreoDTO.class))).thenReturn(plantillaDTO);

        String json = """
            {
                "nombre": "Plantilla Base",
                "descripcion": "Test"
            }
        """;

        mockMvc.perform(post("/api/plantillaMonitoreo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Plantilla Base"));
    }

    @Test
    @DisplayName("POST /api/plantillaMonitoreo/{id}/aplicar - Éxito")
    void aplicarPlantilla_Ok() throws Exception {
        when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioPrueba);

        String json = "{\"email\": \"destino@test.com\"}";

        mockMvc.perform(post("/api/plantillaMonitoreo/1/aplicar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/plantillaMonitoreo/{id} - Actualizar")
    void update_Ok() throws Exception {
        when(plantillaService.findById(1)).thenReturn(plantillaPrueba);
        when(plantillaService.save(any(PlantillaMonitoreoDTO.class))).thenReturn(plantillaDTO);

        String json = "{\"nombre\": \"Plantilla Actualizada\"}";

        mockMvc.perform(put("/api/plantillaMonitoreo/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("DELETE /api/plantillaMonitoreo/{id}")
    void delete_Ok() throws Exception {
        mockMvc.perform(delete("/api/plantillaMonitoreo/1"))
                .andExpect(status().isNoContent());
    }
}
