package com.plataforma.renta.domain.exception;

public class MontoInvalidoException extends RuntimeException {
    public MontoInvalidoException(String mensaje) {
        super(mensaje);
    }
}