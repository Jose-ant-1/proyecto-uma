package com.page.api_uma.DTOs;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class MonitoreoDTO {
    private int id;
    private String nombre;
    private int minutosMonitoreo;
    private int repeticiones;
    private int propietarioId;

    // Información de estado para el usuario
    private Integer ultimoEstado;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaUltimaRevision;
    private boolean activo;
    private Set<String> invitadosCorreo;

    private String paginaUrl;
}
