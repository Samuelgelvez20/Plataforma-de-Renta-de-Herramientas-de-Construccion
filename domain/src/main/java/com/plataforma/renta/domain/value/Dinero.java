package com.plataforma.renta.domain.value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Dinero {

    public static final String MONEDA_COLOMBIA = "COP";
    public static final int ESCALA = 2;

    private final BigDecimal monto;
    private final String moneda;

    private Dinero(BigDecimal monto, String moneda) {
        this.monto = monto.setScale(ESCALA, RoundingMode.HALF_UP);
        this.moneda = moneda;
    }

    public static Dinero cop(BigDecimal monto) {
        return new Dinero(monto, MONEDA_COLOMBIA);
    }

    public static Dinero cero() {
        return new Dinero(BigDecimal.ZERO, MONEDA_COLOMBIA);
    }

    public Dinero sumar(Dinero otro) {
        validarMismaMoneda(otro);
        return new Dinero(this.monto.add(otro.monto), this.moneda);
    }

    public Dinero restar(Dinero otro) {
        validarMismaMoneda(otro);
        return new Dinero(this.monto.subtract(otro.monto), this.moneda);
    }

    public Dinero multiplicar(BigDecimal factor) {
        return new Dinero(this.monto.multiply(factor), this.moneda);
    }

    public Dinero multiplicar(int factor) {
        return new Dinero(this.monto.multiply(BigDecimal.valueOf(factor)), this.moneda);
    }

    public Dinero dividir(BigDecimal divisor, int escala, RoundingMode modo) {
        return new Dinero(this.monto.divide(divisor, escala, modo), this.moneda);
    }

    public boolean esMayorQue(Dinero otro) {
        validarMismaMoneda(otro);
        return this.monto.compareTo(otro.monto) > 0;
    }

    public boolean esMenorQue(Dinero otro) {
        validarMismaMoneda(otro);
        return this.monto.compareTo(otro.monto) < 0;
    }

    public boolean esMenorOIgualQue(Dinero otro) {
        validarMismaMoneda(otro);
        return this.monto.compareTo(otro.monto) <= 0;
    }

    public boolean esCero() { return monto.compareTo(BigDecimal.ZERO) == 0; }
    public boolean esPositivo() { return monto.compareTo(BigDecimal.ZERO) > 0; }

    public BigDecimal getMonto() { return monto; }
    public String getMoneda() { return moneda; }

    private void validarMismaMoneda(Dinero otro) {
        if (!this.moneda.equals(otro.moneda)) {
            throw new IllegalArgumentException("No se pueden operar dineros de distinta moneda: " + this.moneda + " vs " + otro.moneda);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dinero)) return false;
        Dinero dinero = (Dinero) o;
        return Objects.equals(monto, dinero.monto) && Objects.equals(moneda, dinero.moneda);
    }

    @Override
    public int hashCode() { return Objects.hash(monto, moneda); }

    @Override
    public String toString() { return monto + " " + moneda; }
}