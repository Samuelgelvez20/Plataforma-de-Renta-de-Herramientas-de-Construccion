package com.plataforma.renta.application.service;

import com.plataforma.renta.domain.exception.EntidadNoEncontradaException;
import com.plataforma.renta.domain.model.Factura;
import com.plataforma.renta.domain.model.FacturaTipo;
import com.plataforma.renta.domain.model.Pago;
import com.plataforma.renta.domain.model.Reserva;
import com.plataforma.renta.domain.model.Herramienta;
import com.plataforma.renta.domain.port.out.ConfiguracionFiscalRepository;
import com.plataforma.renta.domain.port.out.FacturaRepository;
import com.plataforma.renta.domain.port.out.FacturaPdfPort;
import java.util.List;
import com.plataforma.renta.domain.port.out.HerramientaRepository;
import com.plataforma.renta.domain.port.out.JsonSerializer;
import com.plataforma.renta.domain.port.out.PagoRepository;
import com.plataforma.renta.domain.port.out.PropietarioRepository;
import com.plataforma.renta.domain.port.out.ClienteRepository;
import com.plataforma.renta.domain.port.out.ReservaRepository;
import com.plataforma.renta.domain.model.UserRol;
import java.util.Optional;
import com.plataforma.renta.domain.value.Dinero;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final ConfiguracionFiscalRepository configuracionFiscalRepository;
    private final PropietarioRepository propietarioRepository;
    private final ClienteRepository clienteRepository;
    private final PagoRepository pagoRepository;
    private final HerramientaRepository herramientaRepository;
    private final JsonSerializer jsonSerializer;
    private final FacturaPdfPort facturaPdfPort;
    private final ReservaRepository reservaRepository;

    public FacturaService(FacturaRepository facturaRepository,
                          ConfiguracionFiscalRepository configuracionFiscalRepository,
                          PropietarioRepository propietarioRepository,
                          ClienteRepository clienteRepository,
                          PagoRepository pagoRepository,
                          HerramientaRepository herramientaRepository,
                          JsonSerializer jsonSerializer,
                          FacturaPdfPort facturaPdfPort,
                          ReservaRepository reservaRepository) {
        this.facturaRepository = java.util.Objects.requireNonNull(facturaRepository, "facturaRepository es requerido");
        this.configuracionFiscalRepository = java.util.Objects.requireNonNull(configuracionFiscalRepository, "configuracionFiscalRepository es requerido");
        this.propietarioRepository = java.util.Objects.requireNonNull(propietarioRepository, "propietarioRepository es requerido");
        this.clienteRepository = java.util.Objects.requireNonNull(clienteRepository, "clienteRepository es requerido");
        this.pagoRepository = java.util.Objects.requireNonNull(pagoRepository, "pagoRepository es requerido");
        this.herramientaRepository = java.util.Objects.requireNonNull(herramientaRepository, "herramientaRepository es requerido");
        this.jsonSerializer = java.util.Objects.requireNonNull(jsonSerializer, "jsonSerializer es requerido");
        this.facturaPdfPort = java.util.Objects.requireNonNull(facturaPdfPort, "facturaPdfPort es requerido");
        this.reservaRepository = java.util.Objects.requireNonNull(reservaRepository, "reservaRepository es requerido");
    }

    @Transactional
    public Factura emitirFacturaOriginal(Reserva reserva, Pago pago, Dinero total) {
        // Verificar que no existe ya factura original
        if (facturaRepository.findOriginalByReservaId(reserva.getId()).isPresent()) {
            throw new IllegalStateException("Ya existe factura original para esta reserva");
        }

        // Obtener datos fiscales del propietario (a través de la herramienta del primer detalle)
        if (reserva.getDetalles() == null || reserva.getDetalles().isEmpty()) {
            throw new IllegalStateException("La reserva no contiene detalles para emitir factura");
        }
        var herramientaId = reserva.getDetalles().get(0).getHerramientaId();
        var herramienta = herramientaRepository.findById(herramientaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Herramienta no encontrada: " + herramientaId));
        var propietarioId = herramienta.getPropietarioId();
        
        var propietario = propietarioRepository.findById(propietarioId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Propietario no encontrado: " + propietarioId));
        
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
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("nit", p.getNit() != null ? p.getNit().getValor() : "");
        map.put("razon_social", p.getRazonSocial());
        map.put("direccion", p.getDireccionFiscal());
        map.put("regimen", p.getRegimen());
        map.put("municipio", p.getMunicipio());
        map.put("departamento", p.getDepartamento());
        return jsonSerializer.toJson(map);
    }

    private String toJsonReceptor(com.plataforma.renta.domain.model.Cliente c) {
        String datosFact = c.getDatosFacturacion();
        if (datosFact != null && !datosFact.isBlank() && jsonSerializer.isValidJson(datosFact)) {
            return datosFact.trim();
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("nombre", c.getNombre());
        map.put("apellido", c.getApellido());
        map.put("email", c.getEmail() != null ? c.getEmail().getValor() : "");
        map.put("direccion", c.getDireccionEntregaDefault());
        return jsonSerializer.toJson(map);
    }

    public List<Factura> listarTodas() {
        return facturaRepository.findAll();
    }

    public Factura obtenerPorId(UUID id) {
        return facturaRepository.findById(id)
                .orElseThrow(() -> new EntidadNoEncontradaException("Factura no encontrada: " + id));
    }

    public List<Factura> listarPorReserva(UUID reservaId) {
        return facturaRepository.findByReservaId(reservaId);
    }

    public byte[] generarPdf(UUID facturaId) {
        Factura factura = obtenerPorId(facturaId);
        return facturaPdfPort.generarPdf(factura);
    }


    public List<Factura> listarPorUsuario(UUID usuarioId, UserRol rol) {
        if (rol == UserRol.ADMIN) {
            return facturaRepository.findAll();
        }

        if (rol == UserRol.CLIENTE) {
            List<UUID> misReservas = reservaRepository.findByClienteId(usuarioId).stream()
                    .map(Reserva::getId)
                    .toList();
            return facturaRepository.findAll().stream()
                    .filter(f -> misReservas.contains(f.getReservaId()))
                    .toList();
        }

        if (rol == UserRol.PROPIETARIO) {
            List<UUID> misHerramientas = herramientaRepository.findByPropietarioId(usuarioId).stream()
                    .map(Herramienta::getId)
                    .toList();
            List<UUID> reservasDeMisHerramientas = reservaRepository.findAll().stream()
                    .filter(r -> r.getDetalles() != null && r.getDetalles().stream().anyMatch(d -> misHerramientas.contains(d.getHerramientaId())))
                    .map(Reserva::getId)
                    .toList();
            return facturaRepository.findAll().stream()
                    .filter(f -> reservasDeMisHerramientas.contains(f.getReservaId()))
                    .toList();
        }

        return List.of();
    }

    public boolean tieneAcceso(UUID facturaId, UUID usuarioId, UserRol rol) {
        if (rol == UserRol.ADMIN) {
            return true;
        }

        Factura factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new EntidadNoEncontradaException("Factura no encontrada: " + facturaId));

        Optional<Reserva> reservaOpt = reservaRepository.findById(factura.getReservaId());
        if (reservaOpt.isEmpty()) {
            return false;
        }

        Reserva reserva = reservaOpt.get();
        if (rol == UserRol.CLIENTE) {
            return usuarioId.equals(reserva.getClienteId());
        }

        if (rol == UserRol.PROPIETARIO) {
            List<UUID> misHerramientas = herramientaRepository.findByPropietarioId(usuarioId).stream()
                    .map(Herramienta::getId)
                    .toList();
            return reserva.getDetalles() != null && reserva.getDetalles().stream().anyMatch(d -> misHerramientas.contains(d.getHerramientaId()));
        }

        return false;
    }

}
