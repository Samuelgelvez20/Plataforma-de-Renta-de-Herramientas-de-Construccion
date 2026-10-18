package com.plataforma.renta.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class FacturaDtos {

    public record FacturaResponse(
            UUID id,
            UUID reservaId,
            String numeroFactura,
            String tipo,
            BigDecimal subtotal,
            BigDecimal iva,
            BigDecimal total,
            String datosEmisor,
            String datosReceptor,
            Instant fechaEmision
    ) {}
}
