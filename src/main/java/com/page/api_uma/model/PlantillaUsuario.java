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
@Table(name = "plantillaUsuario")
public class PlantillaUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String nombre;

    @ManyToMany
    @Column(insertable = false, updatable = false)
    @JoinTable(
            name = "usuario_plantillaUsuar",
            joinColumns = @JoinColumn(name = "id_plantilla"),
            inverseJoinColumns = @JoinColumn(name = "id_usuario")
    )
    private Set<Usuario> usuarios;


}
