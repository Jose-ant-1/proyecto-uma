package com.page.api_uma.service;

import com.page.api_uma.model.PlantillaMonitoreo;
import com.page.api_uma.repository.PlantillaMonitoreoRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PlantillaMonitoreoService {

    private final PlantillaMonitoreoRepository plantillaPaginaRepository;

    public PlantillaMonitoreoService(PlantillaMonitoreoRepository plantillaPaginaRepository) {
        this.plantillaPaginaRepository = plantillaPaginaRepository;
    }

    public List<PlantillaMonitoreo> findAll() {
        return plantillaPaginaRepository.findAll();
    }

    public PlantillaMonitoreo findById(Integer id) {
        return plantillaPaginaRepository.findById(id).orElse(null);
    }

    public PlantillaMonitoreo save(PlantillaMonitoreo plantillaPagina) {
        return plantillaPaginaRepository.save(plantillaPagina);
    }

    public void deleteById(Integer id) {
        plantillaPaginaRepository.deleteById(id);
    }

}
