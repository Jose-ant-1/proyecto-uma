package com.page.api_uma.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
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

    @OneToMany(mappedBy = "propietario", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Monitoreo> monitoreosPropios;

    @ManyToMany(mappedBy = "invitados")
    @JsonIgnore
    private Set<Monitoreo> monitoreosInvitado;


    @OneToMany(mappedBy = "propietario", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<PlantillaMonitoreo> plantillasMonitoreoPropias;

    @OneToMany(mappedBy = "propietario", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<PlantillaUsuario> plantillasUsuarioPropias;

}
