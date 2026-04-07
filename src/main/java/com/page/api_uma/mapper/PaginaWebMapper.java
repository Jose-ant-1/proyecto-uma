package com.page.api_uma.mapper;

import com.page.api_uma.dto.PaginaWebDTO;
import com.page.api_uma.model.PaginaWeb;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaginaWebMapper {

    // Convierte de Entidad a DTO
    PaginaWebDTO toDTO(PaginaWeb paginaWeb);

    // Convierte de DTO a Entidad
    @Mapping(target = "monitoreos", ignore = true) // Ignoramos la lista para evitar problemas de persistencia circular
    PaginaWeb toEntity(PaginaWebDTO paginaWebDTO);
}