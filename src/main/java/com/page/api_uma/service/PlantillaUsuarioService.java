package com.page.api_uma.service;

import com.page.api_uma.dto.PlantillaUsuarioDTO;
import com.page.api_uma.mapper.PlantillaUsuarioMapper;
import com.page.api_uma.model.PlantillaUsuario;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.repository.PlantillaUsuarioRepository;
import com.page.api_uma.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PlantillaUsuarioService {

    private final PlantillaUsuarioRepository plantillaUsuarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final PlantillaUsuarioMapper mapper;


    public PlantillaUsuarioService(PlantillaUsuarioRepository plantillaUsuarioRepository, UsuarioRepository usuarioRepository, PlantillaUsuarioMapper mapper) {
        this.plantillaUsuarioRepository = plantillaUsuarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.mapper = mapper;
    }

    public List<PlantillaUsuario> findAll() {
        return plantillaUsuarioRepository.findAll();
    }

    public PlantillaUsuario findById(Integer id) {
        return plantillaUsuarioRepository.findById(id).orElse(null);
    }

    @Transactional
    public PlantillaUsuarioDTO save(PlantillaUsuarioDTO dto, String emailPropietario) {
        PlantillaUsuario entidad = mapper.toEntity(dto);

        // CAMBIO: Añadimos validación con Optional (igual que en findByPropietario)
        Usuario owner = java.util.Optional.ofNullable(usuarioRepository.findByEmail(emailPropietario))
                .orElseThrow(() -> new RuntimeException("Propietario no encontrado: " + emailPropietario));

        entidad.setPropietario(owner);

        if (dto.getUsuarios() != null && !dto.getUsuarios().isEmpty()) {
            Set<Usuario> usuariosReal = dto.getUsuarios().stream()
                    .map(u -> usuarioRepository.findById(u.getId())
                            .orElseThrow(() -> new RuntimeException("Usuario invitado no encontrado: " + u.getId())))
                    .collect(Collectors.toSet());

            entidad.setUsuarios(usuariosReal);
        }

        PlantillaUsuario guardada = plantillaUsuarioRepository.save(entidad);
        return mapper.toDTO(guardada);
    }

    public List<PlantillaUsuario> findByPropietario(String email) {
        Usuario owner = java.util.Optional.ofNullable(usuarioRepository.findByEmail(email))
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return plantillaUsuarioRepository.findByPropietarioOrderByNombreAsc(owner);
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
