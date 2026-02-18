package com.page.api_uma.service;

import com.page.api_uma.model.PlantillaPagina;
import com.page.api_uma.repository.PlantillaPaginaRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PlantillaPaginaService {

    private final PlantillaPaginaRepository plantillaPaginaRepository;

    public PlantillaPaginaService(PlantillaPaginaRepository plantillaPaginaRepository) {
        this.plantillaPaginaRepository = plantillaPaginaRepository;
    }

    public List<PlantillaPagina> findAll() {
        return plantillaPaginaRepository.findAll();
    }

    public PlantillaPagina findById(Integer id) {
        return plantillaPaginaRepository.findById(id).orElse(null);
    }

    public PlantillaPagina save(PlantillaPagina plantillaPagina) {
        return plantillaPaginaRepository.save(plantillaPagina);
    }

    public void deleteById(Integer id) {
        plantillaPaginaRepository.deleteById(id);
    }

}
