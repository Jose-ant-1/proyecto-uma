package com.page.api_uma.repository;

import com.page.api_uma.model.PlantillaPagina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlantillaPaginaRepository extends JpaRepository<PlantillaPagina, Integer> {
}
