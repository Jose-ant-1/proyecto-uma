package com.page.api_uma.controller;


import com.page.api_uma.model.PlantillaPagina;
import com.page.api_uma.service.PlantillaPaginaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plantillaPagina")
public class PlantillaPaginaController {

    private final PlantillaPaginaService service;

    public PlantillaPaginaController(PlantillaPaginaService service) {
        this.service = service;
    }

    @GetMapping
    public List<PlantillaPagina> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlantillaPagina> findById(@PathVariable Integer id) {
        PlantillaPagina pagina = service.findById(id);
        return pagina != null ? ResponseEntity.ok(pagina) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public PlantillaPagina create(@RequestBody PlantillaPagina pagina) {
        return service.save(pagina);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlantillaPagina> update(@PathVariable Integer id, @RequestBody PlantillaPagina detalle) {
        PlantillaPagina existe = service.findById(id);
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