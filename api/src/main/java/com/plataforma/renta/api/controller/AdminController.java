package com.plataforma.renta.api.controller;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plataforma.renta.api.dto.AdminDtos.*;
import com.plataforma.renta.api.dto.AuthDtos.*;
import com.plataforma.renta.api.dto.AuthDtos.AdminCreateUserRequest;
import com.plataforma.renta.api.dto.AuthDtos.AdminUpdateUserRequest;
import com.plataforma.renta.api.dto.AuthDtos.AdminUserDetailResponse;
import com.plataforma.renta.api.dto.AuthDtos.UserResponse;
import com.plataforma.renta.application.service.AuthService;
import com.plataforma.renta.domain.model.Cliente;
import com.plataforma.renta.domain.model.Factura;
import com.plataforma.renta.domain.model.FacturaTipo;
import com.plataforma.renta.domain.model.Herramienta;
import com.plataforma.renta.domain.model.Pago;
import com.plataforma.renta.domain.model.PagoEstado;
import com.plataforma.renta.domain.model.Propietario;
import com.plataforma.renta.domain.model.Reserva;
import com.plataforma.renta.domain.model.UserRol;
import com.plataforma.renta.domain.model.Usuario;
import com.plataforma.renta.domain.port.out.FacturaRepository;
import com.plataforma.renta.domain.port.out.HerramientaRepository;
import com.plataforma.renta.domain.port.out.PagoRepository;
import com.plataforma.renta.domain.port.out.ReservaRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administración", description = "Operaciones restringidas para administradores de la plataforma")
public class AdminController {

    private final AuthService authService;
    private final ReservaRepository reservaRepository;
    private final FacturaRepository facturaRepository;
    private final PagoRepository pagoRepository;
    private final HerramientaRepository herramientaRepository;

    public AdminController(AuthService authService,
                           ReservaRepository reservaRepository,
                           FacturaRepository facturaRepository,
                           PagoRepository pagoRepository,
                           HerramientaRepository herramientaRepository) {
        this.authService = authService;
        this.reservaRepository = reservaRepository;
        this.facturaRepository = facturaRepository;
        this.pagoRepository = pagoRepository;
        this.herramientaRepository = herramientaRepository;
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Listar usuarios", description = "Obtiene todos los usuarios registrados con perfiles consolidados")
    public ResponseEntity<List<UserResponse>> listarUsuarios() {
        var users = authService.listarTodos().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/usuarios/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Consultar detalle de usuario", description = "Retorna la información completa de cuenta y perfil asociado")
    public ResponseEntity<AdminUserDetailResponse> obtenerDetalleUsuario(@PathVariable UUID id) {
        Usuario u = authService.obtenerUsuarioPorId(id);
        Propietario p = u.getRol() == UserRol.PROPIETARIO ? authService.obtenerPropietarioPorUsuarioId(id) : null;
        Cliente c = u.getRol() == UserRol.CLIENTE ? authService.obtenerClientePorUsuarioId(id) : null;

        String razonSocial = p != null ? p.getRazonSocial() : null;
        String dirFiscal = p != null ? p.getDireccionFiscal() : null;
        String dirEntrega = c != null ? c.getDireccionEntregaDefault() : null;

        return ResponseEntity.ok(new AdminUserDetailResponse(
                u.getId(),
                u.getEmail().getValor(),
                u.getNombre(),
                u.getApellido(),
                u.getRol().name(),
                u.getTelefono(),
                u.isActivo(),
                razonSocial,
                dirFiscal,
                dirEntrega,
                u.getCreadoEn()
        ));
    }

    @PostMapping("/usuarios")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Crear usuario administrativo", description = "Registra un cliente o proveedor directamente desde el panel")
    public ResponseEntity<UserResponse> crearUsuario(@Valid @RequestBody AdminCreateUserRequest req) {
        UserRol rol = UserRol.valueOf(req.rol().toUpperCase());
        var cmd = new AuthService.RegisterUserCommand(
                req.email(),
                req.password(),
                req.nombre(),
                req.apellido(),
                rol,
                req.telefono(),
                req.razonSocial(),
                req.direccionFiscal(),
                req.direccionEntregaDefault()
        );
        var res = authService.register(cmd);
        return ResponseEntity.ok(toResponse(res.usuario()));
    }

    @PutMapping("/usuarios/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Actualizar datos de usuario", description = "Modifica información de contacto, identificación o perfil")
    public ResponseEntity<UserResponse> actualizarUsuario(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateUserRequest req) {
        Usuario u = authService.actualizarUsuario(
                id,
                req.nombre(),
                req.apellido(),
                req.telefono(),
                req.direccion(),
                req.razonSocial()
        );
        return ResponseEntity.ok(toResponse(u));
    }

    @PutMapping("/usuarios/{id}/toggle-activo")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Cambiar estado de usuario", description = "Habilita o suspende la cuenta de un usuario")
    public ResponseEntity<UserResponse> toggleActivo(@PathVariable UUID id, Authentication auth) {
        UUID actorId = null;

        if (auth != null) {
            Usuario actor = authService.obtenerUsuarioPorEmail(auth.getName());
            actorId = actor.getId();
        }
        Usuario u = authService.toggleActivo(id, actorId);
        return ResponseEntity.ok(toResponse(u));
    }

    @DeleteMapping("/usuarios/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Eliminar usuario de forma segura", description = "Elimina la cuenta si no posee registros históricos ni financieros")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable UUID id, Authentication auth) {
        UUID actorId = null;
        if (auth != null) {
            Usuario actor = authService.obtenerUsuarioPorEmail(auth.getName());
            actorId = actor.getId();
        }
        authService.eliminarUsuario(id, actorId);
        return ResponseEntity.noContent().build();
    }

    private UserResponse toResponse(Usuario u) {
        Propietario p = u.getRol() == UserRol.PROPIETARIO ? authService.obtenerPropietarioPorUsuarioId(u.getId()) : null;
        Cliente c = u.getRol() == UserRol.CLIENTE ? authService.obtenerClientePorUsuarioId(u.getId()) : null;

        String razonSocial = p != null ? p.getRazonSocial() : null;
        String dir = p != null ? p.getDireccionFiscal() : (c != null ? c.getDireccionEntregaDefault() : null);

        return new UserResponse(
                u.getId(),
                u.getEmail().getValor(),
                u.getNombre(),
                u.getApellido(),
                u.getRol().name(),
                u.getTelefono(),
                u.isActivo(),
                razonSocial,
                dir
        );
    }

    @GetMapping("/reservas")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Listar reservas globalmente", description = "Obtiene todas las reservas con detalles de cliente, propietario, herramienta, daños y facturación asociada")
    public ResponseEntity<List<AdminReservaItemDto>> listarReservas() {
        var lista = reservaRepository.findAll().stream()
                .map(this::toAdminReservaItem)
                .sorted(Comparator.comparing(AdminReservaItemDto::fechaCreacion, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/facturas")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Listar facturas globalmente", description = "Obtiene todas las facturas de alquiler y de reparación con detalle financiero")
    public ResponseEntity<List<AdminFacturaItemDto>> listarFacturas() {
        var lista = facturaRepository.findAll().stream()
                .map(this::toAdminFacturaItem)
                .sorted(Comparator.comparing(AdminFacturaItemDto::fechaEmision, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return ResponseEntity.ok(lista);
    }

    @GetMapping("/pagos")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Listar pagos globalmente", description = "Obtiene todas las transacciones individuales de alquiler, reparación y reembolso")
    public ResponseEntity<List<AdminPagoItemDto>> listarPagos() {
        var lista = pagoRepository.findAll().stream()
                .map(this::toAdminPagoItem)
                .sorted(Comparator.comparing(AdminPagoItemDto::creadoEn, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return ResponseEntity.ok(lista);
    }

    private AdminReservaItemDto toAdminReservaItem(Reserva r) {
        String codigo = "RES-" + r.getId().toString().substring(0, 8).toUpperCase();
        String cliNombre = "Desconocido";
        String cliEmail = "N/A";
        try {
            Usuario uCli = authService.obtenerUsuarioPorId(r.getClienteId());
            if (uCli != null) {
                cliNombre = uCli.getNombre() + " " + uCli.getApellido();
                cliEmail = uCli.getEmail().getValor();
            }
        } catch (Exception ignored) {}

        UUID propId = null;
        String propNombre = "N/A";
        String propRazonSocial = "N/A";
        UUID herrId = null;
        String herrNombre = "Herramienta General";

        if (r.getDetalles() != null && !r.getDetalles().isEmpty()) {
            herrId = r.getDetalles().get(0).getHerramientaId();
            try {
                var hOpt = herramientaRepository.findById(herrId);
                if (hOpt.isPresent()) {
                    var h = hOpt.get();
                    herrNombre = h.getNombre();
                    propId = h.getPropietarioId();
                    Usuario uProp = authService.obtenerUsuarioPorId(propId);
                    if (uProp != null) {
                        propNombre = uProp.getNombre() + " " + uProp.getApellido();
                    }
                    Propietario p = authService.obtenerPropietarioPorUsuarioId(propId);
                    if (p != null) {
                        propRazonSocial = p.getRazonSocial();
                    }
                }
            } catch (Exception ignored) {}
        }

        boolean pagada = pagoRepository.existsCompletadoByReservaId(r.getId());
        BigDecimal costoRep = r.getCostoReparacion() != null ? r.getCostoReparacion().getMonto() : BigDecimal.ZERO;
        String descDano = r.getDescripcionDano();

        UUID facRepId = null;
        String facRepNum = null;
        String facRepEst = null;
        try {
            var facRepOpt = facturaRepository.findReparacionByReservaId(r.getId());
            if (facRepOpt.isPresent()) {
                var fac = facRepOpt.get();
                facRepId = fac.getId();
                facRepNum = fac.getNumeroFactura();
                facRepEst = fac.getEstado();
                if ((descDano == null || descDano.isBlank()) && fac.getDescripcionDano() != null) {
                    descDano = fac.getDescripcionDano();
                }
                if (costoRep.compareTo(BigDecimal.ZERO) == 0 && fac.getTotal() != null) {
                    costoRep = fac.getTotal().getMonto();
                }
            }
        } catch (Exception ignored) {}

        boolean presentaDanos = (costoRep.compareTo(BigDecimal.ZERO) > 0) || (descDano != null && !descDano.isBlank()) || (facRepId != null);

        return new AdminReservaItemDto(
                r.getId(),
                codigo,
                r.getClienteId(),
                cliNombre,
                cliEmail,
                propId,
                propNombre,
                propRazonSocial,
                herrId,
                herrNombre,
                r.getEstado().name(),
                r.getFechaInicio(),
                r.getFechaFin(),
                r.calcularSubtotal().getMonto(),
                r.calcularIva().getMonto(),
                r.calcularTotal().getMonto(),
                pagada,
                r.isSolicitudDevolucion(),
                r.getEstadoDevolucion(),
                presentaDanos,
                descDano,
                costoRep,
                facRepId,
                facRepNum,
                facRepEst,
                r.getFechaCreacion()
        );
    }

    private AdminFacturaItemDto toAdminFacturaItem(Factura f) {
        String codigoReserva = f.getReservaId() != null ? "RES-" + f.getReservaId().toString().substring(0, 8).toUpperCase() : null;
        String herrNombre = "Equipo de Construcción";
        String cliNombre = "N/A";
        String propNombre = "N/A";

        if (f.getReservaId() != null) {
            try {
                var rOpt = reservaRepository.findById(f.getReservaId());
                if (rOpt.isPresent()) {
                    var r = rOpt.get();
                    Usuario uCli = authService.obtenerUsuarioPorId(r.getClienteId());
                    if (uCli != null) cliNombre = uCli.getNombre() + " " + uCli.getApellido();

                    if (r.getDetalles() != null && !r.getDetalles().isEmpty()) {
                        var hOpt = herramientaRepository.findById(r.getDetalles().get(0).getHerramientaId());
                        if (hOpt.isPresent()) {
                            herrNombre = hOpt.get().getNombre();
                            Usuario uProp = authService.obtenerUsuarioPorId(hOpt.get().getPropietarioId());
                            if (uProp != null) propNombre = uProp.getNombre() + " " + uProp.getApellido();
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        String concepto;
        if (f.getTipo() == FacturaTipo.REPARACION) {
            concepto = "Cobro de reparación: " + herrNombre;
        } else if (f.getTipo() == FacturaTipo.NOTA_CREDITO) {
            concepto = "Nota de crédito por reembolso";
        } else {
            concepto = "Alquiler de " + herrNombre;
        }

        return new AdminFacturaItemDto(
                f.getId(),
                f.getReservaId(),
                codigoReserva,
                f.getNumeroFactura(),
                f.getTipo().name(),
                f.getSubtotal().getMonto(),
                f.getIva().getMonto(),
                f.getTotal().getMonto(),
                f.getMoneda(),
                f.getEstado(),
                herrNombre,
                cliNombre,
                propNombre,
                f.getDescripcionDano(),
                concepto,
                f.getPagoId(),
                f.getFechaEmision()
        );
    }

    private AdminPagoItemDto toAdminPagoItem(Pago p) {
        String codigoReserva = p.getReservaId() != null ? "RES-" + p.getReservaId().toString().substring(0, 8).toUpperCase() : null;
        String tipo;
        if (p.getFacturaId() != null) {
            tipo = "REPARACION";
        } else if (p.getEstado() == PagoEstado.REEMBOLSADO || p.getPagoOriginalId() != null) {
            tipo = "REEMBOLSO";
        } else {
            tipo = "ALQUILER";
        }

        String numFactura = null;
        if (p.getFacturaId() != null) {
            numFactura = facturaRepository.findById(p.getFacturaId()).map(Factura::getNumeroFactura).orElse(null);
        } else if (p.getReservaId() != null) {
            numFactura = facturaRepository.findOriginalByReservaId(p.getReservaId()).map(Factura::getNumeroFactura).orElse(null);
        }

        String herrNombre = "Equipo de Construcción";
        String cliNombre = "N/A";
        if (p.getReservaId() != null) {
            try {
                var rOpt = reservaRepository.findById(p.getReservaId());
                if (rOpt.isPresent()) {
                    var r = rOpt.get();
                    Usuario uCli = authService.obtenerUsuarioPorId(r.getClienteId());
                    if (uCli != null) cliNombre = uCli.getNombre() + " " + uCli.getApellido();
                    if (r.getDetalles() != null && !r.getDetalles().isEmpty()) {
                        var hOpt = herramientaRepository.findById(r.getDetalles().get(0).getHerramientaId());
                        if (hOpt.isPresent()) herrNombre = hOpt.get().getNombre();
                    }
                }
            } catch (Exception ignored) {}
        }

        return new AdminPagoItemDto(
                p.getId(),
                p.getReservaId(),
                codigoReserva,
                p.getFacturaId(),
                numFactura,
                tipo,
                p.getMonto().getMonto(),
                p.getMoneda(),
                p.getEstado().name(),
                p.getProveedorPago(),
                p.getTransaccionId(),
                p.getMotivo(),
                herrNombre,
                cliNombre,
                p.getFechaPago(),
                p.getCreadoEn()
        );
    }
}
