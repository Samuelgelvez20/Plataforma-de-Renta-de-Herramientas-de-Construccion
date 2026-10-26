package com.plataforma.renta.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.plataforma.renta.api.controller.AuthController;
import com.plataforma.renta.api.dto.AuthDtos.AuthResponse;
import com.plataforma.renta.api.dto.AuthDtos.LoginRequest;
import com.plataforma.renta.api.dto.AuthDtos.RegisterRequest;
import com.plataforma.renta.api.dto.AuthDtos.UserResponse;
import com.plataforma.renta.application.service.AuthService;
import com.plataforma.renta.application.service.AuthService.AuthResult;
import com.plataforma.renta.application.service.AuthService.RegisterUserCommand;
import com.plataforma.renta.domain.exception.OperacionInvalidaException;
import com.plataforma.renta.domain.model.UserRol;
import com.plataforma.renta.domain.model.Usuario;
import com.plataforma.renta.domain.value.Email;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private Authentication authentication;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService);
    }

    @Test
    @DisplayName("login exitoso debe llamar a AuthService y retornar AuthResponse con token y usuario")
    void login_exitoso_debeRetornarTokenYUsuario() {
        LoginRequest request = new LoginRequest("user@test.com", "password123");
        Usuario usuario = new Usuario(
                UUID.randomUUID(),
                Email.of("user@test.com"),
                "encoded-hash",
                UserRol.CLIENTE,
                "Juan",
                "Perez",
                "3001234567",
                Instant.now(),
                Instant.now()
        );
        AuthResult authResult = new AuthResult("jwt-token-valido", usuario);

        when(authService.login("user@test.com", "password123")).thenReturn(authResult);

        ResponseEntity<AuthResponse> response = authController.login(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isEqualTo("jwt-token-valido");
        assertThat(response.getBody().usuario().email()).isEqualTo("user@test.com");
        assertThat(response.getBody().usuario().rol()).isEqualTo("CLIENTE");
    }

    @Test
    @DisplayName("register con rol ADMIN debe ser rechazado con OperacionInvalidaException")
    void register_conRolAdmin_debeLanzarExcepcion() {
        RegisterRequest request = new RegisterRequest(
                "admin@test.com",
                "Password123!",
                "Admin",
                "User",
                "ADMIN",
                "3001234567",
                null, null, null
        );

        assertThatThrownBy(() -> authController.register(request))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("No está permitido el autorregistro con rol de administrador");

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("register con rol CLIENTE debe crear el comando con rol CLIENTE")
    void register_conRolCliente_debeCrearUsuarioCliente() {
        RegisterRequest request = new RegisterRequest(
                "cliente@test.com",
                "Password123!",
                "Cliente",
                "Test",
                "CLIENTE",
                "3001234567",
                null, null, null
        );

        Usuario usuario = new Usuario(
                UUID.randomUUID(),
                Email.of("cliente@test.com"),
                "encoded-hash",
                UserRol.CLIENTE,
                "Cliente",
                "Test",
                "3001234567",
                Instant.now(),
                Instant.now()
        );

        when(authService.register(any(RegisterUserCommand.class))).thenReturn(new AuthResult("jwt-token", usuario));

        ResponseEntity<AuthResponse> response = authController.register(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isEqualTo("jwt-token");
        assertThat(response.getBody().usuario().rol()).isEqualTo("CLIENTE");

        ArgumentCaptor<RegisterUserCommand> cmdCaptor = ArgumentCaptor.forClass(RegisterUserCommand.class);
        verify(authService).register(cmdCaptor.capture());
        assertThat(cmdCaptor.getValue().rol()).isEqualTo(UserRol.CLIENTE);
        assertThat(cmdCaptor.getValue().email()).isEqualTo("cliente@test.com");
    }

    @Test
    @DisplayName("register con rol PROPIETARIO debe registrar con rol PROPIETARIO")
    void register_conRolPropietario_debeRegistrarPropietario() {
        RegisterRequest request = new RegisterRequest(
                "propietario@test.com",
                "Password123!",
                "Carlos",
                "Gomez",
                "PROPIETARIO",
                "3109876543",
                "Maquinaria SAS",
                "Calle 123",
                null
        );

        Usuario usuario = new Usuario(
                UUID.randomUUID(),
                Email.of("propietario@test.com"),
                "encoded-hash",
                UserRol.PROPIETARIO,
                "Carlos",
                "Gomez",
                "3109876543",
                Instant.now(),
                Instant.now()
        );

        when(authService.register(any(RegisterUserCommand.class))).thenReturn(new AuthResult("jwt-token-prop", usuario));

        ResponseEntity<AuthResponse> response = authController.register(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().usuario().rol()).isEqualTo("PROPIETARIO");

        ArgumentCaptor<RegisterUserCommand> cmdCaptor = ArgumentCaptor.forClass(RegisterUserCommand.class);
        verify(authService).register(cmdCaptor.capture());
        assertThat(cmdCaptor.getValue().rol()).isEqualTo(UserRol.PROPIETARIO);
        assertThat(cmdCaptor.getValue().razonSocial()).isEqualTo("Maquinaria SAS");
    }

    @Test
    @DisplayName("register con rol invalido debe lanzar OperacionInvalidaException")
    void register_conRolInvalido_debeLanzarExcepcion() {
        RegisterRequest request = new RegisterRequest(
                "invalido@test.com",
                "Password123!",
                "Invalido",
                "User",
                "SUPER_USER",
                "3001234567",
                null, null, null
        );

        assertThatThrownBy(() -> authController.register(request))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("Rol no válido para registro público");

        verifyNoInteractions(authService);
    }

    @Test
    @DisplayName("me con usuario autenticado retorna su perfil")
    void me_retornaPerfilUsuarioAutenticado() {
        when(authentication.getName()).thenReturn("usuario@test.com");
        Usuario usuario = new Usuario(
                UUID.randomUUID(),
                Email.of("usuario@test.com"),
                "hash",
                UserRol.CLIENTE,
                "Ana",
                "Gomez",
                "3115551234",
                Instant.now(),
                Instant.now()
        );
        when(authService.obtenerUsuarioPorEmail("usuario@test.com")).thenReturn(usuario);

        ResponseEntity<UserResponse> response = authController.me(authentication);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().email()).isEqualTo("usuario@test.com");
        assertThat(response.getBody().nombre()).isEqualTo("Ana");
        assertThat(response.getBody().apellido()).isEqualTo("Gomez");
        assertThat(response.getBody().rol()).isEqualTo("CLIENTE");
    }

    @Test
    @DisplayName("me sin autenticacion retorna 401 UNAUTHORIZED")
    void me_sinAutenticacion_retorna401() {
        ResponseEntity<UserResponse> response = authController.me(null);
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }
}