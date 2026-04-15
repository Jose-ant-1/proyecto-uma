package com.page.api_uma.service;

import com.page.api_uma.dto.MonitoreoDTODetalle;
import com.page.api_uma.dto.MonitoreoListadoDTO;
import com.page.api_uma.model.Monitoreo;
import com.page.api_uma.model.PaginaWeb;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.repository.MonitoreoRepository;
import com.page.api_uma.repository.PaginaWebRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoreoServiceTest {

    @Mock
    private MonitoreoRepository monitoreoRepository;
    @Mock
    private PaginaWebRepository paginaWebRepository;
    @Mock
    private UsuarioService usuarioService;
    @Mock
    private PaginaWebService paginaWebService;

    @InjectMocks
    private MonitoreoService monitoreoService;

    private Usuario usuarioPrueba;
    private PaginaWeb paginaPrueba;

    @BeforeEach
    void setUp() {
        usuarioPrueba = new Usuario();
        usuarioPrueba.setId(1);
        usuarioPrueba.setNombre("Usuario Test");

        paginaPrueba = new PaginaWeb();
        paginaPrueba.setId(10);
        paginaPrueba.setUrl("https://google.com");
    }

    @Test
    @DisplayName("Crear: Debe crear monitoreo con página existente")
    void crearMonitoreo_PaginaExiste_RetornaDTO() {
        when(paginaWebRepository.findByUrl(anyString())).thenReturn(Optional.of(paginaPrueba));
        when(monitoreoRepository.save(any(Monitoreo.class))).thenAnswer(i -> i.getArguments()[0]);

        MonitoreoDTODetalle resultado = monitoreoService.crearMonitoreo(usuarioPrueba, "https://google.com", "Monitor 1", 5, 3);

        assertNotNull(resultado);
        assertEquals("Monitor 1", resultado.getNombre());
        verify(paginaWebRepository, never()).save(any());
    }

    @Test
    @DisplayName("Crear: Debe crear página nueva si la URL no existe")
    void crearMonitoreo_PaginaNoExiste_CreaPaginaYMonitoreo() {
        when(paginaWebRepository.findByUrl(anyString())).thenReturn(Optional.empty());
        when(paginaWebRepository.save(any(PaginaWeb.class))).thenAnswer(i -> i.getArguments()[0]);
        when(monitoreoRepository.save(any(Monitoreo.class))).thenAnswer(i -> i.getArguments()[0]);

        MonitoreoDTODetalle resultado = monitoreoService.crearMonitoreo(usuarioPrueba, "https://nueva.com", "Nuevo", 10, 1);

        assertNotNull(resultado);
        verify(paginaWebRepository).save(any(PaginaWeb.class));
    }

    @Test
    @DisplayName("Actualizar: Dueño puede actualizar datos básicos")
    void actualizar_EsDuenio_ActualizaCampos() {

        Monitoreo m = new Monitoreo();
        m.setId(100);
        m.setPropietario(usuarioPrueba);
        m.setPaginaWeb(paginaPrueba);

        Map<String, Object> payload = Map.of(
                "nombre", "Nuevo Nombre",
                "minutos", 15
        );

        when(monitoreoRepository.findById(100)).thenReturn(Optional.of(m));
        when(monitoreoRepository.save(any(Monitoreo.class))).thenReturn(m);

        MonitoreoDTODetalle resultado = monitoreoService.actualizar(100, payload, usuarioPrueba.getId(), "USER");

        assertEquals("Nuevo Nombre", resultado.getNombre());
        assertEquals(15, resultado.getMinutos());
    }

    @Test
    @DisplayName("Actualizar: ADMIN puede actualizar aunque no sea dueño")
    void actualizar_EsAdmin_PermiteActualizar() {
        Monitoreo m = new Monitoreo();
        m.setPropietario(new Usuario());
        m.getPropietario().setId(999);
        m.setPaginaWeb(paginaPrueba);

        when(monitoreoRepository.findById(1)).thenReturn(Optional.of(m));
        when(monitoreoRepository.save(any(Monitoreo.class))).thenReturn(m);

        Map<String, Object> payload = Map.of("nombre", "Editado por Admin");

        MonitoreoDTODetalle resultado = monitoreoService.actualizar(1, payload, 1, "ADMIN");

        assertNotNull(resultado);
    }

    @Test
    @DisplayName("Actualizar: Retorna null si no tiene permisos")
    void actualizar_SinPermiso_RetornaNull() {
        Monitoreo m = new Monitoreo();
        m.setPropietario(usuarioPrueba);

        when(monitoreoRepository.findById(1)).thenReturn(Optional.of(m));

        MonitoreoDTODetalle resultado = monitoreoService.actualizar(1, Map.of(), 2, "USER");

        assertNull(resultado);
        verify(monitoreoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Eliminar: Dueño puede eliminar y limpia relaciones")
    void eliminar_EsDuenio_BorradoExitoso() {

        Monitoreo m = new Monitoreo();
        m.setId(1);
        m.setPropietario(usuarioPrueba);
        m.getInvitados().add(new Usuario());

        when(monitoreoRepository.findById(1)).thenReturn(Optional.of(m));

        boolean resultado = monitoreoService.eliminar(1, usuarioPrueba.getId(), "USER");

        assertTrue(resultado);
        assertTrue(m.getInvitados().isEmpty());
        verify(monitoreoRepository).eliminarRelacionesConPlantillas(1);
        verify(monitoreoRepository).delete(m);
    }

    @Test
    @DisplayName("Eliminar: ADMIN puede eliminar cualquier monitoreo")
    void eliminar_EsAdmin_BorradoExitoso() {
        Monitoreo m = new Monitoreo();
        m.setPropietario(new Usuario());
        m.getPropietario().setId(99);

        when(monitoreoRepository.findById(1)).thenReturn(Optional.of(m));

        boolean resultado = monitoreoService.eliminar(1, 1, "ADMIN");

        assertTrue(resultado);
        verify(monitoreoRepository).delete(m);
    }

    @Test
    @DisplayName("Eliminar: Retorna false si el monitoreo no existe o no tiene permiso")
    void eliminar_SinPermisoONoExiste_RetornaFalse() {

        when(monitoreoRepository.findById(1)).thenReturn(Optional.empty());
        assertFalse(monitoreoService.eliminar(1, 1, "USER"));

        Monitoreo m = new Monitoreo();
        m.setPropietario(new Usuario());
        m.getPropietario().setId(50);
        when(monitoreoRepository.findById(2)).thenReturn(Optional.of(m));

        assertFalse(monitoreoService.eliminar(2, 1, "USER"));
        verify(monitoreoRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Detalle: Dueño puede ver su monitoreo")
    void obtenerDetalle_EsDuenio_RetornaDTO() {
        Monitoreo m = new Monitoreo();
        m.setPropietario(usuarioPrueba);
        m.setPaginaWeb(paginaPrueba);

        when(monitoreoRepository.findById(1)).thenReturn(Optional.of(m));
        when(usuarioService.findById(usuarioPrueba.getId())).thenReturn(usuarioPrueba);

        MonitoreoDTODetalle resultado = monitoreoService.obtenerDetalle(1, usuarioPrueba.getId());

        assertNotNull(resultado);
        assertEquals(usuarioPrueba.getId(), resultado.getPropietario().getId());
    }

    @Test
    @DisplayName("Detalle: Invitado puede ver el monitoreo")
    void obtenerDetalle_EsInvitado_RetornaDTO() {
        Usuario invitado = new Usuario();
        invitado.setId(20);
        invitado.setPermiso("USER");

        Monitoreo m = new Monitoreo();
        m.setPropietario(usuarioPrueba);
        m.getInvitados().add(invitado);
        m.setPaginaWeb(paginaPrueba);

        when(monitoreoRepository.findById(1)).thenReturn(Optional.of(m));
        when(usuarioService.findById(20)).thenReturn(invitado);

        MonitoreoDTODetalle resultado = monitoreoService.obtenerDetalle(1, 20);

        assertNotNull(resultado);
    }

    @Test
    @DisplayName("Detalle: ADMIN puede ver monitoreo ajeno")
    void obtenerDetalle_EsAdmin_RetornaDTO() {
        Usuario admin = new Usuario();
        admin.setId(100);
        admin.setPermiso("ADMIN");

        Monitoreo m = new Monitoreo();
        m.setPropietario(usuarioPrueba);
        m.setPaginaWeb(paginaPrueba);

        when(monitoreoRepository.findById(1)).thenReturn(Optional.of(m));
        when(usuarioService.findById(100)).thenReturn(admin);

        MonitoreoDTODetalle resultado = monitoreoService.obtenerDetalle(1, 100);

        assertNotNull(resultado);
    }

    @Test
    @DisplayName("Detalle: Usuario ajeno recibe null (Forbidden)")
    void obtenerDetalle_SinAcceso_RetornaNull() {
        Usuario ajeno = new Usuario();
        ajeno.setId(500);
        ajeno.setPermiso("USER");

        Monitoreo m = new Monitoreo();
        m.setPropietario(usuarioPrueba);

        when(monitoreoRepository.findById(1)).thenReturn(Optional.of(m));
        when(usuarioService.findById(500)).thenReturn(ajeno);

        MonitoreoDTODetalle resultado = monitoreoService.obtenerDetalle(1, 500);

        assertNull(resultado);
    }

    @Test
    @DisplayName("FindAll: Debe retornar lista de todos los monitoreos (Admin)")
    void findAll_RetornaListaCompleta() {

        Monitoreo m1 = new Monitoreo();
        m1.setPropietario(usuarioPrueba);
        m1.setPaginaWeb(paginaPrueba);

        Monitoreo m2 = new Monitoreo();
        m2.setPropietario(usuarioPrueba);
        m2.setPaginaWeb(paginaPrueba);

        when(monitoreoRepository.findAll()).thenReturn(List.of(m1, m2));

        List<MonitoreoListadoDTO> resultado = monitoreoService.findAll();

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        verify(monitoreoRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("ObtenerPagina: Dueño puede obtener la página")
    void obtenerPagina_EsDuenio_RetornaPagina() {

        Monitoreo m = new Monitoreo();
        m.setPropietario(usuarioPrueba);
        m.setPaginaWeb(paginaPrueba);

        when(monitoreoRepository.findById(10)).thenReturn(Optional.of(m));

        PaginaWeb resultado = monitoreoService.obtenerPaginaPorMonitoreoId(10, 1);

        assertNotNull(resultado);
        assertEquals(paginaPrueba.getUrl(), resultado.getUrl());
    }

    @Test
    @DisplayName("ObtenerPagina: Invitado puede obtener la página")
    void obtenerPagina_EsInvitado_RetornaPagina() {

        Usuario invitado = new Usuario();
        invitado.setId(20);

        Monitoreo m = new Monitoreo();
        m.setPropietario(usuarioPrueba);
        m.getInvitados().add(invitado);
        m.setPaginaWeb(paginaPrueba);

        when(monitoreoRepository.findById(10)).thenReturn(Optional.of(m));

        PaginaWeb resultado = monitoreoService.obtenerPaginaPorMonitoreoId(10, 20);

        assertNotNull(resultado);
    }

    @Test
    @DisplayName("ObtenerPagina: Usuario sin relación recibe null")
    void obtenerPagina_SinPermiso_RetornaNull() {

        Monitoreo m = new Monitoreo();
        m.setPropietario(usuarioPrueba);

        when(monitoreoRepository.findById(10)).thenReturn(Optional.of(m));

        PaginaWeb resultado = monitoreoService.obtenerPaginaPorMonitoreoId(10, 99);

        assertNull(resultado);
    }

    @Test
    @DisplayName("Chequeo: Debe actualizar estado y fecha al ejecutar")
    void ejecutarChequeo_MonitoreoExiste_ActualizaEstadoYFecha() {

        Monitoreo m = new Monitoreo();
        m.setId(5);
        m.setPaginaWeb(paginaPrueba);
        m.setPropietario(usuarioPrueba);

        when(monitoreoRepository.findById(5)).thenReturn(Optional.of(m));

        when(paginaWebService.getRemoteStatus("https://google.com")).thenReturn(200);

        when(monitoreoRepository.save(any(Monitoreo.class))).thenAnswer(i -> i.getArguments()[0]);

        MonitoreoDTODetalle resultado = monitoreoService.ejecutarChequeo(5);

        assertNotNull(resultado);
        assertEquals(200, m.getEstado());
        assertNotNull(m.getFechaUltimaRevision());
        verify(monitoreoRepository).save(m);
    }

    @Test
    @DisplayName("Chequeo: Retorna null si el monitoreo no existe")
    void ejecutarChequeo_NoExiste_RetornaNull() {
        when(monitoreoRepository.findById(99)).thenReturn(Optional.empty());

        MonitoreoDTODetalle resultado = monitoreoService.ejecutarChequeo(99);

        assertNull(resultado);
        verify(paginaWebService, never()).getRemoteStatus(anyString());
    }

    @Test
    @DisplayName("Mapeo: Convertir Monitoreo a ListadoDTO correctamente")
    void convertirAListadoDTO_MapeoCorrecto() {

        LocalDateTime ahora = LocalDateTime.now();
        Monitoreo m = new Monitoreo();
        m.setId(1);
        m.setNombre("Test Monitor");
        m.setPropietario(usuarioPrueba);
        m.setEstado(200);
        m.setFechaUltimaRevision(ahora);
        m.setActivo(true);
        m.setPaginaWeb(paginaPrueba);

        MonitoreoListadoDTO dto = monitoreoService.convertirAListadoDTO(m);

        assertAll("Verificación de campos del DTO",
                () -> assertEquals(m.getId(), dto.getId()),
                () -> assertEquals("Test Monitor", dto.getNombre()),
                () -> assertEquals(usuarioPrueba.getId(), dto.getPropietarioId()),
                () -> assertEquals(200, dto.getUltimoEstado()),
                () -> assertEquals(ahora, dto.getFechaUltimaRevision()),
                () -> assertTrue(dto.isActivo()),
                () -> assertEquals(paginaPrueba.getUrl(), dto.getPaginaUrl())
        );
    }

    @Test
    @DisplayName("Mapeo: Convertir Monitoreo a DetalleDTO con invitados")
    void convertirADetalleDTO_MapeoCompleto() {

        Usuario invitado = new Usuario();
        invitado.setId(2);
        invitado.setNombre("Invitado");
        invitado.setEmail("invitado@test.com");

        Monitoreo m = new Monitoreo();
        m.setId(10);
        m.setNombre("Monitor Detallado");
        m.setMinutos(10);
        m.setRepeticiones(5);
        m.setPropietario(usuarioPrueba);
        m.setPaginaWeb(paginaPrueba);
        m.setInvitados(Set.of(invitado));
        m.setEstado(200);

        MonitoreoDTODetalle dto = monitoreoService.convertirADetalleDTO(m);

        assertNotNull(dto);
        assertEquals(m.getNombre(), dto.getNombre());
        assertEquals(1, dto.getInvitados().size());
        assertEquals("Invitado", dto.getInvitados().iterator().next().getNombre());
        assertEquals(usuarioPrueba.getEmail(), dto.getPropietario().getEmail());
        assertEquals(paginaPrueba.getUrl(), dto.getPaginaUrl());
    }

    @Test
    @DisplayName("Búsqueda: Debe retornar monitoreos propios e invitados que coincidan con el término")
    void buscarAccesibles_ConTermino_FiltraCorrectamente() {

        Monitoreo propio = new Monitoreo();
        propio.setNombre("Servidor Produccion");
        propio.setPaginaWeb(paginaPrueba);
        propio.setPropietario(usuarioPrueba);

        Monitoreo invitado = new Monitoreo();
        invitado.setNombre("Web Marketing");

        PaginaWeb p2 = new PaginaWeb();
        p2.setUrl("https://marketing.es");
        invitado.setPaginaWeb(p2);

        Usuario otroUsuario = new Usuario();
        otroUsuario.setId(99);
        invitado.setPropietario(otroUsuario);

        usuarioPrueba.setMonitoreosPropios(List.of(propio));
        usuarioPrueba.setMonitoreosInvitado(Set.of(invitado));

        when(usuarioService.findById(1)).thenReturn(usuarioPrueba);

        List<MonitoreoListadoDTO> resNombre = monitoreoService.buscarAccesibles(1, "Produccion");
        assertEquals(1, resNombre.size());
        assertEquals("Servidor Produccion", resNombre.getFirst().getNombre());

        List<MonitoreoListadoDTO> resUrl = monitoreoService.buscarAccesibles(1, "marketing");
        assertEquals(1, resUrl.size());
        assertEquals("https://marketing.es", resUrl.getFirst().getPaginaUrl());

        List<MonitoreoListadoDTO> resTodo = monitoreoService.buscarAccesibles(1, "");
        assertEquals(2, resTodo.size());
    }

    @Test
    @DisplayName("Búsqueda: Retorna lista vacía si el usuario no existe")
    void buscarAccesibles_UsuarioNoExiste_RetornaVacio() {
        when(usuarioService.findById(999)).thenReturn(null);

        List<MonitoreoListadoDTO> resultado = monitoreoService.buscarAccesibles(999, "test");

        assertTrue(resultado.isEmpty());
    }

    @Test
    void invitacionEnMasa_ProcesaTodosLosElementos() {

        int propietarioId = 10;
        List<Integer> ids = List.of(1, 2);
        List<String> emails = List.of("invitado@test.com");

        when(monitoreoRepository.findById(anyInt())).thenAnswer(inv -> {
            Monitoreo m = new Monitoreo();
            m.setId((Integer) inv.getArgument(0));
            Usuario prop = new Usuario();
            prop.setId(propietarioId);
            m.setPropietario(prop);
            m.setInvitados(new HashSet<>());

            PaginaWeb pw = new PaginaWeb();
            pw.setUrl("http://test.com");
            m.setPaginaWeb(pw);

            return Optional.of(m);
        });

        Usuario invitado = new Usuario();
        invitado.setId(999);
        invitado.setEmail("invitado@test.com");
        when(usuarioService.buscarPorEmail(anyString())).thenReturn(invitado);

        when(monitoreoRepository.save(any(Monitoreo.class))).thenAnswer(i -> i.getArguments()[0]);

        monitoreoService.invitacionEnMasa(propietarioId, ids, emails);

        verify(monitoreoRepository, atLeastOnce()).save(any(Monitoreo.class));
    }

    @Test
    @DisplayName("Masivo: Quitar en masa procesa todas las eliminaciones")
    void quitarEnMasa_ProcesaTodasLasEliminaciones() {

        int propietarioId = 10;
        List<Integer> ids = List.of(1);
        List<String> emails = List.of("quitar1@test.com", "quitar2@test.com");

        Monitoreo m = new Monitoreo();
        m.setId(1);

        PaginaWeb pw = new PaginaWeb();
        pw.setUrl("https://test.com");
        m.setPaginaWeb(pw);

        Usuario propietario = new Usuario();
        propietario.setId(propietarioId);
        m.setPropietario(propietario);

        Usuario u1 = new Usuario();
        u1.setId(101);
        u1.setEmail("quitar1@test.com");

        Usuario u2 = new Usuario();
        u2.setId(102);
        u2.setEmail("quitar2@test.com");

        m.setInvitados(new HashSet<>(Set.of(u1, u2)));

        when(monitoreoRepository.findById(1)).thenReturn(Optional.of(m));

        when(usuarioService.buscarPorEmail("quitar1@test.com")).thenReturn(u1);
        when(usuarioService.buscarPorEmail("quitar2@test.com")).thenReturn(u2);

        when(monitoreoRepository.save(any(Monitoreo.class))).thenAnswer(i -> i.getArguments()[0]);

        monitoreoService.quitarEnMasa(propietarioId, ids, emails);

        verify(monitoreoRepository, times(2)).save(m);

        assertTrue(m.getInvitados().isEmpty(), "La lista de invitados debería estar vacía tras quitar a todos");
    }

    @Test
    @DisplayName("Ordenado: Debe retornar la lista de monitoreos del propietario")
    void getMisMonitoreosOrdenados_RetornaListaConvertida() {

        Monitoreo m1 = new Monitoreo();
        m1.setNombre("A - Monitor");
        m1.setPropietario(usuarioPrueba);
        m1.setPaginaWeb(paginaPrueba);

        Monitoreo m2 = new Monitoreo();
        m2.setNombre("B - Monitor");
        m2.setPropietario(usuarioPrueba);
        m2.setPaginaWeb(paginaPrueba);

        when(monitoreoRepository.findAllByPropietarioOrderByNombreAsc(usuarioPrueba))
                .thenReturn(List.of(m1, m2));

        List<MonitoreoListadoDTO> resultado = monitoreoService.getMisMonitoreosOrdenados(usuarioPrueba);

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals("A - Monitor", resultado.getFirst().getNombre());
        verify(monitoreoRepository).findAllByPropietarioOrderByNombreAsc(usuarioPrueba);
    }

    @Test
    @DisplayName("Borde: Crear con URL nula maneja la excepción y asigna nombre por defecto")
    void crearMonitoreo_UrlNula_AsignaNombrePorDefecto() {

        when(paginaWebRepository.findByUrl(null)).thenReturn(Optional.empty());

        org.mockito.ArgumentCaptor<PaginaWeb> paginaCaptor = org.mockito.ArgumentCaptor.forClass(PaginaWeb.class);
        when(paginaWebRepository.save(paginaCaptor.capture())).thenAnswer(i -> i.getArguments()[0]);
        when(monitoreoRepository.save(any(Monitoreo.class))).thenAnswer(i -> i.getArguments()[0]);

        MonitoreoDTODetalle resultado = monitoreoService.crearMonitoreo(usuarioPrueba, null, "Monitor Raro", 5, 3);

        assertNotNull(resultado);
        assertEquals("Nueva Página", paginaCaptor.getValue().getNombre());
        assertNull(paginaCaptor.getValue().getUrl());
    }

    @Test
    @DisplayName("Borde: Actualizar URL desenlaza la anterior y crea/enlaza la nueva")
    void actualizar_CambioDeUrl_AsignaNuevaPaginaWeb() {

        Monitoreo m = new Monitoreo();
        m.setId(100);
        m.setPropietario(usuarioPrueba);
        m.setPaginaWeb(paginaPrueba);

        String nuevaUrl = "https://mi-nuevo-dominio.com";
        Map<String, Object> payload = Map.of("paginaUrl", nuevaUrl);

        when(monitoreoRepository.findById(100)).thenReturn(Optional.of(m));

        when(paginaWebRepository.findByUrl(nuevaUrl)).thenReturn(Optional.empty());

        org.mockito.ArgumentCaptor<PaginaWeb> paginaCaptor = org.mockito.ArgumentCaptor.forClass(PaginaWeb.class);
        when(paginaWebRepository.save(paginaCaptor.capture())).thenAnswer(i -> i.getArguments()[0]);
        when(monitoreoRepository.save(any(Monitoreo.class))).thenAnswer(i -> i.getArguments()[0]);

        MonitoreoDTODetalle resultado = monitoreoService.actualizar(100, payload, usuarioPrueba.getId(), "USER");

        assertNotNull(resultado);
        assertEquals(nuevaUrl, resultado.getPaginaUrl());

        PaginaWeb paginaGuardada = paginaCaptor.getValue();
        assertEquals(nuevaUrl, paginaGuardada.getUrl());
        assertEquals("mi-nuevo-dominio.com", paginaGuardada.getNombre());
    }

    @Test
    @DisplayName("Borde: Un propietario no puede auto-invitarse a su propio monitoreo")
    void invitacionEnMasa_DuenioSeInvita_NoModificaInvitados() {

        int propietarioId = usuarioPrueba.getId();
        String emailPropietario = "propietario@test.com";
        usuarioPrueba.setEmail(emailPropietario);

        Monitoreo m = new Monitoreo();
        m.setId(10);
        m.setPropietario(usuarioPrueba);
        m.setPaginaWeb(paginaPrueba);
        m.setInvitados(new HashSet<>());

        when(monitoreoRepository.findById(10)).thenReturn(Optional.of(m));

        when(usuarioService.buscarPorEmail(emailPropietario)).thenReturn(usuarioPrueba);

        monitoreoService.invitacionEnMasa(propietarioId, List.of(10), List.of(emailPropietario));

        assertTrue(m.getInvitados().isEmpty(), "La lista de invitados debe seguir vacía porque el dueño no se puede añadir.");

        verify(monitoreoRepository, never()).save(any(Monitoreo.class));
    }

    @Test
    @DisplayName("Resiliencia: Actualizar con tipos de datos incorrectos no cambia el valor")
    void actualizar_PayloadMalformado_NoCambiaElValor() {

        Monitoreo m = new Monitoreo();
        m.setId(1);
        m.setPropietario(usuarioPrueba);
        m.setPaginaWeb(paginaPrueba);
        m.setMinutos(30);

        when(monitoreoRepository.findById(1)).thenReturn(Optional.of(m));
        when(monitoreoRepository.save(any(Monitoreo.class))).thenAnswer(i -> i.getArguments()[0]);

        Map<String, Object> payloadMalformado = Map.of("minutos", "quince");

        MonitoreoDTODetalle resultado = monitoreoService.actualizar(1, payloadMalformado, usuarioPrueba.getId(), "USER");

        assertNotNull(resultado);
        assertEquals(30, resultado.getMinutos());
    }

    @Test
    @DisplayName("Resiliencia: Fallo de red en ejecutarChequeo no rompe el sistema")
    void ejecutarChequeo_FalloServicioRed_ManejaErrorYDevuelveNull() {

        Monitoreo m = new Monitoreo();
        m.setId(5);
        m.setPaginaWeb(paginaPrueba);

        when(monitoreoRepository.findById(5)).thenReturn(Optional.of(m));

        when(paginaWebService.getRemoteStatus(anyString()))
                .thenThrow(new RuntimeException("Timeout de red"));

        MonitoreoDTODetalle resultado = monitoreoService.ejecutarChequeo(5);

        assertNull(resultado, "Si la red falla, el servicio debe devolver null en lugar de lanzar una excepción");

        verify(monitoreoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Borde: Crear monitoreo no valida valores negativos")
    void crearMonitoreo_ValoresNegativos_SeNormalizanAuno() {

        when(paginaWebRepository.findByUrl(anyString())).thenReturn(Optional.of(paginaPrueba));
        when(monitoreoRepository.save(any(Monitoreo.class))).thenAnswer(i -> i.getArguments()[0]);

        MonitoreoDTODetalle resultado = monitoreoService.crearMonitoreo(usuarioPrueba, "https://google.com", "Test Negativo", -10, -5);

        assertNotNull(resultado);
        assertEquals(1, resultado.getMinutos(), "Los minutos negativos deben convertirse en 1");
        assertEquals(1, resultado.getRepeticiones(), "Las repeticiones negativas deben convertirse en 1");
    }

    @Test
    @DisplayName("Borde: buscarAccesibles elimina duplicados si el dueño también está como invitado")
    void buscarAccesibles_UsuarioDuplicadoDuenioEInvitado_RetornaUnico() {
        Monitoreo m = new Monitoreo();
        m.setId(99);
        m.setNombre("Monitor Inconsistente");
        m.setPropietario(usuarioPrueba);
        m.setPaginaWeb(paginaPrueba);

        usuarioPrueba.setMonitoreosPropios(List.of(m));
        usuarioPrueba.setMonitoreosInvitado(Set.of(m));

        when(usuarioService.findById(1)).thenReturn(usuarioPrueba);

        List<MonitoreoListadoDTO> resultado = monitoreoService.buscarAccesibles(1, "");

        assertEquals(1, resultado.size(), "Debería retornar 1 solo elemento gracias al distinct()");
        assertEquals("Monitor Inconsistente", resultado.getFirst().getNombre());
    }

    @Test
    @DisplayName("Resiliencia: Operaciones en masa con listas nulas no deben romper el flujo")
    void operacionesEnMasa_ListasNulas_NoLanzaExcepcion() {

        assertDoesNotThrow(() -> {
            monitoreoService.quitarEnMasa(1, null, List.of("test@test.com"));
        }, "No debería fallar si la lista de IDs es nula");

        assertDoesNotThrow(() -> {
            monitoreoService.quitarEnMasa(1, List.of(1, 2), null);
        }, "No debería fallar si la lista de emails es nula");

        assertDoesNotThrow(() -> {
            monitoreoService.invitacionEnMasa(1, null, List.of("test@test.com"));
        }, "No debería fallar si la lista de IDs es nula en invitación");
    }

    @Test
    @DisplayName("Borde: buscarAccesibles con listas de monitoreo nulas no debe lanzar NPE")
    void buscarAccesibles_ListasNulas_ManejaSeguro() {

        Usuario usuarioIncompleto = new Usuario();
        usuarioIncompleto.setId(1);
        usuarioIncompleto.setMonitoreosPropios(null);
        usuarioIncompleto.setMonitoreosInvitado(null);

        when(usuarioService.findById(1)).thenReturn(usuarioIncompleto);

        List<MonitoreoListadoDTO> resultado = monitoreoService.buscarAccesibles(1, "test");

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty(), "Debería retornar una lista vacía en lugar de lanzar NPE");
    }

    @Test
    @DisplayName("Seguridad: obtenerDetalle cuando el usuario no existe en la DB")
    void obtenerDetalle_UsuarioNoEncontrado_RetornaNull() {
        when(monitoreoRepository.findById(1)).thenReturn(Optional.of(new Monitoreo()));

        when(usuarioService.findById(888)).thenReturn(null);

        MonitoreoDTODetalle resultado = monitoreoService.obtenerDetalle(1, 888);

        assertNull(resultado, "Debe retornar null si el usuario consultante no existe");
    }

    @Test
    @DisplayName("Borde: extraerDominio maneja entradas malformadas sin romper la creación")
    void crearMonitoreo_UrlMalformada_UsaNombrePorDefecto() {

        String urlBasura = "###//invalid-url";

        when(paginaWebRepository.findByUrl(urlBasura)).thenReturn(Optional.empty());
        when(paginaWebRepository.save(any(PaginaWeb.class))).thenAnswer(i -> i.getArguments()[0]);
        when(monitoreoRepository.save(any(Monitoreo.class))).thenAnswer(i -> i.getArguments()[0]);

        MonitoreoDTODetalle resultado = monitoreoService.crearMonitoreo(usuarioPrueba, urlBasura, "Test", 5, 1);

        assertNotNull(resultado);

        assertEquals("Test", resultado.getNombre());
    }

    @Test
    @DisplayName("Borde: Actualizar URL cuando el monitoreo no tenía página previa (Relación null)")
    void actualizar_SinPaginaPrevia_EnlazaNuevaCorrectamente() {
        Monitoreo mIncompleto = new Monitoreo();
        mIncompleto.setId(50);
        mIncompleto.setPropietario(usuarioPrueba);
        mIncompleto.setPaginaWeb(null);

        when(monitoreoRepository.findById(50)).thenReturn(Optional.of(mIncompleto));
        when(paginaWebRepository.findByUrl(anyString())).thenReturn(Optional.of(paginaPrueba));
        when(monitoreoRepository.save(any(Monitoreo.class))).thenAnswer(i -> i.getArguments()[0]);

        Map<String, Object> payload = Map.of("paginaUrl", "https://google.com");

        assertDoesNotThrow(() -> {
            monitoreoService.actualizar(50, payload, usuarioPrueba.getId(), "USER");
        });
    }

    @Test
    @DisplayName("Borde: Actualizar con tipos numéricos mixtos (Double/Float)")
    void actualizar_TiposNumericosDistintos_NoDebeRomper() {

        Monitoreo m = new Monitoreo();
        m.setId(1);
        m.setPropietario(usuarioPrueba);
        m.setPaginaWeb(paginaPrueba);

        Map<String, Object> payload = Map.of(
                "minutos", 15.0,
                "repeticiones", 5.5
        );

        when(monitoreoRepository.findById(1)).thenReturn(Optional.of(m));
        when(monitoreoRepository.save(any(Monitoreo.class))).thenReturn(m);

        assertDoesNotThrow(() -> {
            MonitoreoDTODetalle res = monitoreoService.actualizar(1, payload, 1, "USER");
            assertEquals(15, res.getMinutos());
        }, "El casting a Number en el Service debería soportar Double/Float");
    }

    @Test
    @DisplayName("Borde: Actualizar con valores null en el payload")
    void actualizar_ValoresNullEnPayload_LanzaNullPointerException() {
        Monitoreo m = new Monitoreo();
        m.setId(1);
        m.setPropietario(usuarioPrueba);

        when(monitoreoRepository.findById(1)).thenReturn(Optional.of(m));

        Map<String, Object> payload = new HashMap<>();
        payload.put("minutos", null);

        assertThrows(NullPointerException.class, () -> {
            monitoreoService.actualizar(1, payload, 1, "USER");
        }, "El Service actualmente no valida si el valor en el Map es null antes del casting");
    }

    @Test
    @DisplayName("Robustez: ejecutarChequeo gestiona Monitoreos sin PaginaWeb de forma segura")
    void ejecutarChequeo_SinPaginaWeb_ManejoSeguro() {

        Monitoreo m = new Monitoreo();
        m.setId(5);
        m.setPaginaWeb(null);

        when(monitoreoRepository.findById(5)).thenReturn(Optional.of(m));

        MonitoreoDTODetalle resultado = monitoreoService.ejecutarChequeo(5);

        assertNull(resultado, "El servicio debe retornar null de forma segura si hay inconsistencia de datos");

        verifyNoInteractions(paginaWebService);
    }

    @Test
    @DisplayName("Borde: extraerDominio con URL nula usa nombre por defecto")
    void crearMonitoreo_UrlVacia_UsaNombrePorDefecto() {

        when(paginaWebRepository.findByUrl(nullable(String.class))).thenReturn(Optional.empty());
        when(paginaWebRepository.save(any(PaginaWeb.class))).thenAnswer(i -> i.getArguments()[0]);
        when(monitoreoRepository.save(any(Monitoreo.class))).thenAnswer(i -> i.getArguments()[0]);

        MonitoreoDTODetalle resultado = monitoreoService.crearMonitoreo(usuarioPrueba, null, null, 5, 1);

        assertNotNull(resultado);

        assertNull(resultado.getNombre());
    }

    @Test
    @DisplayName("Masivo: invitacionEnMasa con IDs que no existen")
    void invitacionEnMasa_IdInexistente_NoDebeLanzarExcepcion() {

        Usuario prop = new Usuario();
        prop.setId(1);

        Monitoreo m1 = new Monitoreo();
        m1.setPropietario(prop);
        m1.setInvitados(new HashSet<>());

        when(monitoreoRepository.findById(1)).thenReturn(Optional.of(m1));
        when(monitoreoRepository.findById(99)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> {
            monitoreoService.invitacionEnMasa(1, List.of(1, 99), List.of("test@test.com"));
        }, "El bucle debe continuar aunque un monitoreoId sea inválido o falte");

        verify(monitoreoRepository).findById(1);
        verify(monitoreoRepository).findById(99);
    }

}
