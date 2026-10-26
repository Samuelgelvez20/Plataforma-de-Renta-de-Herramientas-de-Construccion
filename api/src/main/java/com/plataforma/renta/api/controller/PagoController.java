package com.plataforma.renta.api.controller;

import com.plataforma.renta.api.dto.FacturaDtos.FacturaResponse;
import com.plataforma.renta.api.dto.PagoDtos.*;
import com.plataforma.renta.application.command.ConfirmarPagoCommand;
import com.plataforma.renta.application.service.AuthService;
import com.plataforma.renta.application.service.FacturaService;
import com.plataforma.renta.application.service.PagoService;
import com.plataforma.renta.application.service.ReservaService;
import com.plataforma.renta.domain.exception.PagoYaConfirmadoException;
import com.plataforma.renta.domain.model.Factura;
import com.plataforma.renta.domain.model.Pago;
import com.plataforma.renta.domain.model.PagoEstado;
import com.plataforma.renta.domain.model.Reserva;
import com.plataforma.renta.domain.model.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pagos")
@Tag(name = "Pagos", description = "Procesamiento idempotente de pagos y emisión automática de comprobantes")
public class PagoController {

    private final PagoService pagoService;
    private final ReservaService reservaService;
    private final FacturaService facturaService;
    private final AuthService authService;

    public PagoController(PagoService pagoService,
                          ReservaService reservaService,
                          FacturaService facturaService,
                          AuthService authService) {
        this.pagoService = pagoService;
        this.reservaService = reservaService;
        this.facturaService = facturaService;
        this.authService = authService;
    }

    @PostMapping("/procesar")
    @Operation(summary = "Procesar pago", description = "Confirma el pago de una reserva emitiendo factura electrónica con control de idempotencia")
    public ResponseEntity<ProcesarPagoResponse> procesarPago(@Valid @RequestBody ProcesarPagoRequest req, Authentication auth) {
        Usuario actor = authService.obtenerUsuarioPorEmail(auth.getName());

        String proveedor = req.metodo() != null ? req.metodo() : "SIMULADO";
        String txId = req.claveIdempotencia() != null ? req.claveIdempotencia() : UUID.randomUUID().toString();

        if (req.facturaId() != null) {
            Pago pagoRep;
            try {
                pagoRep = pagoService.confirmarPagoReparacion(req.facturaId(), proveedor, txId, actor.getId(), actor.getRol());
            } catch (PagoYaConfirmadoException ex) {
                Pago existingPago = pagoService.listarTodos().stream()
                        .filter(p -> req.facturaId().equals(p.getFacturaId()) && p.getEstado() == PagoEstado.COMPLETADO)
                        .findFirst()
                        .orElseThrow(() -> ex);
                Factura updatedFactura = facturaService.obtenerPorId(req.facturaId());
                return ResponseEntity.ok(new ProcesarPagoResponse(
                        toPagoResponse(existingPago),
                        toFacturaResponse(updatedFactura),
                        "Pago de reparación previamente confirmado (idempotente)"
                ));
            }
            Factura updatedFactura = facturaService.obtenerPorId(req.facturaId());
            return ResponseEntity.ok(new ProcesarPagoResponse(
                    toPagoResponse(pagoRep),
                    toFacturaResponse(updatedFactura),
                    "Pago de reparación procesado exitosamente"
            ));
        }

        if (req.reservaId() == null) {
            throw new com.plataforma.renta.domain.exception.OperacionInvalidaException("Debe especificar reservaId o facturaId");
        }

        Reserva reserva = reservaService.obtenerPorId(req.reservaId());

        var cmd = new ConfirmarPagoCommand(
                req.reservaId(),
                reserva.calcularTotal(),
                proveedor,
                txId
        );

        Pago pago;
        try {
            pago = pagoService.confirmarPago(cmd, actor.getId(), actor.getRol());
        } catch (PagoYaConfirmadoException ex) {
            Pago existingPago = pagoService.listarTodos().stream()
                    .filter(p -> p.getReservaId().equals(req.reservaId()) && p.getEstado() == PagoEstado.COMPLETADO && p.getFacturaId() == null)
                    .findFirst()
                    .orElseThrow(() -> ex);

            Factura existingFactura = facturaService.listarPorReserva(req.reservaId()).stream()
                    .filter(f -> f.getTipo() == com.plataforma.renta.domain.model.FacturaTipo.FACTURA)
                    .findFirst()
                    .orElse(null);

            return ResponseEntity.ok(new ProcesarPagoResponse(
                    toPagoResponse(existingPago),
                    existingFactura != null ? toFacturaResponse(existingFactura) : null,
                    "Pago previamente confirmado (idempotente)"
            ));
        }

        Factura factura = facturaService.listarPorReserva(req.reservaId()).stream()
                .filter(f -> f.getTipo() == com.plataforma.renta.domain.model.FacturaTipo.FACTURA)
                .findFirst()
                .orElse(null);

        var facturaResp = factura != null ? toFacturaResponse(factura) : null;
        var pagoResp = toPagoResponse(pago);

        return ResponseEntity.ok(new ProcesarPagoResponse(
                pagoResp,
                facturaResp,
                "Pago procesado exitosamente"
        ));
    }

    private PagoResponse toPagoResponse(Pago p) {
        return new PagoResponse(
                p.getId(),
                p.getReservaId(),
                p.getMonto().getMonto(),
                p.getEstado().name(),
                p.getProveedorPago(),
                p.getTransaccionId(),
                p.getFechaPago()
        );
    }

    private FacturaResponse toFacturaResponse(Factura f) {
        String concepto;
        if (f.getTipo() == com.plataforma.renta.domain.model.FacturaTipo.REPARACION) {
            concepto = "Costos de reparación";
        } else if (f.getTipo() == com.plataforma.renta.domain.model.FacturaTipo.NOTA_CREDITO) {
            concepto = "Nota de crédito por reembolso";
        } else {
            concepto = "Alquiler de herramienta y equipo";
        }
        return new FacturaResponse(
                f.getId(),
                f.getReservaId(),
                f.getNumeroFactura(),
                f.getTipo().name(),
                f.getSubtotal().getMonto(),
                f.getIva().getMonto(),
                f.getTotal().getMonto(),
                f.getDatosEmisor(),
                f.getDatosReceptor(),
                f.getFechaEmision(),
                f.getEstado(),
                f.getDescripcionDano(),
                null,
                concepto
        );
    }
}
