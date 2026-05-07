package com.page.api_uma.service;

import com.page.api_uma.dto.PlantillaUsuarioDTO;
import com.page.api_uma.mapper.PlantillaUsuarioMapper;
import com.page.api_uma.model.PlantillaUsuario;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.repository.PlantillaUsuarioRepository;
import com.page.api_uma.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlantUsuarioServiceTest {

    @Mock
    private PlantillaUsuarioRepository plantillaUsuarioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PlantillaUsuarioMapper mapper;

    @InjectMocks
    private PlantillaUsuarioService plantillaUsuarioService;

    private PlantillaUsuario plantillaEjemplo;
    private Usuario usuarioEjemplo;

    @BeforeEach
    void setUp() {
        // Usuario propietario de ejemplo
        usuarioEjemplo = new Usuario();
        usuarioEjemplo.setId(1);
        usuarioEjemplo.setEmail("propietario@test.com");
        usuarioEjemplo.setNombre("Admin");

        // Plantilla de ejemplo
        plantillaEjemplo = new PlantillaUsuario();
        plantillaEjemplo.setId(100);
        plantillaEjemplo.setNombre("Plantilla de Usuarios Test");
        plantillaEjemplo.setPropietario(usuarioEjemplo);
        plantillaEjemplo.setUsuarios(new HashSet<>());
    }

    @Test
    @DisplayName("findAll: Debería retornar todas las plantillas de usuario")
    void findAll_DeberiaRetornarListaCompleta() {

        when(plantillaUsuarioRepository.findAll()).thenReturn(List.of(plantillaEjemplo));

        List<PlantillaUsuario> resultado = plantillaUsuarioService.findAll();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(plantillaUsuarioRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("findById: Debería retornar la plantilla si el ID existe")
    void findById_Existente_DeberiaRetornarPlantilla() {

        int id = 100;
        when(plantillaUsuarioRepository.findById(id)).thenReturn(Optional.of(plantillaEjemplo));

        PlantillaUsuario resultado = plantillaUsuarioService.findById(id);

        assertNotNull(resultado);
        assertEquals(id, resultado.getId());
        assertEquals("Plantilla de Usuarios Test", resultado.getNombre());
        verify(plantillaUsuarioRepository, times(1)).findById(id);
    }

    @Test
    @DisplayName("findById: Debería retornar null si el ID no existe")
    void findById_NoExistente_DeberiaRetornarNull() {

        int idInexistente = 999;
        when(plantillaUsuarioRepository.findById(idInexistente)).thenReturn(Optional.empty());

        PlantillaUsuario resultado = plantillaUsuarioService.findById(idInexistente);

        assertNull(resultado);
        verify(plantillaUsuarioRepository, times(1)).findById(idInexistente);
    }

    @Test
    @DisplayName("save: Debería asignar propietario y validar usuarios invitados antes de guardar")
    void save_DeberiaAsignarPropietarioYValidarInvitados() {

        String emailOwner = "propietario@test.com";
        PlantillaUsuarioDTO dto = new PlantillaUsuarioDTO();
        dto.setNombre("Plantilla Compartida");

        // Creamos un usuario invitado para el DTO
        Usuario invitadoDTO = new Usuario();
        invitadoDTO.setId(20);
        dto.setUsuarios(new HashSet<>(Set.of(invitadoDTO)));

        // Configuración de Mocks
        when(mapper.toEntity(dto)).thenReturn(plantillaEjemplo);
        when(usuarioRepository.findByEmail(emailOwner)).thenReturn(usuarioEjemplo);

        // Simulamos la búsqueda del invitado en la DB (el Stream del servicio)
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(invitadoDTO));

        when(plantillaUsuarioRepository.save(any(PlantillaUsuario.class))).thenReturn(plantillaEjemplo);
        when(mapper.toDTO(plantillaEjemplo)).thenReturn(dto);

        PlantillaUsuarioDTO resultado = plantillaUsuarioService.save(dto, emailOwner);

        assertNotNull(resultado);
        verify(usuarioRepository).findByEmail(emailOwner);
        verify(usuarioRepository).findById(20);
        verify(plantillaUsuarioRepository).save(argThat(p ->
                p.getPropietario().equals(usuarioEjemplo) && p.getUsuarios().contains(invitadoDTO)
        ));
    }

    @Test
    @DisplayName("save: Debería lanzar excepción si un usuario invitado no existe")
    void save_UsuarioInvitadoNoExiste_DeberiaLanzarExcepcion() {

        String emailOwner = "propietario@test.com";
        PlantillaUsuarioDTO dto = new PlantillaUsuarioDTO();
        Usuario inexistente = new Usuario();
        inexistente.setId(999);
        dto.setUsuarios(new HashSet<>(Set.of(inexistente)));

        when(mapper.toEntity(dto)).thenReturn(plantillaEjemplo);
        when(usuarioRepository.findByEmail(emailOwner)).thenReturn(usuarioEjemplo);

        when(usuarioRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            plantillaUsuarioService.save(dto, emailOwner);
        });
    }

    @Test
    @DisplayName("findByPropietario: Debería retornar las plantillas del dueño")
    void findByPropietario_DeberiaRetornarLista() {

        String email = "propietario@test.com";
        when(usuarioRepository.findByEmail(email)).thenReturn(usuarioEjemplo);
        when(plantillaUsuarioRepository.findByPropietarioOrderByNombreAsc(usuarioEjemplo))
                .thenReturn(List.of(plantillaEjemplo));

        List<PlantillaUsuario> resultado = plantillaUsuarioService.findByPropietario(email);

        assertFalse(resultado.isEmpty());
        assertEquals(1, resultado.size());
        verify(plantillaUsuarioRepository).findByPropietarioOrderByNombreAsc(usuarioEjemplo);
    }

    @Test
    @DisplayName("esPropietario: Debería retornar true si el email coincide con el dueño")
    void esPropietario_EmailCoincide_DeberiaRetornarTrue() {

        int plantillaId = 100;
        String emailOwner = "propietario@test.com";
        when(plantillaUsuarioRepository.findById(plantillaId)).thenReturn(Optional.of(plantillaEjemplo));

        boolean resultado = plantillaUsuarioService.esPropietario(plantillaId, emailOwner);

        assertTrue(resultado);
        verify(plantillaUsuarioRepository).findById(plantillaId);
    }

    @Test
    @DisplayName("esPropietario: Debería retornar false si el email no coincide o la plantilla no existe")
    void esPropietario_NoCoincideOInexistente_DeberiaRetornarFalse() {

        int idExistente = 100;
        int idInexistente = 999;
        String emailErroneo = "otro@test.com";

        when(plantillaUsuarioRepository.findById(idExistente)).thenReturn(Optional.of(plantillaEjemplo));
        when(plantillaUsuarioRepository.findById(idInexistente)).thenReturn(Optional.empty());

        boolean resultadoEmailFalso = plantillaUsuarioService.esPropietario(idExistente, emailErroneo);
        boolean resultadoPlantillaFalsa = plantillaUsuarioService.esPropietario(idInexistente, "cualquiera@test.com");

        assertFalse(resultadoEmailFalso);
        assertFalse(resultadoPlantillaFalsa);
    }

    @Test
    @DisplayName("deleteById: Debería llamar al repositorio para eliminar")
    void deleteById_DeberiaLlamarAlRepositorio() {

        int idEliminar = 100;
        doNothing().when(plantillaUsuarioRepository).deleteById(idEliminar);

        plantillaUsuarioService.deleteById(idEliminar);

        verify(plantillaUsuarioRepository, times(1)).deleteById(idEliminar);
    }

    @Test
    @DisplayName("findByPropietario: Debería lanzar excepción si el usuario no existe")
    void findByPropietario_UsuarioNoExiste_DeberiaLanzarExcepcion() {

        String email = "fantasma@test.com";
        when(usuarioRepository.findByEmail(email)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> {
            plantillaUsuarioService.findByPropietario(email);
        });
    }

    @Test
    @DisplayName("save: Debería guardar correctamente aunque la lista de usuarios invitados sea nula")
    void save_UsuariosInvitadosNull_DeberiaFuncionar() {

        String emailOwner = "propietario@test.com";
        PlantillaUsuarioDTO dto = new PlantillaUsuarioDTO();
        dto.setNombre("Plantilla Privada");
        dto.setUsuarios(null); // Caso nulo

        when(mapper.toEntity(dto)).thenReturn(plantillaEjemplo);
        when(usuarioRepository.findByEmail(emailOwner)).thenReturn(usuarioEjemplo);
        when(plantillaUsuarioRepository.save(any(PlantillaUsuario.class))).thenReturn(plantillaEjemplo);
        when(mapper.toDTO(plantillaEjemplo)).thenReturn(dto);

        PlantillaUsuarioDTO resultado = plantillaUsuarioService.save(dto, emailOwner);

        assertNotNull(resultado);

        verify(usuarioRepository, never()).findById(anyInt());
        verify(plantillaUsuarioRepository).save(any());
    }

    @Test
    @DisplayName("save: Debería manejar correctamente IDs de usuarios duplicados en el DTO")
    void save_UsuariosDuplicados_DeberiaTratarlosComoSet() {

        String emailOwner = "propietario@test.com";
        Usuario invitado = new Usuario();
        invitado.setId(55);

        PlantillaUsuarioDTO dto = new PlantillaUsuarioDTO();
        // Añadimos el mismo "usuario" o ID dos veces (simulando entrada malformada)
        dto.setUsuarios(new HashSet<>(List.of(invitado, invitado)));

        when(mapper.toEntity(dto)).thenReturn(plantillaEjemplo);
        when(usuarioRepository.findByEmail(emailOwner)).thenReturn(usuarioEjemplo);
        when(usuarioRepository.findById(55)).thenReturn(Optional.of(invitado));
        when(plantillaUsuarioRepository.save(any())).thenReturn(plantillaEjemplo);

        plantillaUsuarioService.save(dto, emailOwner);

        // Verificamos que solo se buscó una vez o se procesó una vez
        verify(usuarioRepository, atMost(1)).findById(55);
    }

    @Test
    @DisplayName("save: Debería lanzar excepción si el propietario no existe en la BD")
    void save_PropietarioNoExiste_DeberiaLanzarExcepcion() {

        String emailInexistente = "no-existe@test.com";
        PlantillaUsuarioDTO dto = new PlantillaUsuarioDTO();

        when(mapper.toEntity(dto)).thenReturn(new PlantillaUsuario());
        when(usuarioRepository.findByEmail(emailInexistente)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> {
            plantillaUsuarioService.save(dto, emailInexistente);
        });
    }

    @Test
    @DisplayName("deleteById: No debería fallar si el ID no existe")
    void deleteById_IdInexistente_NoDeberiaLanzarExcepcion() {

        int idInexistente = 999;
        doNothing().when(plantillaUsuarioRepository).deleteById(idInexistente);

        assertDoesNotThrow(() -> plantillaUsuarioService.deleteById(idInexistente));
        verify(plantillaUsuarioRepository, times(1)).deleteById(idInexistente);
    }

    @Test
    @DisplayName("save: No debería permitir cambiar el propietario de una plantilla existente")
    void save_IntentoCambioPropietario_DeberiaMantenerIntegridad() {

        String emailAtacante = "atacante@test.com";
        Usuario usuarioAtacante = new Usuario();
        usuarioAtacante.setId(2);
        usuarioAtacante.setEmail(emailAtacante);

        PlantillaUsuarioDTO dto = new PlantillaUsuarioDTO();
        dto.setId(100);
        dto.setNombre("Nombre Editado");

        when(mapper.toEntity(dto)).thenReturn(plantillaEjemplo); // plantillaEjemplo es del usuario 1 (Admin)
        when(usuarioRepository.findByEmail(emailAtacante)).thenReturn(usuarioAtacante);
        when(plantillaUsuarioRepository.save(any())).thenReturn(plantillaEjemplo);

        plantillaUsuarioService.save(dto, emailAtacante);

        verify(plantillaUsuarioRepository).save(argThat(p ->
                p.getPropietario().getEmail().equals(emailAtacante)
        ));
    }

    @Test
    @DisplayName("save: Debería ignorar o manejar IDs de invitados nulos dentro de la lista")
    void save_InvitadoConIdNulo_DeberiaManejarlo() {

        String emailOwner = "propietario@test.com";
        PlantillaUsuarioDTO dto = new PlantillaUsuarioDTO();

        Usuario usuarioNulo = new Usuario();
        dto.setUsuarios(new HashSet<>(Set.of(usuarioNulo)));

        when(mapper.toEntity(dto)).thenReturn(plantillaEjemplo);
        when(usuarioRepository.findByEmail(emailOwner)).thenReturn(usuarioEjemplo);

        assertThrows(RuntimeException.class, () -> {
            plantillaUsuarioService.save(dto, emailOwner);
        });
    }

    @Test
    @DisplayName("save: No debería intentar buscar invitados si la lista está vacía")
    void save_ListaInvitadosVacia_NoDeberiaLlamarAlRepositorioUsuarios() {

        String emailOwner = "propietario@test.com";
        PlantillaUsuarioDTO dto = new PlantillaUsuarioDTO();
        dto.setUsuarios(new HashSet<>()); // Lista explícitamente vacía

        when(mapper.toEntity(dto)).thenReturn(plantillaEjemplo);
        when(usuarioRepository.findByEmail(emailOwner)).thenReturn(usuarioEjemplo);
        when(plantillaUsuarioRepository.save(any())).thenReturn(plantillaEjemplo);

        plantillaUsuarioService.save(dto, emailOwner);

        // Nunca se llamó a findById para buscar invitados
        verify(usuarioRepository, never()).findById(anyInt());
    }

    @Test
    @DisplayName("save: La entidad guardada debe mantener el nombre y datos del DTO")
    void save_DeberiaMantenerDatosFielesAlDTO() {

        String emailOwner = "propietario@test.com";
        PlantillaUsuarioDTO dto = new PlantillaUsuarioDTO();
        dto.setNombre("Nombre Muy Específico");

        PlantillaUsuario entidadMapeada = new PlantillaUsuario();
        entidadMapeada.setNombre(dto.getNombre());

        when(mapper.toEntity(dto)).thenReturn(entidadMapeada);
        when(usuarioRepository.findByEmail(emailOwner)).thenReturn(usuarioEjemplo);
        when(plantillaUsuarioRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        plantillaUsuarioService.save(dto, emailOwner);

        verify(plantillaUsuarioRepository).save(argThat(p ->
                p.getNombre().equals("Nombre Muy Específico")
        ));
    }

    @Test
    @DisplayName("deleteById: Debería manejar correctamente si el repositorio lanza excepción al no hallar el ID")
    void deleteById_Inexistente_DeberiaManejarExcepcionDelRepo() {

        int idInexistente = 999;

        doThrow(new RuntimeException("No existe")).when(plantillaUsuarioRepository).deleteById(idInexistente);

        assertThrows(RuntimeException.class, () -> plantillaUsuarioService.deleteById(idInexistente));
    }

}