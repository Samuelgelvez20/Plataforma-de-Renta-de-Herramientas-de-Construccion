package com.plataforma.renta.api.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ReembolsoDtos {

    public record SolicitarReembolsoRequest(
            UUID reservaId,
            UUID pagoId,
            BigDecimal montoConIva,
            String motivo
    ) {}

    public record ResolverReembolsoRequest(
            boolean aprobar,
            String resolucion
    ) {}

    public record ReembolsoResponse(
            UUID id,
            UUID reservaId,
            UUID pagoOriginalId,
            BigDecimal monto,
            String estado,
            String motivo,
            Instant creadoEn,
            String resolucion
    ) {
        public ReembolsoResponse(UUID id, UUID reservaId, UUID pagoOriginalId, BigDecimal monto, String estado, String motivo, Instant creadoEn) {
            this(id, reservaId, pagoOriginalId, monto, estado, motivo, creadoEn, null);
        }
    }
}
