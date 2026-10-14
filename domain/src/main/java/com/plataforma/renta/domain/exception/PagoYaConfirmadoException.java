package com.plataforma.renta.domain.exception;

public class PagoYaConfirmadoException extends RuntimeException {
    public PagoYaConfirmadoException(String mensaje) {
        super(mensaje);
    }
}