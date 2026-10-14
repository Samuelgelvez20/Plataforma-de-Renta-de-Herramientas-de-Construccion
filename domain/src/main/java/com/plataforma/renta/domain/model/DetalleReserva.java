package com.plataforma.renta.domain.model;

import com.plataforma.renta.domain.value.Dinero;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class DetalleReserva {

    private final UUID id;
    private final UUID reservaId;
    private final UUID herramientaId;
    private final int cantidad;
    private final Dinero precioDiaAcordado;
    private final Dinero subtotal;
    private final Dinero iva;
    private final Dinero total;
    private final BigDecimal ivaPorcentaje;
    private final boolean ivaAplica;
    private final Instant creadoEn;

    public DetalleReserva(UUID id, UUID reservaId, UUID herramientaId,
                          int cantidad, Dinero precioDiaAcordado,
                          Dinero subtotal, Dinero iva, Dinero total,
                          BigDecimal ivaPorcentaje, boolean ivaAplica, Instant creadoEn) {
        this.id = id;
        this.reservaId = reservaId;
        this.herramientaId = herramientaId;
        this.cantidad = cantidad;
        this.precioDiaAcordado = precioDiaAcordado;
        this.subtotal = subtotal;
        this.iva = iva;
        this.total = total;
        this.ivaPorcentaje = ivaPorcentaje;
        this.ivaAplica = ivaAplica;
        this.creadoEn = creadoEn;
    }

    public static DetalleReserva crear(UUID reservaId, UUID herramientaId,
                                       int cantidad, Dinero precioDiaAcordado,
                                       LocalDate fechaInicio, LocalDate fechaFin,
                                       BigDecimal ivaPorcentaje, boolean ivaAplica) {
        int dias = calcularDiasInclusivos(fechaInicio, fechaFin);
        Dinero subtotal = precioDiaAcordado.multiplicar(cantidad).multiplicar(dias);
        Dinero iva = ivaAplica
                ? subtotal.multiplicar(ivaPorcentaje).dividir(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : Dinero.cero();
        Dinero total = subtotal.sumar(iva);

        return new DetalleReserva(
                UUID.randomUUID(), reservaId, herramientaId, cantidad,
                precioDiaAcordado, subtotal, iva, total, ivaPorcentaje, ivaAplica, Instant.now()
        );
    }

    private static int calcularDiasInclusivos(LocalDate inicio, LocalDate fin) {
        return (int) (fin.toEpochDay() - inicio.toEpochDay()) + 1;
    }

    public UUID getId() { return id; }
    public UUID getReservaId() { return reservaId; }
    public UUID getHerramientaId() { return herramientaId; }
    public int getCantidad() { return cantidad; }
    public Dinero getPrecioDiaAcordado() { return precioDiaAcordado; }
    public Dinero getSubtotal() { return subtotal; }
    public Dinero getIva() { return iva; }
    public Dinero getTotal() { return total; }
    public BigDecimal getIvaPorcentaje() { return ivaPorcentaje; }
    public boolean isIvaAplica() { return ivaAplica; }
    public Instant getCreadoEn() { return creadoEn; }
}
