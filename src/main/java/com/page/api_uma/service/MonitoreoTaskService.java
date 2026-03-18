package com.page.api_uma.service;

import com.page.api_uma.model.Monitoreo;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.repository.MonitoreoRepository;
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
public class MonitoreoTaskService {

    @Autowired
    private MonitoreoRepository repository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private EmailService emailService;

    @Scheduled(fixedRate = 30000)
    @Transactional
    public void ejecutarRevisionesProgramadas() {
        // Importante: usa el método que carga las páginas y los invitados
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
        String urlOriginal = m.getPaginaWeb().getUrl();
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
        List<String> destinatarios = new ArrayList<>();

        // Dueño
        destinatarios.add(m.getPropietario().getEmail());

        // Invitados (Gracias al @Transactional la lista está disponible)
        m.getInvitados().forEach(inv -> destinatarios.add(inv.getEmail()));

        // Llamada al servicio de Gmail
        emailService.enviarNotificacionFallo(
                destinatarios,
                m.getNombre(),
                m.getPaginaWeb().getUrl(),
                status
        );
    }
}