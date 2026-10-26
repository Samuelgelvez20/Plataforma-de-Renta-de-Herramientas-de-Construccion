package com.plataforma.renta.infrastructure.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.plataforma.renta.domain.model.Factura;
import com.plataforma.renta.domain.model.FacturaTipo;
import com.plataforma.renta.domain.port.out.FacturaPdfPort;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

            String tipoDoc;
            if (factura.getTipo() == FacturaTipo.NOTA_CREDITO) {
                tipoDoc = "NOTA CRÉDITO";
            } else if (factura.getTipo() == FacturaTipo.REPARACION) {
                tipoDoc = "FACTURA DE COBRO POR REPARACIÓN";
            } else {
                tipoDoc = "FACTURA ELECTRÓNICA DE VENTA";
            }
            Paragraph docType = new Paragraph(tipoDoc + " N° " + factura.getNumeroFactura(), subtitleFont);
            docType.setAlignment(Element.ALIGN_CENTER);
            docType.setSpacingAfter(15f);
            document.add(docType);

            // Tabla Datos Emisión
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setSpacingAfter(15f);

            PdfPCell cellEmision = new PdfPCell(new Phrase("Fecha de Emisión: " + FORMATTER.format(factura.getFechaEmision()) + " | Estado: " + (factura.getEstado() != null ? factura.getEstado() : "PAGADA"), normalFont));
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

            PdfPCell emisorData = new PdfPCell();
            emisorData.setPadding(8f);
            emisorData.addElement(formatearDatosEmisor(factura.getDatosEmisor(), boldFont, normalFont));
            partiesTable.addCell(emisorData);

            PdfPCell receptorData = new PdfPCell();
            receptorData.setPadding(8f);
            receptorData.addElement(formatearDatosReceptor(factura.getDatosReceptor(), boldFont, normalFont));
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

            String concepto;
            if (factura.getTipo() == FacturaTipo.NOTA_CREDITO) {
                concepto = "Reembolso / Cancelación de renta de equipo de construcción";
            } else if (factura.getTipo() == FacturaTipo.REPARACION) {
                concepto = "Cobro por costos de reparación de daños: " +
                        (factura.getDescripcionDano() != null && !factura.getDescripcionDano().isBlank()
                                ? factura.getDescripcionDano()
                                : "Daño reportado en devolución de equipo");
            } else {
                concepto = "Servicio de renta de herramienta y equipo de construcción";
            }

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

    private Paragraph formatearDatosEmisor(String json, Font labelFont, Font valFont) {
        Paragraph p = new Paragraph();
        p.setLeading(13f);
        if (json == null || json.isBlank()) {
            p.add(new Chunk("Información de emisor registrada en sistema", valFont));
            return p;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);
            String razonSocial = node.path("razon_social").asText("");
            String nit = node.path("nit").asText("");
            String regimen = node.path("regimen").asText("");
            String direccion = node.path("direccion").asText("");
            String municipio = node.path("municipio").asText("");
            String departamento = node.path("departamento").asText("");

            if (!razonSocial.isBlank()) {
                p.add(new Chunk("Razón Social: ", labelFont));
                p.add(new Chunk(razonSocial + "\n", valFont));
            }
            if (!nit.isBlank()) {
                p.add(new Chunk("NIT: ", labelFont));
                p.add(new Chunk(nit + (regimen.isBlank() ? "" : " (Régimen " + regimen + ")") + "\n", valFont));
            }
            if (!direccion.isBlank()) {
                p.add(new Chunk("Dirección: ", labelFont));
                p.add(new Chunk(direccion + "\n", valFont));
            }
            if (!municipio.isBlank() || !departamento.isBlank()) {
                p.add(new Chunk("Ubicación: ", labelFont));
                p.add(new Chunk(municipio + (!municipio.isBlank() && !departamento.isBlank() ? ", " : "") + departamento + "\n", valFont));
            }
        } catch (Exception e) {
            p.add(new Chunk(json, valFont));
        }
        return p;
    }

    private Paragraph formatearDatosReceptor(String json, Font labelFont, Font valFont) {
        Paragraph p = new Paragraph();
        p.setLeading(13f);
        if (json == null || json.isBlank()) {
            p.add(new Chunk("Información de receptor registrada en sistema", valFont));
            return p;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);
            String nombre = node.path("nombre").asText("");
            String apellido = node.path("apellido").asText("");
            String email = node.path("email").asText("");
            String direccion = node.path("direccion").asText("");
            String documento = node.path("documento").asText("");
            if (documento.isBlank()) {
                documento = node.path("documentoIdentidad").asText("");
            }

            String nombreCompleto = (nombre + " " + apellido).trim();
            if (!nombreCompleto.isBlank()) {
                p.add(new Chunk("Cliente: ", labelFont));
                p.add(new Chunk(nombreCompleto + "\n", valFont));
            }
            if (!documento.isBlank()) {
                p.add(new Chunk("Documento: ", labelFont));
                p.add(new Chunk(documento + "\n", valFont));
            }
            if (!email.isBlank()) {
                p.add(new Chunk("Email: ", labelFont));
                p.add(new Chunk(email + "\n", valFont));
            }
            if (!direccion.isBlank()) {
                p.add(new Chunk("Dirección: ", labelFont));
                p.add(new Chunk(direccion + "\n", valFont));
            }
        } catch (Exception e) {
            p.add(new Chunk(json, valFont));
        }
        return p;
    }
}
