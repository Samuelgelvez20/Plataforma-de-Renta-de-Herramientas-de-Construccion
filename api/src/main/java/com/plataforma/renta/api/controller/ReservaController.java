package com.plataforma.renta.api.controller;

import com.plataforma.renta.api.dto.ReservaDtos.*;
import com.plataforma.renta.application.command.CrearReservaCommand;
import com.plataforma.renta.application.command.CrearReservaCommand.DetalleCommand;
import com.plataforma.renta.application.service.AuthService;
import com.plataforma.renta.application.service.ReservaService;
import com.plataforma.renta.domain.model.DetalleReserva;
import com.plataforma.renta.domain.model.Reserva;
import com.plataforma.renta.domain.model.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reservas")
@Tag(name = "Reservas", description = "Ciclo de vida de reservas: solicitud, confirmación, entrega y devolución")
public class ReservaController {

    private final ReservaService reservaService;
    private final AuthService authService;

    public ReservaController(ReservaService reservaService, AuthService authService) {
        this.reservaService = reservaService;
        this.authService = authService;
    }

    @GetMapping
    @Operation(summary = "Listar reservas", description = "Obtiene las reservas asociadas al usuario autenticado según su rol")
    public ResponseEntity<List<ReservaResponse>> listar(Authentication auth) {
        Usuario actor = authService.obtenerUsuarioPorEmail(auth.getName());
        var lista = reservaService.listarPorUsuario(actor.getId(), actor.getRol()).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(lista);
    }

    @PostMapping
    @Operation(summary = "Crear reserva", description = "Solicita la reserva de una herramienta para un rango de fechas")
    public ResponseEntity<ReservaResponse> crear(@Valid @RequestBody CrearReservaRequest req, Authentication auth) {
        Usuario actor = authService.obtenerUsuarioPorEmail(auth.getName());
        int cant = req.cantidad() > 0 ? req.cantidad() : 1;

        var cmd = new CrearReservaCommand(
                actor.getId(),
                req.fechaInicio(),
                req.fechaFin(),
                List.of(new DetalleCommand(req.herramientaId(), cant))
        );

        Reserva r = reservaService.crearReserva(cmd, actor.getId(), actor.getRol());
        return ResponseEntity.ok(toResponse(r));
    }

    @PutMapping("/{id}/aceptar")
    @Operation(summary = "Aceptar reserva", description = "El propietario o admin aprueba la solicitud de reserva")
    public ResponseEntity<ReservaResponse> aceptar(@PathVariable UUID id, Authentication auth) {
        Usuario actor = authService.obtenerUsuarioPorEmail(auth.getName());
        Reserva r = reservaService.aceptarReserva(id, actor.getId(), actor.getRol());
        return ResponseEntity.ok(toResponse(r));
    }

    @PutMapping("/{id}/rechazar")
    @Operation(summary = "Rechazar reserva", description = "Cancela la reserva especificando el motivo")
    public ResponseEntity<ReservaResponse> rechazar(@PathVariable UUID id, @RequestBody(required = false) RechazarReservaRequest req, Authentication auth) {
        Usuario actor = authService.obtenerUsuarioPorEmail(auth.getName());
        String motivo = req != null ? req.motivo() : "Rechazada por el propietario";
        Reserva r = reservaService.rechazarReserva(id, motivo, actor.getId(), actor.getRol());
        return ResponseEntity.ok(toResponse(r));
    }

    @PutMapping("/{id}/entregar")
    @Operation(summary = "Entregar equipo", description = "Marca el equipo como entregado al cliente (EN_CURSO)")
    public ResponseEntity<ReservaResponse> entregar(@PathVariable UUID id, Authentication auth) {
        Usuario actor = authService.obtenerUsuarioPorEmail(auth.getName());
        Reserva r = reservaService.entregarEquipo(id, actor.getId(), actor.getRol());
        return ResponseEntity.ok(toResponse(r));
    }

    @PutMapping("/{id}/devolver")
    @Operation(summary = "Devolver equipo", description = "Finaliza la renta registrando inspección de daños si aplica")
    public ResponseEntity<ReservaResponse> devolver(@PathVariable UUID id, @RequestBody(required = false) DevolverReservaRequest req, Authentication auth) {
        Usuario actor = authService.obtenerUsuarioPorEmail(auth.getName());
        boolean danos = req != null && req.presentaDanos();
        String desc = req != null ? req.descripcionDano() : null;
        var costo = req != null && req.costoReparacion() != null ? java.math.BigDecimal.valueOf(req.costoReparacion().doubleValue()) : null;

        Reserva r = reservaService.devolverEquipo(id, danos, desc, costo, actor.getId(), actor.getRol());
        return ResponseEntity.ok(toResponse(r));
    }

    private ReservaResponse toResponse(Reserva r) {
        var detalles = r.getDetalles().stream()
                .map(d -> new DetalleReservaResponse(
                        d.getId(),
                        d.getHerramientaId(),
                        d.getCantidad(),
                        d.getPrecioDiaAcordado().getMonto(),
                        d.getSubtotal().getMonto(),
                        d.getIva().getMonto(),
                        d.getTotal().getMonto()
                ))
                .toList();

        return new ReservaResponse(
                r.getId(),
                r.getClienteId(),
                r.getEstado().name(),
                r.getFechaInicio(),
                r.getFechaFin(),
                r.calcularTotal().getMonto(),
                r.calcularSubtotal().getMonto(),
                r.calcularIva().getMonto(),
                detalles
        );
    }
}
