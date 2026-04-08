package com.page.api_uma.controller;


import com.page.api_uma.dto.PlantillaUsuarioDTO;
import com.page.api_uma.mapper.PlantillaUsuarioMapper;
import com.page.api_uma.model.PlantillaUsuario;
import com.page.api_uma.service.PlantillaUsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

import java.util.List;

@RestController
@RequestMapping("/api/plantillaUsuario")
public class PlantillaUsuarioController {

    private final PlantillaUsuarioService service;
    private final PlantillaUsuarioMapper mapper;

    public PlantillaUsuarioController(PlantillaUsuarioService service, PlantillaUsuarioMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<List<PlantillaUsuarioDTO>> findAll(Principal principal) {
        List<PlantillaUsuario> lista = service.findByPropietario(principal.getName());
        return ResponseEntity.ok(mapper.toDTOList(lista));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlantillaUsuario> findById(@PathVariable Integer id, Principal principal) {
        if (!service.esPropietario(id, principal.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<PlantillaUsuarioDTO> create(@RequestBody PlantillaUsuarioDTO dto, Principal principal) {
        // Pasamos el email del principal para que el service asigne el dueño
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(dto, principal.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlantillaUsuarioDTO> update(@PathVariable Integer id, @RequestBody PlantillaUsuarioDTO dto, Principal principal) {
        if (!service.esPropietario(id, principal.getName())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        dto.setId(id);
        return ResponseEntity.ok(service.save(dto, principal.getName()));
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