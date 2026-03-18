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
        Usuario owner = java.util.Optional.ofNullable(usuarioRepository.findByEmail(email))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return plantillaUsuarioRepository.findByPropietario(owner);
    }


    // Verificar si soy el dueño antes de editar/borra
    public boolean esPropietario(Integer plantillaId, String email) {
        return plantillaUsuarioRepository.findById(plantillaId)
                .map(p -> p.getPropietario().getEmail().equals(email))
                .orElse(false);
    }

    public void deleteById(Integer id) {
        plantillaUsuarioRepository.deleteById(id);
    }

}
