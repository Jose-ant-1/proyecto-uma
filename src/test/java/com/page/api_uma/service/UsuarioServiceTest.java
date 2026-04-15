package com.page.api_uma.service;

import com.page.api_uma.model.Monitoreo;
import com.page.api_uma.model.Usuario;
import com.page.api_uma.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuarioEjemplo;

    @BeforeEach
    void setUp() {
        usuarioEjemplo = new Usuario();
        usuarioEjemplo.setId(1);
        usuarioEjemplo.setNombre("Test User");
        usuarioEjemplo.setEmail("test@ejemplo.com");
        usuarioEjemplo.setContrasenia("password123");
        usuarioEjemplo.setPermiso("USER");
    }

    @Test
    @DisplayName("findAll: Debería retornar una lista con todos los usuarios")
    void findAll_DeberiaRetornarListaDeUsuarios() {

        List<Usuario> listaUsuarios = List.of(usuarioEjemplo, new Usuario());
        when(usuarioRepository.findAll()).thenReturn(listaUsuarios);


        List<Usuario> resultado = usuarioService.findAll();


        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        verify(usuarioRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("findById: Debería retornar el usuario si el ID existe")
    void findById_Existente_DeberiaRetornarUsuario() {

        int idExistente = 1;
        when(usuarioRepository.findById(idExistente)).thenReturn(Optional.of(usuarioEjemplo));

        Usuario resultado = usuarioService.findById(idExistente);

        assertNotNull(resultado);
        assertEquals(idExistente, resultado.getId());
        assertEquals("test@ejemplo.com", resultado.getEmail());
        verify(usuarioRepository, times(1)).findById(idExistente);
    }

    @Test
    @DisplayName("findById: Debería retornar null si el ID no existe")
    void findById_NoExistente_DeberiaRetornarNull() {

        int idInexistente = 99;
        when(usuarioRepository.findById(idInexistente)).thenReturn(Optional.empty());

        Usuario resultado = usuarioService.findById(idInexistente);

        assertNull(resultado);
        verify(usuarioRepository, times(1)).findById(idInexistente);
    }

    @Test
    @DisplayName("save: Debería cifrar la contraseña si es texto plano")
    void save_ContraseniaPlana_DeberiaCifrarla() {

        String passPlana = "12345";
        String passCifrada = "$2a$10$hashSimulado";
        usuarioEjemplo.setContrasenia(passPlana);

        when(passwordEncoder.encode(passPlana)).thenReturn(passCifrada);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        Usuario resultado = usuarioService.save(usuarioEjemplo);

        assertEquals(passCifrada, resultado.getContrasenia());
        verify(passwordEncoder, times(1)).encode(passPlana);
        verify(usuarioRepository).save(usuarioEjemplo);
    }

    @Test
    @DisplayName("save: No debería cifrar si ya comienza con prefijo BCrypt")
    void save_ContraseniaYaCifrada_NoDeberiaRecifrar() {

        String passYaCifrada = "$2a$10$algúnHashExistente";
        usuarioEjemplo.setContrasenia(passYaCifrada);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        Usuario resultado = usuarioService.save(usuarioEjemplo);

        assertEquals(passYaCifrada, resultado.getContrasenia());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("deleteById: Debería limpiar relaciones y eliminar si el usuario existe")
    void deleteById_Existente_DeberiaLimpiarRelacionesYEliminar() {

        int id = 1;

        Monitoreo monitoreoMock = mock(Monitoreo.class);
        Set<Usuario> invitadosSet = spy(new HashSet<>(Set.of(usuarioEjemplo)));
        when(monitoreoMock.getInvitados()).thenReturn(invitadosSet);

        usuarioEjemplo.setMonitoreosInvitado(new HashSet<>(Set.of(monitoreoMock)));

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuarioEjemplo));

        usuarioService.deleteById(id);

        verify(invitadosSet).remove(usuarioEjemplo);

        verify(usuarioRepository).eliminarRelacionesDePlantillasDeSusMonitoreos(id);
        verify(usuarioRepository).eliminarRelacionesEnPlantillasUsuario(id);

        verify(usuarioRepository).saveAndFlush(usuarioEjemplo);
        verify(usuarioRepository).delete(usuarioEjemplo);
    }

    @Test
    @DisplayName("deleteById: No debería hacer nada si el usuario no existe")
    void deleteById_NoExistente_NoDeberiaLlamarARepositorio() {

        when(usuarioRepository.findById(99)).thenReturn(Optional.empty());

        usuarioService.deleteById(99);

        verify(usuarioRepository, never()).delete(any());
        verify(usuarioRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("buscarPorEmail: Debería retornar usuario si el email existe")
    void buscarPorEmail_Existente_DeberiaRetornarUsuario() {

        String email = "test@ejemplo.com";
        when(usuarioRepository.findByEmail(email)).thenReturn(usuarioEjemplo);


        Usuario resultado = usuarioService.buscarPorEmail(email);

        assertNotNull(resultado);
        assertEquals(email, resultado.getEmail());
        verify(usuarioRepository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("buscarPorEmail: Debería lanzar UsernameNotFoundException si el email no existe")
    void buscarPorEmail_NoExistente_DeberiaLanzarExcepcion() {

        String email = "noexiste@test.com";
        when(usuarioRepository.findByEmail(email)).thenReturn(null);

        assertThrows(UsernameNotFoundException.class, () -> {
            usuarioService.buscarPorEmail(email);
        });
        verify(usuarioRepository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("buscarUsuarios: Debería llamar a findAll si el término es nulo o vacío")
    void buscarUsuarios_TerminoVacio_DeberiaRetornarTodos() {

        when(usuarioRepository.findAll()).thenReturn(List.of(usuarioEjemplo));

        List<Usuario> resultadoNulo = usuarioService.buscarUsuarios(null);
        List<Usuario> resultadoVacio = usuarioService.buscarUsuarios("   ");

        assertEquals(1, resultadoNulo.size());
        assertEquals(1, resultadoVacio.size());
        verify(usuarioRepository, times(2)).findAll();
        verify(usuarioRepository, never()).buscarPorTermino(anyString());
    }

    @Test
    @DisplayName("buscarUsuarios: Debería llamar a buscarPorTermino si el término tiene contenido")
    void buscarUsuarios_ConTermino_DeberiaFiltrar() {

        String termino = "admin";
        when(usuarioRepository.buscarPorTermino(termino)).thenReturn(List.of(usuarioEjemplo));

        List<Usuario> resultado = usuarioService.buscarUsuarios(termino);

        assertNotNull(resultado);
        assertFalse(resultado.isEmpty());
        verify(usuarioRepository, times(1)).buscarPorTermino(termino);
        verify(usuarioRepository, never()).findAll();
    }

    @Test
    @DisplayName("loadUserByUsername: Debería retornar UserDetails si el email existe")
    void loadUserByUsername_Existente_DeberiaRetornarUserDetails() {

        String email = "test@ejemplo.com";
        usuarioEjemplo.setEmail(email);
        usuarioEjemplo.setContrasenia("encodedPassword");
        usuarioEjemplo.setPermiso("ROLE_USER");

        when(usuarioRepository.findByEmail(email)).thenReturn(usuarioEjemplo);

        UserDetails result = usuarioService.loadUserByUsername(email);

        assertNotNull(result);
        assertEquals(email, result.getUsername());
        assertEquals("encodedPassword", result.getPassword());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        verify(usuarioRepository, times(1)).findByEmail(email);
    }

    @Test
    @DisplayName("loadUserByUsername: Debería lanzar UsernameNotFoundException si el usuario no existe")
    void loadUserByUsername_NoExistente_DeberiaLanzarExcepcion() {

        String email = "fantasma@test.com";
        when(usuarioRepository.findByEmail(email)).thenReturn(null);

        assertThrows(UsernameNotFoundException.class, () -> {
            usuarioService.loadUserByUsername(email);
        });
    }

    @Test
    @DisplayName("getUsuarios: Debería retornar la lista de usuarios ordenada por nombre")
    void getUsuarios_DeberiaRetornarListaOrdenada() {

        List<Usuario> listaOrdenada = List.of(usuarioEjemplo, new Usuario());
        when(usuarioRepository.findAllByOrderByNombreAsc()).thenReturn(listaOrdenada);

        List<Usuario> resultado = usuarioService.getUsuarios();

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        verify(usuarioRepository, times(1)).findAllByOrderByNombreAsc();
    }

    @Test
    @DisplayName("getUsuarioAutenticado: Debería retornar el usuario basado en el contexto de seguridad")
    void getUsuarioAutenticado_DeberiaRetornarUsuarioLogueado() {

        String emailLogueado = "test@ejemplo.com";

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(authentication.isAuthenticated()).thenReturn(true);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(emailLogueado);

        SecurityContextHolder.setContext(securityContext);
        when(usuarioRepository.findByEmail(emailLogueado)).thenReturn(usuarioEjemplo);

        try {

            Usuario resultado = usuarioService.getUsuarioAutenticado();

            assertNotNull(resultado, "El resultado no debería ser null si el usuario está autenticado");
            assertEquals(emailLogueado, resultado.getEmail());
            verify(usuarioRepository, times(1)).findByEmail(emailLogueado);

        }
        finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("save: No debería intentar cifrar si la contraseña es nula")
    void save_ContraseniaNula_NoDeberiaCifrar() {

        usuarioEjemplo.setContrasenia(null);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        Usuario resultado = usuarioService.save(usuarioEjemplo);

        assertNull(resultado.getContrasenia());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("save: Debería lanzar excepción si el repositorio falla (ej. Email duplicado)")
    void save_ErrorRepositorio_DeberiaPropagarExcepcion() {

        when(usuarioRepository.save(any(Usuario.class)))
                .thenThrow(new RuntimeException("Data integrity violation"));

        assertThrows(RuntimeException.class, () -> {
            usuarioService.save(usuarioEjemplo);
        });
    }

    @Test
    @DisplayName("deleteById: No debería fallar si las colecciones de relaciones son nulas")
    void deleteById_ColeccionesNulas_DeberiaManejarloSinError() {

        int id = 1;

        usuarioEjemplo.setMonitoreosInvitado(null);

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuarioEjemplo));

        assertDoesNotThrow(() -> usuarioService.deleteById(id));

        verify(usuarioRepository).delete(usuarioEjemplo);
    }

    @Test
    @DisplayName("loadUserByUsername: Debería asignar ROLE_USER si el usuario no tiene permisos")
    void loadUserByUsername_SinPermisos_DeberiaAsignarRolPorDefecto() {

        String email = "sin@permisos.com";
        usuarioEjemplo.setEmail(email);
        usuarioEjemplo.setPermiso(null);
        when(usuarioRepository.findByEmail(email)).thenReturn(usuarioEjemplo);

        UserDetails result = usuarioService.loadUserByUsername(email);

        assertNotNull(result);

        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    @DisplayName("save: No debería cifrar si la contraseña está en blanco (solo espacios)")
    void save_ContraseniaEnBlanco_NoDeberiaCifrar() {

        usuarioEjemplo.setContrasenia("   ");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        Usuario resultado = usuarioService.save(usuarioEjemplo);

        assertEquals("   ", resultado.getContrasenia());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("buscarUsuarios: Debería retornar lista vacía si no hay coincidencias")
    void buscarUsuarios_SinCoincidencias_DeberiaRetornarListaVacia() {

        String termino = "usuario_inexistente";
        when(usuarioRepository.buscarPorTermino(termino)).thenReturn(List.of());

        List<Usuario> resultado = usuarioService.buscarUsuarios(termino);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
        verify(usuarioRepository).buscarPorTermino(termino);
    }

    @Test
    @DisplayName("deleteById: Debería manejar Monitoreos con lista de invitados nula")
    void deleteById_MonitoreoConInvitadosNulos_NoDeberiaLanzarExcepcion() {

        int id = 1;
        Monitoreo monitoreoRoto = mock(Monitoreo.class);

        when(monitoreoRoto.getInvitados()).thenReturn(null);

        Set<Monitoreo> monitoreosMutables = new HashSet<>();
        monitoreosMutables.add(monitoreoRoto);

        usuarioEjemplo.setMonitoreosInvitado(monitoreosMutables);

        when(usuarioRepository.findById(id)).thenReturn(Optional.of(usuarioEjemplo));

        assertDoesNotThrow(() -> usuarioService.deleteById(id));
        verify(usuarioRepository).delete(usuarioEjemplo);
    }

    @Test
    @DisplayName("getUsuarioAutenticado: Debería retornar null si no hay sesión activa")
    void getUsuarioAutenticado_SinSesion_DeberiaRetornarNull() {

        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        try {

            Usuario resultado = usuarioService.getUsuarioAutenticado();

            assertNull(resultado, "Debería retornar null cuando no hay autenticación en el contexto");

            verify(usuarioRepository, never()).findByEmail(anyString());

        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    @DisplayName("save: Debería lanzar IllegalStateException si el email ya existe en otro usuario")
    void save_EmailDuplicado_DeberiaLanzarExcepcion() {

        Usuario usuarioNuevo = new Usuario();
        usuarioNuevo.setId(0);
        usuarioNuevo.setEmail("duplicado@test.com");

        Usuario usuarioExistente = new Usuario();
        usuarioExistente.setId(50);
        usuarioExistente.setEmail("duplicado@test.com");

        when(usuarioRepository.findByEmail("duplicado@test.com")).thenReturn(usuarioExistente);

        assertThrows(IllegalStateException.class, () -> {
            usuarioService.save(usuarioNuevo);
        });
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("save: Debería lanzar IllegalArgumentException si el usuario es nulo")
    void save_UsuarioNulo_DeberiaLanzarExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> usuarioService.save(null));
    }

    @Test
    @DisplayName("save: Debería permitir guardar si el email existe pero pertenece al mismo usuario (Update)")
    void save_UpdateMismoUsuario_DeberiaFuncionar() {

        Usuario usuarioExistente = new Usuario();
        usuarioExistente.setId(1);
        usuarioExistente.setEmail("test@ejemplo.com");
        usuarioExistente.setContrasenia("$2a$10$hash");

        when(usuarioRepository.findByEmail("test@ejemplo.com")).thenReturn(usuarioExistente);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioExistente);

        assertDoesNotThrow(() -> usuarioService.save(usuarioExistente));
        verify(usuarioRepository).save(usuarioExistente);
    }


}