package com.page.api_uma.repository;

import com.page.api_uma.model.PlantillaMonitoreo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlantillaMonitoreoRepository extends JpaRepository<PlantillaMonitoreo, Integer> {
}
