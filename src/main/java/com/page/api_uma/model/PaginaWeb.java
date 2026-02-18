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
@Builder
@ToString
@Table(name = "pagina")
public class PaginaWeb {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String nombre;

    @Column(unique = true, nullable = false)
    private String url;

    private String notaInfo;

    @ManyToMany(mappedBy = "paginas")
    @JsonIgnore
    private Set<Usuario> usuarios = new HashSet<>();

    @ManyToMany(mappedBy = "paginasWeb")
    @JsonIgnore
    private Set<PlantillaPagina> plantillasPag = new HashSet<>();

}
