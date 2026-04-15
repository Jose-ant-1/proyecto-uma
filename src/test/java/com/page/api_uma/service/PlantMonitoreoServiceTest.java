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
        // Arrange
        when(plantillaMonitoreoRepository.findAll()).thenReturn(List.of(plantillaEjemplo));

        // Act
        List<PlantillaMonitoreo> resultado = plantillaMonitoreoService.findAll();

        // Assert
        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(plantillaMonitoreoRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("findById: Debería retornar la plantilla si existe")
    void findById_Existente_DeberiaRetornarPlantilla() {
        // Arrange
        when(plantillaMonitoreoRepository.findById(1)).thenReturn(Optional.of(plantillaEjemplo));

        // Act
        PlantillaMonitoreo resultado = plantillaMonitoreoService.findById(1);

        // Assert
        assertNotNull(resultado);
        assertEquals("Plantilla Servidores", resultado.getNombre());
        verify(plantillaMonitoreoRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("findById: Debería retornar null si no existe")
    void findById_NoExistente_DeberiaRetornarNull() {
        // Arrange
        when(plantillaMonitoreoRepository.findById(99)).thenReturn(Optional.empty());

        // Act
        PlantillaMonitoreo resultado = plantillaMonitoreoService.findById(99);

        // Assert
        assertNull(resultado);
    }

    @Test
    @DisplayName("save: Debería asignar el usuario autenticado y guardar la plantilla")
    void save_DeberiaAsignarPropietarioYGuardar() {
        // Arrange
        PlantillaMonitoreoDTO dto = new PlantillaMonitoreoDTO();
        dto.setNombre("Nueva Plantilla");
        // Simulamos que el DTO trae algunos monitoreos
        Monitoreo m1 = new Monitoreo();
        m1.setId(50);
        dto.setMonitoreos(new HashSet<>(Set.of(m1)));

        // Configuramos el comportamiento de los mocks
        when(mapper.toEntity(dto)).thenReturn(plantillaEjemplo);
        when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioEjemplo);
        when(plantillaMonitoreoRepository.save(any(PlantillaMonitoreo.class))).thenReturn(plantillaEjemplo);
        when(mapper.toDTO(plantillaEjemplo)).thenReturn(dto);

        // Act
        PlantillaMonitoreoDTO resultado = plantillaMonitoreoService.save(dto);

        // Assert
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
        // Act
        plantillaMonitoreoService.deleteById(1);

        // Assert
        verify(plantillaMonitoreoRepository, times(1)).deleteById(1);
    }

    @Test
    @DisplayName("findByUsuario: Debería filtrar plantillas donde el usuario es dueño de todos los monitoreos")
    void findByUsuario_DeberiaFiltrarPorPropietarioTotal() {
        // Arrange
        int usuarioId = 10;

        // Plantilla A: El usuario es dueño de to do
        PlantillaMonitoreo pA = new PlantillaMonitoreo();
        pA.setId(100);
        Monitoreo mA = new Monitoreo();
        mA.setId(500); // <--- ID único
        mA.setPropietario(usuarioEjemplo);
        pA.setMonitoreos(new HashSet<>(Set.of(mA)));

        // Plantilla B: El usuario NO es dueño de uno de los monitoreos
        PlantillaMonitoreo pB = new PlantillaMonitoreo();
        pB.setId(200);

        Monitoreo mB1 = new Monitoreo();
        mB1.setId(501); // <--- ID único
        mB1.setPropietario(usuarioEjemplo);

        Monitoreo mB2 = new Monitoreo();
        mB2.setId(502); // <--- ID único diferente a mB1
        Usuario otro = new Usuario();
        otro.setId(99);
        mB2.setPropietario(otro);

        // Ahora Set.of ya no fallará porque mB1 y mB2 tienen IDs distintos
        pB.setMonitoreos(new HashSet<>(Set.of(mB1, mB2)));

        when(plantillaMonitoreoRepository.findAllRelatedToUsuario(usuarioId))
                .thenReturn(List.of(pA, pB));

        // Act
        List<PlantillaMonitoreo> resultado = plantillaMonitoreoService.findByUsuario(usuarioId);

        // Assert
        assertEquals(1, resultado.size());
        assertEquals(pA, resultado.getFirst());
        verify(plantillaMonitoreoRepository).findAllRelatedToUsuario(usuarioId);
    }

    @Test
    @DisplayName("findByPropietario: Debería retornar plantillas por email del dueño")
    void findByPropietario_DeberiaRetornarListaSiExisteUsuario() {
        // Arrange
        String email = "propietario@test.com";
        when(usuarioRepository.findByEmail(email)).thenReturn(usuarioEjemplo);
        when(plantillaMonitoreoRepository.findByPropietarioOrderByNombreAsc(usuarioEjemplo))
                .thenReturn(List.of(plantillaEjemplo));

        // Act
        List<PlantillaMonitoreo> resultado = plantillaMonitoreoService.findByPropietario(email);

        // Assert
        assertNotNull(resultado);
        assertFalse(resultado.isEmpty());
        verify(usuarioRepository).findByEmail(email);
        verify(plantillaMonitoreoRepository).findByPropietarioOrderByNombreAsc(usuarioEjemplo);
    }

    @Test
    @DisplayName("findByPropietario: Debería lanzar excepción si el usuario no existe")
    void findByPropietario_NoExistente_DeberiaLanzarExcepcion() {
        // Arrange
        String email = "noexiste@test.com";
        when(usuarioRepository.findByEmail(email)).thenReturn(null);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            plantillaMonitoreoService.findByPropietario(email);
        });
    }

    @Test
    @DisplayName("aplicarPlantilla: Debería añadir invitado solo a los monitoreos que pertenecen al propietario")
    void aplicarPlantilla_DeberiaProcesarSoloMonitoreosDelPropietario() {
        // Arrange
        int plantillaId = 1;
        String emailInvitado = "invitado@test.com";
        int propietarioId = 10;

        // 1. Preparamos el invitado
        Usuario invitado = new Usuario();
        invitado.setId(20);
        invitado.setEmail(emailInvitado);

        // 2. Preparamos monitoreos de la plantilla con IDs distintos para evitar errores de Set
        Monitoreo m1 = new Monitoreo();
        m1.setId(101);
        m1.setPropietario(usuarioEjemplo); // usuarioEjemplo tiene ID 10
        m1.setInvitados(new HashSet<>());

        Monitoreo m2 = new Monitoreo();
        m2.setId(102);
        Usuario otroPropietario = new Usuario();
        otroPropietario.setId(99);
        m2.setPropietario(otroPropietario); // Este no debería procesarse
        m2.setInvitados(new HashSet<>());

        plantillaEjemplo.setMonitoreos(new HashSet<>(Set.of(m1, m2)));

        // 3. Configuración de Mocks
        when(plantillaMonitoreoRepository.findById(plantillaId)).thenReturn(Optional.of(plantillaEjemplo));
        when(usuarioService.buscarPorEmail(emailInvitado)).thenReturn(invitado);
        when(monitoreoRepository.save(any(Monitoreo.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        plantillaMonitoreoService.aplicarPlantillaAUsuario(plantillaId, emailInvitado, propietarioId);

        // Assert
        // Verificamos que el invitado se añadió al monitoreo del propietario (m1)
        assertTrue(m1.getInvitados().contains(invitado));
        // Verificamos que NO se añadió al monitoreo del otro (m2)
        assertFalse(m2.getInvitados().contains(invitado));

        // Verificamos que se llamó al save del repo solo 1 vez (para m1)
        verify(monitoreoRepository, times(1)).save(m1);
        verify(monitoreoRepository, never()).save(m2);
    }

    @Test
    @DisplayName("aplicarPlantilla: Debería lanzar excepción si la plantilla no existe")
    void aplicarPlantilla_PlantillaNoExiste_DeberiaLanzarExcepcion() {
        // Arrange
        when(plantillaMonitoreoRepository.findById(anyInt())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(com.page.api_uma.exception.ResourceNotFoundException.class, () -> {
            plantillaMonitoreoService.aplicarPlantillaAUsuario(1, "test@test.com", 10);
        });
    }

    @Test
    @DisplayName("save: Debería ignorar el propietario del DTO y usar siempre el usuario autenticado")
    void save_DeberiaGarantizarUsuarioAutenticadoComoPropietario() {
        // Arrange
        Usuario usuarioMalintencionado = new Usuario();
        usuarioMalintencionado.setId(666);

        PlantillaMonitoreoDTO dto = new PlantillaMonitoreoDTO();
        dto.setNombre("Plantilla Hacker");

        // El mapper podría devolver una entidad con el usuario del DTO
        PlantillaMonitoreo entidadMapeada = new PlantillaMonitoreo();
        entidadMapeada.setPropietario(usuarioMalintencionado);

        when(mapper.toEntity(dto)).thenReturn(entidadMapeada);
        when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioEjemplo); // ID 10
        when(plantillaMonitoreoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toDTO(any())).thenReturn(dto);

        // Act
        plantillaMonitoreoService.save(dto);

        // Assert
        // Verificamos que al final se guardó con usuarioEjemplo (ID 10) y NO con el 666
        verify(plantillaMonitoreoRepository).save(argThat(p ->
                p.getPropietario().getId() == 10
        ));
    }

    @Test
    @DisplayName("aplicarPlantilla: Debería lanzar excepción si el invitado no existe")
    void aplicarPlantilla_InvitadoNoExiste_DeberiaLanzarExcepcion() {
        // Arrange
        int plantillaId = 1;
        String emailInvalido = "no-existe@test.com";

        when(plantillaMonitoreoRepository.findById(plantillaId)).thenReturn(Optional.of(plantillaEjemplo));
        when(usuarioService.buscarPorEmail(emailInvalido)).thenReturn(null);

        // Act & Assert
        assertThrows(com.page.api_uma.exception.ResourceNotFoundException.class, () -> {
            plantillaMonitoreoService.aplicarPlantillaAUsuario(plantillaId, emailInvalido, 10);
        });
    }

    @Test
    @DisplayName("save: Debería garantizar la asignación del usuario autenticado y manejar monitoreos nulos")
    void save_DeberiaGarantizarPropietarioYProcesarNulos() {
        // Arrange
        Usuario usuarioMalintencionado = new Usuario();
        usuarioMalintencionado.setId(666);

        PlantillaMonitoreoDTO dto = new PlantillaMonitoreoDTO();
        dto.setNombre("Plantilla Test");
        dto.setMonitoreos(null); // Caso crítico de nulos

        // El mapper podría devolver el usuario erróneo del DTO
        PlantillaMonitoreo entidadMapeada = new PlantillaMonitoreo();
        entidadMapeada.setPropietario(usuarioMalintencionado);

        when(mapper.toEntity(dto)).thenReturn(entidadMapeada);
        when(usuarioService.getUsuarioAutenticado()).thenReturn(usuarioEjemplo); // ID 10
        when(plantillaMonitoreoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toDTO(any())).thenReturn(dto);

        // Act & Assert
        assertDoesNotThrow(() -> plantillaMonitoreoService.save(dto));

        // Verificamos que se guardó con el usuario real (ID 10) y persistió el estado de monitoreos
        verify(plantillaMonitoreoRepository).save(argThat(p ->
                p.getPropietario().getId() == 10 &&
                        (p.getMonitoreos() == null || p.getMonitoreos().isEmpty())
        ));
    }

    @Test
    @DisplayName("findByUsuario: Debería incluir plantillas sin monitoreos (propiedad total vacía)")
    void findByUsuario_PlantillaSinMonitoreos_DeberiaIncluirla() {
        // Arrange
        int usuarioId = 10;
        PlantillaMonitoreo pVacia = new PlantillaMonitoreo();
        pVacia.setMonitoreos(new HashSet<>()); // Vacía

        when(plantillaMonitoreoRepository.findAllRelatedToUsuario(usuarioId))
                .thenReturn(List.of(pVacia));

        // Act
        List<PlantillaMonitoreo> resultado = plantillaMonitoreoService.findByUsuario(usuarioId);

        // Assert
        assertEquals(1, resultado.size());
        assertTrue(resultado.contains(pVacia));
    }

    @Test
    @DisplayName("aplicarPlantilla: Debería inicializar invitados si vienen como null y guardar")
    void aplicarPlantilla_InvitadosNull_DeberiaManejarloYGuardar() {
        // Arrange
        int plantillaId = 1;
        String emailInvitado = "invitado@test.com";
        int propietarioId = 10;

        Monitoreo m1 = new Monitoreo();
        m1.setId(101);
        m1.setPropietario(usuarioEjemplo); // ID 10
        m1.setInvitados(null); // Simulamos el dato corrupto/nulo

        plantillaEjemplo.setMonitoreos(new HashSet<>(Set.of(m1)));
        Usuario invitado = new Usuario();

        when(plantillaMonitoreoRepository.findById(plantillaId)).thenReturn(Optional.of(plantillaEjemplo));
        when(usuarioService.buscarPorEmail(emailInvitado)).thenReturn(invitado);

        // Act
        // Cambiamos assertThrows por assertDoesNotThrow o simplemente ejecutamos el método
        assertDoesNotThrow(() -> {
            plantillaMonitoreoService.aplicarPlantillaAUsuario(plantillaId, emailInvitado, propietarioId);
        });

        // Assert
        assertNotNull(m1.getInvitados(), "La colección debería haberse inicializado en el service");
        assertTrue(m1.getInvitados().contains(invitado), "El invitado debería estar en la lista");
        verify(monitoreoRepository).save(m1);
    }

    @Test
    @DisplayName("findByUsuario: Debería ignorar plantillas con datos corruptos sin lanzar error")
    void findByUsuario_MonitoreoSinPropietario_DeberiaFiltrarYNoFallar() {
        // Arrange
        int usuarioId = 10;
        PlantillaMonitoreo pA = new PlantillaMonitoreo();

        Monitoreo mCorrupto = new Monitoreo();
        mCorrupto.setPropietario(null); // Simulamos error en DB

        pA.setMonitoreos(new HashSet<>(Set.of(mCorrupto)));

        when(plantillaMonitoreoRepository.findAllRelatedToUsuario(usuarioId))
                .thenReturn(List.of(pA));

        // Act
        // Ya no usamos assertThrows, ejecutamos el método normalmente
        List<PlantillaMonitoreo> resultado = plantillaMonitoreoService.findByUsuario(usuarioId);

        // Assert
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty(), "La plantilla con monitoreo sin dueño no debería incluirse");
        // El test pasará si no hay excepciones y el resultado es correcto
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
        // Arrange
        int usuarioId = 10;
        PlantillaMonitoreo pA = new PlantillaMonitoreo();
        pA.setId(100);

        Monitoreo mIncompleto = new Monitoreo();
        mIncompleto.setPropietario(null); // Dato corrupto
        pA.setMonitoreos(new HashSet<>(Set.of(mIncompleto)));

        when(plantillaMonitoreoRepository.findAllRelatedToUsuario(usuarioId))
                .thenReturn(List.of(pA));

        // Act
        // Ya no usamos assertThrows; el método debe ejecutarse normalmente
        List<PlantillaMonitoreo> resultado = plantillaMonitoreoService.findByUsuario(usuarioId);

        // Assert
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty(), "La plantilla debería ser filtrada por no cumplir la propiedad total");
        // Verificamos que no hubo excepciones y el flujo terminó bien
    }

    @Test
    @DisplayName("aplicarPlantilla: No debería guardar nada si ningún monitoreo pertenece al solicitante")
    void aplicarPlantilla_SinMonitoreosPropios_NoDeberiaLlamarAlRepo() {
        // Arrange
        int propietarioId = 10;
        Usuario otroDuenio = new Usuario();
        otroDuenio.setId(99);

        Monitoreo m1 = new Monitoreo();
        m1.setPropietario(otroDuenio); // No es del propietarioId 10

        plantillaEjemplo.setMonitoreos(new HashSet<>(Set.of(m1)));

        when(plantillaMonitoreoRepository.findById(1)).thenReturn(Optional.of(plantillaEjemplo));
        when(usuarioService.buscarPorEmail(anyString())).thenReturn(new Usuario());

        // Act
        plantillaMonitoreoService.aplicarPlantillaAUsuario(1, "test@test.com", propietarioId);

        // Assert
        verify(monitoreoRepository, never()).save(any());
    }

    @Test
    @DisplayName("findByUsuario: Debería retornar lista vacía si el repo no encuentra nada")
    void findByUsuario_SinResultados_DeberiaRetornarListaVacia() {
        // Arrange
        when(plantillaMonitoreoRepository.findAllRelatedToUsuario(anyInt())).thenReturn(Collections.emptyList());

        // Act
        List<PlantillaMonitoreo> resultado = plantillaMonitoreoService.findByUsuario(10);

        // Assert
        assertTrue(resultado.isEmpty());
        verify(plantillaMonitoreoRepository).findAllRelatedToUsuario(10);
    }

}