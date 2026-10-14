package com.plataforma.renta.domain.value;

import java.util.Objects;

public final class Nit {

    private final String valor;

    private Nit(String valor) {
        this.valor = valor;
    }

    public static Nit of(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            throw new IllegalArgumentException("NIT no puede ser nulo o vacío");
        }
        String trimmed = valor.trim().toUpperCase();
        if (!trimmed.matches("^\\d{1,15}-\\d$")) {
            throw new IllegalArgumentException("Formato de NIT inválido (esperado NÚMERO-DV): " + valor);
        }
        if (!validarDigitoVerificacion(trimmed)) {
            throw new IllegalArgumentException("Dígito de verificación del NIT inválido: " + valor);
        }
        return new Nit(trimmed);
    }

    private static boolean validarDigitoVerificacion(String nit) {
        String[] partes = nit.split("-");
        String numero = partes[0];
        int dvIngresado = Integer.parseInt(partes[1]);
        return calcularDV(numero) == dvIngresado;
    }

    private static int calcularDV(String numero) {
        int[] pesos = {3, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47, 53, 59, 67, 71};
        int suma = 0;
        for (int i = 0; i < numero.length(); i++) {
            int digito = Character.getNumericValue(numero.charAt(numero.length() - 1 - i));
            suma += digito * pesos[i];
        }
        int residuo = suma % 11;
        return residuo > 1 ? 11 - residuo : residuo;
    }

    public static boolean esValido(String nit) {
        try {
            Nit.of(nit);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String getValor() { return valor; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Nit)) return false;
        Nit nit = (Nit) o;
        return Objects.equals(valor, nit.valor);
    }

    @Override
    public int hashCode() { return Objects.hash(valor); }

    @Override
    public String toString() { return valor; }
}