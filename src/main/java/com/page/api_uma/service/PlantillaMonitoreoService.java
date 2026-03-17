package com.page.api_uma.service;

import com.page.api_uma.model.Monitoreo;
import com.page.api_uma.model.PlantillaMonitoreo;
import com.page.api_uma.model.PlantillaUsuario;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.repository.MonitoreoRepository;
import com.page.api_uma.repository.PlantillaMonitoreoRepository;
import com.page.api_uma.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlantillaMonitoreoService {

    private final PlantillaMonitoreoRepository plantillaMonitoreoRepository;
    private final UsuarioService usuarioService;
    private final MonitoreoRepository monitoreoRepository;
    private final UsuarioRepository usuarioRepository;


    public PlantillaMonitoreoService(PlantillaMonitoreoRepository plantillaMonitoreoRepository, UsuarioService usuarioService, MonitoreoRepository monitoreoRepository, UsuarioRepository usuarioRepository) {
        this.plantillaMonitoreoRepository = plantillaMonitoreoRepository;
        this.usuarioService = usuarioService;
        this.monitoreoRepository = monitoreoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<PlantillaMonitoreo> findAll() {
        return plantillaMonitoreoRepository.findAll();
    }

    public PlantillaMonitoreo findById(Integer id) {
        return plantillaMonitoreoRepository.findById(id).orElse(null);
    }

    public PlantillaMonitoreo save(PlantillaMonitoreo plantillaPagina) {
        return plantillaMonitoreoRepository.save(plantillaPagina);
    }

    public void deleteById(Integer id) {
        plantillaMonitoreoRepository.deleteById(id);
    }

    public List<PlantillaMonitoreo> findByUsuario(int usuarioId) {
        // 1. Obtenemos las plantillas candidatas
        List<PlantillaMonitoreo> todasLasPlantillas = plantillaMonitoreoRepository.findAllRelatedToUsuario(usuarioId);

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

    public List<PlantillaMonitoreo> findByPropietario(String email) {
        // Solución para "Cannot resolve method orElseThrow"
        Usuario owner = java.util.Optional.ofNullable(usuarioRepository.findByEmail(email))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return plantillaMonitoreoRepository.findByPropietario(owner);
    }

    @Transactional
    public void aplicarPlantillaAUsuario(int plantillaId, String emailInvitado, int propietarioId) {
        // 1. Buscamos la plantilla
        PlantillaMonitoreo plantilla = plantillaMonitoreoRepository.findById(plantillaId)
                .orElseThrow(() -> new RuntimeException("Plantilla no encontrada"));

        // 2. Buscamos al usuario que va a recibir los accesos (Arturo)
        Usuario invitado = usuarioService.buscarPorEmail(emailInvitado);
        if (invitado == null) throw new RuntimeException("Usuario a invitar no encontrado");

        // 3. Recorremos los monitoreos de la plantilla
        for (Monitoreo m : plantilla.getMonitoreos()) {
            // SEGURIDAD: Solo aplicamos si Pedro (propietarioId) es el dueño del monitoreo
            if (m.getPropietario().getId() == propietarioId) {
                // Añadimos a Arturo a la lista de invitados de este monitoreo
                m.getInvitados().add(invitado);
                // Guardamos el cambio individual
                monitoreoRepository.save(m);
            }
        }
    }


}
