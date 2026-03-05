package com.page.api_uma.service;

import com.page.api_uma.model.Monitoreo;
import com.page.api_uma.model.PaginaWeb;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.repository.UsuarioRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    public Usuario findById(Integer id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    // UsuarioService.java corregido
    public Usuario save(Usuario usuario) {
        if (usuario.getContrasenia() != null && !usuario.getContrasenia().isBlank()) {
            // IMPORTANTE: Solo encriptar si NO es ya un hash de BCrypt
            if (!usuario.getContrasenia().startsWith("$2a$")) {
                usuario.setContrasenia(passwordEncoder.encode(usuario.getContrasenia()));
            }
        }
        return usuarioRepository.save(usuario);
    }

    public void deleteById(Integer id) {
        usuarioRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Usuario findByEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    /**
     * REVISADO: Ahora obtenemos las páginas a través de los monitoreos.
     * Un usuario "tiene" páginas si es dueño de un monitoreo o invitado a uno.
     */
    public Set<Monitoreo> findMonitoreosAccessibles(Integer usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (usuario == null) {
            return Collections.emptySet();
        }

        return Stream.concat(
                        usuario.getMonitoreosPropios().stream(),
                        usuario.getMonitoreosInvitado().stream()
                )
                .collect(Collectors.toSet());
    }

    public Usuario buscarPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email);
        if (usuario == null) throw new UsernameNotFoundException("Usuario no encontrado con email: " + email);
        return usuario;
    }

    /**
     * Requerido por Spring Security para la autenticación.
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email);
        if (usuario == null) throw new UsernameNotFoundException("Usuario no encontrado con email: " + email);

        return User.withUsername(usuario.getEmail())
                .password(usuario.getContrasenia())
                .authorities(usuario.getPermiso()) // Rol: ADMIN, USER, etc.
                .build();
    }

    /**
     * Método central de seguridad: identifica al usuario logueado en la sesión actual.
     */
    public Usuario getUsuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email);
    }

}