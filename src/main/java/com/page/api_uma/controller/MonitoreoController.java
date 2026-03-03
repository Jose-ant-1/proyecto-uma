package com.page.api_uma.controller;


import com.page.api_uma.model.Monitoreo;
import com.page.api_uma.service.MonitoreoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitoreo")
public class MonitoreoController {

    private final MonitoreoService service;

    public MonitoreoController(MonitoreoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Monitoreo> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Monitoreo> findById(@PathVariable Integer id) {
        Monitoreo monitoreo = service.findById(id);
        return monitoreo != null ? ResponseEntity.ok(monitoreo) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public Monitoreo create(@RequestBody Monitoreo pagina) {
        return service.save(pagina);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Monitoreo> update(@PathVariable Integer id, @RequestBody Monitoreo detalle) {
        Monitoreo existe = service.findById(id);
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