package com.plataforma.renta.platform.security;

import com.plataforma.renta.application.service.AuthService;
import com.plataforma.renta.domain.exception.EntidadNoEncontradaException;
import com.plataforma.renta.domain.exception.OperacionInvalidaException;
import com.plataforma.renta.domain.model.Cliente;
import com.plataforma.renta.domain.model.Propietario;
import com.plataforma.renta.domain.model.UserRol;
import com.plataforma.renta.domain.model.Usuario;
import com.plataforma.renta.domain.port.out.ClienteRepository;
import com.plataforma.renta.domain.port.out.PropietarioRepository;
import com.plataforma.renta.domain.port.out.UsuarioRepository;
import com.plataforma.renta.domain.value.Email;
import com.plataforma.renta.domain.value.Nit;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UsuarioRepository usuarioRepository;
    private final PropietarioRepository propietarioRepository;
    private final ClienteRepository clienteRepository;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           CustomUserDetailsService userDetailsService,
                           UsuarioRepository usuarioRepository,
                           PropietarioRepository propietarioRepository,
                           ClienteRepository clienteRepository) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.usuarioRepository = usuarioRepository;
        this.propietarioRepository = propietarioRepository;
        this.clienteRepository = clienteRepository;
    }

    @Override
    public AuthResult login(String email, String password) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
        } catch (BadCredentialsException e) {
            throw new OperacionInvalidaException("Credenciales inválidas: correo o contraseña incorrectos");
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntidadNoEncontradaException("Usuario no encontrado: " + email));

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String token = jwtUtil.generateToken(userDetails);

        return new AuthResult(token, usuario);
    }

    @Override
    @Transactional
    public AuthResult register(RegisterUserCommand cmd) {
        if (cmd.rol() == UserRol.ADMIN) {
            throw new OperacionInvalidaException("No está permitido el registro público de cuentas con rol de administrador");
        }
        if (usuarioRepository.findByEmail(cmd.email()).isPresent()) {
            throw new OperacionInvalidaException("El correo electrónico ya se encuentra registrado: " + cmd.email());
        }

        String encodedPassword = passwordEncoder.encode(cmd.password());
        UUID usuarioId = UUID.randomUUID();
        Instant ahora = Instant.now();

        Usuario usuario = new Usuario(
                usuarioId,
                Email.of(cmd.email()),
                encodedPassword,
                cmd.rol() != null ? cmd.rol() : UserRol.CLIENTE,
                cmd.nombre(),
                cmd.apellido(),
                cmd.telefono(),
                ahora,
                ahora
        );
        usuario = usuarioRepository.save(usuario);

        // Crear perfil correspondiente
        if (usuario.getRol() == UserRol.PROPIETARIO) {
            String nitStr = cmd.nit() != null && !cmd.nit().isBlank() ? cmd.nit() : "900123456-1";
            Propietario propietario = new Propietario(
                    usuarioId,
                    Email.of(cmd.email()),
                    Nit.of(nitStr),
                    cmd.razonSocial() != null ? cmd.razonSocial() : cmd.nombre() + " " + cmd.apellido(),
                    cmd.direccionFiscal() != null ? cmd.direccionFiscal() : "Dirección fiscal principal",
                    cmd.telefono(),
                    "COMUN",
                    "Bucaramanga",
                    "Santander",
                    ahora
            );
            propietarioRepository.save(propietario);
        } else if (usuario.getRol() == UserRol.CLIENTE) {
            Cliente cliente = new Cliente(
                    usuarioId,
                    Email.of(cmd.email()),
                    cmd.nombre(),
                    cmd.apellido(),
                    cmd.telefono(),
                    cmd.direccionEntregaDefault() != null ? cmd.direccionEntregaDefault() : "Dirección de entrega predeterminada",
                    null
            );
            clienteRepository.save(cliente);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(usuario.getEmail().getValor());
        String token = jwtUtil.generateToken(userDetails);

        return new AuthResult(token, usuario);
    }

    @Override
    public Usuario obtenerUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntidadNoEncontradaException("Usuario no encontrado: " + email));
    }

    @Override
    public Usuario obtenerUsuarioPorId(UUID id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Usuario no encontrado: " + id));
    }

    @Override
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @Override
    public Usuario toggleActivo(UUID id) {
        // En esquema actual usuario no tiene flag activo, se retorna el usuario existente
        return obtenerUsuarioPorId(id);
    }
}
