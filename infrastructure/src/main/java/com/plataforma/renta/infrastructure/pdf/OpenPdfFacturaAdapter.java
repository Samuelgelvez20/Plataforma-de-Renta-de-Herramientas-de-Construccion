package com.plataforma.renta.infrastructure.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.plataforma.renta.domain.model.Factura;
import com.plataforma.renta.domain.model.FacturaTipo;
import com.plataforma.renta.domain.port.out.FacturaPdfPort;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class OpenPdfFacturaAdapter implements FacturaPdfPort {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
            .withZone(ZoneId.of("America/Bogota"));

    @Override
    public byte[] generarPdf(Factura factura) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Tipografías
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(30, 41, 59));
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(71, 85, 105));
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(51, 65, 85));
            Font headerTableFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

            // Encabezado
            Paragraph title = new Paragraph("PLATAFORMA DE RENTA DE EQUIPOS DE CONSTRUCCIÓN", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(4f);
            document.add(title);

            String tipoDoc = factura.getTipo() == FacturaTipo.NOTA_CREDITO ? "NOTA CRÉDITO" : "FACTURA ELECTRÓNICA DE VENTA";
            Paragraph docType = new Paragraph(tipoDoc + " N° " + factura.getNumeroFactura(), subtitleFont);
            docType.setAlignment(Element.ALIGN_CENTER);
            docType.setSpacingAfter(15f);
            document.add(docType);

            // Tabla Datos Emisión
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setSpacingAfter(15f);

            PdfPCell cellEmision = new PdfPCell(new Phrase("Fecha de Emisión: " + FORMATTER.format(factura.getFechaEmision()), normalFont));
            cellEmision.setBorder(Rectangle.NO_BORDER);
            metaTable.addCell(cellEmision);

            PdfPCell cellMoneda = new PdfPCell(new Phrase("Moneda: " + factura.getMoneda() + " | Reserva ID: " + factura.getReservaId(), normalFont));
            cellMoneda.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cellMoneda.setBorder(Rectangle.NO_BORDER);
            metaTable.addCell(cellMoneda);

            document.add(metaTable);

            // Tabla Emisor y Receptor
            PdfPTable partiesTable = new PdfPTable(2);
            partiesTable.setWidthPercentage(100);
            partiesTable.setSpacingAfter(20f);

            PdfPCell emisorHeader = new PdfPCell(new Phrase("DATOS DEL EMISOR / PROPIETARIO", headerTableFont));
            emisorHeader.setBackgroundColor(new Color(234, 88, 12)); // Naranja de construcción
            emisorHeader.setPadding(6f);
            partiesTable.addCell(emisorHeader);

            PdfPCell receptorHeader = new PdfPCell(new Phrase("DATOS DEL RECEPTOR / CLIENTE", headerTableFont));
            receptorHeader.setBackgroundColor(new Color(15, 23, 42)); // Slate dark
            receptorHeader.setPadding(6f);
            partiesTable.addCell(receptorHeader);

            PdfPCell emisorData = new PdfPCell(new Phrase(factura.getDatosEmisor() != null ? factura.getDatosEmisor() : "Información de emisor registrada en sistema", normalFont));
            emisorData.setPadding(8f);
            partiesTable.addCell(emisorData);

            PdfPCell receptorData = new PdfPCell(new Phrase(factura.getDatosReceptor() != null ? factura.getDatosReceptor() : "Información de receptor registrada en sistema", normalFont));
            receptorData.setPadding(8f);
            partiesTable.addCell(receptorData);

            document.add(partiesTable);

            // Tabla Detalle Financiero
            PdfPTable itemsTable = new PdfPTable(4);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{45f, 15f, 15f, 25f});
            itemsTable.setSpacingAfter(15f);

            String[] headers = {"CONCEPTO", "SUBTOTAL", "IVA (" + factura.getIvaPorcentaje() + "%)", "TOTAL"};
            for (String h : headers) {
                PdfPCell c = new PdfPCell(new Phrase(h, headerTableFont));
                c.setBackgroundColor(new Color(51, 65, 85));
                c.setPadding(6f);
                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                itemsTable.addCell(c);
            }

            String concepto = factura.getTipo() == FacturaTipo.NOTA_CREDITO ?
                    "Reembolso / Cancelación de renta de equipo de construcción" :
                    "Servicio de renta de herramienta y equipo de construcción";

            PdfPCell cConcepto = new PdfPCell(new Phrase(concepto, normalFont));
            cConcepto.setPadding(8f);
            itemsTable.addCell(cConcepto);

            PdfPCell cSubtotal = new PdfPCell(new Phrase(factura.getSubtotal().toString(), normalFont));
            cSubtotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cSubtotal.setPadding(8f);
            itemsTable.addCell(cSubtotal);

            PdfPCell cIva = new PdfPCell(new Phrase(factura.getIva().toString(), normalFont));
            cIva.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cIva.setPadding(8f);
            itemsTable.addCell(cIva);

            PdfPCell cTotal = new PdfPCell(new Phrase(factura.getTotal().toString(), boldFont));
            cTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cTotal.setPadding(8f);
            itemsTable.addCell(cTotal);

            document.add(itemsTable);

            // Resumen de Totales
            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(40);
            totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalTable.setSpacingAfter(25f);

            totalTable.addCell(createCellNoBorder("Subtotal:", boldFont, Element.ALIGN_RIGHT));
            totalTable.addCell(createCellNoBorder(factura.getSubtotal().toString(), normalFont, Element.ALIGN_RIGHT));

            totalTable.addCell(createCellNoBorder("IVA (" + factura.getIvaPorcentaje() + "%):", boldFont, Element.ALIGN_RIGHT));
            totalTable.addCell(createCellNoBorder(factura.getIva().toString(), normalFont, Element.ALIGN_RIGHT));

            totalTable.addCell(createCellNoBorder("TOTAL A PAGAR:", boldFont, Element.ALIGN_RIGHT));
            totalTable.addCell(createCellNoBorder(factura.getTotal().toString(), boldFont, Element.ALIGN_RIGHT));

            document.add(totalTable);

            // Pie de página y validez fiscal
            Paragraph footer = new Paragraph(
                    "Documento soporte de transacción comercial generado electrónicamente por la Plataforma de Renta de Equipos de Construcción. " +
                    "Resolución DIAN aplicable - Todos los derechos reservados.",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(100, 116, 139))
            );
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Error generando PDF de factura con OpenPDF: " + e.getMessage(), e);
        }
    }

    private PdfPCell createCellNoBorder(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(4f);
        return cell;
    }
}
