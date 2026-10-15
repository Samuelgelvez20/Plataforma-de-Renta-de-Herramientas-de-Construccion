package com.plataforma.renta.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plataforma.renta.domain.model.Usuario;
import com.plataforma.renta.domain.model.UserRol;
import com.plataforma.renta.domain.port.out.ClienteRepository;
import com.plataforma.renta.domain.port.out.PropietarioRepository;
import com.plataforma.renta.domain.port.out.UsuarioRepository;
import com.plataforma.renta.domain.value.Email;
import com.plataforma.renta.platform.dto.LoginRequest;
import com.plataforma.renta.platform.dto.LoginResponse;
import com.plataforma.renta.platform.dto.RegisterRequest;
import com.plataforma.renta.platform.security.JwtUtil;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    AuthenticationManager authenticationManager;

    @Mock
    JwtUtil jwtUtil;

    @Mock
    UsuarioRepository usuarioRepository;

    @Mock
    PropietarioRepository propietarioRepository;

    @Mock
    ClienteRepository clienteRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    Authentication authentication;

    @Mock
    UserDetails userDetails;

    AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(
                authenticationManager,
                jwtUtil,
                usuarioRepository,
                propietarioRepository,
                clienteRepository,
                passwordEncoder
        );
    }

    @Test
    void register_conRolAdmin_debeFallarCon400() {
        RegisterRequest request = new RegisterRequest(
                "admin@test.com",
                "password123",
                "Admin",
                "User",
                "3001234567",
                "ADMIN",
                null, null, null, null, null, null, null
        );

        ResponseEntity<Map<String, Object>> response = authController.register(request);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).containsEntry("error", "No se permite registrar usuarios con rol ADMIN");
    }

    @Test
    void register_conRolCliente_debeCrearUsuario() {
        RegisterRequest request = new RegisterRequest(
                "cliente@test.com",
                "password123",
                "Cliente",
                "Test",
                "3001234567",
                "CLIENTE",
                null, null, null, null, null, null, null
        );

        when(usuarioRepository.findByEmail("cliente@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");

        ResponseEntity<Map<String, Object>> response = authController.register(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("mensaje", "Usuario registrado correctamente");
        assertThat(response.getBody()).containsKey("usuarioId");

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertThat(usuarioCaptor.getValue().getRol()).isEqualTo(UserRol.CLIENTE);
        assertThat(usuarioCaptor.getValue().getEmail().getValor()).isEqualTo("cliente@test.com");
        assertThat(usuarioCaptor.getValue().getPasswordHash()).isEqualTo("encoded-password");

        verify(clienteRepository).save(any());
    }

    @Test
    void register_conRolPropietario_debeCrearUsuarioYPropietario() {
        // NIT válido: 900123450-4 (dígito de verificación correcto)
        RegisterRequest request = new RegisterRequest(
                "propietario@test.com",
                "password123",
                "Propietario",
                "Test",
                "3001234567",
                "PROPIETARIO",
                "Mi Empresa S.A.S.",
                "900123450-4",
                "Calle 123",
                "3001234567",
                "COMUN",
                "Bogotá",
                "Cundinamarca"
        );

        when(usuarioRepository.findByEmail("propietario@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("encoded-password");

        ResponseEntity<Map<String, Object>> response = authController.register(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        assertThat(usuarioCaptor.getValue().getRol()).isEqualTo(UserRol.PROPIETARIO);

        verify(propietarioRepository).save(any());
    }

    @Test
    void register_conRolInvalido_debeFallarCon400() {
        RegisterRequest request = new RegisterRequest(
                "invalido@test.com",
                "password123",
                "Invalido",
                "User",
                "3001234567",
                "SUPER_ADMIN",
                null, null, null, null, null, null, null
        );

        ResponseEntity<Map<String, Object>> response = authController.register(request);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).containsEntry("error", "Rol inválido: solo se permiten CLIENTE o PROPIETARIO");
    }

    @Test
    void register_conRolNull_debeFallarConValidacion() {
        RegisterRequest request = new RegisterRequest(
                "null@test.com",
                "password123",
                "Null",
                "User",
                "3001234567",
                null,
                null, null, null, null, null, null, null
        );

        ResponseEntity<Map<String, Object>> response = authController.register(request);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).containsEntry("error", "Rol es obligatorio: debe ser CLIENTE o PROPIETARIO");
    }

    @Test
    void register_conEmailExistente_debeFallarCon400() {
        RegisterRequest request = new RegisterRequest(
                "existente@test.com",
                "password123",
                "Existente",
                "User",
                "3001234567",
                "CLIENTE",
                null, null, null, null, null, null, null
        );

        when(usuarioRepository.findByEmail("existente@test.com")).thenReturn(Optional.of(new Usuario(
                java.util.UUID.randomUUID(),
                com.plataforma.renta.domain.value.Email.of("existente@test.com"),
                "hash",
                com.plataforma.renta.domain.model.UserRol.CLIENTE,
                "Existente",
                "User",
                "3001234567",
                java.time.Instant.now(),
                java.time.Instant.now()
        )));

        ResponseEntity<Map<String, Object>> response = authController.register(request);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).containsEntry("error", "Email ya registrado");
    }

    @Test
    void login_exitoso_debeRetornarToken() {
        LoginRequest request = new LoginRequest("user@test.com", "password123");

        GrantedAuthority authority = () -> "CLIENTE";
        Collection authorities = java.util.Collections.singletonList(authority);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("user@test.com");
        when(userDetails.getAuthorities()).thenReturn(authorities);
        when(jwtUtil.generateToken(userDetails)).thenReturn("test-jwt-token");

        ResponseEntity<LoginResponse> response = authController.login(request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().token()).isEqualTo("test-jwt-token");
        assertThat(response.getBody().email()).isEqualTo("user@test.com");
        assertThat(response.getBody().rol()).isEqualTo("CLIENTE");
    }
}