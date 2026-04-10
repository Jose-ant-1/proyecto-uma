package com.page.api_uma.controller;


import com.page.api_uma.dto.PlantillaMonitoreoDTO;
import com.page.api_uma.model.PlantillaMonitoreo;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.service.PlantillaMonitoreoService;
import com.page.api_uma.service.UsuarioService;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<List<PlantillaMonitoreo>> findAll(Principal principal) {
        return ResponseEntity.ok(service.findByPropietario(principal.getName()));
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
    public ResponseEntity<PlantillaMonitoreoDTO> create(@RequestBody PlantillaMonitoreoDTO dto) {
        // Guardamos a través del service
        PlantillaMonitoreoDTO resultado = service.save(dto);

        // Devolvemos 201 Created con el objeto en el cuerpo
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    @PostMapping("/{id}/aplicar")
    public ResponseEntity<Void> aplicarPlantilla(@PathVariable Integer id, @RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        // Obtenemos al usuario logueado para validar propiedad
        Usuario actual = usuarioService.getUsuarioAutenticado();
        service.aplicarPlantillaAUsuario(id, email, actual.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlantillaMonitoreoDTO> update(@PathVariable Integer id, @RequestBody PlantillaMonitoreoDTO dto) {
        // Validamos si existe antes de actualizar
        if (service.findById(id) == null) {
            return ResponseEntity.notFound().build();
        }

        dto.setId(id); // Aseguramos que actualice el ID correcto
        PlantillaMonitoreoDTO actualizado = service.save(dto);

        return ResponseEntity.ok(actualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}