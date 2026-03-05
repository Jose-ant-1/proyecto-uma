package com.page.api_uma.controller;

import com.page.api_uma.DTOs.MonitoreoDTO;
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

    /**
     * Método privado de ayuda.
     * Al llamarlo dentro de cada método, garantizamos que el usuario sea
     * el del hilo (thread) actual de la petición, evitando el Error 500.
     */
    private Usuario getActual() {
        return usuarioService.getUsuarioAutenticado();
    }

    // --- 1. CREATE ---
    @PostMapping
    public ResponseEntity<MonitoreoDTO> create(@RequestBody Map<String, Object> payload) {
        Usuario actual = getActual(); // Lo obtenemos localmente
        String url = (String) payload.get("url");
        String nombreMonitoreo = (String) payload.get("nombre");
        int minutos = ((Number) payload.get("minutos")).intValue();
        int repeticiones = ((Number) payload.getOrDefault("repeticiones", 3)).intValue();

        MonitoreoDTO nuevo = monitoreoService.crearMonitoreo(actual, url, nombreMonitoreo, minutos, repeticiones);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    // --- 2. READ ALL ---
    @GetMapping
    public ResponseEntity<List<MonitoreoDTO>> getAllMyMonitoreos() {
        Usuario actual = getActual();
        return ResponseEntity.ok(actual.getMonitoreosPropios().stream()
                .map(monitoreoService::convertirADTO).collect(Collectors.toList()));
    }

    @GetMapping("/colaboraciones")
    public ResponseEntity<List<MonitoreoDTO>> getMonitoreosInvitado() {
        Usuario actual = getActual();
        return ResponseEntity.ok(actual.getMonitoreosInvitado().stream()
                .map(monitoreoService::convertirADTO).collect(Collectors.toList()));
    }

    // --- ENDPOINT PARA ADMINS ---
    @GetMapping("/all")
    public ResponseEntity<List<MonitoreoDTO>> getAllForAdmin() {
        Usuario actual = getActual();
        if (!"ADMIN".equals(actual.getPermiso())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(monitoreoService.obtenerTodosLosMonitoreos());
    }

    // --- 3. READ ONE ---
    @GetMapping("/{id}")
    public ResponseEntity<MonitoreoDTO> getById(@PathVariable int id) {
        // Usamos el ID directamente del usuario autenticado
        MonitoreoDTO dto = monitoreoService.obtenerPorIdSiTieneAcceso(id, getActual().getId());
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @GetMapping("/{id}/pagina")
    public ResponseEntity<PaginaWeb> getPaginaByMonitoreoId(@PathVariable int id) {
        PaginaWeb pagina = monitoreoService.obtenerPaginaPorMonitoreoId(id, getActual().getId());
        return pagina != null ? ResponseEntity.ok(pagina) : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // --- 4. UPDATE ---
    @PutMapping("/{id}")
    public ResponseEntity<MonitoreoDTO> update(@PathVariable int id, @RequestBody Map<String, Object> payload) {
        MonitoreoDTO actualizado = monitoreoService.actualizarConfiguracion(id, getActual().getId(), payload);
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // --- 5. DELETE ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        boolean eliminado = monitoreoService.eliminarMonitoreo(id, getActual().getId());
        return eliminado ? ResponseEntity.noContent().build() : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // --- MÉTODOS EXTRA ---
    @PostMapping("/{id}/check")
    public ResponseEntity<MonitoreoDTO> checkNow(@PathVariable int id) {
        MonitoreoDTO actualizado = monitoreoService.ejecutarChequeo(id);
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/invitar")
    public ResponseEntity<MonitoreoDTO> invitar(@PathVariable int id, @RequestBody Map<String, String> payload) {
        String emailInvitado = payload.get("email");
        if (emailInvitado == null || emailInvitado.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        MonitoreoDTO actualizado = monitoreoService.invitarUsuario(id, getActual().getId(), emailInvitado);
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}