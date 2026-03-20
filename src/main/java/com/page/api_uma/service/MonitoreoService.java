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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MonitoreoService {

    private final MonitoreoRepository monitoreoRepository;
    private final PaginaWebRepository paginaWebRepository;
    private final PaginaWebService paginaWebService;
    private final UsuarioService usuarioService;

    public MonitoreoService(MonitoreoRepository monitoreoRepository, PaginaWebRepository paginaWebRepository, PaginaWebService paginaWebService, UsuarioService usuarioService) {
        this.monitoreoRepository = monitoreoRepository;
        this.paginaWebRepository = paginaWebRepository;
        this.paginaWebService = paginaWebService;
        this.usuarioService = usuarioService;
    }

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
        m.setMinutos(minutos);
        m.setRepeticiones(repeticiones);
        m.setActivo(true);

        return convertirADetalleDTO(monitoreoRepository.save(m));
    }

    @Transactional
    public MonitoreoDTODetalle actualizar(int id, Map<String, Object> payload, int usuarioId, String permiso) {
        Monitoreo m = monitoreoRepository.findById(id).orElse(null);

        // Lógica de autorización: Dueño o ADMIN
        boolean esAutorizado = m != null && (m.getPropietario().getId() == usuarioId || "ADMIN".equals(permiso));

        if (esAutorizado) {
            if (payload.containsKey("nombre")) m.setNombre((String) payload.get("nombre"));
            if (payload.containsKey("minutos")) m.setMinutos(((Number) payload.get("minutos")).intValue());
            if (payload.containsKey("repeticiones")) m.setRepeticiones(((Number) payload.get("repeticiones")).intValue());

            return convertirADetalleDTO(monitoreoRepository.save(m));
        }
        return null;
    }

    @Transactional
    public boolean eliminar(int id, int usuarioId, String permiso) {
        Monitoreo m = monitoreoRepository.findById(id).orElse(null);
        if (m == null) return false;

        boolean esAutorizado = m.getPropietario().getId() == usuarioId || "ADMIN".equals(permiso);

        if (esAutorizado) {
            // Limpiar invitados
            m.getInvitados().clear();
            monitoreoRepository.saveAndFlush(m);

            // Limpiar la tabla de plantillas
            monitoreoRepository.eliminarRelacionesConPlantillas(id);

            // borrar sin errores de FK
            monitoreoRepository.delete(m);
            return true;
        }
        return false;
    }

    public MonitoreoDTODetalle obtenerDetalle(int id, int usuarioId) {
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
        return null; // Si llega aquí, el controlador devolverá 403
    }

    public List<MonitoreoListadoDTO> findAll() {
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
                .minutos(m.getMinutos())
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

    public List<MonitoreoListadoDTO> buscarAccesibles(int usuarioId, String termino) {
        // Buscamos al usuario
        Usuario usuario = usuarioService.findById(usuarioId);

        if (usuario == null) return List.of();

        // Unimos sus monitoreos propios y los que es invitado
        Stream<Monitoreo> streamUnificado = Stream.concat(
                usuario.getMonitoreosPropios().stream(),
                usuario.getMonitoreosInvitado().stream()
        );

        // Filtramos por el término y convertimos a DTO
        return streamUnificado
                .filter(m -> {
                    if (termino == null || termino.isBlank()) return true;
                    String t = termino.toLowerCase();
                    // Buscamos en nombre del monitoreo OR en la URL de la página
                    return m.getNombre().toLowerCase().contains(t) ||
                            (m.getPaginaWeb() != null && m.getPaginaWeb().getUrl().toLowerCase().contains(t));
                })
                .map(this::convertirAListadoDTO)
                .distinct()
                .collect(Collectors.toList());
    }

    @Transactional
    protected MonitoreoDTODetalle eliminarInvitado(int monitoreoId, int propietarioId, String emailInvitado) {
        Monitoreo m = monitoreoRepository.findById(monitoreoId).orElse(null);
        Usuario invitado = usuarioService.buscarPorEmail(emailInvitado);

        if (m != null && m.getPropietario().getId() == propietarioId && invitado != null) {
            // SI NO ESTÁ EN LA LISTA: No hacemos nada, devolvemos el actual
            if (!m.getInvitados().contains(invitado)) {
                return convertirADetalleDTO(m);
            }

            // SI ESTÁ: Lo quitamos
            m.getInvitados().remove(invitado);
            return convertirADetalleDTO(monitoreoRepository.save(m));
        }
        return null;
    }

    @Transactional
    protected MonitoreoDTODetalle toggleInvitado(int monitoreoId, int propietarioId, String emailInvitado) {
        Monitoreo m = monitoreoRepository.findById(monitoreoId).orElse(null);
        Usuario invitado = usuarioService.buscarPorEmail(emailInvitado);

        // Si el invitado es el mismo que el dueño, no hacemos nada
        if (invitado != null && invitado.getId() == propietarioId) {
            return convertirADetalleDTO(m); // Salimos sin añadir
        }

        if (m != null && m.getPropietario().getId() == propietarioId && invitado != null) {
            if (m.getInvitados().contains(invitado)) return convertirADetalleDTO(m);
            m.getInvitados().add(invitado);
            return convertirADetalleDTO(monitoreoRepository.save(m));
        }
        return null;
    }

    @Transactional
    public void invitacionEnMasa(int propietarioId, List<Integer> monitoreosIds, List<String> emails) {
        for (Integer monitoreoId : monitoreosIds) {
            for (String email : emails) {
                // Llamamos a tu lógica individual existente
                this.toggleInvitado(monitoreoId, propietarioId, email);
            }
        }
    }

    @Transactional
    public void quitarEnMasa(int propietarioId, List<Integer> monitoreosIds, List<String> emails) {
        for (Integer monitoreoId : monitoreosIds) {
            for (String email : emails) {
                // Llamamos a tu lógica de borrado existente
                this.eliminarInvitado(monitoreoId, propietarioId, email);
            }
        }
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