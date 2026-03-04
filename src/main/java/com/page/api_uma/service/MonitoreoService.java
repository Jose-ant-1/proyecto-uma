package com.page.api_uma.service;

import com.page.api_uma.DTOs.MonitoreoDTO;
import com.page.api_uma.model.Monitoreo;
import com.page.api_uma.model.PaginaWeb;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.repository.MonitoreoRepository;
import com.page.api_uma.repository.PaginaWebRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MonitoreoService {

    private final MonitoreoRepository monitoreoRepository;
    private final PaginaWebRepository paginaWebRepository;
    private final PaginaWebService paginaWebService;

    public MonitoreoService(MonitoreoRepository monitoreoRepository,
                            PaginaWebRepository paginaWebRepository,
                            PaginaWebService paginaWebService) {
        this.monitoreoRepository = monitoreoRepository;
        this.paginaWebRepository = paginaWebRepository;
        this.paginaWebService = paginaWebService;
    }

    // --- 1. CREATE ---
    @Transactional
    public MonitoreoDTO crearMonitoreo(Usuario propietario, String url, String nombrePagina, int minutos) {
        PaginaWeb pagina = paginaWebRepository.findByUrl(url)
                .orElseGet(() -> {
                    PaginaWeb nueva = PaginaWeb.builder()
                            .url(url)
                            .nombre(nombrePagina)
                            .build();
                    return paginaWebRepository.save(nueva);
                });

        Monitoreo monitoreo = new Monitoreo();
        monitoreo.setNombre("Monitoreo: " + nombrePagina);
        monitoreo.setMinutosMonitoreo(minutos);
        monitoreo.setPropietario(propietario);
        monitoreo.setPaginaWeb(pagina);
        monitoreo.setRepeticiones(1);
        monitoreo.setActivo(true);

        return convertirADTO(monitoreoRepository.save(monitoreo));
    }

    // --- 2. READ (Obtener uno solo con seguridad) ---
    public MonitoreoDTO obtenerPorIdSiTieneAcceso(int idMonitoreo, int idUsuario) {
        return monitoreoRepository.findById(idMonitoreo)
                .filter(m -> m.getPropietario().getId() == idUsuario ||
                        m.getInvitados().stream().anyMatch(inv -> inv.getId() == idUsuario))
                .map(this::convertirADTO)
                .orElse(null);
    }

    // --- 3. UPDATE (Actualizar configuración) ---
    @Transactional
    public MonitoreoDTO actualizarConfiguracion(int idMonitoreo, int idUsuario, Map<String, Object> datos) {
        Optional<Monitoreo> opt = monitoreoRepository.findById(idMonitoreo);

        if (opt.isPresent()) {
            Monitoreo m = opt.get();
            // Solo el dueño puede editar la configuración básica
            if (m.getPropietario().getId() == idUsuario) {
                if (datos.containsKey("nombre")) m.setNombre((String) datos.get("nombre"));
                if (datos.containsKey("minutos")) m.setMinutosMonitoreo(((Number) datos.get("minutos")).intValue());
                if (datos.containsKey("activo")) m.setActivo((Boolean) datos.get("activo"));

                return convertirADTO(monitoreoRepository.save(m));
            }
        }
        return null;
    }

    // --- 4. DELETE ---
    public boolean eliminarMonitoreo(int idMonitoreo, int idUsuario) {
        Optional<Monitoreo> opt = monitoreoRepository.findById(idMonitoreo);
        if (opt.isPresent()) {
            Monitoreo m = opt.get();
            if (m.getPropietario().getId() == idUsuario) {
                monitoreoRepository.delete(m);
                return true;
            }
        }
        return false;
    }

    // --- 5. LOGICA DE CHEQUEO (Ping) ---
    @Transactional
    public MonitoreoDTO ejecutarChequeo(int id) {
        Monitoreo monitoreo = monitoreoRepository.findById(id).orElse(null);
        if (monitoreo == null) return null;

        int nuevoEstado = paginaWebService.getRemoteStatus(monitoreo.getPaginaWeb().getUrl());

        monitoreo.setEstado(nuevoEstado);
        monitoreo.setFechaUltimaRevision(LocalDateTime.now());

        return convertirADTO(monitoreoRepository.save(monitoreo));
    }

    // --- 6. CONVERSOR DTO ---
    public MonitoreoDTO convertirADTO(Monitoreo m) {
        return MonitoreoDTO.builder()
                .id(m.getId())
                .nombre(m.getNombre())
                .minutosMonitoreo(m.getMinutosMonitoreo())
                .ultimoEstado(m.getEstado())
                .fechaUltimaRevision(m.getFechaUltimaRevision())
                .activo(m.isActivo())
                .paginaUrl(m.getPaginaWeb().getUrl())
                .propietarioNombre(m.getPropietario().getNombre())
                .build();
    }
}