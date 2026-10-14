package com.plataforma.renta.domain.exception;

public class EstadoInvalidoException extends RuntimeException {
    public EstadoInvalidoException(String mensaje) {
        super(mensaje);
    }
}