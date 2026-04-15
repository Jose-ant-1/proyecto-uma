package com.page.api_uma.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;


@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    @DisplayName("enviarNotificacionFallo: Debería configurar correctamente el mensaje y enviarlo")
    void enviarNotificacionFallo_DeberiaConfigurarYEnviarMensaje() {
        // Arrange
        List<String> destinatarios = List.of("usuario1@test.com", "usuario2@test.com");
        String nombrePagina = "Mi Aplicación";
        String url = "https://mi-app.com";
        int codigoEstado = 500;

        // Usamos ArgumentCaptor para capturar el mensaje que se crea internamente
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        emailService.enviarNotificacionFallo(destinatarios, nombrePagina, url, codigoEstado);

        // Assert
        verify(mailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage mensajeEnviado = messageCaptor.getValue();

        // Verificaciones del contenido del correo
        assertNotNull(mensajeEnviado.getTo());
        assertEquals(2, mensajeEnviado.getTo().length);
        assertEquals("usuario1@test.com", mensajeEnviado.getTo()[0]);

        assertNotNull(mensajeEnviado.getSubject());
        assertTrue(mensajeEnviado.getSubject().contains(nombrePagina));

        assertNotNull(mensajeEnviado.getText());
        assertTrue(mensajeEnviado.getText().contains(url));
        assertTrue(mensajeEnviado.getText().contains(String.valueOf(codigoEstado)));
    }

    @Test
    @DisplayName("enviarNotificacionFallo: No debería intentar enviar si no hay destinatarios")
    void enviarNotificacionFallo_ListaVacia() {
        emailService.enviarNotificacionFallo(List.of(), "Pagina", "url", 500);

        // Ahora sí pasará, porque el 'return' evitará que se llegue a la línea del send
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("enviarNotificacionFallo: No debería fallar ni enviar si la lista es null")
    void enviarNotificacionFallo_ListaNull() {
        // Act
        emailService.enviarNotificacionFallo(null, "Pagina", "url", 500);

        // Assert
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("enviarNotificacionFallo: Debería incluir todos los destinatarios en el orden correcto")
    void enviarNotificacionFallo_VerificarTodosLosDestinatarios() {
        List<String> destinatarios = List.of("a@test.com", "b@test.com", "c@test.com");
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.enviarNotificacionFallo(destinatarios, "Test", "url", 404);

        verify(mailSender).send(messageCaptor.capture());
        // Comparación directa de arrays
        assertArrayEquals(destinatarios.toArray(), messageCaptor.getValue().getTo());
    }

    @Test
    @DisplayName("Robustez: No debe propagar excepciones si el servidor de correo falla")
    void enviarNotificacionFallo_ServidorCorreoCaido() {
        // Arrange
        List<String> destinatarios = List.of("admin@test.com");
        // Simulamos que el mailSender lanza una excepción de runtime (común en Spring Mail)
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailSendException("SMTP Timeout"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert
        // Si tu intención es que el error no rompa el flujo principal,
        // el assert debe verificar que NO se lanza una excepción.
        assertDoesNotThrow(() -> {
            emailService.enviarNotificacionFallo(destinatarios, "Web", "http://test.com", 500);
        });
    }

    @Test
    @DisplayName("Robustez: Debe manejar correctamente caracteres especiales y nombres vacíos")
    void enviarNotificacionFallo_DatosInusuales() {
        // Arrange
        List<String> destinatarios = List.of("dev@test.com");
        String nombreRaro = "Página con Ñ y emojis 🚀";
        String urlLarga = "https://muy-larga.com/" + "a".repeat(100);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        emailService.enviarNotificacionFallo(destinatarios, nombreRaro, urlLarga, 404);

        // Assert
        verify(mailSender).send(captor.capture());
        assertTrue(captor.getValue().getSubject().contains(nombreRaro));
        assertTrue(captor.getValue().getText().contains(urlLarga));
    }

    @Test
    @DisplayName("Robustez: Manejo de entrada null sin lanzar excepción")
    void enviarNotificacionFallo_NullInput() {
        assertDoesNotThrow(() -> {
            emailService.enviarNotificacionFallo(null, null, null, 0);
        });
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Debería enviar múltiples correos si se llama varias veces")
    void enviarMultiplesNotificaciones() {
        emailService.enviarNotificacionFallo(List.of("a@t.com"), "P1", "u1", 500);
        emailService.enviarNotificacionFallo(List.of("b@t.com"), "P2", "u2", 404);

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Robustez: No debe propagar excepciones si un destinatario tiene formato inválido")
    void enviarNotificacionFallo_EmailMalformado() {
        // Arrange
        List<String> destinatarios = List.of("email-invalido");
        // Simulamos que Spring Mail detecta el error de formato y lanza la excepción
        org.mockito.Mockito.doThrow(new org.springframework.mail.MailParseException("Invalid address"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert
        assertDoesNotThrow(() -> {
            emailService.enviarNotificacionFallo(destinatarios, "Test", "url", 500);
        });
        // Verificamos que al menos intentó enviarlo
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Robustez: Manejo de strings nulos en el cuerpo del mensaje")
    void enviarNotificacionFallo_StringsNulos() {
        // Arrange
        List<String> destinatarios = List.of("admin@test.com");
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        // Pasamos null en nombre y url
        emailService.enviarNotificacionFallo(destinatarios, null, null, 0);

        // Assert
        verify(mailSender).send(captor.capture());
        String cuerpo = captor.getValue().getText();

        // Verificamos que no explotó (NPE) y que al menos contiene el código
        assertNotNull(cuerpo);
        assertTrue(cuerpo.contains("0"));
    }

    @Test
    @DisplayName("Robustez: Lista con strings vacíos o nulos")
    void enviarNotificacionFallo_ListaConElementosInvalidos() {
        // La lista no está vacía, pero contiene basura
        List<String> destinatarios = Arrays.asList("", " ", null);

        // El servicio intentará enviarlo porque la lista != null y !isEmpty()
        assertDoesNotThrow(() -> {
            emailService.enviarNotificacionFallo(destinatarios, "Test", "url", 500);
        });

        // Verificamos que el flujo pasó por el sender
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Formato: El asunto debe seguir el patrón de ALERTA")
    void enviarNotificacionFallo_FormatoAsunto() {
        String nombre = "DB-Server";
        emailService.enviarNotificacionFallo(List.of("admin@test.com"), nombre, "url", 500);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertEquals("ALERTA: Sitio caído - DB-Server", captor.getValue().getSubject());
    }


}