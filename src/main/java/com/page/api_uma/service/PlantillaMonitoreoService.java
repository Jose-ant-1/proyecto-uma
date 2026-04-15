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

import java.util.HashSet;
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
        // 1. El mapper convierte el DTO a Entidad (incluyendo la colección de monitoreos si existe)
        PlantillaMonitoreo entidad = mapper.toEntity(dto);

        // 2. Forzamos el propietario real desde el contexto de seguridad
        Usuario actual = usuarioService.getUsuarioAutenticado();
        entidad.setPropietario(actual);

        // 3. Guardar y retornar
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

    public List<PlantillaMonitoreo> findByPropietario(String email) {
        Usuario owner = java.util.Optional.ofNullable(usuarioRepository.findByEmail(email))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return plantillaMonitoreoRepository.findByPropietarioOrderByNombreAsc(owner);
    }

    @Transactional
    public void aplicarPlantillaAUsuario(int plantillaId, String emailInvitado, int propietarioId) {
        PlantillaMonitoreo plantilla = plantillaMonitoreoRepository.findById(plantillaId)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada"));

        Usuario invitado = usuarioService.buscarPorEmail(emailInvitado);
        if (invitado == null) throw new ResourceNotFoundException("Usuario a invitar no encontrado");

        for (Monitoreo m : plantilla.getMonitoreos()) {
            // Validamos que el monitoreo tenga propietario antes de comparar IDs
            if (m.getPropietario() != null && m.getPropietario().getId() == propietarioId) {

                // Si por alguna razón los invitados son null, lo inicializamos al vuelo
                if (m.getInvitados() == null) {
                    m.setInvitados(new HashSet<>());
                }

                m.getInvitados().add(invitado);
                monitoreoRepository.save(m);
            }
        }
    }

    private boolean isPropietarioTotal(PlantillaMonitoreo plantilla, int usuarioId) {
        if (plantilla.getMonitoreos() == null) return true; // O false, según tu lógica de negocio

        return plantilla.getMonitoreos().stream()
                .allMatch(m -> m.getPropietario() != null && m.getPropietario().getId() == usuarioId);
    }


}
