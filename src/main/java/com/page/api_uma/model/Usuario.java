package com.page.api_uma.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
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
public class Usuario  implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private int id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String contrasenia;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String permiso;

    @ManyToMany(mappedBy = "usuarios")
    @JsonIgnore
    private transient Set<PlantillaUsuario> plantillaUsuarios = new HashSet<>();

    @OneToMany(mappedBy = "propietario", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private transient List<Monitoreo> monitoreosPropios;

    @ManyToMany(mappedBy = "invitados")
    @JsonIgnore
    private transient Set<Monitoreo> monitoreosInvitado;

    @OneToMany(mappedBy = "propietario", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private transient List<PlantillaMonitoreo> plantillasMonitoreoPropias;

    @OneToMany(mappedBy = "propietario", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private transient List<PlantillaUsuario> plantillasUsuarioPropias;


    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Esto le dice a Spring qué permisos tiene el usuario.
        // Usamos tu campo 'permiso' (que suele ser ADMIN o USER)
        return List.of(new SimpleGrantedAuthority(this.permiso));
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return this.contrasenia; // El campo donde guardas la clave BCrypt
    }

    @Override
    @JsonIgnore
    public String getUsername() {
        return this.email; // Tu "username" es el email para el login
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true; // La cuenta no expira
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true; // La cuenta no está bloqueada
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true; // Las credenciales no expiran
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return true; // El usuario está habilitado
    }

}
