package com.page.api_uma.service;

import com.page.api_uma.model.Monitoreo;
import com.page.api_uma.repository.MonitoreoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MonitoreoTaskService {

    @Autowired
    private MonitoreoRepository repository;

    @Autowired
    private RestTemplate restTemplate; // Para hacer la petición HTTP

    @Scheduled(fixedRate = 30000)
    @Transactional // <--- MUY IMPORTANTE: Mantiene la sesión abierta para las relaciones
    public void ejecutarRevisionesProgramadas() {
        // Usamos el nuevo método que trae la relación cargada
        List<Monitoreo> monitoreos = repository.findAllWithPagina();
        LocalDateTime ahora = LocalDateTime.now();

        for (Monitoreo m : monitoreos) {
            // Verificamos si toca o si nunca se ha revisado (null)
            if (m.getFechaUltimaRevision() == null ||
                    ahora.isAfter(m.getFechaUltimaRevision().plusMinutes(m.getMinutos()))) {

                realizarCheck(m);
            }
        }
    }

    private void realizarCheck(Monitoreo m) {
        String urlOriginal = m.getPaginaWeb().getUrl();

        try {
            // 1. Validación de URL vacía
            if (urlOriginal == null || urlOriginal.isBlank()) {
                throw new IllegalArgumentException("La URL está vacía");
            }

            // 2. "Arreglar" la URL si no tiene protocolo (URI is not absolute fix)
            String urlFinal = urlOriginal;
            if (!urlFinal.startsWith("http://") && !urlFinal.startsWith("https://")) {
                urlFinal = "https://" + urlFinal; // Forzamos HTTPS por defecto
            }

            System.out.println("Intentando conectar a: " + urlFinal);

            ResponseEntity<String> response = restTemplate.getForEntity(urlFinal, String.class);
            m.setEstado(response.getStatusCode().value());

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            System.out.println("Error HTTP (" + e.getStatusCode() + ") en: " + urlOriginal);
            m.setEstado(e.getStatusCode().value());
        } catch (Exception e) {
            // Aquí es donde caía el "URI is not absolute"
            System.err.println("FALLO CRÍTICO en " + urlOriginal + " -> " + e.getMessage());
            m.setEstado(0); // Usamos 0 para errores de formación de URL o Red
        } finally {
            // SIEMPRE guardamos para que el Scheduler sepa que ya lo procesó
            m.setFechaUltimaRevision(LocalDateTime.now());
            repository.save(m);
        }
    }
}