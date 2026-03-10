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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@Table(name = "plantillaMonitoreo")
public class PlantillaMonitoreo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String nombre;

    @ManyToMany
    @JoinTable(
            name = "monitoreo_plantillaMon",
            joinColumns = @JoinColumn(name = "id_plantillaMon"),
            inverseJoinColumns = @JoinColumn(name = "id_monitoreo")
    )
    private Set<Monitoreo> monitoreos;


}
