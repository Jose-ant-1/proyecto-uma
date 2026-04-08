package com.page.api_uma.mapper;

import com.page.api_uma.dto.PlantillaUsuarioDTO;
import com.page.api_uma.model.PlantillaUsuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PlantillaUsuarioMapper {
    PlantillaUsuarioDTO toDTO(PlantillaUsuario entidad);

    @Mapping(target = "propietario", ignore = true)
    @Mapping(target = "usuarios", ignore = true)
    PlantillaUsuario toEntity(PlantillaUsuarioDTO dto);

    List<PlantillaUsuarioDTO> toDTOList(List<PlantillaUsuario> lista);
}
