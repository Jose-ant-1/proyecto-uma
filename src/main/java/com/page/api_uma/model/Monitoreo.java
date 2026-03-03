package com.page.api_uma.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "monitoreos")
public class Monitoreo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String nombre;
    private int repeticiones;
    private int minutosMonitoreo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propietario_id", nullable = false)
    private Usuario propietario;

    @ManyToMany
    @JoinTable(
            name = "monitoreo_invitados",
            joinColumns = @JoinColumn(name = "monitoreo_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id")
    )
    private Set<Usuario> invitados;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pagina", nullable = false)
    private PaginaWeb paginaWeb;

    @ManyToMany(mappedBy = "monitoreos")
    @JsonIgnore
    private Set<PlantillaMonitoreo> plantillasMon = new HashSet<>();

}