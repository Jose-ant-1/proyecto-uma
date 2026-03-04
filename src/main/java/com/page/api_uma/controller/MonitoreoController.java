package com.page.api_uma.controller;

import com.page.api_uma.DTOs.MonitoreoDTO;
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

    // --- 1. CREATE (Personalizado para tu lógica de URL) ---
    @PostMapping
    public ResponseEntity<MonitoreoDTO> create(@RequestBody Map<String, Object> payload) {
        Usuario actual = usuarioService.getUsuarioAutenticado();
        String url = (String) payload.get("url");
        String nombrePagina = (String) payload.get("nombrePagina");
        int minutos = ((Number) payload.get("minutos")).intValue();

        MonitoreoDTO nuevo = monitoreoService.crearMonitoreo(actual, url, nombrePagina, minutos);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    // --- 2. READ ALL (Filtrado por usuario para seguridad) ---
    @GetMapping
    public ResponseEntity<List<MonitoreoDTO>> getAll() {
        Usuario actual = usuarioService.getUsuarioAutenticado();
        // Devolvemos la unión de los propios e invitados
        List<MonitoreoDTO> propios = actual.getMonitoreosPropios().stream()
                .map(monitoreoService::convertirADTO).collect(Collectors.toList());
        return ResponseEntity.ok(propios);
    }

    // --- 3. READ ONE (Por ID) ---
    @GetMapping("/{id}")
    public ResponseEntity<MonitoreoDTO> getById(@PathVariable int id) {
        MonitoreoDTO dto = monitoreoService.obtenerPorIdSiTieneAcceso(id, usuarioService.getUsuarioAutenticado().getId());
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // --- 4. UPDATE (Actualizar nombre o minutos) ---
    @PutMapping("/{id}")
    public ResponseEntity<MonitoreoDTO> update(@PathVariable int id, @RequestBody Map<String, Object> payload) {
        Usuario actual = usuarioService.getUsuarioAutenticado();
        // Solo el dueño debería poder editar la configuración
        MonitoreoDTO actualizado = monitoreoService.actualizarConfiguracion(id, actual.getId(), payload);
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // --- 5. DELETE ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        Usuario actual = usuarioService.getUsuarioAutenticado();
        boolean eliminado = monitoreoService.eliminarMonitoreo(id, actual.getId());
        return eliminado ? ResponseEntity.noContent().build() : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // --- MÉTODOS EXTRA (Chequeo de estado) ---
    @PostMapping("/{id}/check")
    public ResponseEntity<MonitoreoDTO> checkNow(@PathVariable int id) {
        MonitoreoDTO actualizado = monitoreoService.ejecutarChequeo(id);
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.notFound().build();
    }
}