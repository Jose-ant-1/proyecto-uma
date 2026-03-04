package com.page.api_uma.repository;

import com.page.api_uma.model.Monitoreo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonitoreoRepository extends JpaRepository<Monitoreo, Integer> {
    @Query("SELECT m FROM Monitoreo m JOIN FETCH m.paginaWeb")
    List<Monitoreo> findAllWithPagina();
}