package com.page.api_uma.controller;


import com.page.api_uma.model.PlantillaPagina;
import com.page.api_uma.model.PlantillaUsuario;
import com.page.api_uma.service.PlantillaPaginaService;
import com.page.api_uma.service.PlantillaUsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plantillaUsuario")
public class PlantillaUsuarioController {

    private final PlantillaUsuarioService service;

    public PlantillaUsuarioController(PlantillaUsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<PlantillaUsuario> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlantillaUsuario> findById(@PathVariable Integer id) {
        PlantillaUsuario pagina = service.findById(id);
        return pagina != null ? ResponseEntity.ok(pagina) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public PlantillaUsuario create(@RequestBody PlantillaUsuario pagina) {
        return service.save(pagina);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlantillaUsuario> update(@PathVariable Integer id, @RequestBody PlantillaUsuario detalle) {
        PlantillaUsuario existe = service.findById(id);
        if (existe != null) {
            detalle.setId(id);
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