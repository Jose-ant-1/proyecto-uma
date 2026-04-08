package com.page.api_uma.service;

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

import java.util.List;

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

    public Usuario save(Usuario usuario) {
        if (usuario.getContrasenia() != null &&
                !usuario.getContrasenia().isBlank() &&
                !usuario.getContrasenia().startsWith("$2a$")) {
            usuario.setContrasenia(passwordEncoder.encode(usuario.getContrasenia()));
        }
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void deleteById(Integer id) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);

        if (usuario != null) {
            // Limpiar relaciones donde el usuario es "invitado"

            usuario.getMonitoreosInvitado().forEach(monitoreo ->
                    monitoreo.getInvitados().remove(usuario)
            );
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

    public List<Usuario> getUsuarios() {
        return usuarioRepository.findAllByOrderByNombreAsc();
    }

     // identifica al usuario logueado en la sesión actual.

    public Usuario getUsuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email);
    }

}