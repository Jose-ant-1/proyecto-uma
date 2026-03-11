package com.page.api_uma.controller;

import com.page.api_uma.DTOs.UsuarioDTO;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    // --- PERFIL DEL USUARIO ---

    // En UsuarioController.java (Sugerencia para que el usuario se edite a sí mismo)
    @PutMapping("/me")
    public ResponseEntity<Usuario> updateMe(@RequestBody Usuario datosRecibidos) {

        Usuario actual = service.getUsuarioAutenticado();

        actual.setNombre(datosRecibidos.getNombre());
        actual.setEmail(datosRecibidos.getEmail());

        // 3. Verificamos si se ha enviado una nueva contraseña
        if (datosRecibidos.getContrasenia() != null && !datosRecibidos.getContrasenia().isBlank()) {
            actual.setContrasenia(datosRecibidos.getContrasenia());
        }

        // 4. Guardamos usando tu método 'save' que ya encripta
        Usuario guardado = service.save(actual);

        return ResponseEntity.ok(guardado);
    }

    // UsuarioController.java
    @GetMapping("/me")
    public ResponseEntity<UsuarioDTO> getMe() {
        Usuario actual = service.getUsuarioAutenticado();
        if (actual == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(convertirADTO(actual));
    }

    // --- ADMINISTRACIÓN ---

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> findAll() {
        List<UsuarioDTO> usuarios = service.findAll().stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/buscar")
    public ResponseEntity<UsuarioDTO> findByEmail(@RequestParam String email) {
        Usuario usuario = service.buscarPorEmail(email);
        return ResponseEntity.ok(convertirADTO(usuario));
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
    public ResponseEntity<UsuarioDTO> update(@PathVariable Integer id, @RequestBody Usuario datosRecibidos) {
        // 1. Buscamos el usuario real que está en la base de datos
        Usuario usuarioExistente = service.findById(id);

        if (usuarioExistente != null) {
            // 2. Actualizamos solo los campos básicos
            usuarioExistente.setNombre(datosRecibidos.getNombre());
            usuarioExistente.setEmail(datosRecibidos.getEmail());
            usuarioExistente.setPermiso(datosRecibidos.getPermiso());

            // 3. LÓGICA DE CONTRASEÑA:
            // Solo si recibimos una contraseña que no esté vacía, la actualizamos.
            // Si viene vacía o nula, mantenemos la que ya tenía el usuarioExistente (su hash).
            if (datosRecibidos.getContrasenia() != null && !datosRecibidos.getContrasenia().isBlank()) {
                usuarioExistente.setContrasenia(datosRecibidos.getContrasenia());
            }

            // 4. Guardamos el objeto 'usuarioExistente' (que ya tiene su ID y relaciones intactas)
            // Tu service.save() ya se encarga de encriptar si no empieza por $2a$
            Usuario guardado = service.save(usuarioExistente);

            return ResponseEntity.ok(convertirADTO(guardado));
        }

        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!tienePermiso(id)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

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