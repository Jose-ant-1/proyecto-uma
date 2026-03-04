package com.page.api_uma.controller;

import com.page.api_uma.DTOs.UsuarioDTO;
import com.page.api_uma.model.Monitoreo;
import com.page.api_uma.model.PaginaWeb;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    // --- PERFIL DEL USUARIO ---

    @GetMapping("/me")
    public ResponseEntity<UsuarioDTO> getMyProfile() {
        Usuario autenticado = service.getUsuarioAutenticado();
        if (autenticado == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(convertirADTO(autenticado));
    }

    @GetMapping("/{id}/monitoreos")
    public ResponseEntity<Set<Monitoreo>> findMonitoreosAccessibles(@PathVariable Integer id) {
        if (!tienePermiso(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        // Aquí devolvemos PaginaWeb porque no suele tener datos sensibles,
        // pero recuerda ponerle el @JsonIgnoreProperties en la entidad.
        return ResponseEntity.ok(service.findMonitoreosAccessibles(id));
    }

    // --- ADMINISTRACIÓN ---

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> findAll() {
        List<UsuarioDTO> usuarios = service.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> findById(@PathVariable Integer id) {
        Usuario usuario = service.findById(id);
        if (usuario == null) return ResponseEntity.notFound().build();

        return tienePermiso(id) ?
                ResponseEntity.ok(convertirADTO(usuario)) :
                ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping
    public ResponseEntity<UsuarioDTO> create(@RequestBody Usuario usuario) {
        // Al crear, devolvemos el DTO para no exponer la contraseña recién creada
        Usuario guardado = service.save(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertirADTO(guardado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> update(@PathVariable Integer id, @RequestBody Usuario usuario) {
        Usuario existe = service.findById(id);
        if (existe != null) {
            usuario.setId(id);
            return ResponseEntity.ok(convertirADTO(service.save(usuario)));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        // Solo un ADMIN o el propio usuario pueden borrar la cuenta
        if (!tienePermiso(id)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // --- UTILIDADES ---

    private UsuarioDTO convertirADTO(Usuario u) {
        return UsuarioDTO.builder()
                .id(u.getId())
                .nombre(u.getNombre())
                .email(u.getEmail())
                .permiso(u.getPermiso())
                .build();
    }

    private boolean tienePermiso(Integer targetId) {
        Usuario autenticado = service.getUsuarioAutenticado();
        if (autenticado == null) return false;

        boolean isAdmin = "ADMIN".equalsIgnoreCase(autenticado.getPermiso());
        boolean esElMismo = Objects.equals(autenticado.getId(), targetId);

        return isAdmin || esElMismo;
    }
}