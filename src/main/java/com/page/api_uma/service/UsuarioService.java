package com.page.api_uma.service;

import com.page.api_uma.model.PaginaWeb;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.repository.PaginaWebRepository;
import com.page.api_uma.repository.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    private final PaginaWebRepository paginaWebRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PaginaWebRepository paginaWebRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;

        this.paginaWebRepository = paginaWebRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    public Usuario findById(Integer id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    public Usuario save(Usuario usuario) {
        usuario.setContrasenia(passwordEncoder.encode(usuario.getContrasenia()));
        return usuarioRepository.save(usuario);
    }

    public void deleteById(Integer id) {
        usuarioRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Usuario findByEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    public List<PaginaWeb> findPaginasByUsuarioId(Integer id){
        Usuario usuario = usuarioRepository.findById(id).orElse(null);
        if (usuario == null) {
            return Collections.emptyList();
        }

        return usuario.getPaginas().stream().toList();
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email);
        if (usuario == null) throw new UsernameNotFoundException("No existe: " + email);

        return User.withUsername(usuario.getEmail())
                .password(usuario.getContrasenia())
                // Usamos authorities para evitar el lío del prefijo ROLE_
                .authorities("ROLE_" + usuario.getPermiso())
                .build();
    }
}
