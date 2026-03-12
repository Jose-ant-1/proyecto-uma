package com.page.api_uma.service;

import com.page.api_uma.model.PlantillaUsuario;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.repository.PlantillaUsuarioRepository;
import com.page.api_uma.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlantillaUsuarioService {

    private final PlantillaUsuarioRepository plantillaUsuarioRepository;
    private final UsuarioRepository usuarioRepository;

    public PlantillaUsuarioService(PlantillaUsuarioRepository plantillaInvitacionRepository, UsuarioRepository usuarioRepository) {
        this.plantillaUsuarioRepository = plantillaInvitacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<PlantillaUsuario> findAll() {
        return plantillaUsuarioRepository.findAll();
    }

    public PlantillaUsuario findById(Integer id) {
        return plantillaUsuarioRepository.findById(id).orElse(null);
    }

    public PlantillaUsuario save(PlantillaUsuario plantillaUsuario) {
        return plantillaUsuarioRepository.save(plantillaUsuario);
    }

    public List<PlantillaUsuario> findByPropietario(String email) {
        UsuarioService usuarioRepository;
        Usuario owner = usuarioRepository.findByEmail(email).orElseThrow();
        return repository.findByPropietario(owner);
    }

    // Verificar si soy el dueño antes de editar/borrar
    public boolean esPropietario(Integer plantillaId, String email) {
        PlantillaUsuario p = repository.findById(plantillaId).orElse(null);
        return p != null && p.getPropietario().getEmail().equals(email);
    }

    public void deleteById(Integer id) {
        plantillaUsuarioRepository.deleteById(id);
    }

}
