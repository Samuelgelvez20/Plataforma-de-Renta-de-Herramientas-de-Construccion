package com.plataforma.renta.domain.exception;

import java.util.UUID;

public class StockInsuficienteException extends RuntimeException {
    private final UUID herramientaId;
    private final int disponible;
    private final int solicitado;

    public StockInsuficienteException(UUID herramientaId, int disponible, int solicitado) {
        super("Stock insuficiente para herramienta " + herramientaId + ": disponible=" + disponible + ", solicitado=" + solicitado);
        this.herramientaId = herramientaId;
        this.disponible = disponible;
        this.solicitado = solicitado;
    }

    public UUID getHerramientaId() { return herramientaId; }
    public int getDisponible() { return disponible; }
    public int getSolicitado() { return solicitado; }
}