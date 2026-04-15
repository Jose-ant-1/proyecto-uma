package com.page.api_uma.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarNotificacionFallo(List<String> destinatarios, String nombrePagina, String url, int codigoEstado) {
        if (destinatarios == null || destinatarios.isEmpty()) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();

            // Convertimos la lista de correos a un array de Strings que es lo que pide setTo
            message.setTo(destinatarios.toArray(new String[0]));

            message.setSubject("ALERTA: Sitio caído - " + nombrePagina);
            message.setText("El sistema de monitoreo ha detectado que la página " + nombrePagina +
                    " (" + url + ") no está respondiendo correctamente.\n\n" +
                    "Código de estado detectado: " + codigoEstado + "\n" +
                    "Se ha alcanzado el límite de reintentos configurado.");

            mailSender.send(message);
        } catch (MailException e) {
            log.error("Error al enviar el correo de notificación a {}: {}", destinatarios, e.getMessage());
        }
        }
}