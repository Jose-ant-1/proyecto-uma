package com.page.api_uma.service;

import com.page.api_uma.model.PaginaWeb;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.repository.PaginaWebRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PaginaWebService {

    private final PaginaWebRepository paginaWebRepository;

    public PaginaWebService(PaginaWebRepository paginaWebRepository) {
        this.paginaWebRepository = paginaWebRepository;
    }

    public List<PaginaWeb> findAll() {
        return paginaWebRepository.findAll();
    }

    public PaginaWeb findById(Integer id) {
        return paginaWebRepository.findById(id).orElse(null);
    }

    public PaginaWeb save(PaginaWeb paginaWeb) {
        return paginaWebRepository.save(paginaWeb);
    }

    public void deleteById(Integer id) {
        paginaWebRepository.deleteById(id);
    }

    public boolean usuarioTieneAcceso(Usuario usuario, Integer paginaId) {
        if ("ADMIN".equalsIgnoreCase(usuario.getPermiso())) return true;

        // Verificamos si la página está en su Set de la relación N:M
        return usuario.getPaginas().stream()
                .anyMatch(p -> p.getId() == paginaId);
    }

}
