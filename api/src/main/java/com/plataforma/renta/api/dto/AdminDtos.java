package com.plataforma.renta.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class AdminDtos {

    public record AdminReservaItemDto(
            UUID id,
            String codigoReserva,
            UUID clienteId,
            String clienteNombre,
            String clienteEmail,
            UUID propietarioId,
            String propietarioNombre,
            String propietarioRazonSocial,
            UUID herramientaId,
            String herramientaNombre,
            String estado,
            LocalDate fechaInicio,
            LocalDate fechaFin,
            BigDecimal subtotal,
            BigDecimal iva,
            BigDecimal total,
            boolean pagada,
            boolean solicitudDevolucion,
            String estadoDevolucion,
            boolean presentaDanos,
            String descripcionDano,
            BigDecimal costoReparacion,
            UUID facturaReparacionId,
            String facturaReparacionNumero,
            String facturaReparacionEstado,
            Instant fechaCreacion
    ) {}

    public record AdminFacturaItemDto(
            UUID id,
            UUID reservaId,
            String codigoReserva,
            String numeroFactura,
            String tipo,
            BigDecimal subtotal,
            BigDecimal iva,
            BigDecimal total,
            String moneda,
            String estado,
            String herramientaNombre,
            String clienteNombre,
            String propietarioNombre,
            String descripcionDano,
            String concepto,
            UUID pagoId,
            Instant fechaEmision
    ) {}

    public record AdminPagoItemDto(
            UUID id,
            UUID reservaId,
            String codigoReserva,
            UUID facturaId,
            String numeroFactura,
            String tipo,
            BigDecimal monto,
            String moneda,
            String estado,
            String proveedorPago,
            String transaccionId,
            String motivo,
            String herramientaNombre,
            String clienteNombre,
            Instant fechaPago,
            Instant creadoEn
    ) {}
}
