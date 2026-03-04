package com.page.api_uma.DTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonitoreoDTO {
    private int id;
    private String nombre;
    private int minutosMonitoreo;

    // Información de estado para el usuario
    private Integer ultimoEstado;
    private LocalDateTime fechaUltimaRevision;
    private boolean activo;

    private String paginaUrl;
    private String propietarioNombre;
}
