package com.page.api_uma.service;

import com.page.api_uma.model.Monitoreo;
import com.page.api_uma.model.PaginaWeb;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.repository.MonitoreoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitoreoTaskServiceTest {

    @Captor
    private ArgumentCaptor<List<String>> destinatariosCaptor;

    @Mock
    private MonitoreoRepository repository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private MonitoreoTaskService monitoreoTaskService;

    private Monitoreo monitoreoEjemplo;
    private Usuario propietario;

    @BeforeEach
    void setUp() {
        propietario = new Usuario();
        propietario.setEmail("owner@test.com");

        PaginaWeb pagina = new PaginaWeb();
        pagina.setUrl("google.com");

        monitoreoEjemplo = new Monitoreo();
        monitoreoEjemplo.setId(1);
        monitoreoEjemplo.setNombre("Test Monitoreo");
        monitoreoEjemplo.setPaginaWeb(pagina);
        monitoreoEjemplo.setPropietario(propietario);
        monitoreoEjemplo.setActivo(true);
        monitoreoEjemplo.setMinutos(5);
        monitoreoEjemplo.setRepeticiones(3);
        monitoreoEjemplo.setFallosConsecutivos(0);
        monitoreoEjemplo.setAlertaEnviada(false);
        monitoreoEjemplo.setInvitados(new HashSet<>());
    }

    @Test
    @DisplayName("ejecutarRevisiones: Debería marcar estado 200 y resetear fallos si la web responde OK")
    void ejecutarRevisiones_SitioUp_DeberiaActualizarEstadoYResetearFallos() {

        monitoreoEjemplo.setFechaUltimaRevision(LocalDateTime.now().minusHours(1));
        when(repository.findAll()).thenReturn(List.of(monitoreoEjemplo));

        // Simulamos respuesta OK
        ResponseEntity<String> response = ResponseEntity.ok("Todo correcto");
        when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(response);

        monitoreoTaskService.ejecutarRevisionesProgramadas();

        assertEquals(200, monitoreoEjemplo.getEstado());
        assertEquals(0, monitoreoEjemplo.getFallosConsecutivos());
        assertFalse(monitoreoEjemplo.isAlertaEnviada());

        verify(repository).save(monitoreoEjemplo);
        // Verificamos que NO se envió email
        verify(emailService, never()).enviarNotificacionFallo(any(), anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("ejecutarRevisiones: Debería incrementar fallos pero NO enviar alerta si no llega al límite")
    void ejecutarRevisiones_PrimerFallo_DeberiaIncrementarContadorSinNotificar() {

        monitoreoEjemplo.setFechaUltimaRevision(null);
        monitoreoEjemplo.setFallosConsecutivos(0);
        monitoreoEjemplo.setRepeticiones(3); // Necesita 3 fallos para avisar

        when(repository.findAll()).thenReturn(List.of(monitoreoEjemplo));

        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new org.springframework.web.client.HttpServerErrorException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));

        monitoreoTaskService.ejecutarRevisionesProgramadas();

        assertEquals(500, monitoreoEjemplo.getEstado());
        assertEquals(1, monitoreoEjemplo.getFallosConsecutivos());
        assertFalse(monitoreoEjemplo.isAlertaEnviada());

        verify(emailService, never()).enviarNotificacionFallo(any(), anyString(), anyString(), anyInt());
        verify(repository).save(monitoreoEjemplo);
    }

    @Test
    @DisplayName("ejecutarRevisiones: Debería enviar alerta cuando se alcanza el límite de fallos")
    void ejecutarRevisiones_AlcanzaLimiteFallos_DeberiaEnviarEmail() {

        // Simulamos que ya lleva 2 fallos y el límite es 3.
        monitoreoEjemplo.setFallosConsecutivos(2);
        monitoreoEjemplo.setRepeticiones(3);
        monitoreoEjemplo.setAlertaEnviada(false);

        when(repository.findAll()).thenReturn(List.of(monitoreoEjemplo));

        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new org.springframework.web.client.HttpClientErrorException(org.springframework.http.HttpStatus.NOT_FOUND));

        monitoreoTaskService.ejecutarRevisionesProgramadas();

        assertEquals(404, monitoreoEjemplo.getEstado());
        assertEquals(3, monitoreoEjemplo.getFallosConsecutivos());
        assertTrue(monitoreoEjemplo.isAlertaEnviada(), "La marca de alertaEnviada debe ser true");

        verify(emailService, times(1)).enviarNotificacionFallo(
                anyList(),
                eq(monitoreoEjemplo.getNombre()),
                anyString(),
                eq(404)
        );
        verify(repository).save(monitoreoEjemplo);
    }

    @Test
    @DisplayName("ejecutarRevisiones: No debería re-enviar alerta si ya fue enviada (Evitar SPAM)")
    void ejecutarRevisiones_SigueCaidoPeroAlertaEnviada_NoDeberiaEnviarMasEmails() {
        // El sitio ya falló 5 veces y ya se avisó
        monitoreoEjemplo.setFallosConsecutivos(5);
        monitoreoEjemplo.setRepeticiones(3);
        monitoreoEjemplo.setAlertaEnviada(true);

        when(repository.findAll()).thenReturn(List.of(monitoreoEjemplo));

        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("Timeout"));

        monitoreoTaskService.ejecutarRevisionesProgramadas();

        assertEquals(0, monitoreoEjemplo.getEstado());
        assertEquals(6, monitoreoEjemplo.getFallosConsecutivos());
        assertTrue(monitoreoEjemplo.isAlertaEnviada());

        // Verificamos que NO se llamó a pesar del nuevo fallo
        verify(emailService, never()).enviarNotificacionFallo(any(), anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("ejecutarRevisiones: Debería resetear alertaEnviada si el sitio se recupera (Fallo -> OK)")
    void ejecutarRevisiones_SitioRecuperado_DeberiaResetearAlertaEnviada() {

        monitoreoEjemplo.setFallosConsecutivos(3);
        monitoreoEjemplo.setAlertaEnviada(true);
        monitoreoEjemplo.setFechaUltimaRevision(LocalDateTime.now().minusHours(1));

        when(repository.findAll()).thenReturn(List.of(monitoreoEjemplo));
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("UP"));

        monitoreoTaskService.ejecutarRevisionesProgramadas();

        assertEquals(200, monitoreoEjemplo.getEstado());
        assertEquals(0, monitoreoEjemplo.getFallosConsecutivos());
        assertFalse(monitoreoEjemplo.isAlertaEnviada(), "La alerta debería resetearse a false al recuperar el servicio");
        verify(repository).save(monitoreoEjemplo);
    }

    @Test
    @DisplayName("Debería incluir al propietario y a todos los invitados en el email")
    void enviarNotificaciones_ConInvitados_DeberiaLlamarEmailServiceConTodosLosDestinatarios() {

        Usuario invitado = new Usuario();
        invitado.setId(2);
        invitado.setEmail("invitado@test.com");

        monitoreoEjemplo.getInvitados().add(invitado);

        monitoreoEjemplo.setFallosConsecutivos(2);
        monitoreoEjemplo.setRepeticiones(3);

        when(repository.findAll()).thenReturn(List.of(monitoreoEjemplo));
        // Simulamos un fallo de red para activar gestionarFallo()
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("Fallo de conexión"));

        monitoreoTaskService.ejecutarRevisionesProgramadas();

        verify(emailService).enviarNotificacionFallo(
                destinatariosCaptor.capture(),
                eq(monitoreoEjemplo.getNombre()),
                anyString(),
                anyInt()
        );

        List<String> listaEnviada = destinatariosCaptor.getValue();

        assertAll("Validación de destinatarios",
                () -> assertTrue(listaEnviada.contains("owner@test.com"), "Debe contener al dueño"),
                () -> assertTrue(listaEnviada.contains("invitado@test.com"), "Debe contener al invitado"),
                () -> assertEquals(2, listaEnviada.size(), "Debería haber exactamente 2 destinatarios")
        );
    }

    @Test
    @DisplayName("ejecutarRevisiones: NO debería realizar check si no ha pasado el tiempo configurado")
    void ejecutarRevisiones_TiempoNoCumplido_NoDeberiaLlamarARestTemplate() {

        monitoreoEjemplo.setFechaUltimaRevision(LocalDateTime.now().minusMinutes(1));
        monitoreoEjemplo.setMinutos(5);

        when(repository.findAll()).thenReturn(List.of(monitoreoEjemplo));

        monitoreoTaskService.ejecutarRevisionesProgramadas();

        verify(restTemplate, never()).getForEntity(anyString(), any());
        verify(repository, never()).save(monitoreoEjemplo);
    }

    @Test
    @DisplayName("ejecutarRevisiones: NO debería realizar check si el monitoreo está desactivado")
    void ejecutarRevisiones_MonitoreoInactivo_DeberiaIgnorar() {
        monitoreoEjemplo.setActivo(false);
        when(repository.findAll()).thenReturn(List.of(monitoreoEjemplo));

        monitoreoTaskService.ejecutarRevisionesProgramadas();

        verify(restTemplate, never()).getForEntity(anyString(), any());
    }

    @Test
    @DisplayName("realizarCheck: Debería añadir prefijo https:// si la URL no lo tiene")
    void realizarCheck_UrlSinHttp_DeberiaAgregarHttps() {

        monitoreoEjemplo.getPaginaWeb().setUrl("miweb.com");
        when(repository.findAll()).thenReturn(List.of(monitoreoEjemplo));
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));

        monitoreoTaskService.ejecutarRevisionesProgramadas();

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).getForEntity(urlCaptor.capture(), eq(String.class));

        assertEquals("https://miweb.com", urlCaptor.getValue());
    }

    @Test
    @DisplayName("ejecutarRevisiones: No debe interrumpir el bucle si un monitoreo tiene URL nula")
    void ejecutarRevisiones_UrlNula_DeberiaContinuarConSiguientes() {
        //Monitoreo 1 con URL nula, Monitoreo 2 válido
        Monitoreo m1 = new Monitoreo();
        m1.setActivo(true);
        m1.setPaginaWeb(new PaginaWeb());

        monitoreoEjemplo.setFechaUltimaRevision(null);

        when(repository.findAll()).thenReturn(List.of(m1, monitoreoEjemplo));
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));

        assertDoesNotThrow(() -> monitoreoTaskService.ejecutarRevisionesProgramadas());

        verify(restTemplate, times(1)).getForEntity(contains("google.com"), eq(String.class));
    }

    @Test
    @DisplayName("ejecutarRevisiones: Solo debe procesar los monitoreos que cumplen el intervalo de tiempo")
    void ejecutarRevisiones_VariosMonitoreos_SoloProcesaLosNecesarios() {
        // Monitoreo A: Necesita revisión
        Monitoreo mA = this.crearMonitoreo(1, "Web A", "web-a.com", 3);
        mA.setFechaUltimaRevision(LocalDateTime.now().minusMinutes(10));

        // Monitoreo B: No necesita
        Monitoreo mB = this.crearMonitoreo(2, "Web B", "web-b.com", 3);
        mB.setFechaUltimaRevision(LocalDateTime.now().minusMinutes(1));

        when(repository.findAll()).thenReturn(List.of(mA, mB));
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));

        monitoreoTaskService.ejecutarRevisionesProgramadas();

        verify(restTemplate, times(1)).getForEntity(contains("web-a.com"), any());
        verify(restTemplate, never()).getForEntity(contains("web-b.com"), any());
    }

    @Test
    @DisplayName("ejecutarRevisiones: Si falla el guardado de un item, lanza excepción (Rollback)")
    void ejecutarRevisiones_ErrorEnSave_DeberiaLanzarExcepcion() {

        monitoreoEjemplo.setFechaUltimaRevision(null);
        when(repository.findAll()).thenReturn(List.of(monitoreoEjemplo));
        when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(ResponseEntity.ok("OK"));

        // Simulamos que la DB explota al guardar
        doThrow(new RuntimeException("DB Error")).when(repository).save(any());

        assertThrows(RuntimeException.class, () -> {
            monitoreoTaskService.ejecutarRevisionesProgramadas();
        });
    }

    @Test
    @DisplayName("enviarNotificaciones: Debería manejar propietarios sin email sin romper el flujo")
    void enviarNotificaciones_PropietarioSinEmail_NoDeberiaLanzarExcepcion() {

        propietario.setEmail(null);
        monitoreoEjemplo.setFallosConsecutivos(2);
        monitoreoEjemplo.setRepeticiones(3);

        when(repository.findAll()).thenReturn(List.of(monitoreoEjemplo));
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("Down"));

        assertDoesNotThrow(() -> monitoreoTaskService.ejecutarRevisionesProgramadas());
    }

    @Test
    @DisplayName("realizarCheck: Debería gestionar un Timeout como un error de estado 0")
    void realizarCheck_Timeout_DeberiaResultarEnEstadoCero() {

        when(repository.findAll()).thenReturn(List.of(monitoreoEjemplo));
        // Simulamos un error de Timeout
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new org.springframework.web.client.ResourceAccessException("Connection timed out"));

        monitoreoTaskService.ejecutarRevisionesProgramadas();

        assertEquals(0, monitoreoEjemplo.getEstado());
        assertEquals(1, monitoreoEjemplo.getFallosConsecutivos());
        verify(repository).save(monitoreoEjemplo);
    }

    @Test
    @DisplayName("enviarNotificaciones: No debería fallar si la lista de invitados es null o contiene emails nulos")
    void enviarNotificaciones_InvitadosIncompletos_DeberiaFuncionar() {

        monitoreoEjemplo.setInvitados(null);
        monitoreoEjemplo.setFallosConsecutivos(2);
        monitoreoEjemplo.setRepeticiones(3);

        when(repository.findAll()).thenReturn(List.of(monitoreoEjemplo));
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("Error"));

        assertDoesNotThrow(() -> monitoreoTaskService.ejecutarRevisionesProgramadas());

        verify(emailService).enviarNotificacionFallo(anyList(), anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("ejecutarRevisiones: Un error catastrófico en un monitoreo no debe impedir la revisión de los demás")
    void ejecutarRevisiones_ErrorCatastrofico_NoDetieneElProceso() {

        Monitoreo m1 = this.crearMonitoreo(1, "Bomba", "error.com", 3);

        m1.setPaginaWeb(null);

        Monitoreo m2 = this.crearMonitoreo(2, "Sano", "google.com", 5);

        when(repository.findAll()).thenReturn(List.of(m1, m2));
        when(restTemplate.getForEntity(contains("google.com"), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));

        monitoreoTaskService.ejecutarRevisionesProgramadas();

        // Verificamos que, a pesar de que m1 falló, m2 fue procesado correctamente
        verify(restTemplate).getForEntity(contains("google.com"), eq(String.class));
    }


    private Monitoreo crearMonitoreo(int id, String nombre, String url, int minutos) {
        Monitoreo m = new Monitoreo();
        m.setId(id);
        m.setNombre(nombre);
        m.setActivo(true);
        m.setMinutos(minutos);
        m.setRepeticiones(3);
        m.setFallosConsecutivos(0);
        m.setAlertaEnviada(false);

        PaginaWeb p = new PaginaWeb();
        p.setUrl(url);
        m.setPaginaWeb(p);

        Usuario u = new Usuario();
        u.setEmail("test@correo.com");
        m.setPropietario(u);

        return m;
    }

}