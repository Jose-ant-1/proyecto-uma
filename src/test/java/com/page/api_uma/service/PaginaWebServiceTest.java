package com.page.api_uma.service;

import com.page.api_uma.dto.PaginaWebDTO;
import com.page.api_uma.mapper.PaginaWebMapper;
import com.page.api_uma.model.PaginaWeb;
import com.page.api_uma.repository.PaginaWebRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaginaWebServiceTest {

    @Mock
    private PaginaWebRepository paginaWebRepository;

    @Mock
    private PaginaWebMapper paginaWebMapper;

    @InjectMocks
    private PaginaWebService paginaWebService;

    private PaginaWeb paginaEjemplo;
    private PaginaWebDTO paginaDTOEjemplo;

    @BeforeEach
    void setUp() {
        paginaEjemplo = new PaginaWeb();
        paginaEjemplo.setId(1);
        paginaEjemplo.setNombre("Google");
        paginaEjemplo.setUrl("https://google.com");
        paginaEjemplo.setNotaInfo("Buscador principal");

        paginaDTOEjemplo = new PaginaWebDTO();
        paginaDTOEjemplo.setNombre("Google");
        paginaDTOEjemplo.setUrl("https://google.com");
    }

    @Test
    @DisplayName("findAll: Debería retornar lista de páginas ordenada por nombre")
    void findAll_DeberiaRetornarListaOrdenada() {

        List<PaginaWeb> paginas = List.of(paginaEjemplo, new PaginaWeb());
        when(paginaWebRepository.findAllByOrderByNombreAsc()).thenReturn(paginas);

        List<PaginaWeb> resultado = paginaWebService.findAll();

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        verify(paginaWebRepository, times(1)).findAllByOrderByNombreAsc();
    }

    @Test
    @DisplayName("findById: Debería retornar la página si el ID existe")
    void findById_Existente_DeberiaRetornarPagina() {

        int id = 1;
        when(paginaWebRepository.findById(id)).thenReturn(Optional.of(paginaEjemplo));

        PaginaWeb resultado = paginaWebService.findById(id);

        assertNotNull(resultado);
        assertEquals(id, resultado.getId());
        assertEquals("Google", resultado.getNombre());
        verify(paginaWebRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("findById: Debería retornar null si el ID no existe")
    void findById_NoExistente_DeberiaRetornarNull() {

        int idInexistente = 99;
        when(paginaWebRepository.findById(idInexistente)).thenReturn(Optional.empty());

        PaginaWeb resultado = paginaWebService.findById(idInexistente);

        assertNull(resultado);
        verify(paginaWebRepository, times(1)).findById(idInexistente);
    }

    @Test
    @DisplayName("save: Debería convertir DTO a entidad y guardarla")
    void save_DeberiaLlamarAlMapperYGuardar() {

        when(paginaWebMapper.toEntity(paginaDTOEjemplo)).thenReturn(paginaEjemplo);

        when(paginaWebRepository.save(paginaEjemplo)).thenReturn(paginaEjemplo);

        PaginaWeb resultado = paginaWebService.save(paginaDTOEjemplo);

        assertNotNull(resultado);
        assertEquals("Google", resultado.getNombre());
        assertEquals("https://google.com", resultado.getUrl());

        verify(paginaWebMapper, times(1)).toEntity(paginaDTOEjemplo);
        verify(paginaWebRepository, times(1)).save(paginaEjemplo);
    }

    @Test
    @DisplayName("deleteById: Debería llamar al repositorio para eliminar")
    void deleteById_DeberiaLlamarAlRepositorio() {

        int idEliminar = 1;
        doNothing().when(paginaWebRepository).deleteById(idEliminar);

        paginaWebService.deleteById(idEliminar);

        verify(paginaWebRepository, times(1)).deleteById(idEliminar);
    }

    @Test
    @DisplayName("buscarPaginas: Debería retornar lista vacía si el término es nulo o vacío")
    void buscarPaginas_TerminoVacio_DeberiaRetornarListaVacia() {

        List<PaginaWeb> resultadoNulo = paginaWebService.buscarPaginas(null);
        List<PaginaWeb> resultadoVacio = paginaWebService.buscarPaginas("");

        assertTrue(resultadoNulo.isEmpty());
        assertTrue(resultadoVacio.isEmpty());
        verify(paginaWebRepository, never()).buscarPorTermino(anyString());
    }

    @Test
    @DisplayName("buscarPaginas: Debería llamar al repositorio si hay término")
    void buscarPaginas_ConTermino_DeberiaLlamarAlRepo() {

        String termino = "google";
        when(paginaWebRepository.buscarPorTermino(termino)).thenReturn(List.of(paginaEjemplo));

        List<PaginaWeb> resultado = paginaWebService.buscarPaginas(termino);

        assertFalse(resultado.isEmpty());
        assertEquals(1, resultado.size());
        verify(paginaWebRepository, times(1)).buscarPorTermino(termino);
    }

    @Test
    @DisplayName("getRemoteStatus: Debería retornar 200 para una URL válida (Integración Real)")
    void getRemoteStatus_UrlValida_DeberiaRetornar200() {

        int status = paginaWebService.getRemoteStatus("google.com");

        assertTrue(status >= 200 && status < 400);
    }

    @Test
    @DisplayName("getRemoteStatus: Debería retornar 404 si el host no existe")
    void getRemoteStatus_HostInexistente_DeberiaRetornar404() {

        int status = paginaWebService.getRemoteStatus("https://esta-url-no-existe-nunca-12345.com");

        assertEquals(404, status);
    }

    @Test
    @DisplayName("getRemoteStatus: Debería retornar 404 ante un host que no se puede resolver")
    void getRemoteStatus_UrlInvalida_DeberiaRetornar404() {

        int status = paginaWebService.getRemoteStatus("esto-no-es-una-url-valida-!!!!");

        assertEquals(404, status);
    }

    @Test
    @DisplayName("save: Debería lanzar IllegalArgumentException si el DTO es nulo")
    void save_DtoNulo_DeberiaLanzarExcepcion() {

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            paginaWebService.save(null);
        });

        assertEquals("La página web no puede ser nula", ex.getMessage());

        verify(paginaWebRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteById: Debería manejar errores si el ID no existe")
    void deleteById_NoExistente_DeberiaManejarExcepcion() {
        int idInexistente = 999;

        doThrow(new RuntimeException("Elemento no encontrado"))
                .when(paginaWebRepository).deleteById(idInexistente);

        assertThrows(RuntimeException.class, () -> {
            paginaWebService.deleteById(idInexistente);
        });
    }

    @Test
    @DisplayName("getRemoteStatus: Debería devolver 500 ante un error inesperado")
    void getRemoteStatus_ErrorInesperado_DeberiaRetornar500() {

        int status = paginaWebService.getRemoteStatus("://url-mal-formada");

        assertEquals(500, status);
    }

    @Test
    @DisplayName("buscarPaginas: Debería manejar cuando el repositorio devuelve null")
    void buscarPaginas_RepoDevuelveNull_DeberiaRetornarVacio() {
        when(paginaWebRepository.buscarPorTermino("error")).thenReturn(null);

        List<PaginaWeb> resultado = paginaWebService.buscarPaginas("error");

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("save: Debería lanzar IllegalStateException si el mapper devuelve null")
    void save_MapperDevuelveNull_DeberiaLanzarException() {

        when(paginaWebMapper.toEntity(any())).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> {
            paginaWebService.save(paginaDTOEjemplo);
        });
    }

    @Test
    @DisplayName("save: Debería lanzar IllegalArgumentException si el nombre en el DTO es vacío")
    void save_NombreVacio_DeberiaLanzarException() {

        paginaDTOEjemplo.setNombre("");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            paginaWebService.save(paginaDTOEjemplo);
        });
        assertEquals("El nombre de la página es obligatorio", ex.getMessage());
    }

    @Test
    @DisplayName("findById: Debería manejar ID nulo")
    void findById_IdNulo_DeberiaRetornarNullOManejarError() {

        PaginaWeb resultado = paginaWebService.findById(null);
        assertNull(resultado);
    }

    @Test
    @DisplayName("getRemoteStatus: Debería retornar 404 si el protocolo mal formado resulta en un host inexistente")
    void getRemoteStatus_ProtocoloInvalido_DeberiaRetornar404() {
        int status = paginaWebService.getRemoteStatus("mailto:admin@test.com");
        assertEquals(404, status);
    }

    @Test
    @DisplayName("deleteById: Debería lanzar IllegalArgumentException si el ID es nulo")
    void deleteById_IdNulo_DeberiaLanzarExcepcion() {

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            paginaWebService.deleteById(null);
        });

        assertEquals("El ID no puede ser nulo", ex.getMessage());
        verify(paginaWebRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("save: Debería lanzar IllegalArgumentException si el nombre son solo espacios")
    void save_NombreConEspacios_DeberiaLanzarException() {

        paginaDTOEjemplo.setNombre("   ");

        assertThrows(IllegalArgumentException.class, () -> {
            paginaWebService.save(paginaDTOEjemplo);
        });

        verify(paginaWebRepository, never()).save(any());
    }

    @Test
    @DisplayName("findAll: Debería retornar lista vacía si no hay datos")
    void findAll_Vacio_DeberiaRetornarListaVacia() {

        when(paginaWebRepository.findAllByOrderByNombreAsc()).thenReturn(Collections.emptyList());


        List<PaginaWeb> resultado = paginaWebService.findAll();

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("getRemoteStatus: Debería manejar una URL con espacios o malformada")
    void getRemoteStatus_UrlConEspacios_DeberiaRetornar500() {

        int status = paginaWebService.getRemoteStatus("https://google .com/ prueba");

        assertEquals(500, status);
    }

    @Test
    @DisplayName("getRemoteStatus: Debería manejar protocolos no soportados (ClassCastException)")
    void getRemoteStatus_ProtocoloNoHttp_DeberiaRetornar500() {

        int status = paginaWebService.getRemoteStatus("mailto:test@test.com");
        assertEquals(404, status);
    }

    @Test
    @DisplayName("getRemoteStatus: Debería retornar 500 si hay un timeout de conexión")
    void getRemoteStatus_Timeout_DeberiaRetornar500() {
        // Nota: Este test es difícil de forzar sin mockear la red,
        // pero puedes usar una IP no enrutable
        int status = paginaWebService.getRemoteStatus("http://10.255.255.1");
        assertEquals(500, status);
    }

    @Test
    @DisplayName("save: Debería lanzar Exception si la URL es nula o vacía")
    void save_UrlInvalida_DeberiaLanzarException() {
        paginaDTOEjemplo.setUrl(""); // URL vacía

        assertThrows(IllegalArgumentException.class, () -> {
            paginaWebService.save(paginaDTOEjemplo);
        });
    }

    @Test
    @DisplayName("save: Debería propagar error si el repositorio falla (ej. nombre duplicado)")
    void save_ErrorPersistencia_DeberiaPropagarException() {
        when(paginaWebMapper.toEntity(any())).thenReturn(paginaEjemplo);
        when(paginaWebRepository.save(any())).thenThrow(new RuntimeException("Error de DB"));

        assertThrows(RuntimeException.class, () -> {
            paginaWebService.save(paginaDTOEjemplo);
        });
    }

    @Test
    @DisplayName("buscarPaginas: Debería retornar lista vacía si el término solo contiene espacios")
    void buscarPaginas_TerminoConEspacios_DeberiaRetornarListaVacia() {
        // Simulamos que el usuario envía espacios
        List<PaginaWeb> resultado = paginaWebService.buscarPaginas("   ");

        assertTrue(resultado.isEmpty(), "Debería tratar strings de espacios como vacíos");
        verify(paginaWebRepository, never()).buscarPorTermino(anyString());
    }

    @Test
    @DisplayName("save: Debería lanzar Exception si la URL tiene un formato inválido")
    void save_UrlFormatoInvalido_DeberiaLanzarException() {
        paginaDTOEjemplo.setUrl("esto-no-es-una-url");

        // Si decides implementar una validación por Regex en el futuro,
        // este test te servirá para asegurar que no se guarden datos basura.
        assertThrows(IllegalArgumentException.class, () -> {
            paginaWebService.save(paginaDTOEjemplo);
        });
    }

    @Test
    @DisplayName("save: No debería tocar el repositorio si el mapper falla")
    void save_MapperFalla_NoDeberiaPersistir() {
        when(paginaWebMapper.toEntity(any())).thenReturn(null);

        assertThrows(IllegalStateException.class, () -> {
            paginaWebService.save(paginaDTOEjemplo);
        });

        verify(paginaWebRepository, never()).save(any());
    }

    @Test
    @DisplayName("getRemoteStatus: Debería retornar 500 ante una URL con sintaxis ilegal")
    void getRemoteStatus_SintaxisIlegal_DeberiaRetornar500() {
        // Caracteres que URI.create() no puede procesar sin codificar
        int status = paginaWebService.getRemoteStatus("https://google.com/search?q=un espacio");

        assertEquals(500, status);
    }

}