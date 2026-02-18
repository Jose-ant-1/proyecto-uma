package com.page.api_uma.service;

import com.page.api_uma.model.PlantillaUsuario;
import com.page.api_uma.repository.PlantillaUsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlantillaUsuarioService {

    private final PlantillaUsuarioRepository plantillaUsuarioRepository;

    public PlantillaUsuarioService(PlantillaUsuarioRepository plantillaInvitacionRepository) {
        this.plantillaUsuarioRepository = plantillaInvitacionRepository;
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

    public void deleteById(Integer id) {
        plantillaUsuarioRepository.deleteById(id);
    }

}
