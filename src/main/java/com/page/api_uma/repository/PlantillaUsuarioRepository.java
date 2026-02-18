package com.page.api_uma.repository;

import com.page.api_uma.model.PlantillaPagina;
import com.page.api_uma.model.PlantillaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlantillaUsuarioRepository extends JpaRepository<PlantillaUsuario, Integer> {
}
