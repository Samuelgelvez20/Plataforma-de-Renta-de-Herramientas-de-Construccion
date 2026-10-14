package com.plataforma.renta.domain.exception;

public class ReservaVencidaException extends RuntimeException {
    public ReservaVencidaException(String mensaje) {
        super(mensaje);
    }
}