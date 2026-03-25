package com.page.api_uma.repository;

import com.page.api_uma.model.PaginaWeb;
import jakarta.persistence.Entity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaginaWebRepository extends JpaRepository<PaginaWeb, Integer> {
    Optional<PaginaWeb> findByUrl(String url);

    @Query("SELECT p FROM PaginaWeb p WHERE p.nombre LIKE %:term% OR p.url LIKE %:term%")
    List<PaginaWeb> buscarPorTermino(@Param("term") String term);



}
