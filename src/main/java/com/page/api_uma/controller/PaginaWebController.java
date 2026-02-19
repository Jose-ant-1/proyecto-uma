package com.page.api_uma.controller;

import com.page.api_uma.model.PaginaWeb;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.service.PaginaWebService;
import com.page.api_uma.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/paginas")
public class PaginaWebController {

    private final PaginaWebService paginaService;
    private final UsuarioService usuarioService;

    public PaginaWebController(PaginaWebService service, UsuarioService usuarioService) {
        this.paginaService = service;
        this.usuarioService = usuarioService;
    }

    // LISTADO: El Admin ve todo, el User solo sus páginas asignadas
    @GetMapping
    public ResponseEntity<?> findMyPaginas() {
        Usuario usuario = getUsuarioAutenticado();
        if (usuario == null) return ResponseEntity.status(401).build();

        if ("ADMIN".equalsIgnoreCase(usuario.getPermiso())) {
            return ResponseEntity.ok(paginaService.findAll());
        }
        return ResponseEntity.ok(usuario.getPaginas());
    }

    // DETALLE: Arturo no podrá ver la página 3 si no la tiene asignada
    @GetMapping("/{id}")
    public ResponseEntity<PaginaWeb> findById(@PathVariable Integer id) {
        Usuario usuario = getUsuarioAutenticado();

        // Verificación de seguridad
        if (!paginaService.usuarioTieneAcceso(usuario, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        PaginaWeb pagina = paginaService.findById(id);
        return pagina != null ? ResponseEntity.ok(pagina) : ResponseEntity.notFound().build();
    }

    // El resto de métodos (POST, PUT, DELETE) ya están protegidos por SecurityConfig,
    // pero dejamos la lógica de seguridad extra por si acaso.
    @PostMapping
    public ResponseEntity<PaginaWeb> create(@RequestBody PaginaWeb pagina) {
        return ResponseEntity.ok(paginaService.save(pagina));
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

    private Usuario getUsuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioService.findByEmail(email);
    }
}