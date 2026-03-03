package com.page.api_uma.controller;

import com.page.api_uma.model.PaginaWeb;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    // --- MÉTODOS DEL PERFIL DEL USUARIO ---

    @GetMapping("/me")
    public ResponseEntity<Usuario> getMyProfile() {
        Usuario autenticado = service.getUsuarioAutenticado();
        if (autenticado == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(autenticado);
    }

    /**
     * REVISADO: Obtiene las páginas a las que el usuario tiene acceso
     * (vía monitoreos propios o invitaciones).
     */
    @GetMapping("/{id}/paginas")
    public ResponseEntity<Set<PaginaWeb>> findPaginasAccessibles(@PathVariable Integer id) {
        // Validación de seguridad: Solo el propio usuario o un ADMIN pueden ver esta lista
        if (!tienePermiso(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(service.findPaginasAccessibles(id));
    }

    // --- MÉTODOS DE ADMINISTRACIÓN ---

    @GetMapping
    public ResponseEntity<List<Usuario>> findAll() {
        // La restricción de ADMIN suele estar en SecurityConfig,
        // pero devolvemos ResponseEntity por consistencia.
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> findById(@PathVariable Integer id) {
        Usuario usuario = service.findById(id);
        return usuario != null ? ResponseEntity.ok(usuario) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Usuario> create(@RequestBody Usuario usuario){
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(usuario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id){
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Lógica de seguridad compartida:
     * Permite la acción si el usuario es ADMIN o si actúa sobre su propio ID.
     */
    private boolean tienePermiso(Integer targetId) {
        Usuario autenticado = service.getUsuarioAutenticado();
        if (autenticado == null) return false;

        boolean isAdmin = "ADMIN".equalsIgnoreCase(autenticado.getPermiso());
        boolean esElMismo = Objects.equals(autenticado.getId(), targetId);

        return isAdmin || esElMismo;
    }
}