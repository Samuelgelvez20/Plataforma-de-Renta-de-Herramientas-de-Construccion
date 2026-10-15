package com.plataforma.renta.platform.controller;

import com.plataforma.renta.domain.exception.EntidadNoEncontradaException;
import com.plataforma.renta.domain.model.Usuario;
import com.plataforma.renta.domain.model.UserRol;
import com.plataforma.renta.domain.port.out.UsuarioRepository;
import com.plataforma.renta.domain.port.out.PropietarioRepository;
import com.plataforma.renta.domain.port.out.ClienteRepository;
import com.plataforma.renta.domain.value.Email;
import com.plataforma.renta.domain.value.Nit;
import com.plataforma.renta.platform.dto.LoginRequest;
import com.plataforma.renta.platform.dto.LoginResponse;
import com.plataforma.renta.platform.dto.RegisterRequest;
import com.plataforma.renta.platform.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;
    private final PropietarioRepository propietarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtUtil jwtUtil,
                          UsuarioRepository usuarioRepository,
                          PropietarioRepository propietarioRepository,
                          ClienteRepository clienteRepository,
                          PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.usuarioRepository = usuarioRepository;
        this.propietarioRepository = propietarioRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        UserDetails userDetails = (UserDetails) auth.getPrincipal();
        String token = jwtUtil.generateToken(userDetails);
        return ResponseEntity.ok(new LoginResponse(token, "Bearer", userDetails.getUsername(), userDetails.getAuthorities().iterator().next().getAuthority()));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        if (usuarioRepository.findByEmail(request.email()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email ya registrado"));
        }

        String rolStr = request.rol();
        if (rolStr == null || rolStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Rol es obligatorio: debe ser CLIENTE o PROPIETARIO"));
        }

        UserRol rol;
        try {
            rol = UserRol.valueOf(rolStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Rol inválido: solo se permiten CLIENTE o PROPIETARIO"));
        }

        if (rol == UserRol.ADMIN) {
            return ResponseEntity.badRequest().body(Map.of("error", "No se permite registrar usuarios con rol ADMIN"));
        }

        Usuario usuario = new Usuario(
                java.util.UUID.randomUUID(),
                Email.of(request.email()),
                passwordEncoder.encode(request.password()),
                rol,
                request.nombre(),
                request.apellido(),
                request.telefono(),
                java.time.Instant.now(),
                java.time.Instant.now()
        );
        usuarioRepository.save(usuario);

        if (rol == UserRol.PROPIETARIO) {
            Nit.of(request.nit()); // valida formato
            com.plataforma.renta.domain.model.Propietario propietario = new com.plataforma.renta.domain.model.Propietario(
                    usuario.getId(),
                    usuario.getEmail(),
                    Nit.of(request.nit()),
                    request.razonSocial(),
                    request.direccionFiscal(),
                    request.telefonoContacto(),
                    request.regimen(),
                    request.municipio(),
                    request.departamento(),
                    null
            );
            propietarioRepository.save(propietario);
        } else if (rol == UserRol.CLIENTE) {
            com.plataforma.renta.domain.model.Cliente cliente = new com.plataforma.renta.domain.model.Cliente(
                    usuario.getId(),
                    usuario.getEmail(),
                    usuario.getNombre(),
                    usuario.getApellido(),
                    usuario.getTelefono(),
                    null,
                    null
            );
            clienteRepository.save(cliente);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Usuario registrado correctamente");
        response.put("usuarioId", usuario.getId());
        return ResponseEntity.ok(response);
    }
}