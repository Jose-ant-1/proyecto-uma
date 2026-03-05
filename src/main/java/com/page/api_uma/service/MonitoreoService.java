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
    private final UsuarioService usuarioService;

    public MonitoreoService(MonitoreoRepository monitoreoRepository,
                            PaginaWebRepository paginaWebRepository,
                            PaginaWebService paginaWebService,
                            UsuarioService usuarioService) {
        this.monitoreoRepository = monitoreoRepository;
        this.paginaWebRepository = paginaWebRepository;
        this.paginaWebService = paginaWebService;
        this.usuarioService = usuarioService;
    }

    // --- 1. CREATE ---
    @Transactional
    public MonitoreoDTO crearMonitoreo(Usuario propietario, String url, String nombreMonitoreo, int minutos, int repeticiones) {
        // 1. Buscar si la página ya existe por URL
        PaginaWeb pagina = paginaWebRepository.findByUrl(url)
                .orElseGet(() -> {
                    // 2. Si no existe, crearla con el nombre del dominio
                    String dominio = extraerDominio(url);
                    PaginaWeb nuevaPaso = new PaginaWeb();
                    nuevaPaso.setUrl(url);
                    nuevaPaso.setNombre(dominio);
                    nuevaPaso.setNotaInfo(""); // Nota vacía como pediste
                    return paginaWebRepository.save(nuevaPaso);
                });

        // 3. Crear el nuevo monitoreo vinculado a esa página
        Monitoreo m = new Monitoreo();
        m.setNombre(nombreMonitoreo);
        m.setPaginaWeb(pagina);
        m.setPropietario(propietario);
        m.setMinutosMonitoreo(minutos);
        m.setRepeticiones(repeticiones);
        m.setActivo(true);
        m.setEstado(null); // Empezará a chequearse en el siguiente ciclo del Scheduler

        return convertirADTO(monitoreoRepository.save(m));
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

    public PaginaWeb obtenerPaginaPorMonitoreoId(int monitoreoId, int usuarioId) {

        Monitoreo monitoreo = monitoreoRepository.findById(monitoreoId).orElse(null);

        if (monitoreo != null) {
            boolean esDueno = monitoreo.getPropietario().getId() == usuarioId;
            boolean esInvitado = monitoreo.getInvitados().stream()
                    .anyMatch(u -> u.getId() == usuarioId);

            if (esDueno || esInvitado) {
                return monitoreo.getPaginaWeb();
            }
        }
        return null;
    }

    @Transactional
    public MonitoreoDTO invitarUsuario(int idMonitoreo, int idPropietario, String emailInvitado) {
        Optional<Monitoreo> optMonitoreo = monitoreoRepository.findById(idMonitoreo);
        Usuario invitado = usuarioService.findByEmail(emailInvitado);

        if (optMonitoreo.isPresent() && invitado != null) {
            Monitoreo m = optMonitoreo.get();

            // 1. Validar que solo el dueño puede invitar
            if (m.getPropietario().getId() == idPropietario) {

                // 2. No invitarse a sí mismo
                if (m.getPropietario().getId() == invitado.getId()) {
                    return null;
                }

                // 3. Añadir al Set (Hibernate gestiona la tabla monitoreo_invitados)
                m.getInvitados().add(invitado);
                return convertirADTO(monitoreoRepository.save(m));
            }
        }
        return null;
    }

    // --- 6. CONVERSOR DTO ---
    public MonitoreoDTO convertirADTO(Monitoreo m) {
        return MonitoreoDTO.builder()
                .id(m.getId())
                .nombre(m.getNombre())
                .minutosMonitoreo(m.getMinutosMonitoreo())
                .repeticiones(m.getRepeticiones())
                .ultimoEstado(m.getEstado())
                .fechaUltimaRevision(m.getFechaUltimaRevision())
                .activo(m.isActivo())
                .paginaUrl(m.getPaginaWeb().getUrl())
                .propietarioId(m.getPropietario().getId())

                // ESTA ES LA LÍNEA QUE FALTA:
                .invitadosCorreo(m.getInvitados().stream()
                        .map(Usuario::getEmail)
                        .collect(Collectors.toSet()))
                .build();
    }

    public List<MonitoreoDTO> obtenerTodosLosMonitoreos() {
        return monitoreoRepository.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    // Método auxiliar para limpiar la URL y sacar el dominio
    private String extraerDominio(String url) {
        try {
            String domain = url.replaceFirst("^(https?://)?(www\\.)?", "");
            int index = domain.indexOf('/');
            return index != -1 ? domain.substring(0, index) : domain;
        } catch (Exception e) {
            return "Nueva Página";
        }
    }

}