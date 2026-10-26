package com.plataforma.renta.api.controller;

import com.plataforma.renta.api.dto.ReembolsoDtos.*;
import com.plataforma.renta.application.command.CrearReembolsoCommand;
import com.plataforma.renta.application.service.AuthService;
import com.plataforma.renta.application.service.PagoService;
import com.plataforma.renta.domain.model.Pago;
import com.plataforma.renta.domain.model.Usuario;
import com.plataforma.renta.domain.value.Dinero;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reembolsos")
@Tag(name = "Reembolsos", description = "Gestión de solicitudes y aprobación de reembolsos con emisión de notas crédito")
public class ReembolsoController {

    private final PagoService pagoService;
    private final AuthService authService;

    public ReembolsoController(PagoService pagoService, AuthService authService) {
        this.pagoService = pagoService;
        this.authService = authService;
    }

    @GetMapping
    @Operation(summary = "Listar reembolsos", description = "Obtiene el histórico de solicitudes de reembolso según los permisos del usuario")
    public ResponseEntity<List<ReembolsoResponse>> listar(Authentication auth) {
        Usuario actor = authService.obtenerUsuarioPorEmail(auth.getName());
        var reembolsos = pagoService.listarReembolsosPorUsuario(actor.getId(), actor.getRol()).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(reembolsos);
    }

    @PostMapping
    @Operation(summary = "Solicitar reembolso", description = "Crea una nueva solicitud de reembolso para una reserva pagada")
    public ResponseEntity<ReembolsoResponse> solicitar(@Valid @RequestBody SolicitarReembolsoRequest req, Authentication auth) {
        Usuario actor = authService.obtenerUsuarioPorEmail(auth.getName());

        UUID targetReservaId = req.reservaId();
        Pago pagoOriginal = null;
        if (req.pagoId() != null) {
            pagoOriginal = pagoService.obtenerPorId(req.pagoId());
            targetReservaId = pagoOriginal.getReservaId();
        }

        Dinero monto;
        if (req.montoConIva() != null) {
            monto = Dinero.cop(req.montoConIva());
        } else if (pagoOriginal != null) {
            monto = pagoOriginal.getMonto();
        } else {
            monto = Dinero.cop(java.math.BigDecimal.valueOf(1000));
        }

        var cmd = new CrearReembolsoCommand(
                targetReservaId,
                monto,
                req.motivo() != null ? req.motivo() : "Solicitud de reembolso por cliente"
        );

        Pago reembolso = pagoService.crearReembolso(cmd, actor.getId(), actor.getRol());
        return ResponseEntity.ok(toResponse(reembolso));
    }

    @PutMapping("/{id}/resolver")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'PROPIETARIO')")
    @Operation(summary = "Resolver reembolso", description = "Aprueba o rechaza una solicitud de reembolso pendiente")
    public ResponseEntity<ReembolsoResponse> resolver(@PathVariable UUID id, @RequestBody ResolverReembolsoRequest req, Authentication auth) {
        Usuario actor = authService.obtenerUsuarioPorEmail(auth.getName());
        if (req.aprobar()) {
            pagoService.aprobarReembolso(id, actor.getId(), actor.getRol());
        } else {
            pagoService.rechazarReembolso(id, req.resolucion(), actor.getId(), actor.getRol());
        }
        Pago p = pagoService.obtenerPorId(id);
        return ResponseEntity.ok(toResponse(p));
    }

    private ReembolsoResponse toResponse(Pago p) {
        String estadoPublico;
        if (p.getPagoOriginalId() != null) {
            if (p.getEstado() == com.plataforma.renta.domain.model.PagoEstado.REEMBOLSADO) {
                estadoPublico = "APROBADO";
            } else if (p.getEstado() == com.plataforma.renta.domain.model.PagoEstado.FALLIDO) {
                estadoPublico = "RECHAZADO";
            } else {
                estadoPublico = p.getEstado().name();
            }
        } else {
            estadoPublico = p.getEstado().name();
        }

        String resolucion = null;
        if (p.getMotivo() != null && p.getMotivo().contains(" | Rechazo: ")) {
            String[] parts = p.getMotivo().split(" \\| Rechazo: ", 2);
            resolucion = parts.length > 1 ? parts[1] : null;
        }

        return new ReembolsoResponse(
                p.getId(),
                p.getReservaId(),
                p.getPagoOriginalId(),
                p.getMonto().getMonto(),
                estadoPublico,
                p.getMotivo(),
                p.getCreadoEn(),
                resolucion
        );
    }
}
