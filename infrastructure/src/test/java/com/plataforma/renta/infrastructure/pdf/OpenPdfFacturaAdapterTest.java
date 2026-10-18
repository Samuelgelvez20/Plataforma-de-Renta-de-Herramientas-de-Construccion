package com.plataforma.renta.infrastructure.pdf;

import com.plataforma.renta.domain.model.Factura;
import com.plataforma.renta.domain.value.Dinero;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OpenPdfFacturaAdapterTest {

    @Test
    void debeGenerarDocumentoPdfValidoParaFactura() {
        OpenPdfFacturaAdapter adapter = new OpenPdfFacturaAdapter();

        Factura factura = Factura.original(
                UUID.randomUUID(),
                "F-000123",
                Dinero.cop(new BigDecimal("100000.00")),
                Dinero.cop(new BigDecimal("19000.00")),
                Dinero.cop(new BigDecimal("119000.00")),
                new BigDecimal("19.00"),
                true,
                "NIT: 900123456-1 - Equipos del Norte SAS",
                "Cliente: Juan Perez (juan@example.com)"
        );

        byte[] pdfBytes = adapter.generarPdf(factura);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(500);

        String magicNumber = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertThat(magicNumber).isEqualTo("%PDF-");
    }
}
