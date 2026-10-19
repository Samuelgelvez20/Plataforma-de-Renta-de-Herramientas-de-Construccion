package com.plataforma.renta.platform.security;

import com.plataforma.renta.api.controller.AuthController;
import com.plataforma.renta.api.dto.AuthDtos.AuthResponse;
import com.plataforma.renta.api.dto.AuthDtos.RegisterRequest;
import com.plataforma.renta.application.service.AuthService;
import com.plataforma.renta.application.service.AuthService.AuthResult;
import com.plataforma.renta.domain.exception.OperacionInvalidaException;
import com.plataforma.renta.domain.model.UserRol;
import com.plataforma.renta.domain.model.Usuario;
import com.plataforma.renta.domain.value.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicRegistrationRoleSecurityTest {

    @Mock
    private AuthService authService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService);
    }

    @Test
    @DisplayName("Registro con rol nulo asigna por defecto CLIENTE")
    void registroConRolNuloAsignaCliente() {
        RegisterRequest req = new RegisterRequest(
                "cliente.default@test.com", "Password123!", "Juan", "Perez",
                null, "3001234567", null, null, null, null
        );

        Usuario mockUser = new Usuario(
                UUID.randomUUID(), Email.of(req.email()), "hash", UserRol.CLIENTE,
                req.nombre(), req.apellido(), req.telefono(), Instant.now(), Instant.now()
        );
        when(authService.register(any())).thenReturn(new AuthResult("mock-token", mockUser));

        ResponseEntity<AuthResponse> res = authController.register(req);

        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        ArgumentCaptor<AuthService.RegisterUserCommand> captor = ArgumentCaptor.forClass(AuthService.RegisterUserCommand.class);
        verify(authService).register(captor.capture());
        assertThat(captor.getValue().rol()).isEqualTo(UserRol.CLIENTE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CLIENTE", "cliente", "Cliente", "ROLE_CLIENTE", "role_cliente"})
    @DisplayName("Registro explícito de cliente en cualquier combinación de mayúsculas/minúsculas")
    void registroExplicitoCliente(String rolInput) {
        RegisterRequest req = new RegisterRequest(
                "cliente.case@test.com", "Password123!", "Maria", "Lopez",
                rolInput, "3001234567", null, null, null, null
        );

        Usuario mockUser = new Usuario(
                UUID.randomUUID(), Email.of(req.email()), "hash", UserRol.CLIENTE,
                req.nombre(), req.apellido(), req.telefono(), Instant.now(), Instant.now()
        );
        when(authService.register(any())).thenReturn(new AuthResult("mock-token", mockUser));

        ResponseEntity<AuthResponse> res = authController.register(req);

        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        ArgumentCaptor<AuthService.RegisterUserCommand> captor = ArgumentCaptor.forClass(AuthService.RegisterUserCommand.class);
        verify(authService).register(captor.capture());
        assertThat(captor.getValue().rol()).isEqualTo(UserRol.CLIENTE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"PROPIETARIO", "propietario", "Propietario", "ROLE_PROPIETARIO"})
    @DisplayName("Registro de propietario permitido")
    void registroPropietarioPermitido(String rolInput) {
        RegisterRequest req = new RegisterRequest(
                "propietario@test.com", "Password123!", "Carlos", "Gomez",
                rolInput, "3109876543", "Maquinaria SAS", "900123456-8", "Calle 10", null
        );

        Usuario mockUser = new Usuario(
                UUID.randomUUID(), Email.of(req.email()), "hash", UserRol.PROPIETARIO,
                req.nombre(), req.apellido(), req.telefono(), Instant.now(), Instant.now()
        );
        when(authService.register(any())).thenReturn(new AuthResult("mock-token", mockUser));

        ResponseEntity<AuthResponse> res = authController.register(req);

        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        ArgumentCaptor<AuthService.RegisterUserCommand> captor = ArgumentCaptor.forClass(AuthService.RegisterUserCommand.class);
        verify(authService).register(captor.capture());
        assertThat(captor.getValue().rol()).isEqualTo(UserRol.PROPIETARIO);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ADMIN", "admin", "Admin", "ROLE_ADMIN", "role_admin",
            "ADMINISTRADOR", "administrador", "ADMIN_GLOBAL", "SUPER_ADMIN"
    })
    @DisplayName("Cualquier variación de ADMIN debe ser rechazada de inmediato y no debe invocar el servicio de persistencia")
    void rechazaAutorregistroAdmin(String rolInvalido) {
        RegisterRequest req = new RegisterRequest(
                "hacker@test.com", "Password123!", "Hacker", "Evil",
                rolInvalido, "3000000000", null, null, null, null
        );

        assertThatThrownBy(() -> authController.register(req))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("No está permitido el autorregistro con rol de administrador");

        verifyNoInteractions(authService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROOT", "SUPERUSER", "MODERATOR", "UNKNOWN", "12345", "'; DROP TABLE usuario;--"})
    @DisplayName("Roles desconocidos o malformados son rechazados")
    void rechazaRolesDesconocidos(String rolDesconocido) {
        RegisterRequest req = new RegisterRequest(
                "unknown@test.com", "Password123!", "Test", "User",
                rolDesconocido, "3000000000", null, null, null, null
        );

        assertThatThrownBy(() -> authController.register(req))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("Rol no válido para registro público");

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("Registro de cliente con nombre y apellido válidos invoca AuthService correctamente")
    void registroClienteValidoInvocaService() {
        RegisterRequest req = new RegisterRequest(
                "cliente.valido@test.com", "Password123!", "Juan", "Perez",
                "CLIENTE", "3001234567", null, null, null, "Calle 100 # 15-20"
        );

        Usuario mockUser = new Usuario(
                UUID.randomUUID(), Email.of(req.email()), "hash", UserRol.CLIENTE,
                req.nombre(), req.apellido(), req.telefono(), Instant.now(), Instant.now()
        );
        when(authService.register(any())).thenReturn(new AuthResult("jwt-token-123", mockUser));

        ResponseEntity<AuthResponse> res = authController.register(req);

        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().usuario().nombre()).isEqualTo("Juan");
        assertThat(res.getBody().usuario().apellido()).isEqualTo("Perez");
    }

    @Test
    @DisplayName("Registro de propietario con NIT válido (DV 8 verificado por módulo 11) pasa al servicio")
    void registroPropietarioValidoInvocaService() {
        RegisterRequest req = new RegisterRequest(
                "propietario.valido@test.com", "Password123!", "Carlos", "Gomez",
                "PROPIETARIO", "3109876543", "Maquinaria y Equipos SAS", "900123456-8", "Calle 26 # 68-35", null
        );

        Usuario mockUser = new Usuario(
                UUID.randomUUID(), Email.of(req.email()), "hash", UserRol.PROPIETARIO,
                req.nombre(), req.apellido(), req.telefono(), Instant.now(), Instant.now()
        );
        when(authService.register(any())).thenReturn(new AuthResult("jwt-token-456", mockUser));

        ResponseEntity<AuthResponse> res = authController.register(req);

        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        ArgumentCaptor<AuthService.RegisterUserCommand> captor = ArgumentCaptor.forClass(AuthService.RegisterUserCommand.class);
        verify(authService).register(captor.capture());
        assertThat(captor.getValue().nit()).isEqualTo("900123456-8");
        assertThat(captor.getValue().razonSocial()).isEqualTo("Maquinaria y Equipos SAS");
    }

    @Test
    @DisplayName("NIT con dígito de verificación inválido es rechazado por la lógica de dominio")
    void nitConDvInvalidoEsRechazadoPorDominio() {
        // Para 900123456 el DV válido calculado por módulo 11 es 8, por tanto 900123456-1 debe fallar
        org.junit.jupiter.api.Assertions.assertFalse(com.plataforma.renta.domain.value.Nit.esValido("900123456-1"));
        org.junit.jupiter.api.Assertions.assertTrue(com.plataforma.renta.domain.value.Nit.esValido("900123456-8"));
    }
}
