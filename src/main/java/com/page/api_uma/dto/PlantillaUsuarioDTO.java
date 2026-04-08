package com.page.api_uma.dto;

import com.page.api_uma.model.Usuario;
import lombok.Data;

import java.util.Set;

@Data
public class PlantillaUsuarioDTO {
    private int id;

    private String nombre;

    private Usuario propietario;

    private Set<Usuario> usuarios;

}
