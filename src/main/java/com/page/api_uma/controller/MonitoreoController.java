package com.page.api_uma.controller;

import com.page.api_uma.DTOs.MonitoreoDTODetalle;
import com.page.api_uma.DTOs.MonitoreoListadoDTO;
import com.page.api_uma.model.PaginaWeb;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.service.MonitoreoService;
import com.page.api_uma.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/monitoreos")
public class MonitoreoController {

    private final MonitoreoService monitoreoService;
    private final UsuarioService usuarioService;

    public MonitoreoController(MonitoreoService monitoreoService, UsuarioService usuarioService) {
        this.monitoreoService = monitoreoService;
        this.usuarioService = usuarioService;
    }

    private Usuario getActual() {
        return usuarioService.getUsuarioAutenticado();
    }

    @PostMapping
    public ResponseEntity<MonitoreoDTODetalle> create(@RequestBody Map<String, Object> payload) {
        Usuario actual = getActual();
        String paginaUrl = (String) payload.get("paginaUrl");
        String nombreMonitoreo = (String) payload.get("nombre");
        int minutos = ((Number) payload.get("minutos")).intValue();
        int repeticiones = ((Number) payload.get("repeticiones")).intValue();

        // VALIDACIÓN DE SEGURIDAD
        if (minutos < 1 || repeticiones < 0) {
            return ResponseEntity.badRequest().build();
        }
        MonitoreoDTODetalle nuevo = monitoreoService.crearMonitoreo(actual, paginaUrl, nombreMonitoreo, minutos, repeticiones);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }


    @GetMapping
    public ResponseEntity<List<MonitoreoListadoDTO>> getAllMyMonitoreos() {
        Usuario actual = getActual();
        return ResponseEntity.ok(actual.getMonitoreosPropios().stream()
                .map(monitoreoService::convertirAListadoDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<MonitoreoListadoDTO>> buscarMonitoreos(@RequestParam(required = false) String termino) {
        Usuario actual = getActual();

        List<MonitoreoListadoDTO> resultados = monitoreoService.buscarAccesibles(actual.getId(), termino);

        return ResponseEntity.ok(resultados);
    }

    @GetMapping("/colaboraciones")
    public ResponseEntity<List<MonitoreoListadoDTO>> getMonitoreosInvitado() {
        Usuario actual = getActual();
        return ResponseEntity.ok(actual.getMonitoreosInvitado().stream()
                .map(monitoreoService::convertirAListadoDTO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/all")
    public ResponseEntity<List<MonitoreoListadoDTO>> getAllForAdmin() {
        Usuario actual = getActual();
        if (!"ADMIN".equals(actual.getPermiso())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(monitoreoService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MonitoreoDTODetalle> getById(@PathVariable int id) {
        MonitoreoDTODetalle dto = monitoreoService.obtenerDetalle(id, getActual().getId());

        if (dto == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}/pagina")
    public ResponseEntity<PaginaWeb> getPaginaByMonitoreoId(@PathVariable int id) {
        PaginaWeb pagina = monitoreoService.obtenerPaginaPorMonitoreoId(id, getActual().getId());
        return pagina != null ? ResponseEntity.ok(pagina) : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<MonitoreoDTODetalle> update(@PathVariable int id, @RequestBody Map<String, Object> payload) {
        Usuario actual = getActual();
        MonitoreoDTODetalle actualizado = monitoreoService.actualizar(id, payload, actual.getId(), actual.getPermiso());
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        Usuario actual = getActual();
        boolean eliminado = monitoreoService.eliminar(id, actual.getId(), actual.getPermiso());
        return eliminado ? ResponseEntity.noContent().build() : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping("/{id}/check")
    public ResponseEntity<MonitoreoDTODetalle> checkNow(@PathVariable int id) {
        MonitoreoDTODetalle actualizado = monitoreoService.ejecutarChequeo(id);
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.notFound().build();
    }


    @PutMapping("/invitar")
    public ResponseEntity<?> invitar(@RequestBody List<Integer> ids, @RequestParam List<String> emails) {
        if (ids == null || emails == null || ids.isEmpty() || emails.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        monitoreoService.invitacionEnMasa(getActual().getId(), ids, emails);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/invitar")
    public ResponseEntity<?> quitarInvitados(@RequestBody List<Integer> ids, @RequestParam List<String> emails) {
        if (ids == null || emails == null || ids.isEmpty() || emails.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        monitoreoService.quitarEnMasa(getActual().getId(), ids, emails);
        return ResponseEntity.ok().build();
    }

}