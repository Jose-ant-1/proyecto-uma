package com.page.api_uma.service;

import com.page.api_uma.DTOs.MonitoreoDTODetalle;
import com.page.api_uma.DTOs.MonitoreoListadoDTO;
import com.page.api_uma.DTOs.UsuarioDTO;
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

    // ==========================================
    // 1. MÉTODOS DE CREACIÓN Y ESCRITURA
    // ==========================================

    @Transactional
    public MonitoreoDTODetalle crearMonitoreo(Usuario propietario, String url, String nombreMonitoreo, int minutos, int repeticiones) {
        PaginaWeb pagina = paginaWebRepository.findByUrl(url)
                .orElseGet(() -> {
                    String dominio = extraerDominio(url);
                    PaginaWeb nueva = new PaginaWeb();
                    nueva.setUrl(url);
                    nueva.setNombre(dominio);
                    nueva.setNotaInfo("");
                    return paginaWebRepository.save(nueva);
                });

        Monitoreo m = new Monitoreo();
        m.setNombre(nombreMonitoreo);
        m.setPaginaWeb(pagina);
        m.setPropietario(propietario);
        m.setMinutosMonitoreo(minutos);
        m.setRepeticiones(repeticiones);
        m.setActivo(true);

        return convertirADetalleDTO(monitoreoRepository.save(m));
    }


    @Transactional
    public MonitoreoDTODetalle actualizarConfiguracion(int id, int userId, Map<String, Object> payload) {
        Monitoreo m = monitoreoRepository.findById(id).orElse(null);
        if (m == null || m.getPropietario().getId() != userId) return null;

        // 1. Manejo de la URL
        if (payload.containsKey("url")) {
            String nuevaUrl = (String) payload.get("url");

            // Solo actuamos si la URL realmente cambió
            if (!nuevaUrl.equals(m.getPaginaWeb().getUrl())) {
                String nuevoDominio = extraerDominio(nuevaUrl);

                // BUSCAMOS O CREAMOS (MUDANZA)
                // En lugar de hacer m.getPaginaWeb().setUrl(...), que editaría la fila actual...
                PaginaWeb nuevaPagina = paginaWebService.obtenerOCrearPagina(nuevaUrl, nuevoDominio);

                // ...le asignamos al monitoreo la nueva "habitación"
                m.setPaginaWeb(nuevaPagina);
            }
        }

        // 2. Manejo del resto de campos (estos sí son propios del monitoreo)
        if (payload.containsKey("nombre")) m.setNombre((String) payload.get("nombre"));
        if (payload.containsKey("minutos")) m.setMinutosMonitoreo(((Number) payload.get("minutos")).intValue());
        if (payload.containsKey("repeticiones")) m.setRepeticiones(((Number) payload.get("repeticiones")).intValue());

        return convertirADetalleDTO(monitoreoRepository.save(m));
    }

    @Transactional
    public MonitoreoDTODetalle invitarUsuario(int idMonitoreo, int idPropietario, String emailInvitado) {
        Optional<Monitoreo> optMonitoreo = monitoreoRepository.findById(idMonitoreo);
        Usuario invitado = usuarioService.findByEmail(emailInvitado);

        if (optMonitoreo.isPresent() && invitado != null) {
            Monitoreo m = optMonitoreo.get();
            if (m.getPropietario().getId() == idPropietario && m.getPropietario().getId() != invitado.getId()) {
                m.getInvitados().add(invitado);
                return convertirADetalleDTO(monitoreoRepository.save(m));
            }
        }
        return null;
    }

    @Transactional
    public boolean eliminarMonitoreo(int idMonitoreo, int idUsuario) {
        return monitoreoRepository.findById(idMonitoreo)
                .filter(m -> m.getPropietario().getId() == idUsuario)
                .map(m -> {
                    monitoreoRepository.delete(m);
                    return true;
                }).orElse(false);
    }

    public MonitoreoDTODetalle obtenerDetalleSeguro(int id, int usuarioId) {
        Monitoreo m = monitoreoRepository.findById(id).orElse(null);
        Usuario u = usuarioService.findById(usuarioId); // Buscamos al usuario que hace la petición

        if (m != null && u != null) {
            // Lógica de permisos:
            boolean esDuenio = m.getPropietario().getId() == usuarioId;
            boolean esInvitado = m.getInvitados().stream().anyMatch(inv -> inv.getId() == usuarioId);
            boolean esAdmin = "ADMIN".equalsIgnoreCase(u.getPermiso());

            // SI ES ADMIN, LE DEJAMOS PASAR SIEMPRE
            if (esDuenio || esInvitado || esAdmin) {
                return convertirADetalleDTO(m);
            }
        }
        return null; // Si llega aquí, el controlador devolverá 403 (Prohibido)
    }

    public List<MonitoreoListadoDTO> obtenerTodosLosMonitoreos() {
        return monitoreoRepository.findAll().stream()
                .map(this::convertirAListadoDTO)
                .collect(Collectors.toList());
    }

    public PaginaWeb obtenerPaginaPorMonitoreoId(int monitoreoId, int usuarioId) {
        return monitoreoRepository.findById(monitoreoId)
                .filter(m -> m.getPropietario().getId() == usuarioId ||
                        m.getInvitados().stream().anyMatch(inv -> inv.getId() == usuarioId))
                .map(Monitoreo::getPaginaWeb)
                .orElse(null);
    }

    // ==========================================
    // 3. LÓGICA DE CONTROL (CHECK)
    // ==========================================

    @Transactional
    public MonitoreoDTODetalle ejecutarChequeo(int id) {
        return monitoreoRepository.findById(id)
                .map(m -> {
                    int nuevoEstado = paginaWebService.getRemoteStatus(m.getPaginaWeb().getUrl());
                    m.setEstado(nuevoEstado);
                    m.setFechaUltimaRevision(LocalDateTime.now());
                    return convertirADetalleDTO(monitoreoRepository.save(m));
                }).orElse(null);
    }

    // ==========================================
    // 4. CONVERSORES Y MAPPERS
    // ==========================================

    public MonitoreoListadoDTO convertirAListadoDTO(Monitoreo m) {
        return MonitoreoListadoDTO.builder()
                .id(m.getId())
                .nombre(m.getNombre())
                .propietarioId(m.getPropietario().getId())
                .ultimoEstado(m.getEstado())
                .fechaUltimaRevision(m.getFechaUltimaRevision())
                .activo(m.isActivo())
                .paginaUrl(m.getPaginaWeb().getUrl())
                .build();
    }

    public MonitoreoDTODetalle convertirADetalleDTO(Monitoreo m) {
        return MonitoreoDTODetalle.builder()
                .id(m.getId())
                .nombre(m.getNombre())
                .minutosMonitoreo(m.getMinutosMonitoreo())
                .repeticiones(m.getRepeticiones())
                .ultimoEstado(m.getEstado())
                .fechaUltimaRevision(m.getFechaUltimaRevision())
                .activo(m.isActivo())
                .paginaUrl(m.getPaginaWeb().getUrl())
                .propietario(mapearUsuarioADTO(m.getPropietario()))
                .invitados(m.getInvitados().stream()
                        .map(this::mapearUsuarioADTO)
                        .collect(Collectors.toSet()))
                .build();
    }

    private UsuarioDTO mapearUsuarioADTO(Usuario u) {
        return UsuarioDTO.builder()
                .id(u.getId())
                .nombre(u.getNombre())
                .email(u.getEmail())
                .permiso(u.getPermiso())
                .build();
    }

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