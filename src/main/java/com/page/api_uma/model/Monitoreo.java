package com.page.api_uma.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // Limpia el JSON de basura técnica
@Table(name = "monitoreos")
public class Monitoreo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private int id;

    private String nombre;
    private int repeticiones;
    private int minutos;

    private int fallosConsecutivos = 0;
    private boolean alertaEnviada = false; // Para no saturar el correo si sigue caída

    private Integer estado; // Almacena el código HTTP (200, 404, 500...)

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaUltimaRevision;
    private boolean activo = true; // Para que el usuario pueda pausar el monitoreo

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propietario_id", nullable = false)
    private Usuario propietario;

    @ManyToMany
    @JoinTable(
            name = "monitoreo_invitados",
            joinColumns = @JoinColumn(name = "monitoreo_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    private Set<Usuario> invitados = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pagina", nullable = false)
    private PaginaWeb paginaWeb;

    @ManyToMany(mappedBy = "monitoreos")
    @JsonIgnore
    private Set<PlantillaMonitoreo> plantillasMon = new HashSet<>();
}