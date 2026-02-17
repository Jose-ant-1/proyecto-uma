package com.page.api_uma.service;

import com.page.api_uma.model.PaginaWeb;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.repository.PaginaWebRepository;
import com.page.api_uma.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    private final PaginaWebRepository paginaWebRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, PaginaWebRepository paginaWebRepository) {
        this.usuarioRepository = usuarioRepository;

        this.paginaWebRepository = paginaWebRepository;
    }

    public List<Usuario> findAll() {
        return usuarioRepository.findAll();
    }

    public Usuario findById(Integer id) {
        return usuarioRepository.findById(id).orElse(null);
    }

    public Usuario save(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

    public void deleteById(Integer id) {
        usuarioRepository.deleteById(id);
    }

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

}
