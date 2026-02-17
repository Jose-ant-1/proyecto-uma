package com.page.api_uma.controller;


import com.page.api_uma.model.Invitacion;
import com.page.api_uma.service.InvitacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invitaciones")
public class InvitacionController {

    private final InvitacionService service;

    public InvitacionController(InvitacionService service) {
        this.service = service;
    }
    @GetMapping
    public List<Invitacion> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Invitacion> findById(@PathVariable Integer id) {
        Invitacion invitacion = service.findById(id);
        return invitacion != null ? ResponseEntity.ok(invitacion) : ResponseEntity.notFound().build();

    }


    @PostMapping
    public Invitacion create(@RequestBody Invitacion invitacion){
        return service.save(invitacion);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Invitacion> update(@PathVariable Integer id, @RequestBody Invitacion detalles){
        Invitacion existente = service.findById(id);
        if(existente != null){
            detalles.setId(id);
            return ResponseEntity.ok(service.save(detalles));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id){
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
