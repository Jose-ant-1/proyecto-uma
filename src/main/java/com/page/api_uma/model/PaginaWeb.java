package com.page.api_uma.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
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

    @OneToMany(mappedBy = "paginaWeb", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Monitoreo> monitoreos;

}
