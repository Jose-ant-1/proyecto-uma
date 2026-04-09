package com.page.api_uma.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@ToString
@Table(name = "plantillaUsuario")
public class PlantillaUsuario implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "id_propietario", nullable = false)
    private Usuario propietario;

    @ManyToMany
    @JoinTable(
            name = "usuario_plantillaUsuar",
            joinColumns = @JoinColumn(name = "id_plantilla"),
            inverseJoinColumns = @JoinColumn(name = "id_usuario")
    )
    private Set<Usuario> usuarios;


}
