package com.page.api_uma.dto;

import com.page.api_uma.model.Monitoreo;
import com.page.api_uma.model.Usuario;
import lombok.Data;

import java.util.Set;

@Data
public class PlantillaMonitoreoDTO {

    private int id;

    private String nombre;
    private Set<Monitoreo> monitoreos;

    private Usuario propietario;

}
