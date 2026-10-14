package com.plataforma.renta.domain.value;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class NitValidatorTest {

    @ParameterizedTest
    @CsvSource({
            "123456789-6, true",
            "900123456-8, true",
            "900123456-1, false",
            "900123456, false",
            "ABC-1, false",
            "INVALID, false"
    })
    @DisplayName("Validar NIT con dígito de verificación módulo 11")
    void validarNit(String nit, boolean esperado) {
        assertEquals(esperado, Nit.esValido(nit), "NIT: " + nit);
    }

    @ParameterizedTest
    @CsvSource({
            "123456789-6",
            "900123456-8"
    })
    @DisplayName("NIT válidos no deben lanzar excepción")
    void nitValidosNoLanzanExcepcion(String nit) {
        assertDoesNotThrow(() -> Nit.of(nit));
    }

    @ParameterizedTest
    @CsvSource({
            "900123456-1",
            "900123456",
            "ABC-1",
            "INVALID"
    })
    @DisplayName("NIT inválidos deben lanzar excepción")
    void nitInvalidosLanzanExcepcion(String nit) {
        assertThrows(IllegalArgumentException.class, () -> Nit.of(nit));
    }
}
