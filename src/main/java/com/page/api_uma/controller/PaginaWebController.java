package com.page.api_uma.controller;

import com.page.api_uma.model.PaginaWeb;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.service.PaginaWebService;
import com.page.api_uma.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/paginas")
public class PaginaWebController {

    private final PaginaWebService paginaService;
    private final UsuarioService usuarioService;

    public PaginaWebController(PaginaWebService service, UsuarioService usuarioService) {
        this.paginaService = service;
        this.usuarioService = usuarioService;
    }

    /**
     * LISTADO GLOBAL
     * Útil para que un ADMIN gestione la base de datos de páginas
     * o para funciones de búsqueda/autocompletado.
     */
    @GetMapping
    public ResponseEntity<List<PaginaWeb>> findAll() {
        return ResponseEntity.ok(paginaService.findAll());
    }

    /**
     * DETALLE DE PÁGINA
     * Obtiene la información técnica de una página por su ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaginaWeb> findById(@PathVariable Integer id) {
        PaginaWeb pagina = paginaService.findById(id);
        return pagina != null ? ResponseEntity.ok(pagina) : ResponseEntity.notFound().build();
    }

    /**
     * COMPROBACIÓN DE ESTADO (Health Check)
     * Lógica de red para verificar si la URL responde, sin necesidad de ser dueño.
     */
    @GetMapping("/{id}/check-status")
    public ResponseEntity<Integer> checkHealth(@PathVariable int id) {
        PaginaWeb pagina = paginaService.findById(id);
        if (pagina == null) return ResponseEntity.notFound().build();

        int statusCode = paginaService.getRemoteStatus(pagina.getUrl());
        return ResponseEntity.ok(statusCode);
    }

    /**
     * CREAR PÁGINA
     * Aunque se suelen crear vía Monitoreo, este endpoint permite registrar
     * páginas en la biblioteca (ej. para plantillas).
     */
    @PostMapping
    public ResponseEntity<PaginaWeb> create(@RequestBody PaginaWeb pagina) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paginaService.save(pagina));
    }

    /**
     * ACTUALIZAR PÁGINA
     * Permite editar el nombre o la nota informativa de una URL globalmente.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PaginaWeb> update(@PathVariable Integer id, @RequestBody PaginaWeb detalle) {
        PaginaWeb existe = paginaService.findById(id);
        if (existe != null) {
            detalle.setId(id);
            return ResponseEntity.ok(paginaService.save(detalle));
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * ELIMINAR PÁGINA
     * Elimina el registro de la biblioteca. Precaución con la integridad referencial.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        paginaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<PaginaWeb>> buscar(@RequestParam String q) {
        return ResponseEntity.ok(paginaService.buscarPaginas(q));
    }

    /**
     * HELPER: Obtiene el usuario autenticado desde el contexto de seguridad.
     * Se usa para identificar quién hace la petición basándose en su Token/Sesión.
     */
    private Usuario getUsuarioAutenticado() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioService.findByEmail(email);
    }
}