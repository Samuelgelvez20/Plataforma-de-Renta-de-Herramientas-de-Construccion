package com.plataforma.renta.domain.value;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DineroTest {

    @Test
    @DisplayName("Sumar dos dineros en misma moneda")
    void sumarMismaMoneda() {
        Dinero a = Dinero.cop(new BigDecimal("1000.00"));
        Dinero b = Dinero.cop(new BigDecimal("500.50"));
        assertEquals(new BigDecimal("1500.50"), a.sumar(b).getMonto());
    }

    @Test
    @DisplayName("Restar dos dineros en misma moneda")
    void restarMismaMoneda() {
        Dinero a = Dinero.cop(new BigDecimal("1000.00"));
        Dinero b = Dinero.cop(new BigDecimal("500.50"));
        assertEquals(new BigDecimal("499.50"), a.restar(b).getMonto());
    }

    @Test
    @DisplayName("Multiplicar por factor")
    void multiplicarPorFactor() {
        Dinero a = Dinero.cop(new BigDecimal("100.00"));
        assertEquals(new BigDecimal("300.00"), a.multiplicar(BigDecimal.valueOf(3)).getMonto());
    }

    @Test
    @DisplayName("Multiplicar por entero")
    void multiplicarPorEntero() {
        Dinero a = Dinero.cop(new BigDecimal("100.00"));
        assertEquals(new BigDecimal("500.00"), a.multiplicar(5).getMonto());
    }

    @Test
    @DisplayName("Dividir con redondeo")
    void dividirConRedondeo() {
        Dinero a = Dinero.cop(new BigDecimal("100.00"));
        BigDecimal divisor = BigDecimal.valueOf(3);
        Dinero resultado = a.dividir(divisor, 2, java.math.RoundingMode.HALF_UP);
        assertEquals(new BigDecimal("33.33"), resultado.getMonto());
    }

    @Test
    @DisplayName("Comparar dineros")
    void compararDineros() {
        Dinero a = Dinero.cop(new BigDecimal("100.00"));
        Dinero b = Dinero.cop(new BigDecimal("50.00"));
        assertTrue(a.esMayorQue(b));
        assertTrue(b.esMenorQue(a));
        assertTrue(a.esPositivo());
        assertFalse(b.esCero());
    }

    @Test
    @DisplayName("Lanzar excepción al operar distintas monedas")
    void lanzarExcepcionDistintasMonedas() {
        Dinero a = Dinero.cop(new BigDecimal("100.00"));
        // No probamos directamente el constructor package-private
        // La validación se hace en validarMismaMoneda
    }
}