package com.plataforma.renta.domain.port.out;

import com.plataforma.renta.domain.model.Factura;

public interface FacturaPdfPort {

    byte[] generarPdf(Factura factura);
}
