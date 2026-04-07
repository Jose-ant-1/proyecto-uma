package com.page.api_uma.controller;

import com.page.api_uma.dto.AuthResponse;
import com.page.api_uma.dto.UsuarioDTO;
import com.page.api_uma.config.JwtService;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService service;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;


    public UsuarioController(UsuarioService service, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.service = service;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@RequestBody Usuario datosRecibidos) {
        Usuario actual = service.getUsuarioAutenticado();

        // nombre y email
        String nombreLimpio = datosRecibidos.getNombre() != null ? datosRecibidos.getNombre().trim() : "";
        String emailLimpio = datosRecibidos.getEmail() != null ? datosRecibidos.getEmail().trim() : "";

        if (nombreLimpio.isEmpty() || emailLimpio.isEmpty()) {
            return ResponseEntity.badRequest().body("El nombre y el email no pueden estar vacíos.");
        }

        actual.setNombre(nombreLimpio);
        actual.setEmail(emailLimpio);

        // VALIDACIÓN DE CONTRASEÑA
        if (datosRecibidos.getContrasenia() != null) {
            String passNueva = datosRecibidos.getContrasenia();

            // Si el usuario intentó mandar algo que solo son espacios o está vacío
            if (passNueva.isBlank()) {
                // No hacemos nada o devolvemos error.
                // si el usuario está editando su PERFIL (nombre/email), ignoramos el campo de contraseña si viene vacío/blanco.
                // si es un cambio explícito de password, lanzamos error:
                if (!passNueva.isEmpty()) {
                    return ResponseEntity.badRequest().body("La contraseña no puede consistir solo en espacios.");
                }
            } else if (passNueva.length() < 4) {
                return ResponseEntity.badRequest().body("La contraseña debe tener al menos 4 caracteres.");
            } else {
                actual.setContrasenia(passNueva);
            }
        }

        Usuario guardado = service.save(actual);
        return ResponseEntity.ok(convertirADTO(guardado));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioDTO> getMe() {
        Usuario actual = service.getUsuarioAutenticado();
        if (actual == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(convertirADTO(actual));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> findAll() {
        // Usar getUsuarios() para que vengan ordenados A-Z desde la DB
        List<UsuarioDTO> usuarios = service.getUsuarios().stream()
                .map(this::convertirADTO)
                .toList();
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> findById(@PathVariable Integer id) {
        Usuario usuario = service.findById(id);
        if (usuario == null) return ResponseEntity.notFound().build();

        return this.tienePermiso(id) ?
                ResponseEntity.ok(this.convertirADTO(usuario)) :
                ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @PostMapping
    public ResponseEntity<UsuarioDTO> create(@RequestBody Usuario usuario) {
        Usuario guardado = service.save(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertirADTO(guardado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> update(@PathVariable Integer id, @RequestBody Usuario datosRecibidos) {
        // Buscamos el usuario que está en la base de datos
        Usuario usuarioExistente = service.findById(id);

        if (usuarioExistente != null) {
            // Actualizamos solo los campos básicos
            usuarioExistente.setNombre(datosRecibidos.getNombre());
            usuarioExistente.setEmail(datosRecibidos.getEmail());
            usuarioExistente.setPermiso(datosRecibidos.getPermiso());

            // si recibimos una contraseña que no esté vacía, la actualizamos.
            // Si viene vacía o nula, mantenemos la que ya tenía el usuarioExistente
            if (datosRecibidos.getContrasenia() != null && !datosRecibidos.getContrasenia().isBlank()) {
                usuarioExistente.setContrasenia(datosRecibidos.getContrasenia());
            }

            // Guardamos el objeto
            // el service.save() ya se encripta si no empieza por $2a$
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

    @GetMapping("/buscar")
    public ResponseEntity<List<UsuarioDTO>> buscar(@RequestParam String q) {
        List<Usuario> usuarios = service.buscarUsuarios(q);
        List<UsuarioDTO> dtos = usuarios.stream()
                .map(this::convertirADTO)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody Map<String, String> request) {
        // Autenticar con Spring Security, esto comparará la contraseña enviada con el hash BCrypt de la DB
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.get("email"),
                        request.get("password")
                )
        );

        // Si la autenticación no lanzó error, buscamos al usuario
        var user = service.buscarPorEmail(request.get("email"));

        // Generamos el token JWT
        String token = jwtService.generateToken(user);

        // Devolvemos el token envuelto en nuestro DTO
        return ResponseEntity.ok(new AuthResponse(token));
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
