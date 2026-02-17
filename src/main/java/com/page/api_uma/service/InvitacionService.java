package com.page.api_uma.service;

import com.page.api_uma.model.Invitacion;
import com.page.api_uma.repository.InvitacionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InvitacionService {

    private final InvitacionRepository invitacionRepository;

    public InvitacionService(InvitacionRepository invitacionRepository) {
        this.invitacionRepository = invitacionRepository;
    }

    public List<Invitacion> findAll() {
        return invitacionRepository.findAll();
    }

    public Invitacion findById(Integer id) {
        return invitacionRepository.findById(id).orElse(null);
    }

    public Invitacion save(Invitacion invitacion) {
        return invitacionRepository.save(invitacion);
    }

    public void deleteById(Integer id) {
        invitacionRepository.deleteById(id);
    }

}
