package com.page.api_uma.service;

import com.page.api_uma.dto.PlantillaMonitoreoDTO;
import com.page.api_uma.exception.ResourceNotFoundException;
import com.page.api_uma.mapper.PlantillaMonitoreoMapper;
import com.page.api_uma.model.Monitoreo;
import com.page.api_uma.model.PlantillaMonitoreo;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.repository.MonitoreoRepository;
import com.page.api_uma.repository.PlantillaMonitoreoRepository;
import com.page.api_uma.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlantillaMonitoreoService {

    private final PlantillaMonitoreoRepository plantillaMonitoreoRepository;
    private final UsuarioService usuarioService;
    private final MonitoreoRepository monitoreoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PlantillaMonitoreoMapper mapper;

    public PlantillaMonitoreoService(PlantillaMonitoreoRepository plantillaMonitoreoRepository, UsuarioService usuarioService, MonitoreoRepository monitoreoRepository, UsuarioRepository usuarioRepository, PlantillaMonitoreoMapper mapper) {
        this.plantillaMonitoreoRepository = plantillaMonitoreoRepository;
        this.usuarioService = usuarioService;
        this.monitoreoRepository = monitoreoRepository;
        this.usuarioRepository = usuarioRepository;
        this.mapper = mapper;
    }

    public List<PlantillaMonitoreo> findAll() {
        return plantillaMonitoreoRepository.findAll();
    }

    public PlantillaMonitoreo findById(Integer id) {
        return plantillaMonitoreoRepository.findById(id).orElse(null);
    }

    @Transactional
    public PlantillaMonitoreoDTO save(PlantillaMonitoreoDTO dto) {
        // Convertimos a entidad (propietario vendrá null por el mapper)
        PlantillaMonitoreo entidad = mapper.toEntity(dto);

        // obtener usuario autenticado
        Usuario actual = usuarioService.getUsuarioAutenticado();
        entidad.setPropietario(actual);

        // Manejar los monitoreos
        if (dto.getMonitoreos() != null) {
            entidad.setMonitoreos(dto.getMonitoreos());
        }

        PlantillaMonitoreo guardada = plantillaMonitoreoRepository.save(entidad);
        return mapper.toDTO(guardada);
    }

    public void deleteById(Integer id) {
        plantillaMonitoreoRepository.deleteById(id);
    }

    public List<PlantillaMonitoreo> findByUsuario(int usuarioId) {
        // Obtenemos las plantillas candidatas
        List<PlantillaMonitoreo> todasLasPlantillas = plantillaMonitoreoRepository.findAllRelatedToUsuario(usuarioId);

        // Filtramos mediante Stream para asegurar la regla de propiedad total
        return todasLasPlantillas.stream()
                .filter(plantilla -> this.isPropietarioTotal(plantilla, usuarioId))
                .toList();
    }

    private boolean isPropietarioTotal(PlantillaMonitoreo plantilla, int usuarioId) {
        // Todos los monitoreos de esta plantilla deben tener como propietario al usuarioId
        return plantilla.getMonitoreos().stream()
                .allMatch(m -> m.getPropietario().getId() == usuarioId);
    }

    public List<PlantillaMonitoreo> findByPropietario(String email) {
        Usuario owner = java.util.Optional.ofNullable(usuarioRepository.findByEmail(email))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return plantillaMonitoreoRepository.findByPropietarioOrderByNombreAsc(owner);
    }

    @Transactional
    public void aplicarPlantillaAUsuario(int plantillaId, String emailInvitado, int propietarioId) {
        // Buscamos la plantilla
        PlantillaMonitoreo plantilla = plantillaMonitoreoRepository.findById(plantillaId)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada"));

        // Buscamos al usuario que va a recibir los accesos
        Usuario invitado = usuarioService.buscarPorEmail(emailInvitado);
        if (invitado == null) throw new ResourceNotFoundException("Usuario a invitar no encontrado");

        // Recorremos los monitoreos de la plantilla
        for (Monitoreo m : plantilla.getMonitoreos()) {
            // Solo aplicamos si es el dueño del monitoreo
            if (m.getPropietario().getId() == propietarioId) {
                // Añadimos a la lista de invitados de este monitoreo
                m.getInvitados().add(invitado);
                // Guardamos el cambio individual
                monitoreoRepository.save(m);
            }
        }
    }


}
