package com.page.api_uma.service;

import com.page.api_uma.dto.PlantillaMonitoreoDTO;
import com.page.api_uma.mapper.PlantillaMonitoreoMapper;
import com.page.api_uma.model.Monitoreo;
import com.page.api_uma.model.PlantillaMonitoreo;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.repository.MonitoreoRepository;
import com.page.api_uma.repository.PlantillaMonitoreoRepository;
import com.page.api_uma.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlantMonitoreoServiceTest {

    @Mock
    private PlantillaMonitoreoRepository plantillaMonitoreoRepository;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private MonitoreoRepository monitoreoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PlantillaMonitoreoMapper mapper;

    @InjectMocks
    private PlantillaMonitoreoService plantillaMonitoreoService;

    private PlantillaMonitoreo plantillaEjemplo;
    private Usuario usuarioEjemplo;

    @BeforeEach
    void setUp() {
        usuarioEjemplo = new Usuario();
        usuarioEjemplo.setId(10);
        usuarioEjemplo.setEmail("propietario@test.com");

        plantillaEjemplo = new PlantillaMonitoreo();
        plantillaEjemplo.setId(1);
        plantillaEjemplo.setNombre("Plantilla Servidores");
        plantillaEjemplo.setPropietario(usuarioEjemplo);
        plantillaEjemplo.setMonitoreos(new HashSet<>());
    }

    @Test
    @DisplayName("findAll: Debería retornar todas las plantillas")
    void findAll_DeberiaRetornarListaCompleta() {

        when(plantillaMonitoreoRepository.findAll()).thenReturn(List.of(plantillaEjemplo));

        List<PlantillaMonitoreo> resultado = plantillaMonitoreoService.findAll();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(plantillaMonitoreoRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("findById: Debería retornar la plantilla si existe")
    void findById_Existente_DeberiaRetornarPlantilla() {

        when(plantillaMonitoreoRepository.findById(1)).thenReturn(Optional.of(plantillaEjemplo));

        PlantillaMonitoreo resultado = plantillaMonitoreoService.findById(1);

        assertNotNull(resultado);
        assertEquals("Plantilla Servidores", resultado.getNombre());
        verify(plantillaMonitoreoRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("findById: Debería retornar null si no existe")
    void findById_NoExistente_DeberiaRetornarNull() {

        when(plantillaMonitoreoRepository.findById(99)).thenReturn(Optional.empty());

        PlantillaMonitoreo resultado = plantillaMonitoreoService.findById(99);

        assertNull(resultado);
    }

    @Test
    @DisplayName("save: Debería asignar el usuario autenticado y guardar la plantilla")
    void save_DeberiaAsignarPropietarioYGuardar() {

        PlantillaMonitoreoDTO dto = new PlantillaMonitoreoDTO();
        dto.setNombre("Nueva Plantilla");

        Monitoreo m1 = new Monitoreo();
        m1.setId(50);
        dto.setMonitoreos(new HashSet<>(Set.of(m1)));

        //Configuramos comportamiento
        when(mapper.toEntity(dto)).thenReturn(plantillaEjemplo);
        when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioEjemplo);
        when(plantillaMonitoreoRepository.save(any(PlantillaMonitoreo.class))).thenReturn(plantillaEjemplo);
        when(mapper.toDTO(plantillaEjemplo)).thenReturn(dto);

        PlantillaMonitoreoDTO resultado = plantillaMonitoreoService.save(dto);

        assertNotNull(resultado);
        // Verificamos que se llamó al servicio de usuario para establecer el dueño
        verify(usuarioService, times(1)).getUsuarioAutenticado();
        // Verificamos que la entidad recibió el propietario antes de ser guardada
        verify(plantillaMonitoreoRepository).save(argThat(p ->
                p.getPropietario().equals(usuarioEjemplo)
        ));
    }

    @Test
    @DisplayName("deleteById: Debería llamar al repositorio para eliminar")
    void deleteById_DeberiaLlamarAlRepositorio() {

        plantillaMonitoreoService.deleteById(1);

        verify(plantillaMonitoreoRepository, times(1)).deleteById(1);
    }

    @Test
    @DisplayName("findByUsuario: Debería filtrar plantillas donde el usuario es dueño de todos los monitoreos")
    void findByUsuario_DeberiaFiltrarPorPropietarioTotal() {

        int usuarioId = 10;

        PlantillaMonitoreo pA = new PlantillaMonitoreo();
        pA.setId(100);
        Monitoreo mA = new Monitoreo();
        mA.setId(500);
        mA.setPropietario(usuarioEjemplo);
        pA.setMonitoreos(new HashSet<>(Set.of(mA)));

        PlantillaMonitoreo pB = new PlantillaMonitoreo();
        pB.setId(200);

        Monitoreo mB1 = new Monitoreo();
        mB1.setId(501);
        mB1.setPropietario(usuarioEjemplo);

        Monitoreo mB2 = new Monitoreo();
        mB2.setId(502);
        Usuario otro = new Usuario();
        otro.setId(99);
        mB2.setPropietario(otro);

        // Set.of no fallará porque mB1 y mB2 tienen IDs distintos
        pB.setMonitoreos(new HashSet<>(Set.of(mB1, mB2)));

        when(plantillaMonitoreoRepository.findAllRelatedToUsuario(usuarioId))
                .thenReturn(List.of(pA, pB));

        List<PlantillaMonitoreo> resultado = plantillaMonitoreoService.findByUsuario(usuarioId);

        assertEquals(1, resultado.size());
        assertEquals(pA, resultado.getFirst());
        verify(plantillaMonitoreoRepository).findAllRelatedToUsuario(usuarioId);
    }

    @Test
    @DisplayName("findByPropietario: Debería retornar plantillas por email del dueño")
    void findByPropietario_DeberiaRetornarListaSiExisteUsuario() {

        String email = "propietario@test.com";
        when(usuarioRepository.findByEmail(email)).thenReturn(usuarioEjemplo);
        when(plantillaMonitoreoRepository.findByPropietarioOrderByNombreAsc(usuarioEjemplo))
                .thenReturn(List.of(plantillaEjemplo));

        List<PlantillaMonitoreo> resultado = plantillaMonitoreoService.findByPropietario(email);

        assertNotNull(resultado);
        assertFalse(resultado.isEmpty());
        verify(usuarioRepository).findByEmail(email);
        verify(plantillaMonitoreoRepository).findByPropietarioOrderByNombreAsc(usuarioEjemplo);
    }

    @Test
    @DisplayName("findByPropietario: Debería lanzar excepción si el usuario no existe")
    void findByPropietario_NoExistente_DeberiaLanzarExcepcion() {

        String email = "noexiste@test.com";
        when(usuarioRepository.findByEmail(email)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> {
            plantillaMonitoreoService.findByPropietario(email);
        });
    }

    @Test
    @DisplayName("aplicarPlantilla: Debería añadir invitado solo a los monitoreos que pertenecen al propietario")
    void aplicarPlantilla_DeberiaProcesarSoloMonitoreosDelPropietario() {

        int plantillaId = 1;
        String emailInvitado = "invitado@test.com";
        int propietarioId = 10;

        Usuario invitado = new Usuario();
        invitado.setId(20);
        invitado.setEmail(emailInvitado);

        Monitoreo m1 = new Monitoreo();
        m1.setId(101);
        m1.setPropietario(usuarioEjemplo);
        m1.setInvitados(new HashSet<>());

        Monitoreo m2 = new Monitoreo();
        m2.setId(102);
        Usuario otroPropietario = new Usuario();
        otroPropietario.setId(99);
        m2.setPropietario(otroPropietario);
        m2.setInvitados(new HashSet<>());

        plantillaEjemplo.setMonitoreos(new HashSet<>(Set.of(m1, m2)));

        when(plantillaMonitoreoRepository.findById(plantillaId)).thenReturn(Optional.of(plantillaEjemplo));
        when(usuarioService.buscarPorEmail(emailInvitado)).thenReturn(invitado);
        when(monitoreoRepository.save(any(Monitoreo.class))).thenAnswer(i -> i.getArgument(0));

        plantillaMonitoreoService.aplicarPlantillaAUsuario(plantillaId, emailInvitado, propietarioId);

        // Verificamos que el invitado se añadió al monitoreo del propietario (m1)
        assertTrue(m1.getInvitados().contains(invitado));
        // Verificamos que NO se añadió al monitoreo del otro (m2)
        assertFalse(m2.getInvitados().contains(invitado));

        verify(monitoreoRepository, times(1)).save(m1);
        verify(monitoreoRepository, never()).save(m2);
    }

    @Test
    @DisplayName("aplicarPlantilla: Debería lanzar excepción si la plantilla no existe")
    void aplicarPlantilla_PlantillaNoExiste_DeberiaLanzarExcepcion() {

        when(plantillaMonitoreoRepository.findById(anyInt())).thenReturn(Optional.empty());

        assertThrows(com.page.api_uma.exception.ResourceNotFoundException.class, () -> {
            plantillaMonitoreoService.aplicarPlantillaAUsuario(1, "test@test.com", 10);
        });
    }

    @Test
    @DisplayName("save: Debería ignorar el propietario del DTO y usar siempre el usuario autenticado")
    void save_DeberiaGarantizarUsuarioAutenticadoComoPropietario() {

        Usuario usuarioMalintencionado = new Usuario();
        usuarioMalintencionado.setId(666);

        PlantillaMonitoreoDTO dto = new PlantillaMonitoreoDTO();
        dto.setNombre("Plantilla Hacker");

        PlantillaMonitoreo entidadMapeada = new PlantillaMonitoreo();
        entidadMapeada.setPropietario(usuarioMalintencionado);

        when(mapper.toEntity(dto)).thenReturn(entidadMapeada);
        when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioEjemplo);
        when(plantillaMonitoreoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toDTO(any())).thenReturn(dto);

        plantillaMonitoreoService.save(dto);

        verify(plantillaMonitoreoRepository).save(argThat(p ->
                p.getPropietario().getId() == 10
        ));
    }

    @Test
    @DisplayName("aplicarPlantilla: Debería lanzar excepción si el invitado no existe")
    void aplicarPlantilla_InvitadoNoExiste_DeberiaLanzarExcepcion() {

        int plantillaId = 1;
        String emailInvalido = "no-existe@test.com";

        when(plantillaMonitoreoRepository.findById(plantillaId)).thenReturn(Optional.of(plantillaEjemplo));
        when(usuarioService.buscarPorEmail(emailInvalido)).thenReturn(null);

        assertThrows(com.page.api_uma.exception.ResourceNotFoundException.class, () -> {
            plantillaMonitoreoService.aplicarPlantillaAUsuario(plantillaId, emailInvalido, 10);
        });
    }

    @Test
    @DisplayName("save: Debería garantizar la asignación del usuario autenticado y manejar monitoreos nulos")
    void save_DeberiaGarantizarPropietarioYProcesarNulos() {

        Usuario usuarioMalintencionado = new Usuario();
        usuarioMalintencionado.setId(666);

        PlantillaMonitoreoDTO dto = new PlantillaMonitoreoDTO();
        dto.setNombre("Plantilla Test");
        dto.setMonitoreos(null);

        // El mapper podría devolver el usuario erróneo
        PlantillaMonitoreo entidadMapeada = new PlantillaMonitoreo();
        entidadMapeada.setPropietario(usuarioMalintencionado);

        when(mapper.toEntity(dto)).thenReturn(entidadMapeada);
        when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioEjemplo); // ID 10
        when(plantillaMonitoreoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toDTO(any())).thenReturn(dto);

        assertDoesNotThrow(() -> plantillaMonitoreoService.save(dto));

        // Verificamos que se guardó con el usuario real y persistió el estado de monitoreos
        verify(plantillaMonitoreoRepository).save(argThat(p ->
                p.getPropietario().getId() == 10 &&
                        (p.getMonitoreos() == null || p.getMonitoreos().isEmpty())
        ));
    }

    @Test
    @DisplayName("findByUsuario: Debería incluir plantillas sin monitoreos (propiedad total vacía)")
    void findByUsuario_PlantillaSinMonitoreos_DeberiaIncluirla() {

        int usuarioId = 10;
        PlantillaMonitoreo pVacia = new PlantillaMonitoreo();
        pVacia.setMonitoreos(new HashSet<>());

        when(plantillaMonitoreoRepository.findAllRelatedToUsuario(usuarioId))
                .thenReturn(List.of(pVacia));

        List<PlantillaMonitoreo> resultado = plantillaMonitoreoService.findByUsuario(usuarioId);

        assertEquals(1, resultado.size());
        assertTrue(resultado.contains(pVacia));
    }

    @Test
    @DisplayName("aplicarPlantilla: Debería inicializar invitados si vienen como null y guardar")
    void aplicarPlantilla_InvitadosNull_DeberiaManejarloYGuardar() {

        int plantillaId = 1;
        String emailInvitado = "invitado@test.com";
        int propietarioId = 10;

        Monitoreo m1 = new Monitoreo();
        m1.setId(101);
        m1.setPropietario(usuarioEjemplo);
        m1.setInvitados(null);

        plantillaEjemplo.setMonitoreos(new HashSet<>(Set.of(m1)));
        Usuario invitado = new Usuario();

        when(plantillaMonitoreoRepository.findById(plantillaId)).thenReturn(Optional.of(plantillaEjemplo));
        when(usuarioService.buscarPorEmail(emailInvitado)).thenReturn(invitado);

        assertDoesNotThrow(() -> {
            plantillaMonitoreoService.aplicarPlantillaAUsuario(plantillaId, emailInvitado, propietarioId);
        });

        assertNotNull(m1.getInvitados(), "La colección debería haberse inicializado en el service");
        assertTrue(m1.getInvitados().contains(invitado), "El invitado debería estar en la lista");
        verify(monitoreoRepository).save(m1);
    }

    @Test
    @DisplayName("findByUsuario: Debería ignorar plantillas con datos corruptos sin lanzar error")
    void findByUsuario_MonitoreoSinPropietario_DeberiaFiltrarYNoFallar() {

        int usuarioId = 10;
        PlantillaMonitoreo pA = new PlantillaMonitoreo();

        Monitoreo mCorrupto = new Monitoreo();
        mCorrupto.setPropietario(null);

        pA.setMonitoreos(new HashSet<>(Set.of(mCorrupto)));

        when(plantillaMonitoreoRepository.findAllRelatedToUsuario(usuarioId))
                .thenReturn(List.of(pA));

        List<PlantillaMonitoreo> resultado = plantillaMonitoreoService.findByUsuario(usuarioId);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty(), "La plantilla con monitoreo sin dueño no debería incluirse");

    }

    @Test
    @DisplayName("save: Debería persistir correctamente cuando el DTO no trae monitoreos")
    void save_SinMonitoreosEnDto_DeberiaGuardarCorrectamente() {
        PlantillaMonitoreoDTO dto = new PlantillaMonitoreoDTO();
        dto.setMonitoreos(null);

        when(mapper.toEntity(dto)).thenReturn(new PlantillaMonitoreo());
        when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioEjemplo);
        when(plantillaMonitoreoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> plantillaMonitoreoService.save(dto));
    }

    @Test
    @DisplayName("findByUsuario: Debería ignorar plantillas con datos corruptos (monitoreo sin propietario)")
    void findByUsuario_MonitoreoSinPropietario_DeberiaFiltrarSinError() {

        int usuarioId = 10;
        PlantillaMonitoreo pA = new PlantillaMonitoreo();
        pA.setId(100);

        Monitoreo mIncompleto = new Monitoreo();
        mIncompleto.setPropietario(null);
        pA.setMonitoreos(new HashSet<>(Set.of(mIncompleto)));

        when(plantillaMonitoreoRepository.findAllRelatedToUsuario(usuarioId))
                .thenReturn(List.of(pA));

        List<PlantillaMonitoreo> resultado = plantillaMonitoreoService.findByUsuario(usuarioId);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty(), "La plantilla debería ser filtrada por no cumplir la propiedad total");
    }

    @Test
    @DisplayName("aplicarPlantilla: No debería guardar nada si ningún monitoreo pertenece al solicitante")
    void aplicarPlantilla_SinMonitoreosPropios_NoDeberiaLlamarAlRepo() {

        int propietarioId = 10;
        Usuario otroDuenio = new Usuario();
        otroDuenio.setId(99);

        Monitoreo m1 = new Monitoreo();
        m1.setPropietario(otroDuenio);

        plantillaEjemplo.setMonitoreos(new HashSet<>(Set.of(m1)));

        when(plantillaMonitoreoRepository.findById(1)).thenReturn(Optional.of(plantillaEjemplo));
        when(usuarioService.buscarPorEmail(anyString())).thenReturn(new Usuario());

        plantillaMonitoreoService.aplicarPlantillaAUsuario(1, "test@test.com", propietarioId);

        verify(monitoreoRepository, never()).save(any());
    }

    @Test
    @DisplayName("findByUsuario: Debería retornar lista vacía si el repo no encuentra nada")
    void findByUsuario_SinResultados_DeberiaRetornarListaVacia() {

        when(plantillaMonitoreoRepository.findAllRelatedToUsuario(anyInt())).thenReturn(Collections.emptyList());

        List<PlantillaMonitoreo> resultado = plantillaMonitoreoService.findByUsuario(10);

        assertTrue(resultado.isEmpty());
        verify(plantillaMonitoreoRepository).findAllRelatedToUsuario(10);
    }

}