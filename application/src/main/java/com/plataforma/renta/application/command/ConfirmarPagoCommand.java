package com.plataforma.renta.application.command;

import com.plataforma.renta.domain.value.Dinero;

import java.util.UUID;

public record ConfirmarPagoCommand(
        UUID reservaId,
        Dinero monto,
        String proveedorPago,
        String transaccionId
) {}