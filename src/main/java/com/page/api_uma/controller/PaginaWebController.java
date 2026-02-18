package com.page.api_uma.controller;

import com.page.api_uma.model.PaginaWeb;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.service.PaginaWebService;
import com.page.api_uma.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/paginas")
public class PaginaWebController {

    private final PaginaWebService paginaService;
    private final PaginaWebService paginaWebService;
    private final UsuarioService usuarioService;


    public PaginaWebController(PaginaWebService service, PaginaWebService paginaWebService, UsuarioService usuarioService) {
        this.paginaService = service;
        this.paginaWebService = paginaWebService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<PaginaWeb> findAll() {
        return paginaService.findAll();
    }

    @GetMapping
    public Set<PaginaWeb> findMyPaginas() {
        // 1. Obtener email del contexto de seguridad
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        Usuario usuario = usuarioService.findByEmail(email);

        if (usuario == null) {
            return Collections.emptySet();
        }

        // 2. Si es ADMIN, queremos ver todas las páginas de la app
        if ("ADMIN".equalsIgnoreCase(usuario.getPermiso())) {
            return new HashSet<>(paginaWebService.findAll());
        }

        // 3. Si es USER, devolvemos su Set personal
        return usuario.getPaginas();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaginaWeb> findById(@PathVariable Integer id) {
        PaginaWeb pagina = paginaService.findById(id);
        return pagina != null ? ResponseEntity.ok(pagina) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public PaginaWeb create(@RequestBody PaginaWeb pagina) {
        return paginaService.save(pagina);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaginaWeb> update(@PathVariable Integer id, @RequestBody PaginaWeb detalle) {
        PaginaWeb existe = paginaService.findById(id);
        if (existe != null) {
            detalle.setId(id);
            return ResponseEntity.ok(paginaService.save(detalle));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        paginaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
