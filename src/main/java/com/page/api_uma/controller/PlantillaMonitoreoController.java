package com.page.api_uma.controller;


import com.page.api_uma.model.PlantillaMonitoreo;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.service.PlantillaMonitoreoService;
import com.page.api_uma.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plantillaMonitoreo")
public class PlantillaMonitoreoController {

    private final PlantillaMonitoreoService service;
    private final UsuarioService usuarioService;

    public PlantillaMonitoreoController(PlantillaMonitoreoService service, UsuarioService usuarioService) {
        this.service = service;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<PlantillaMonitoreo> findAll(Principal principal) {
        return service.findByPropietario(principal.getName());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlantillaMonitoreo> findById(@PathVariable Integer id) {
        PlantillaMonitoreo pagina = service.findById(id);
        return pagina != null ? ResponseEntity.ok(pagina) : ResponseEntity.notFound().build();
    }

    @GetMapping("/propietario/{id}")
    public ResponseEntity<List<PlantillaMonitoreo>> findByPropietario(@PathVariable Integer id) {
        List<PlantillaMonitoreo> monitoreos = service.findByUsuario(id);
        return ResponseEntity.ok(monitoreos);
    }

    @PostMapping
    public PlantillaMonitoreo create(@RequestBody PlantillaMonitoreo pagina) {
        // 1. Obtenemos el usuario autenticado (el que está usando la app)
        Usuario actual = usuarioService.getUsuarioAutenticado();

        // 2. Le asignamos ese usuario como propietario a la plantilla
        // Esto rellena la columna 'id_propietario' que te está dando error
        pagina.setPropietario(actual);

        // 3. Ahora guardamos
        return service.save(pagina);
    }

    @PostMapping("/{id}/aplicar")
    public ResponseEntity<Void> aplicarPlantilla(@PathVariable Integer id, @RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        // Obtenemos al usuario logueado (Pedro) para validar propiedad
        Usuario actual = usuarioService.getUsuarioAutenticado();

        service.aplicarPlantillaAUsuario(id, email, actual.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlantillaMonitoreo> update(@PathVariable Integer id, @RequestBody PlantillaMonitoreo detalle) {
        PlantillaMonitoreo existe = service.findById(id);

        if (existe != null) {
            // 1. Mantenemos el propietario que ya tenía la plantilla en la DB
            detalle.setPropietario(existe.getPropietario());

            // 2. Aseguramos el ID
            detalle.setId(id);

            // 3. Guardamos
            return ResponseEntity.ok(service.save(detalle));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}