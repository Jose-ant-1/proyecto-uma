package com.page.api_uma.mapper;

import com.page.api_uma.dto.PlantillaMonitoreoDTO;
import com.page.api_uma.model.PlantillaMonitoreo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlantillaMonitoreoMapper {

    // Convierte de Entidad a DTO (para enviar al Frontend)
    PlantillaMonitoreoDTO toDTO(PlantillaMonitoreo plantilla);

    // Convierte de DTO a Entidad (para guardar en DB)
    @Mapping(target = "propietario", ignore = true) // El propietario lo asignamos manualmente en el Service
    @Mapping(target = "monitoreos", ignore = true)  // Las relaciones complejas mejor manejarlas en el Service
    PlantillaMonitoreo toEntity(PlantillaMonitoreoDTO dto);
}