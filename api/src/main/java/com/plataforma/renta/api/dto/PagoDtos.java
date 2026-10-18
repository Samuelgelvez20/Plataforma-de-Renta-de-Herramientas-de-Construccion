package com.plataforma.renta.api.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PagoDtos {

    public record ProcesarPagoRequest(
            @NotNull UUID reservaId,
            String metodo,
            String claveIdempotencia
    ) {}

    public record PagoResponse(
            UUID id,
            UUID reservaId,
            BigDecimal monto,
            String estado,
            String proveedorPago,
            String transaccionId,
            Instant fechaPago
    ) {}

    public record ProcesarPagoResponse(
            PagoResponse pago,
            FacturaDtos.FacturaResponse factura,
            String mensaje
    ) {}
}
