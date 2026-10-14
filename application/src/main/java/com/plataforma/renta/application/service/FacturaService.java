package com.plataforma.renta.application.service;

import com.plataforma.renta.domain.exception.EntidadNoEncontradaException;
import com.plataforma.renta.domain.model.Factura;
import com.plataforma.renta.domain.model.FacturaTipo;
import com.plataforma.renta.domain.model.Pago;
import com.plataforma.renta.domain.model.Reserva;
import com.plataforma.renta.domain.port.out.ConfiguracionFiscalRepository;
import com.plataforma.renta.domain.port.out.FacturaRepository;
import com.plataforma.renta.domain.port.out.PagoRepository;
import com.plataforma.renta.domain.port.out.PropietarioRepository;
import com.plataforma.renta.domain.port.out.ClienteRepository;
import com.plataforma.renta.domain.value.Dinero;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final ConfiguracionFiscalRepository configuracionFiscalRepository;
    private final PropietarioRepository propietarioRepository;
    private final ClienteRepository clienteRepository;
    private final PagoRepository pagoRepository;

    public FacturaService(FacturaRepository facturaRepository,
                          ConfiguracionFiscalRepository configuracionFiscalRepository,
                          PropietarioRepository propietarioRepository,
                          ClienteRepository clienteRepository,
                          PagoRepository pagoRepository) {
        this.facturaRepository = facturaRepository;
        this.configuracionFiscalRepository = configuracionFiscalRepository;
        this.propietarioRepository = propietarioRepository;
        this.clienteRepository = clienteRepository;
        this.pagoRepository = pagoRepository;
    }

    @Transactional
    public Factura emitirFacturaOriginal(Reserva reserva, Pago pago, Dinero total) {
        // Verificar que no existe ya factura original
        if (facturaRepository.findOriginalByReservaId(reserva.getId()).isPresent()) {
            throw new IllegalStateException("Ya existe factura original para esta reserva");
        }

        // Obtener datos fiscales del propietario (a través de la herramienta del primer detalle)
        var herramientaId = reserva.getDetalles().get(0).getHerramientaId();
        var propietarioId = propietarioRepository.findHerramientaIdsByPropietarioId(herramientaId)
                .stream().findFirst()
                .orElseThrow(() -> new EntidadNoEncontradaException("Propietario no encontrado"));
        
        var propietario = propietarioRepository.findById(propietarioId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Propietario no encontrado"));
        
        var cliente = clienteRepository.findByUsuarioId(reserva.getClienteId())
                .orElseThrow(() -> new EntidadNoEncontradaException("Cliente no encontrado"));

        String numeroFactura = generarNumeroFactura();
        BigDecimal ivaPct = new BigDecimal(configuracionFiscalRepository.findValor("iva_porcentaje").orElse("19.00"));
        boolean ivaAplica = true; // configurable por operación en futuro

        Dinero subtotal = reserva.calcularSubtotal();
        Dinero iva = reserva.calcularIva();

        Factura factura = Factura.original(
                reserva.getId(), numeroFactura,
                subtotal, iva, total,
                ivaPct, true,
                toJsonEmisor(propietario), toJsonReceptor(cliente)
        );

        return facturaRepository.save(factura);
    }

    @Transactional
    public Factura emitirNotaCredito(Reserva reserva, Dinero montoConIva, UUID pagoReembolsoId) {
        Factura original = facturaRepository.findOriginalByReservaId(reserva.getId())
                .orElseThrow(() -> new EntidadNoEncontradaException("No hay factura original para nota de crédito"));

        Pago reembolso = pagoRepository.findById(pagoReembolsoId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Reembolso no encontrado"));

        BigDecimal ivaPct = original.getIvaPorcentaje();
        boolean ivaAplica = original.isIvaAplica();

        Dinero subtotal, iva;
        if (ivaAplica && ivaPct.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal divisor = BigDecimal.ONE.add(ivaPct.divide(BigDecimal.valueOf(100)));
            subtotal = montoConIva.dividir(divisor, 2, RoundingMode.HALF_UP);
            iva = montoConIva.restar(subtotal);
        } else {
            subtotal = montoConIva;
            iva = Dinero.cero();
        }

        // Ajuste redondeo
        var diff = montoConIva.restar(subtotal.sumar(iva));
        if (!diff.esCero()) {
            iva = iva.sumar(diff);
        }

        String numeroFactura = generarNumeroFactura();

        Factura nota = Factura.notaCredito(
                reserva.getId(), numeroFactura,
                original.getId(), pagoReembolsoId,
                subtotal, iva, montoConIva,
                ivaPct, ivaAplica,
                original.getDatosEmisor(), original.getDatosReceptor()
        );

        return facturaRepository.save(nota);
    }

    private String generarNumeroFactura() {
        // En implementación real usar secuencia de BD
        return "F-" + String.format("%06d", System.currentTimeMillis() % 1000000);
    }

    private String toJsonEmisor(com.plataforma.renta.domain.model.Propietario p) {
        return String.format("{\"nit\":\"%s\",\"razon_social\":\"%s\",\"direccion\":\"%s\",\"regimen\":\"%s\",\"municipio\":\"%s\",\"departamento\":\"%s\"}",
                p.getNit().getValor(), p.getRazonSocial(), p.getDireccionFiscal(), p.getRegimen(), p.getMunicipio(), p.getDepartamento());
    }

    private String toJsonReceptor(com.plataforma.renta.domain.model.Cliente c) {
        return String.format("{\"nit\":\"%s\",\"razon_social\":\"%s\",\"direccion\":\"%s\",\"email\":\"%s\"}",
                c.getDatosFacturacion(), c.getDatosFacturacion(), 
                c.getDireccionEntregaDefault(), c.getEmail());
    }
}