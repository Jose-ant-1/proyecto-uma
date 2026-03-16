package com.page.api_uma.controller;

import com.page.api_uma.DTOs.MonitoreoDTODetalle;
import com.page.api_uma.DTOs.MonitoreoListadoDTO; // Importante añadirlo
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

    // --- 1. CREATE ---
    @PostMapping
    public ResponseEntity<MonitoreoDTODetalle> create(@RequestBody Map<String, Object> payload) {
        Usuario actual = getActual();
        String url = (String) payload.get("url");
        String nombreMonitoreo = (String) payload.get("nombre");
        int minutos = ((Number) payload.get("minutos")).intValue();
        int repeticiones = ((Number) payload.getOrDefault("repeticiones", 3)).intValue();

        MonitoreoDTODetalle nuevo = monitoreoService.crearMonitoreo(actual, url, nombreMonitoreo, minutos, repeticiones);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    // --- 2. READ ALL (MODIFICADOS PARA LISTADO DTO) ---
    @GetMapping
    public ResponseEntity<List<MonitoreoListadoDTO>> getAllMyMonitoreos() {
        Usuario actual = getActual();
        return ResponseEntity.ok(actual.getMonitoreosPropios().stream()
                .map(monitoreoService::convertirAListadoDTO) // Cambio a mapper ligero
                .collect(Collectors.toList()));
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<MonitoreoListadoDTO>> buscarMonitoreos(@RequestParam(required = false) String termino) {
        Usuario actual = getActual();

        // Llamamos a un nuevo método en el service que junte y filtre
        List<MonitoreoListadoDTO> resultados = monitoreoService.buscarAccesibles(actual.getId(), termino);

        return ResponseEntity.ok(resultados);
    }

    @GetMapping("/colaboraciones")
    public ResponseEntity<List<MonitoreoListadoDTO>> getMonitoreosInvitado() {
        Usuario actual = getActual();
        return ResponseEntity.ok(actual.getMonitoreosInvitado().stream()
                .map(monitoreoService::convertirAListadoDTO) // Cambio a mapper ligero
                .collect(Collectors.toList()));
    }

    @GetMapping("/all")
    public ResponseEntity<List<MonitoreoListadoDTO>> getAllForAdmin() {
        Usuario actual = getActual();
        if (!"ADMIN".equals(actual.getPermiso())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        // Este método en el service ya devuelve List<MonitoreoListadoDTO>
        return ResponseEntity.ok(monitoreoService.findAll());
    }

    // --- 3. READ ONE (MODIFICADO PARA DETALLE DTO) ---

    @GetMapping("/{id}")
    public ResponseEntity<MonitoreoDTODetalle> getById(@PathVariable int id) {
        // IMPORTANTE: Pasamos el ID del usuario que está logueado (getActual().getId())
        MonitoreoDTODetalle dto = monitoreoService.obtenerDetalleSeguro(id, getActual().getId());

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

    // --- 4. UPDATE ---
    @PutMapping("/{id}")
    public ResponseEntity<MonitoreoDTODetalle> update(@PathVariable int id, @RequestBody Map<String, Object> payload) {
        Usuario actual = getActual();
        // Añadimos actual.getPermiso() a la llamada
        MonitoreoDTODetalle actualizado = monitoreoService.actualizar(id, payload, actual.getId(), actual.getPermiso());
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // --- 5. DELETE ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        Usuario actual = getActual();
        // Añadimos actual.getPermiso() a la llamada
        boolean eliminado = monitoreoService.eliminar(id, actual.getId(), actual.getPermiso());
        return eliminado ? ResponseEntity.noContent().build() : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // --- MÉTODOS EXTRA ---
    @PostMapping("/{id}/check")
    public ResponseEntity<MonitoreoDTODetalle> checkNow(@PathVariable int id) {
        MonitoreoDTODetalle actualizado = monitoreoService.ejecutarChequeo(id);
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.notFound().build();
    }


    @PutMapping("/{id}/invitar")
    public ResponseEntity<MonitoreoDTODetalle> invitar(@PathVariable int id, @RequestBody Map<String, String> payload) {
        String emailInvitado = payload.get("email");
        if (emailInvitado == null || emailInvitado.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Si el usuario ya existe, el service devolverá el DTO sin error
        MonitoreoDTODetalle actualizado = monitoreoService.toggleInvitado(id, getActual().getId(), emailInvitado);

        if (actualizado == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}/invitar")
    public ResponseEntity<MonitoreoDTODetalle> quitarInvitado(@PathVariable int id, @RequestParam String email) {
        // Si el usuario no estaba invitado, el service devolverá el DTO sin error
        MonitoreoDTODetalle actualizado = monitoreoService.eliminarInvitado(id, getActual().getId(), email);

        if (actualizado == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(actualizado);
    }

}