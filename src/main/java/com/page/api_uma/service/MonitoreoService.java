package com.page.api_uma.service;

import com.page.api_uma.model.Monitoreo;
import com.page.api_uma.repository.MonitoreoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MonitoreoService {

    private final MonitoreoRepository monitoreoRepository;

    public MonitoreoService(MonitoreoRepository monitoreoRepository) {
        this.monitoreoRepository = monitoreoRepository;
    }

    public List<Monitoreo> findAll() {
        return monitoreoRepository.findAll();
    }

    public Monitoreo findById(Integer id) {
        return monitoreoRepository.findById(id).orElse(null);
    }

    public Monitoreo save(Monitoreo monitoreo) {
        return monitoreoRepository.save(monitoreo);
    }

    public void deleteById(Integer id) {
        monitoreoRepository.deleteById(id);
    }

}