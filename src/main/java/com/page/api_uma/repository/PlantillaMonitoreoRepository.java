package com.page.api_uma.repository;

import com.page.api_uma.model.PlantillaMonitoreo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlantillaMonitoreoRepository extends JpaRepository<PlantillaMonitoreo, Integer> {
    @Query("SELECT DISTINCT p FROM PlantillaMonitoreo p JOIN p.monitoreos m WHERE m.propietario.id = :usuarioId")
    List<PlantillaMonitoreo> findAllRelatedToUsuario(@Param("usuarioId") int usuarioId);
}
