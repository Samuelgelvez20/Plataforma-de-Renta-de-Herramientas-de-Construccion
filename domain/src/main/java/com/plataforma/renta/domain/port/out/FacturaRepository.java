package com.plataforma.renta.domain.port.out;

import com.plataforma.renta.domain.model.Factura;
import com.plataforma.renta.domain.model.FacturaTipo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FacturaRepository {

    Factura save(Factura factura);

    Optional<Factura> findById(UUID id);

    List<Factura> findByReservaId(UUID reservaId);

    Optional<Factura> findOriginalByReservaId(UUID reservaId);

    List<Factura> findNotasCreditoByReservaId(UUID reservaId);

    Optional<Factura> findByPagoReembolsoId(UUID pagoReembolsoId);

    boolean existsByNumeroFactura(String numeroFactura);

    List<Factura> findAll();
}