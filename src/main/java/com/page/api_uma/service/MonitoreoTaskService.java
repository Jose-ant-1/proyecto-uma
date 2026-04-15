package com.page.api_uma.service;

import com.page.api_uma.model.Monitoreo;
import com.page.api_uma.repository.MonitoreoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class MonitoreoTaskService {

    private final MonitoreoRepository repository;

    private final RestTemplate restTemplate;

    private final EmailService emailService;

    public MonitoreoTaskService(MonitoreoRepository repository, RestTemplate restTemplate, EmailService emailService) {
        this.repository = repository;
        this.restTemplate = restTemplate;
        this.emailService = emailService;
    }

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void ejecutarRevisionesProgramadas() {
        // usa el metodo que carga las páginas y los invitados
        List<Monitoreo> monitoreos = repository.findAll();
        LocalDateTime ahora = LocalDateTime.now();

        for (Monitoreo m : monitoreos) {
            if (!m.isActivo()) continue;

            if (m.getFechaUltimaRevision() == null ||
                    ahora.isAfter(m.getFechaUltimaRevision().plusMinutes(m.getMinutos()))) {
                realizarCheck(m);
            }
        }
    }

    private void realizarCheck(Monitoreo m) {

        if (m.getPaginaWeb() == null || m.getPaginaWeb().getUrl() == null) {
            log.error("El monitoreo '{}' (ID: {}) no tiene una URL válida configurada.", m.getNombre(), m.getId());
            return; // Salimos de este check, pero el bucle sigue con el siguiente
        }
        String urlOriginal = m.getPaginaWeb().getUrl();

        log.info("Iniciando revisión para: {} ({})", m.getNombre(), urlOriginal);
        try {
            String urlFinal = urlOriginal.startsWith("http") ? urlOriginal : "https://" + urlOriginal;

            ResponseEntity<String> response = restTemplate.getForEntity(urlFinal, String.class);
            int status = response.getStatusCode().value();
            m.setEstado(status);

            // SI FUNCIONA (200-299)
            if (status >= 200 && status < 300) {
                m.setFallosConsecutivos(0);
                m.setAlertaEnviada(false); // Reset para la próxima caída
            } else {
                gestionarFallo(m, status);
            }

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            m.setEstado(e.getStatusCode().value());
            gestionarFallo(m, e.getStatusCode().value());
        } catch (Exception e) {
            m.setEstado(0); // Error de red/DNS
            gestionarFallo(m, 0);
        } finally {
            m.setFechaUltimaRevision(LocalDateTime.now());
            repository.save(m);
        }
    }

    private void gestionarFallo(Monitoreo m, int status) {
        m.setFallosConsecutivos(m.getFallosConsecutivos() + 1);

        // Si llegamos al límite y NO hemos enviado alerta todavía
        if (m.getFallosConsecutivos() >= m.getRepeticiones() && !m.isAlertaEnviada()) {
            enviarNotificaciones(m, status);
            m.setAlertaEnviada(true); // Marcamos como enviado para evitar SPAM
        }
    }

    private void enviarNotificaciones(Monitoreo m, int status) {

        if (m.getPropietario() == null || m.getPropietario().getEmail() == null) {
            log.error("No se pudo enviar notificación para '{}': El propietario no tiene email.", m.getNombre());
            return;
        }

        List<String> destinatarios = new ArrayList<>();
        destinatarios.add(m.getPropietario().getEmail());

        // Invitados (Gracias al @Transactional la lista está disponible)
        if (m.getInvitados() != null) {
            m.getInvitados().forEach(inv -> {
                if (inv.getEmail() != null) destinatarios.add(inv.getEmail());
            });
        }
        // Llamada al servicio de Gmail
        emailService.enviarNotificacionFallo(
                destinatarios,
                m.getNombre(),
                m.getPaginaWeb().getUrl(),
                status
        );
    }
}
