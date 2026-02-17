package com.page.api_uma.service;

import com.page.api_uma.model.PlantillaInvitar;
import com.page.api_uma.repository.PlantillaInvitacionRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PlantillaInvitaService {

    private final PlantillaInvitacionRepository plantillaInvitacionRepository;

    public PlantillaInvitaService(PlantillaInvitacionRepository plantillaInvitacionRepository) {
        this.plantillaInvitacionRepository = plantillaInvitacionRepository;
    }

    public List<PlantillaInvitar> findAll() {
        return plantillaInvitacionRepository.findAll();
    }

    public PlantillaInvitar findById(Integer id) {
        return plantillaInvitacionRepository.findById(id).orElse(null);
    }

    public PlantillaInvitar save(PlantillaInvitar plantillaInvitar) {
        return plantillaInvitacionRepository.save(plantillaInvitar);
    }

    public void deleteById(Integer id) {
        plantillaInvitacionRepository.deleteById(id);
    }

}
