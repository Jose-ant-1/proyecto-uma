package com.page.api_uma.controller;


import com.page.api_uma.model.PlantillaMonitoreo;
import com.page.api_uma.service.PlantillaMonitoreoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plantillaPagina")
public class PlantillaMonitoreoController {

    private final PlantillaMonitoreoService service;

    public PlantillaMonitoreoController(PlantillaMonitoreoService service) {
        this.service = service;
    }

    @GetMapping
    public List<PlantillaMonitoreo> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlantillaMonitoreo> findById(@PathVariable Integer id) {
        PlantillaMonitoreo pagina = service.findById(id);
        return pagina != null ? ResponseEntity.ok(pagina) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public PlantillaMonitoreo create(@RequestBody PlantillaMonitoreo pagina) {
        return service.save(pagina);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlantillaMonitoreo> update(@PathVariable Integer id, @RequestBody PlantillaMonitoreo detalle) {
        PlantillaMonitoreo existe = service.findById(id);
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