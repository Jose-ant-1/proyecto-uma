package com.page.api_uma.controller;

import com.page.api_uma.dto.PaginaWebDTO;
import com.page.api_uma.model.PaginaWeb;
import com.page.api_uma.service.PaginaWebService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PaginaWebControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaginaWebService paginaService;

    @InjectMocks
    private PaginaWebController paginaWebController;

    private PaginaWeb paginaPrueba;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paginaWebController).build();

        paginaPrueba = new PaginaWeb();
        paginaPrueba.setId(1);
        paginaPrueba.setNombre("Google");
        paginaPrueba.setUrl("https://google.com");
    }

    @Test
    @DisplayName("GET /api/paginas - Listar todas")
    void findAll_Ok() throws Exception {
        when(paginaService.findAll()).thenReturn(List.of(paginaPrueba));

        mockMvc.perform(get("/api/paginas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Google"));
    }

    @Test
    @DisplayName("GET /api/paginas/{id} - Encontrada")
    void findById_Ok() throws Exception {
        when(paginaService.findById(1)).thenReturn(paginaPrueba);

        mockMvc.perform(get("/api/paginas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://google.com"));
    }

    @Test
    @DisplayName("GET /api/paginas/{id} - No encontrada")
    void findById_NotFound() throws Exception {
        when(paginaService.findById(99)).thenReturn(null);

        mockMvc.perform(get("/api/paginas/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/paginas - Crear")
    void create_Ok() throws Exception {
        when(paginaService.save(any(PaginaWebDTO.class))).thenReturn(paginaPrueba);

        String json = """
            {
                "nombre": "Google",
                "url": "https://google.com"
            }
        """;

        mockMvc.perform(post("/api/paginas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("PUT /api/paginas/{id} - Actualizar éxito")
    void update_Ok() throws Exception {
        when(paginaService.findById(1)).thenReturn(paginaPrueba);
        when(paginaService.save(any(PaginaWebDTO.class))).thenReturn(paginaPrueba);

        String json = """
            {
                "nombre": "Google Editado",
                "url": "https://google.es"
            }
        """;

        mockMvc.perform(put("/api/paginas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Google"));
        // Nota: devuelve 'Google' porque el mock devuelve paginaPrueba sin cambios
    }

    @Test
    @DisplayName("GET /api/paginas/{id}/check-status - Salud de la web")
    void checkHealth_Ok() throws Exception {
        when(paginaService.findById(1)).thenReturn(paginaPrueba);
        when(paginaService.getRemoteStatus(anyString())).thenReturn(200);

        mockMvc.perform(get("/api/paginas/1/check-status"))
                .andExpect(status().isOk())
                .andExpect(content().string("200"));
    }

    @Test
    @DisplayName("DELETE /api/paginas/{id} - Borrar")
    void delete_Ok() throws Exception {
        mockMvc.perform(delete("/api/paginas/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/paginas/buscar - Búsqueda por query")
    void buscar_Ok() throws Exception {
        // 1. Preparamos el resultado esperado
        PaginaWeb resultado = new PaginaWeb();
        resultado.setId(2);
        resultado.setNombre("Resultado Buscado");
        resultado.setUrl("https://resultado.com");

        // 2. Mockeamos el servicio (el controller usa .buscarPaginas(q))
        when(paginaService.buscarPaginas("google")).thenReturn(List.of(resultado));

        // 3. Ejecutamos la petición con el parámetro ?q=google
        mockMvc.perform(get("/api/paginas/buscar")
                        .param("q", "google"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Resultado Buscado"))
                .andExpect(jsonPath("$[0].url").value("https://resultado.com"));
    }
}
