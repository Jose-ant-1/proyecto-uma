package com.page.api_uma.controller;

import com.page.api_uma.dto.AuthResponse;
import com.page.api_uma.dto.UsuarioDTO;
import com.page.api_uma.config.JwtService;
import com.page.api_uma.dto.UsuarioUpdateDTO;
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
    public ResponseEntity<Object> updateMe(@RequestBody UsuarioUpdateDTO datosRecibidos) {
        // Obtenemos el usuario real de la base de datos (sesión segura)
        Usuario actual = service.getUsuarioAutenticado();
        if (actual == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 1. Limpieza y validación de Nombre/Email
        String nombreLimpio = datosRecibidos.getNombre() != null ? datosRecibidos.getNombre().trim() : "";
        String emailLimpio = datosRecibidos.getEmail() != null ? datosRecibidos.getEmail().trim() : "";

        if (nombreLimpio.isEmpty() || emailLimpio.isEmpty()) {
            return ResponseEntity.badRequest().body("El nombre y el email no pueden estar vacíos.");
        }

        // Seteamos solo lo permitido para el propio usuario
        actual.setNombre(nombreLimpio);
        actual.setEmail(emailLimpio);

        // IMPORTANTE: No mapeamos datosRecibidos.getPermiso() aquí por seguridad.

        // 2. Validación de contraseña
        if (datosRecibidos.getContrasenia() != null) {
            String passNueva = datosRecibidos.getContrasenia();

            // Si es una cadena de puros espacios pero no está vacía ("   ")
            if (passNueva.isBlank() && !passNueva.isEmpty()) {
                return ResponseEntity.badRequest().body("La contraseña no puede consistir solo en espacios.");
            }
            // Si el usuario escribió algo, validamos longitud mínima
            else if (!passNueva.isEmpty()) {
                if (passNueva.length() < 4) {
                    return ResponseEntity.badRequest().body("La contraseña debe tener al menos 4 caracteres.");
                }
                actual.setContrasenia(passNueva);
            }
            // Si passNueva.isEmpty() es true, simplemente no entramos al else y no se cambia
        }

        // 3. Guardar (el service se encarga del hashing) y devolver el DTO de lectura
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
    public ResponseEntity<UsuarioDTO> create(@RequestBody UsuarioUpdateDTO dto) {
        Usuario nuevo = new Usuario();
        nuevo.setNombre(dto.getNombre());
        nuevo.setEmail(dto.getEmail());
        nuevo.setContrasenia(dto.getContrasenia());
        // Si el admin no manda permiso, por defecto es USER
        nuevo.setPermiso(dto.getPermiso() != null ? dto.getPermiso() : "USER");

        Usuario guardado = service.save(nuevo);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertirADTO(guardado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> update(@PathVariable Integer id, @RequestBody UsuarioUpdateDTO datosRecibidos) {
        Usuario usuarioExistente = service.findById(id);

        if (usuarioExistente != null) {
            // Actualizamos campos permitidos para el admin
            if (datosRecibidos.getNombre() != null) usuarioExistente.setNombre(datosRecibidos.getNombre());
            if (datosRecibidos.getEmail() != null) usuarioExistente.setEmail(datosRecibidos.getEmail());
            if (datosRecibidos.getPermiso() != null) usuarioExistente.setPermiso(datosRecibidos.getPermiso());

            // Solo actualizamos contraseña si viene algo de texto
            if (datosRecibidos.getContrasenia() != null && !datosRecibidos.getContrasenia().isBlank()) {
                usuarioExistente.setContrasenia(datosRecibidos.getContrasenia());
            }

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
