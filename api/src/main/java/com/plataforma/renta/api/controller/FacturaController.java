package com.plataforma.renta.api.controller;

import com.plataforma.renta.api.dto.FacturaDtos.FacturaResponse;
import com.plataforma.renta.application.service.AuthService;
import com.plataforma.renta.application.service.FacturaService;
import com.plataforma.renta.domain.model.Factura;
import com.plataforma.renta.domain.model.Usuario;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/facturas")
@Tag(name = "Facturación", description = "Consulta y descarga de comprobantes electrónicos en formato PDF")
public class FacturaController {

    private final FacturaService facturaService;
    private final AuthService authService;

    public FacturaController(FacturaService facturaService, AuthService authService) {
        this.facturaService = facturaService;
        this.authService = authService;
    }

    @GetMapping
    @Operation(summary = "Listar facturas", description = "Obtiene el historial de facturas y notas de crédito según los permisos del usuario")
    public ResponseEntity<List<FacturaResponse>> listar(Authentication auth) {
        Usuario actor = authService.obtenerUsuarioPorEmail(auth.getName());
        var facturas = facturaService.listarPorUsuario(actor.getId(), actor.getRol()).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(facturas);
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Descargar factura en PDF", description = "Genera y transmite el documento tributario en binario PDF con validación de autorización")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable UUID id, Authentication auth) {
        Usuario actor = authService.obtenerUsuarioPorEmail(auth.getName());
        if (!facturaService.tieneAcceso(id, actor.getId(), actor.getRol())) {
            throw new AccessDeniedException("No tiene permisos para descargar esta factura");
        }
        Factura factura = facturaService.obtenerPorId(id);
        byte[] pdfBytes = facturaService.generarPdf(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + factura.getNumeroFactura() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    private FacturaResponse toResponse(Factura f) {
        return new FacturaResponse(
                f.getId(),
                f.getReservaId(),
                f.getNumeroFactura(),
                f.getTipo().name(),
                f.getSubtotal().getMonto(),
                f.getIva().getMonto(),
                f.getTotal().getMonto(),
                f.getDatosEmisor(),
                f.getDatosReceptor(),
                f.getFechaEmision()
        );
    }
}
