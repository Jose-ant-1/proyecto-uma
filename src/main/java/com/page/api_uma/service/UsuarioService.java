package com.page.api_uma.service;

import com.page.api_uma.model.Monitoreo;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
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
    private final EntityManager entityManager;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, EntityManager entityManager) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.entityManager = entityManager;
    }

    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    public Usuario findById(Integer id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    public Usuario save(Usuario usuario) {
        if (usuario.getContrasenia() != null && !usuario.getContrasenia().isBlank()) {
            // Solo encriptar si NO es ya un hash de BCrypt
            if (!usuario.getContrasenia().startsWith("$2a$")) {
                usuario.setContrasenia(passwordEncoder.encode(usuario.getContrasenia()));
            }
        }
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void deleteById(Integer id) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);

        if (usuario != null) {
            // Limpiar relaciones donde el usuario es "invitado"

            usuario.getMonitoreosInvitado().forEach(monitoreo -> {
                monitoreo.getInvitados().remove(usuario);
            });
            usuario.getMonitoreosInvitado().clear();

            // Limpiar tablas intermedias conflictivas vía Repositorio

            usuarioRepository.eliminarRelacionesDePlantillasDeSusMonitoreos(id);
            usuarioRepository.eliminarRelacionesEnPlantillasUsuario(id);

            // Asegurar que los cambios se manden a la DB antes del delete final
            usuarioRepository.saveAndFlush(usuario);

            usuarioRepository.delete(usuario);
        }
    }

    @Transactional(readOnly = true)
    public Usuario findByEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

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

    public List<Usuario> buscarUsuarios(String termino) {
        if (termino == null || termino.isBlank()) {
            return usuarioRepository.findAll();
        }
        return usuarioRepository.buscarPorTermino(termino);
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email);
        if (usuario == null) throw new UsernameNotFoundException("Usuario no encontrado con email: " + email);

        return User.withUsername(usuario.getEmail())
                .password(usuario.getContrasenia())
                .authorities(usuario.getPermiso())
                .build();
    }


     // identifica al usuario logueado en la sesión actual.

    public Usuario getUsuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email);
    }

}