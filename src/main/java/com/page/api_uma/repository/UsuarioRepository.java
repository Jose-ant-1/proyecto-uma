package com.page.api_uma.repository;

import com.page.api_uma.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Usuario findByEmail(String email);

    @Query("SELECT u FROM Usuario u WHERE u.nombre LIKE %:term% OR u.email LIKE %:term%")
    List<Usuario> buscarPorTermino(@Param("term") String term);
}
