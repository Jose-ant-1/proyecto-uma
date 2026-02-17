package com.page.api_uma.repository;

import com.page.api_uma.model.Invitacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvitacionRepository extends JpaRepository<Invitacion, Integer> {
}
