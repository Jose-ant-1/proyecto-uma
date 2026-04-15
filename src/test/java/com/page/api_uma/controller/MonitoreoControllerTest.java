package com.page.api_uma.controller;

import com.page.api_uma.dto.MonitoreoDTODetalle;
import com.page.api_uma.dto.MonitoreoListadoDTO;
import com.page.api_uma.model.PaginaWeb;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.service.MonitoreoService;
import com.page.api_uma.service.UsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MonitoreoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MonitoreoService monitoreoService;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private MonitoreoController monitoreoController;

    private Usuario usuarioPrueba;
    private MonitoreoDTODetalle dtoDetalle;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(monitoreoController).build();

        usuarioPrueba = new Usuario();
        usuarioPrueba.setId(1);
        usuarioPrueba.setEmail("user@test.com");
        usuarioPrueba.setPermiso("USER");
        usuarioPrueba.setMonitoreosInvitado(null);

        dtoDetalle = new MonitoreoDTODetalle();
        dtoDetalle.setNombre("Monitor Test");

        when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioPrueba);
    }

    @Test
    @DisplayName("GET /api/monitoreos - Listar mis monitoreos")
    void getAllMyMonitoreos_Ok() throws Exception {
        MonitoreoListadoDTO item = new MonitoreoListadoDTO();
        item.setNombre("Mi Web Monitorizada");

        when(monitoreoService.getMisMonitoreosOrdenados(any(Usuario.class)))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/monitoreos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Mi Web Monitorizada"));
    }

    @Test
    @DisplayName("GET /api/monitoreos/buscar - Búsqueda por término")
    void buscarMonitoreos_Ok() throws Exception {
        MonitoreoListadoDTO resultado = new MonitoreoListadoDTO();
        resultado.setNombre("Resultado de búsqueda");

        // Mockeamos el servicio de búsqueda accesible
        when(monitoreoService.buscarAccesibles(anyInt(), eq("test")))
                .thenReturn(List.of(resultado));

        mockMvc.perform(get("/api/monitoreos/buscar")
                        .param("termino", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Resultado de búsqueda"));
    }

    @Test
    @DisplayName("GET /api/monitoreos/colaboraciones - Lista vacía")
    void getMonitoreosInvitado_Ok() throws Exception {
        mockMvc.perform(get("/api/monitoreos/colaboraciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/monitoreos/all - Error 403 si no es ADMIN")
    void getAllForAdmin_Forbidden() throws Exception {
        mockMvc.perform(get("/api/monitoreos/all"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/monitoreos/all - OK si es ADMIN")
    void getAllForAdmin_Ok() throws Exception {
        usuarioPrueba.setPermiso("ADMIN");
        when(monitoreoService.findAll()).thenReturn(List.of(new MonitoreoListadoDTO()));

        mockMvc.perform(get("/api/monitoreos/all"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/monitoreos/{id}/pagina - Éxito")
    void getPaginaByMonitoreoId_Ok() throws Exception {
        PaginaWeb pagina = new PaginaWeb();
        pagina.setId(10);
        pagina.setUrl("https://test.com");

        when(monitoreoService.obtenerPaginaPorMonitoreoId(eq(1), anyInt())).thenReturn(pagina);

        mockMvc.perform(get("/api/monitoreos/1/pagina"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://test.com"));
    }

    @Test
    @DisplayName("POST /api/monitoreos/{id}/check - Ejecutar ahora")
    void checkNow_Ok() throws Exception {
        when(monitoreoService.ejecutarChequeo(1)).thenReturn(dtoDetalle);

        mockMvc.perform(post("/api/monitoreos/1/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Monitor Test"));
    }

    @Test
    @DisplayName("PUT /api/monitoreos/invitar - Éxito")
    void invitar_Ok() throws Exception {
        // En el controller usas @RequestBody List<Integer> y @RequestParam List<String>
        String jsonIds = "[1, 2]";

        mockMvc.perform(put("/api/monitoreos/invitar")
                        .param("emails", "test1@gmail.com", "test2@gmail.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonIds))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/monitoreos/{id} - Éxito")
    void delete_Ok() throws Exception {
        when(monitoreoService.eliminar(eq(1), anyInt(), anyString())).thenReturn(true);

        mockMvc.perform(delete("/api/monitoreos/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/monitoreos - Bad Request por validación")
    void create_BadRequest() throws Exception {
        
        String jsonInvalido = """
            {
                "paginaUrl": "https://test.com",
                "nombre": "Monitor Malo",
                "minutos": 0,
                "repeticiones": 1
            }
        """;

        mockMvc.perform(post("/api/monitoreos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonInvalido))
                .andExpect(status().isBadRequest());
    }
}