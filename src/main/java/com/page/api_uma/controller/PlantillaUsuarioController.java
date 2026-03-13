package com.page.api_uma.controller;


import com.page.api_uma.model.PlantillaUsuario;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.service.PlantillaUsuarioService;
import com.page.api_uma.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

import java.util.List;

@RestController
@RequestMapping("/api/plantillaUsuario")
public class PlantillaUsuarioController {

    private final PlantillaUsuarioService service;
    private final UsuarioService usuarioService; // Para obtener el objeto Usuario completo

    public PlantillaUsuarioController(PlantillaUsuarioService service, UsuarioService usuarioService) {
        this.service = service;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<PlantillaUsuario> findAll(Principal principal) {
        // Solo devolvemos las del usuario logueado
        return service.findByPropietario(principal.getName());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlantillaUsuario> findById(@PathVariable Integer id, Principal principal) {
        if (!service.esPropietario(id, principal.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public PlantillaUsuario create(@RequestBody PlantillaUsuario plantilla, Principal principal) {
        // Asignamos el dueño automáticamente según el token/sesión
        Usuario owner = usuarioService.findByEmail(principal.getName());
        plantilla.setPropietario(owner);
        return service.save(plantilla);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlantillaUsuario> update(@PathVariable Integer id, @RequestBody PlantillaUsuario detalle, Principal principal) {
        if (!service.esPropietario(id, principal.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        detalle.setId(id);
        // Mantener el propietario original
        Usuario owner = usuarioService.findByEmail(principal.getName());
        detalle.setPropietario(owner);
        return ResponseEntity.ok(service.save(detalle));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id, Principal principal) {
        if (!service.esPropietario(id, principal.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}