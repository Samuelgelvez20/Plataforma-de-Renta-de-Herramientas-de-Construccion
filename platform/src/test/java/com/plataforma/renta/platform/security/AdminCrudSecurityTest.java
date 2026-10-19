package com.plataforma.renta.platform.security;

import com.plataforma.renta.domain.exception.OperacionInvalidaException;
import com.plataforma.renta.domain.model.Cliente;
import com.plataforma.renta.domain.model.Propietario;
import com.plataforma.renta.domain.model.Reserva;
import com.plataforma.renta.domain.model.UserRol;
import com.plataforma.renta.domain.model.Usuario;
import com.plataforma.renta.domain.port.out.ClienteRepository;
import com.plataforma.renta.domain.port.out.PropietarioRepository;
import com.plataforma.renta.domain.port.out.ReservaRepository;
import com.plataforma.renta.domain.port.out.UsuarioRepository;
import com.plataforma.renta.domain.value.Email;
import com.plataforma.renta.domain.value.Nit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminCrudSecurityTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PropietarioRepository propietarioRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private ReservaRepository reservaRepository;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                authenticationManager, passwordEncoder, jwtUtil,
                userDetailsService, usuarioRepository, propietarioRepository,
                clienteRepository, reservaRepository
        );
    }

    @Test
    @DisplayName("Admin no puede desactivar su propia cuenta")
    void testAdminNoPuedeDesactivarSuPropiaCuenta() {
        UUID adminId = UUID.randomUUID();
        Usuario admin = new Usuario(adminId, Email.of("admin@renta.com"), "hash", UserRol.ADMIN, "Admin", "Sys", "+57300", true, Instant.now(), Instant.now());
        when(usuarioRepository.findById(adminId)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> authService.toggleActivo(adminId, adminId))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("no puede desactivar su propia cuenta");
    }

    @Test
    @DisplayName("No se puede desactivar el único administrador activo del sistema")
    void testNoSePuedeDesactivarUltimoAdminActivo() {
        UUID actorAdminId = UUID.randomUUID();
        UUID targetAdminId = UUID.randomUUID();
        Usuario targetAdmin = new Usuario(targetAdminId, Email.of("admin2@renta.com"), "hash", UserRol.ADMIN, "Admin2", "Sys", "+57300", true, Instant.now(), Instant.now());

        when(usuarioRepository.findById(targetAdminId)).thenReturn(Optional.of(targetAdmin));
        when(usuarioRepository.findByRol(UserRol.ADMIN)).thenReturn(List.of(targetAdmin));

        assertThatThrownBy(() -> authService.toggleActivo(targetAdminId, actorAdminId))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("único administrador activo");
    }

    @Test
    @DisplayName("Admin puede alternar estado activo de un cliente o proveedor")
    void testAdminPuedeAlternarEstadoDeUsuario() {
        UUID actorAdminId = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        Usuario cliente = new Usuario(clienteId, Email.of("cli@renta.com"), "hash", UserRol.CLIENTE, "Carlos", "Perez", "+57300", true, Instant.now(), Instant.now());

        when(usuarioRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = authService.toggleActivo(clienteId, actorAdminId);

        assertThat(resultado.isActivo()).isFalse();
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    @DisplayName("No permite eliminar cliente que tiene reservas históricas")
    void testNoPermiteEliminarClienteConReservas() {
        UUID actorAdminId = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        Usuario cliente = new Usuario(clienteId, Email.of("cli@renta.com"), "hash", UserRol.CLIENTE, "Carlos", "Perez", "+57300", true, Instant.now(), Instant.now());

        when(usuarioRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(reservaRepository.findByClienteId(clienteId)).thenReturn(List.of(mock(Reserva.class)));

        assertThatThrownBy(() -> authService.eliminarUsuario(clienteId, actorAdminId))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("reservas asociadas en el historial");

        verify(usuarioRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("No permite eliminar proveedor que tiene herramientas registradas")
    void testNoPermiteEliminarProveedorConHerramientas() {
        UUID actorAdminId = UUID.randomUUID();
        UUID provId = UUID.randomUUID();
        Usuario prov = new Usuario(provId, Email.of("prov@renta.com"), "hash", UserRol.PROPIETARIO, "Empresa", "SAS", "+57300", true, Instant.now(), Instant.now());

        when(usuarioRepository.findById(provId)).thenReturn(Optional.of(prov));
        when(propietarioRepository.findHerramientaIdsByPropietarioId(provId)).thenReturn(List.of(UUID.randomUUID()));

        assertThatThrownBy(() -> authService.eliminarUsuario(provId, actorAdminId))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("herramientas registradas en la plataforma");

        verify(usuarioRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Permite eliminar cliente sin registros históricos de forma segura")
    void testPermiteEliminarClienteSinHistorial() {
        UUID actorAdminId = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        Usuario cliente = new Usuario(clienteId, Email.of("cli@renta.com"), "hash", UserRol.CLIENTE, "Carlos", "Perez", "+57300", true, Instant.now(), Instant.now());

        when(usuarioRepository.findById(clienteId)).thenReturn(Optional.of(cliente));
        when(reservaRepository.findByClienteId(clienteId)).thenReturn(Collections.emptyList());

        authService.eliminarUsuario(clienteId, actorAdminId);

        verify(usuarioRepository).deleteById(clienteId);
    }
}
