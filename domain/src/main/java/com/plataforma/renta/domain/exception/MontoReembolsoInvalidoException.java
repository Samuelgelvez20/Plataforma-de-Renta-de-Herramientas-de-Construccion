package com.plataforma.renta.domain.exception;

public class MontoReembolsoInvalidoException extends RuntimeException {
    public MontoReembolsoInvalidoException(String mensaje) {
        super(mensaje);
    }
}