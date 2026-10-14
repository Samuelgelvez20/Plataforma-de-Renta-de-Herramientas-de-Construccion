package com.plataforma.renta.domain.model;

import com.plataforma.renta.domain.exception.StockInsuficienteException;
import com.plataforma.renta.domain.value.Dinero;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReservaTest {

    @Test
    void calcularTotalSumaDetalles() {
        Reserva reserva = new Reserva(
                UUID.randomUUID(), UUID.randomUUID(), ReservaEstado.PENDIENTE,
                LocalDate.now(), LocalDate.now().plusDays(2),
                Instant.now(), Instant.now().plus(Duration.ofMinutes(30))
        );

        DetalleReserva d1 = DetalleReserva.crear(
                reserva.getId(), UUID.randomUUID(), 2,
                Dinero.cop(new BigDecimal("10000")), 
                LocalDate.now(), LocalDate.now().plusDays(2),
                new BigDecimal("19.00"), true
        );
        DetalleReserva d2 = DetalleReserva.crear(
                reserva.getId(), UUID.randomUUID(), 1,
                Dinero.cop(new BigDecimal("50000")), 
                LocalDate.now(), LocalDate.now().plusDays(2),
                new BigDecimal("19.00"), true
        );

        reserva.agregarDetalle(d1);
        reserva.agregarDetalle(d2);

        // d1: 10000 * 2 * 3 = 60000 + IVA 11400 = 71400
        // d2: 50000 * 1 * 3 = 150000 + IVA 28500 = 178500
        // Total = 249900
        assertEquals(new BigDecimal("249900.00"), reserva.calcularTotal().getMonto());
    }

    @Test
    void transicionesEstadoValidas() {
        Reserva reserva = new Reserva(
                UUID.randomUUID(), UUID.randomUUID(), ReservaEstado.PENDIENTE,
                LocalDate.now(), LocalDate.now().plusDays(2),
                Instant.now(), Instant.now().plus(Duration.ofMinutes(30))
        );

        // PENDIENTE -> CONFIRMADA
        reserva.confirmar(Instant.now());
        assertEquals(ReservaEstado.CONFIRMADA, reserva.getEstado());

        // CONFIRMADA -> EN_CURSO
        reserva.iniciar(Instant.now());
        assertEquals(ReservaEstado.EN_CURSO, reserva.getEstado());

        // EN_CURSO -> FINALIZADA
        reserva.finalizar(Instant.now());
        assertEquals(ReservaEstado.FINALIZADA, reserva.getEstado());
    }

    @Test
    void transicionesInvalidasLanzanExcepcion() {
        Reserva reserva = new Reserva(
                UUID.randomUUID(), UUID.randomUUID(), ReservaEstado.CONFIRMADA,
                LocalDate.now(), LocalDate.now().plusDays(2),
                Instant.now(), null
        );

        assertThrows(IllegalStateException.class, () -> reserva.confirmar(Instant.now()));
        assertThrows(IllegalStateException.class, () -> reserva.expirar(Instant.now()));
    }

    @Test
    void expiracionSoloDesdePendiente() {
        Reserva reserva = new Reserva(
                UUID.randomUUID(), UUID.randomUUID(), ReservaEstado.PENDIENTE,
                LocalDate.now(), LocalDate.now().plusDays(2),
                Instant.now(), Instant.now().minus(Duration.ofMinutes(1)) // ya vencida
        );

        assertTrue(reserva.estaVencida(Instant.now()));
        
        reserva.expirar(Instant.now());
        assertEquals(ReservaEstado.EXPIRADA, reserva.getEstado());
    }
}