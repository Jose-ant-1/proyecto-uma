package com.page.api_uma.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(unique = true, nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String contrasenia;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String permiso;

    @ManyToMany(mappedBy = "usuarios")
    @JsonIgnore
    private Set<PlantillaUsuario> plantillaUsuarios = new HashSet<>();

    @OneToMany(mappedBy = "propietario", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Monitoreo> monitoreosPropios;

    @ManyToMany(mappedBy = "invitados")
    @JsonIgnore
    private Set<Monitoreo> monitoreosInvitado;


}
