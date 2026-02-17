package com.page.api_uma.repository;

import com.page.api_uma.model.PlantillaInvitar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlantillaInvitacionRepository extends JpaRepository<PlantillaInvitar, Integer> {
}
