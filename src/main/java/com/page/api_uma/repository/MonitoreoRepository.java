package com.page.api_uma.repository;

import com.page.api_uma.model.Monitoreo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MonitoreoRepository extends JpaRepository<Monitoreo, Integer> {

}