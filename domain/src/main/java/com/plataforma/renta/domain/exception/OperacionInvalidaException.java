package com.plataforma.renta.domain.exception;

public class OperacionInvalidaException extends RuntimeException {
    public OperacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}