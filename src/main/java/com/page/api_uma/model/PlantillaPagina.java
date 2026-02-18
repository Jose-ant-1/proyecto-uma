package com.page.api_uma.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Table(name = "plantillaPag")
public class PlantillaPagina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String nombre;

    @ManyToMany
    @Column(insertable = false, updatable = false)
    @JoinTable(
            name = "pagina_plantillaPag",
            joinColumns = @JoinColumn(name = "id_plantillaPag"),
            inverseJoinColumns = @JoinColumn(name = "id_pagina")
    )
    private Set<PaginaWeb> paginasWeb;


}
