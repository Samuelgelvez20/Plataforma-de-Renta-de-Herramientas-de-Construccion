package com.plataforma.renta.application.command;

import java.util.UUID;

public record CancelarReservaCommand(
        UUID reservaId,
        String motivo,
        UUID canceladoPorUsuarioId
) {}