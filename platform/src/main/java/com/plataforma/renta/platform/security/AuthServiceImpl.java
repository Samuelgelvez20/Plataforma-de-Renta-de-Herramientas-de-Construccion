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
import com.plataforma.renta.domain.port.out.ReservaRepository;
import com.plataforma.renta.domain.port.out.UsuarioRepository;
import com.plataforma.renta.domain.value.Email;
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
import java.util.Optional;
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
    private final ReservaRepository reservaRepository;

    public AuthServiceImpl(AuthenticationManager authenticationManager,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil,
                           CustomUserDetailsService userDetailsService,
                           UsuarioRepository usuarioRepository,
                           PropietarioRepository propietarioRepository,
                           ClienteRepository clienteRepository,
                           ReservaRepository reservaRepository) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.usuarioRepository = usuarioRepository;
        this.propietarioRepository = propietarioRepository;
        this.clienteRepository = clienteRepository;
        this.reservaRepository = reservaRepository;
    }

    @Override
    public AuthResult login(String email, String password) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
        } catch (org.springframework.security.authentication.DisabledException e) {
            throw new org.springframework.security.authentication.DisabledException("La cuenta de usuario se encuentra suspendida o inactiva");
        } catch (BadCredentialsException e) {
            throw new OperacionInvalidaException("Credenciales inválidas: correo o contraseña incorrectos");
        }

        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new EntidadNoEncontradaException("Usuario no encontrado: " + email));

        if (!usuario.isActivo()) {
            throw new org.springframework.security.authentication.DisabledException("La cuenta de usuario se encuentra suspendida o inactiva");
        }

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
                true,
                ahora,
                ahora
        );
        usuario = usuarioRepository.save(usuario);

        // Crear perfil correspondiente
        if (usuario.getRol() == UserRol.PROPIETARIO) {
            Propietario propietario = new Propietario(
                    usuarioId,
                    Email.of(cmd.email()),
                    cmd.razonSocial() != null && !cmd.razonSocial().isBlank() ? cmd.razonSocial() : cmd.nombre() + " " + cmd.apellido(),
                    cmd.direccionFiscal() != null && !cmd.direccionFiscal().isBlank() ? cmd.direccionFiscal() : "Dirección fiscal principal",
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
    @Transactional
    public Usuario toggleActivo(UUID id, UUID actorId) {
        Usuario usuario = obtenerUsuarioPorId(id);

        if (actorId != null && actorId.equals(id)) {
            throw new OperacionInvalidaException("Un administrador no puede desactivar su propia cuenta");
        }

        if (usuario.getRol() == UserRol.ADMIN && usuario.isActivo()) {
            long adminsActivos = usuarioRepository.findByRol(UserRol.ADMIN).stream()
                    .filter(Usuario::isActivo)
                    .count();
            if (adminsActivos <= 1) {
                throw new OperacionInvalidaException("No se puede desactivar el único administrador activo del sistema");
            }
        }

        Usuario modificado = usuario.conActivo(!usuario.isActivo());
        return usuarioRepository.save(modificado);
    }

    @Override
    @Transactional
    public Usuario actualizarUsuario(UUID id, String nombre, String apellido, String telefono, String direccion, String razonSocial) {
        Usuario usuario = obtenerUsuarioPorId(id);
        Usuario modificado = usuario.conDatos(nombre, apellido, telefono);
        modificado = usuarioRepository.save(modificado);

        if (usuario.getRol() == UserRol.PROPIETARIO) {
            Optional<Propietario> propOpt = propietarioRepository.findById(id);
            if (propOpt.isPresent()) {
                Propietario p = propOpt.get();
                Propietario actualizado = new Propietario(
                        p.getId(),
                        p.getEmail(),
                        razonSocial != null && !razonSocial.isBlank() ? razonSocial : p.getRazonSocial(),
                        direccion != null && !direccion.isBlank() ? direccion : p.getDireccionFiscal(),
                        telefono != null && !telefono.isBlank() ? telefono : p.getTelefonoContacto(),
                        p.getRegimen(),
                        p.getMunicipio(),
                        p.getDepartamento(),
                        p.getVerificadoEn()
                );
                propietarioRepository.save(actualizado);
            }
        } else if (usuario.getRol() == UserRol.CLIENTE) {
            Optional<Cliente> cliOpt = clienteRepository.findById(id);
            if (cliOpt.isPresent()) {
                Cliente c = cliOpt.get();
                Cliente actualizado = new Cliente(
                        c.getId(),
                        c.getEmail(),
                        nombre,
                        apellido,
                        telefono,
                        direccion != null && !direccion.isBlank() ? direccion : c.getDireccionEntregaDefault(),
                        c.getDatosFacturacion()
                );
                clienteRepository.save(actualizado);
            }
        }

        return modificado;
    }

    @Override
    @Transactional
    public void eliminarUsuario(UUID id, UUID actorId) {
        Usuario usuario = obtenerUsuarioPorId(id);

        if (actorId != null && actorId.equals(id)) {
            throw new OperacionInvalidaException("Un administrador no puede eliminar su propia cuenta");
        }

        if (usuario.getRol() == UserRol.ADMIN) {
            long adminsActivos = usuarioRepository.findByRol(UserRol.ADMIN).stream()
                    .filter(Usuario::isActivo)
                    .count();
            if (adminsActivos <= 1) {
                throw new OperacionInvalidaException("No se puede eliminar el único administrador activo del sistema");
            }
        }

        if (usuario.getRol() == UserRol.PROPIETARIO) {
            var herramientas = propietarioRepository.findHerramientaIdsByPropietarioId(id);
            if (!herramientas.isEmpty()) {
                throw new OperacionInvalidaException("No es posible eliminar el proveedor porque posee " + herramientas.size() +
                        " herramientas registradas en la plataforma. Utilice la desactivación de cuenta en su lugar.");
            }
        }

        if (usuario.getRol() == UserRol.CLIENTE) {
            var reservas = reservaRepository.findByClienteId(id);
            if (!reservas.isEmpty()) {
                throw new OperacionInvalidaException("No es posible eliminar el cliente porque posee " + reservas.size() +
                        " reservas asociadas en el historial. Utilice la desactivación de cuenta en su lugar.");
            }
        }

        usuarioRepository.deleteById(id);
    }

    @Override
    public Propietario obtenerPropietarioPorUsuarioId(UUID usuarioId) {
        return propietarioRepository.findById(usuarioId).orElse(null);
    }

    @Override
    public Cliente obtenerClientePorUsuarioId(UUID usuarioId) {
        return clienteRepository.findById(usuarioId).orElse(null);
    }
}
