package com.page.api_uma.service;

import com.page.api_uma.model.PlantillaMonitoreo;
import com.page.api_uma.repository.PlantillaMonitoreoRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

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

    public List<PlantillaMonitoreo> findByUsuario(int usuarioId) {
        // 1. Obtenemos las plantillas candidatas
        List<PlantillaMonitoreo> todasLasPlantillas = plantillaPaginaRepository.findAllRelatedToUsuario(usuarioId);

        // 2. Filtramos mediante Stream para asegurar la regla de propiedad total
        return todasLasPlantillas.stream()
                .filter(plantilla -> isPropietarioTotal(plantilla, usuarioId))
                .collect(Collectors.toList());
    }

    private boolean isPropietarioTotal(PlantillaMonitoreo plantilla, int usuarioId) {
        // Regla: Todos los monitoreos de esta plantilla deben tener como propietario al usuarioId
        return plantilla.getMonitoreos().stream()
                .allMatch(m -> m.getPropietario().getId() == usuarioId);
    }
}
