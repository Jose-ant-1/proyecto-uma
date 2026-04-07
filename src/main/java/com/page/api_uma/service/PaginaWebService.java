package com.page.api_uma.service;

import com.page.api_uma.dto.PaginaWebDTO;
import com.page.api_uma.mapper.PaginaWebMapper;
import com.page.api_uma.model.PaginaWeb;
import com.page.api_uma.repository.PaginaWebRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class PaginaWebService {

    private final PaginaWebRepository paginaWebRepository;
    private final PaginaWebMapper paginaWebMapper;

    public PaginaWebService(PaginaWebRepository paginaWebRepository, PaginaWebMapper paginaWebMapper) {
        this.paginaWebRepository = paginaWebRepository;
        this.paginaWebMapper = paginaWebMapper;
    }

    public List<PaginaWeb> findAll() {
        return paginaWebRepository.findAllByOrderByNombreAsc();
    }

    public PaginaWeb findById(Integer id) {
        return paginaWebRepository.findById(id).orElse(null);
    }

    public PaginaWeb save(PaginaWebDTO paginaWeb) {
        PaginaWeb entidad = paginaWebMapper.toEntity(paginaWeb);
        return paginaWebRepository.save(entidad);
    }

    public void deleteById(Integer id) {
        paginaWebRepository.deleteById(id);
    }

    public int getRemoteStatus(String urlString) {
        try {
            if (!urlString.startsWith("http")) urlString = "https://" + urlString;

            java.net.URL url = new java.net.URL(urlString);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();

            // Disfraz de usuario normal
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            return connection.getResponseCode();
        } catch (java.net.UnknownHostException e) {
            return 404;
        } catch (Exception e) {
            return 500;
        }
    }

    public List<PaginaWeb> buscarPaginas(String termino) {
        if (termino == null || termino.isEmpty()) {
            return Collections.emptyList();
        }
        return paginaWebRepository.buscarPorTermino(termino);
    }

}
