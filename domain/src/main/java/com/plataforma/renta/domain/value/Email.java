package com.plataforma.renta.domain.value;

import java.util.Objects;

public final class Email {

    private final String valor;

    private Email(String valor) {
        this.valor = valor;
    }

    public static Email of(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("Email no puede ser nulo o vacío");
        }
        String trimmed = valor.trim().toLowerCase();
        if (!trimmed.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("Formato de email inválido: " + valor);
        }
        return new Email(trimmed);
    }

    public String getValor() { return valor; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Email)) return false;
        Email email = (Email) o;
        return Objects.equals(valor, email.valor);
    }

    @Override
    public int hashCode() { return Objects.hash(valor); }

    @Override
    public String toString() { return valor; }
}