package com.page.api_uma.repository;

import com.page.api_uma.model.PaginaWeb;
import jakarta.persistence.Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaginaWebRepository extends JpaRepository<PaginaWeb, Integer> {

}
