package com.page.api_uma.service;

import com.page.api_uma.model.Usuario;
import com.page.api_uma.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
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

    @Transactional
    public Usuario save(Usuario usuario) {
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo.");
        }

        // 1. Buscamos si el email ya existe
        Usuario existente = usuarioRepository.findByEmail(usuario.getEmail());

        // 2. Validación de email duplicado para tipos primitivos (int)
        if (existente != null && (usuario.getId() == 0 || usuario.getId() != existente.getId())) {
                throw new IllegalStateException("El email ya está registrado por otro usuario.");
            }

        // 3. Cifrado (Tu lógica original, sigue siendo robusta)
        String pass = usuario.getContrasenia();
        if (pass != null && !pass.isBlank() && !pass.startsWith("$2a$")) {
            usuario.setContrasenia(passwordEncoder.encode(pass));
        }

        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void deleteById(Integer id) {
        Usuario usuario = usuarioRepository.findById(id).orElse(null);

        if (usuario != null) {
            // proteccion
            if (usuario.getMonitoreosInvitado() != null) {
                usuario.getMonitoreosInvitado().forEach(monitoreo -> {
                    // Protección adicional por si la lista de invitados del monitoreo es nula
                    if (monitoreo.getInvitados() != null) {
                        monitoreo.getInvitados().remove(usuario);
                    }
                });
                usuario.getMonitoreosInvitado().clear();
            }

            // Limpiar tablas intermedias conflictivas vía Repositorio
            usuarioRepository.eliminarRelacionesDePlantillasDeSusMonitoreos(id);
            usuarioRepository.eliminarRelacionesEnPlantillasUsuario(id);

            usuarioRepository.saveAndFlush(usuario);
            usuarioRepository.delete(usuario);
        }
    }

    public Usuario buscarPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email);
        if (usuario == null) throw new UsernameNotFoundException("Credenciales inválidas o usuario inexistente.");
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

        // Si el permiso es nulo o vacío, asignamos uno genérico o una lista vacía
        String rol = (usuario.getPermiso() != null && !usuario.getPermiso().isBlank())
                ? usuario.getPermiso()
                : "ROLE_USER"; // O el rol mínimo de tu App

        return User.withUsername(usuario.getEmail())
                .password(usuario.getContrasenia())
                .authorities(rol)
                .build();
    }

    public List<Usuario> getUsuarios() {
        return usuarioRepository.findAllByOrderByNombreAsc();
    }

     // identifica al usuario logueado en la sesión actual.

    public Usuario getUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return usuarioRepository.findByEmail(auth.getName());
    }

}