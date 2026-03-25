package com.page.api_uma.repository;

import com.page.api_uma.model.Monitoreo;
import com.page.api_uma.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonitoreoRepository extends JpaRepository<Monitoreo, Integer> {

    @Modifying
    @Query(value = "DELETE FROM monitoreo_plantilla_mon WHERE id_monitoreo = :id", nativeQuery = true)
    void eliminarRelacionesConPlantillas(@Param("id") int id);

    List<Monitoreo> findAllByPropietarioOrderByNombreAsc(Usuario propietario);

}