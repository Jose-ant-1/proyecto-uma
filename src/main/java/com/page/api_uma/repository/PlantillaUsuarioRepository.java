package com.page.api_uma.repository;

import com.page.api_uma.model.PlantillaUsuario;
import com.page.api_uma.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlantillaUsuarioRepository extends JpaRepository<PlantillaUsuario, Integer> {
    List<PlantillaUsuario> findByPropietarioOrderByNombreAsc(Usuario propietario);
}
