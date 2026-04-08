package com.page.api_uma.dto;

import lombok.Data;

@Data
public class UsuarioUpdateDTO {
    private Integer id;
    private String nombre;
    private String email;
    private String permiso;
    private String contrasenia;
}
