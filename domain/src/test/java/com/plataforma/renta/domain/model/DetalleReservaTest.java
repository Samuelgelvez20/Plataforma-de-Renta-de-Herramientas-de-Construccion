package com.plataforma.renta.domain.model;

import com.plataforma.renta.domain.value.Dinero;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DetalleReservaTest {

    @ParameterizedTest
    @CsvSource({
            "10000, 2, 3, 19.00, true, 60000, 11400, 71400",
            "50000, 1, 3, 19.00, true, 150000, 28500, 178500",
            "10000, 1, 1, 19.00, false, 10000, 0, 10000",
            "10000, 1, 1, 0.00, true, 10000, 0, 10000"
    })
    @DisplayName("Crear detalle con cálculo fiscal correcto")
    void crearDetalleCalculoFiscal(BigDecimal precioDia, int cantidad, int dias,
                                   BigDecimal ivaPorcentaje, boolean ivaAplica,
                                   BigDecimal subtotalEsperado, BigDecimal ivaEsperado, BigDecimal totalEsperado) {
        LocalDate inicio = LocalDate.now();
        LocalDate fin = inicio.plusDays(dias - 1);

        DetalleReserva detalle = DetalleReserva.crear(
                UUID.randomUUID(), UUID.randomUUID(), cantidad,
                Dinero.cop(precioDia), inicio, fin,
                ivaPorcentaje, ivaAplica
        );

        assertEquals(subtotalEsperado.setScale(2), detalle.getSubtotal().getMonto());
        assertEquals(ivaEsperado.setScale(2), detalle.getIva().getMonto());
        assertEquals(totalEsperado.setScale(2), detalle.getTotal().getMonto());
        assertEquals(ivaPorcentaje, detalle.getIvaPorcentaje());
        assertEquals(ivaAplica, detalle.isIvaAplica());
    }

    @Test
    @DisplayName("Días inclusivos: mismo día = 1")
    void diasInclusivosMismoDia() {
        LocalDate hoy = LocalDate.now();
        DetalleReserva detalle = DetalleReserva.crear(
                UUID.randomUUID(), UUID.randomUUID(), 1,
                Dinero.cop(new BigDecimal("10000")), hoy, hoy,
                new BigDecimal("19.00"), true
        );
        assertEquals(new BigDecimal("10000.00"), detalle.getSubtotal().getMonto());
    }

    @Test
    @DisplayName("Días inclusivos: rango de 3 días")
    void diasInclusivosTresDias() {
        LocalDate inicio = LocalDate.now();
        LocalDate fin = inicio.plusDays(2); // 3 días: hoy, mañana, pasado mañana
        DetalleReserva detalle = DetalleReserva.crear(
                UUID.randomUUID(), UUID.randomUUID(), 1,
                Dinero.cop(new BigDecimal("10000")), inicio, fin,
                new BigDecimal("19.00"), true
        );
        assertEquals(new BigDecimal("30000.00"), detalle.getSubtotal().getMonto());
    }
}