package com.plataforma.renta.application.command;

import com.plataforma.renta.domain.value.Dinero;

import java.util.UUID;

public record CrearReembolsoCommand(
        UUID reservaId,
        Dinero montoConIva,
        String motivo
) {}