package com.page.api_uma.DTOs;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.page.api_uma.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonitoreoDTODetalle {
    private int id;
    private String nombre;
    private int minutos;
    private int repeticiones;

    private UsuarioDTO propietario;

    private Integer ultimoEstado;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaUltimaRevision;
    private boolean activo;

    // Lista de DTOs para los invitados
    private Set<UsuarioDTO> invitados;

    private String paginaUrl;
}
