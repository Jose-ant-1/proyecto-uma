package com.page.api_uma.controller;

import com.page.api_uma.model.PaginaWeb;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    // --- MÉTODOS PÚBLICOS / ACCESIBLES ---

    @GetMapping("/me")
    public ResponseEntity<Usuario> getMyProfile() {
        Usuario autenticado = service.getUsuarioAutenticado();
        return ResponseEntity.ok(autenticado);
    }

    @GetMapping("/{id}/paginas")
    public ResponseEntity<List<PaginaWeb>> findPaginasByUsuarioId(@PathVariable Integer id) {
        if (!tienePermisoLectura(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(service.findPaginasByUsuarioId(id));
    }

    // --- MÉTODOS DE ADMINISTRACIÓN (Protegidos por SecurityConfig) ---

    @GetMapping
    public List<Usuario> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> findById(@PathVariable Integer id) {
        Usuario usuario = service.findById(id);
        return usuario != null ? ResponseEntity.ok(usuario) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public Usuario create(@RequestBody Usuario usuario){
        return service.save(usuario);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id){
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private boolean tienePermisoLectura(Integer targetId) {
        Usuario autenticado = service.getUsuarioAutenticado();
        if (autenticado == null) return false;

        boolean isAdmin = "ADMIN".equalsIgnoreCase(autenticado.getPermiso());
        boolean esElMismo = Objects.equals(autenticado.getId(), targetId);

        return isAdmin || esElMismo;
    }
}
