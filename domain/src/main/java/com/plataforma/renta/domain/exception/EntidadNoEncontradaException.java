package com.plataforma.renta.domain.exception;

public class EntidadNoEncontradaException extends RuntimeException {
    public EntidadNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}