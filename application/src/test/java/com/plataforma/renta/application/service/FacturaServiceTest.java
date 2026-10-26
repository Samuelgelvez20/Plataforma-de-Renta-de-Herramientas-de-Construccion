package com.plataforma.renta.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plataforma.renta.domain.model.*;
import com.plataforma.renta.domain.port.out.*;
import com.plataforma.renta.domain.value.Dinero;
import com.plataforma.renta.domain.value.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FacturaServiceTest {

    @Mock
    private FacturaRepository facturaRepository;
    @Mock
    private ConfiguracionFiscalRepository configuracionFiscalRepository;
    @Mock
    private PropietarioRepository propietarioRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private HerramientaRepository herramientaRepository;
    @Mock
    private FacturaPdfPort facturaPdfPort;
    @Mock
    private ReservaRepository reservaRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final JsonSerializer jsonSerializer = new JsonSerializer() {
        @Override
        public String toJson(Object object) {
            try {
                return objectMapper.writeValueAsString(object);
            } catch (Exception e) {
                throw new RuntimeException("Error en serialización", e);
            }
        }

        @Override
        public boolean isValidJson(String json) {
            if (json == null || json.isBlank()) return false;
            try {
                objectMapper.readTree(json);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    };

    private FacturaService facturaService;

    @BeforeEach
    void setUp() {
        facturaService = new FacturaService(
                facturaRepository,
                configuracionFiscalRepository,
                propietarioRepository,
                clienteRepository,
                pagoRepository,
                herramientaRepository,
                jsonSerializer,
                facturaPdfPort,
                reservaRepository
        );
    }

    @Test
    @DisplayName("emitirFacturaOriginal resuelve el propietario correcto a través de la herramienta y formatea JSON válido")
    void emitirFacturaOriginalResuelvePropietarioCorrectoYJsonValido() throws Exception {
        UUID clienteId = UUID.randomUUID();
        UUID propietarioId = UUID.randomUUID();
        UUID herramientaId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();

        Reserva reserva = new Reserva(
                reservaId, clienteId, ReservaEstado.CONFIRMADA,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3),
                Instant.now(), null
        );
        reserva.agregarDetalle(DetalleReserva.crear(
                reservaId, herramientaId, 2,
                Dinero.cop(new BigDecimal("50000")),
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3),
                new BigDecimal("19.00"), true
        ));

        Pago pago = new Pago(
                UUID.randomUUID(), reservaId, null,
                PagoEstado.COMPLETADO, "SIMULADO", "TX-1",
                Dinero.cop(new BigDecimal("119000")), "Pago",
                Instant.now(), Instant.now()
        );

        Herramienta herramienta = new Herramienta(
                herramientaId, UUID.randomUUID(), propietarioId,
                "Generador Eléctrico", "Desc", "Honda", "EU22i",
                Dinero.cop(new BigDecimal("50000")), 5,
                HerramientaPubEstado.PUBLICADA, new BigDecimal("19.00"), true,
                Instant.now(), Instant.now()
        );

        Propietario propietario = new Propietario(
                propietarioId, Email.of("propietario@construccion.com"),
                "Equipos del Norte SAS", "Cra 10 # 20-30", "3101234567", "COMUN", "Bogota", "Cundinamarca", Instant.now()
        );

        String jsonClienteOriginal = "{\"nit\":\"901234567-1\",\"razon_social\":\"Construcciones Gómez\"}";
        Cliente cliente = new Cliente(
                clienteId, Email.of("cliente@empresa.com"), "Carlos", "Gómez", "3009876543",
                "Calle 100 # 15-20", jsonClienteOriginal
        );

        when(facturaRepository.findOriginalByReservaId(reservaId)).thenReturn(Optional.empty());
        when(herramientaRepository.findById(herramientaId)).thenReturn(Optional.of(herramienta));
        when(propietarioRepository.findById(propietarioId)).thenReturn(Optional.of(propietario));
        when(clienteRepository.findByUsuarioId(clienteId)).thenReturn(Optional.of(cliente));
        when(configuracionFiscalRepository.findValor("iva_porcentaje")).thenReturn(Optional.of("19.00"));
        when(facturaRepository.save(any(Factura.class))).thenAnswer(inv -> inv.getArgument(0));

        Factura factura = facturaService.emitirFacturaOriginal(reserva, pago, Dinero.cop(new BigDecimal("119000")));

        assertThat(factura).isNotNull();
        assertThat(factura.getTipo()).isEqualTo(FacturaTipo.FACTURA);

        // Validar JSON del emisor con Jackson
        JsonNode emisorNode = objectMapper.readTree(factura.getDatosEmisor());
        assertThat(emisorNode.get("razon_social").asText()).isEqualTo("Equipos del Norte SAS");

        // Validar JSON del receptor con Jackson
        JsonNode receptorNode = objectMapper.readTree(factura.getDatosReceptor());
        assertThat(receptorNode.get("nit").asText()).isEqualTo("901234567-1");
        assertThat(receptorNode.get("razon_social").asText()).isEqualTo("Construcciones Gómez");

        verify(herramientaRepository).findById(herramientaId);
        verify(propietarioRepository).findById(propietarioId);
    }

    @Test
    @DisplayName("emitirFactura con caracteres especiales escapa correctamente sin corromper el JSON")
    void emitirFacturaConCaracteresEspecialesEscapaCorrectamente() throws Exception {
        UUID clienteId = UUID.randomUUID();
        UUID propietarioId = UUID.randomUUID();
        UUID herramientaId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();

        Reserva reserva = new Reserva(
                reservaId, clienteId, ReservaEstado.CONFIRMADA,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                Instant.now(), null
        );
        reserva.agregarDetalle(DetalleReserva.crear(
                reservaId, herramientaId, 1,
                Dinero.cop(new BigDecimal("100000")),
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                new BigDecimal("19.00"), true
        ));

        Pago pago = Pago.completado(reservaId, Dinero.cop(new BigDecimal("119000")), "SIMULADO", "TX-SPEC");

        Herramienta herramienta = new Herramienta(
                herramientaId, UUID.randomUUID(), propietarioId,
                "Sierra \"Pro\" & Más", "Desc", "Bosch", "X-1",
                Dinero.cop(new BigDecimal("100000")), 1,
                HerramientaPubEstado.PUBLICADA, new BigDecimal("19.00"), true,
                Instant.now(), Instant.now()
        );

        // Propietario con comillas, saltos de línea, barras y caracteres especiales en sus campos
        String razonSocialEspecial = "Ferretería & Construcciones \"El Roble\" S.A.S.";
        String direccionEspecial = "Av. Calle 26 # 68D-35\nPiso 4, Of. 401 \\ Sector \"A\"";
        Propietario propietario = new Propietario(
                propietarioId, Email.of("contacto@elroble.com.co"),
                razonSocialEspecial, direccionEspecial, "3157778899", "Régimen Ordinario", "Bogotá, D.C.", "Cundinamarca", Instant.now()
        );

        // Cliente sin datosFacturacion preexistentes, con caracteres acentuados y comillas
        Cliente cliente = new Cliente(
                clienteId, Email.of("mariano.oconnor@cliente.com"), "Mariano \"Junior\"", "O'Connor Peña", "3201112233",
                "Diagonal 45 # 12-34\nApto 201", null
        );

        when(facturaRepository.findOriginalByReservaId(reservaId)).thenReturn(Optional.empty());
        when(herramientaRepository.findById(herramientaId)).thenReturn(Optional.of(herramienta));
        when(propietarioRepository.findById(propietarioId)).thenReturn(Optional.of(propietario));
        when(clienteRepository.findByUsuarioId(clienteId)).thenReturn(Optional.of(cliente));
        when(configuracionFiscalRepository.findValor("iva_porcentaje")).thenReturn(Optional.of("19.00"));
        when(facturaRepository.save(any(Factura.class))).thenAnswer(inv -> inv.getArgument(0));

        Factura factura = facturaService.emitirFacturaOriginal(reserva, pago, Dinero.cop(new BigDecimal("119000")));

        assertThat(factura).isNotNull();

        // Validar que Jackson puede parsear el JSON del emisor y que los valores se conservan intactos
        JsonNode emisorNode = objectMapper.readTree(factura.getDatosEmisor());
        assertThat(emisorNode.get("razon_social").asText()).isEqualTo(razonSocialEspecial);
        assertThat(emisorNode.get("direccion").asText()).isEqualTo(direccionEspecial);
        assertThat(emisorNode.get("municipio").asText()).isEqualTo("Bogotá, D.C.");

        // Validar que Jackson puede parsear el JSON generado para el receptor
        JsonNode receptorNode = objectMapper.readTree(factura.getDatosReceptor());
        assertThat(receptorNode.get("nombre").asText()).isEqualTo("Mariano \"Junior\"");
        assertThat(receptorNode.get("apellido").asText()).isEqualTo("O'Connor Peña");
        assertThat(receptorNode.get("email").asText()).isEqualTo("mariano.oconnor@cliente.com");
        assertThat(receptorNode.get("direccion").asText()).isEqualTo("Diagonal 45 # 12-34\nApto 201");
    }

    @Test
    @DisplayName("emitirFactura cuando cliente tiene datosFacturacion inválidos genera JSON sintácticamente correcto")
    void emitirFacturaConDatosFacturacionInvalidosFallbackCorrecto() throws Exception {
        UUID clienteId = UUID.randomUUID();
        UUID propietarioId = UUID.randomUUID();
        UUID herramientaId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();

        Reserva reserva = new Reserva(
                reservaId, clienteId, ReservaEstado.CONFIRMADA,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                Instant.now(), null
        );
        reserva.agregarDetalle(DetalleReserva.crear(
                reservaId, herramientaId, 1,
                Dinero.cop(new BigDecimal("50000")),
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                new BigDecimal("19.00"), true
        ));

        Pago pago = Pago.completado(reservaId, Dinero.cop(new BigDecimal("59500")), "SIMULADO", "TX-INV");

        Herramienta herramienta = new Herramienta(
                herramientaId, UUID.randomUUID(), propietarioId,
                "Pulidora", "Desc", "Makita", "9557NB",
                Dinero.cop(new BigDecimal("50000")), 1,
                HerramientaPubEstado.PUBLICADA, new BigDecimal("19.00"), true,
                Instant.now(), Instant.now()
        );

        Propietario propietario = new Propietario(
                propietarioId, Email.of("contacto@alquileres.com"),
                "Alquileres SAS", "Calle 1", "3001234567", "COMUN", "Medellin", "Antioquia", Instant.now()
        );

        // Cliente con datosFacturacion corruptos/inválidos (ej. texto no-JSON o JSON roto)
        Cliente cliente = new Cliente(
                clienteId, Email.of("carlos@obras.com"), "Carlos", "Suárez", "3100001122",
                "Carrera 7 # 10-20", "{ datos corruptos no json: 123"
        );

        when(facturaRepository.findOriginalByReservaId(reservaId)).thenReturn(Optional.empty());
        when(herramientaRepository.findById(herramientaId)).thenReturn(Optional.of(herramienta));
        when(propietarioRepository.findById(propietarioId)).thenReturn(Optional.of(propietario));
        when(clienteRepository.findByUsuarioId(clienteId)).thenReturn(Optional.of(cliente));
        when(configuracionFiscalRepository.findValor("iva_porcentaje")).thenReturn(Optional.of("19.00"));
        when(facturaRepository.save(any(Factura.class))).thenAnswer(inv -> inv.getArgument(0));

        Factura factura = facturaService.emitirFacturaOriginal(reserva, pago, Dinero.cop(new BigDecimal("59500")));

        // El resultado debe ser un JSON válido estructurado con los datos del cliente
        JsonNode receptorNode = objectMapper.readTree(factura.getDatosReceptor());
        assertThat(receptorNode.get("nombre").asText()).isEqualTo("Carlos");
        assertThat(receptorNode.get("apellido").asText()).isEqualTo("Suárez");
        assertThat(receptorNode.get("email").asText()).isEqualTo("carlos@obras.com");
    }

    @Test
    @DisplayName("emitirNotaCredito genera factura de tipo NOTA_CREDITO preservando emisor y receptor originales")
    void emitirNotaCreditoPreservaDatosFiscalesYCalculaIva() {
        UUID reservaId = UUID.randomUUID();
        UUID pagoReembolsoId = UUID.randomUUID();
        UUID facturaOriginalId = UUID.randomUUID();

        Factura original = Factura.original(
                reservaId, "F-000001",
                Dinero.cop(new BigDecimal("100000")), Dinero.cop(new BigDecimal("19000")), Dinero.cop(new BigDecimal("119000")),
                new BigDecimal("19.00"), true,
                "{\"nit\":\"900123450-4\",\"razon_social\":\"Equipos del Norte\"}",
                "{\"nombre\":\"Carlos\",\"apellido\":\"Gómez\"}"
        );

        Pago reembolso = new Pago(
                pagoReembolsoId, reservaId, UUID.randomUUID(),
                PagoEstado.PENDIENTE, "SIMULADO", null,
                Dinero.cop(new BigDecimal("59500")), "Reembolso parcial",
                Instant.now(), Instant.now()
        );

        Reserva reserva = new Reserva(
                reservaId, UUID.randomUUID(), ReservaEstado.CONFIRMADA,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                Instant.now(), null
        );

        when(facturaRepository.findOriginalByReservaId(reservaId)).thenReturn(Optional.of(original));
        when(pagoRepository.findById(pagoReembolsoId)).thenReturn(Optional.of(reembolso));
        when(facturaRepository.save(any(Factura.class))).thenAnswer(inv -> inv.getArgument(0));

        Factura notaCredito = facturaService.emitirNotaCredito(reserva, Dinero.cop(new BigDecimal("59500")), pagoReembolsoId);

        assertThat(notaCredito).isNotNull();
        assertThat(notaCredito.getTipo()).isEqualTo(FacturaTipo.NOTA_CREDITO);
        assertThat(notaCredito.getFacturaOriginalId()).isEqualTo(original.getId());
        assertThat(notaCredito.getPagoReembolsoId()).isEqualTo(pagoReembolsoId);
        assertThat(notaCredito.getTotal().getMonto()).isEqualByComparingTo(new BigDecimal("59500"));
        assertThat(notaCredito.getSubtotal().getMonto()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(notaCredito.getIva().getMonto()).isEqualByComparingTo(new BigDecimal("9500.00"));
        assertThat(notaCredito.getDatosEmisor()).isEqualTo(original.getDatosEmisor());
        assertThat(notaCredito.getDatosReceptor()).isEqualTo(original.getDatosReceptor());
    }
    @Test
    @DisplayName("generarPdf delega en FacturaPdfPort y retorna los bytes del PDF")
    void generarPdfDelegaEnFacturaPdfPort() {
        UUID facturaId = UUID.randomUUID();
        Factura factura = Factura.original(
                UUID.randomUUID(), "F-000100",
                Dinero.cop(new BigDecimal("100000")), Dinero.cop(new BigDecimal("19000")), Dinero.cop(new BigDecimal("119000")),
                new BigDecimal("19.00"), true,
                "{\"nit\":\"900123456-8\",\"razon_social\":\"Construcciones SAS\"}",
                "{\"nombre\":\"Juan\",\"apellido\":\"Pérez\"}"
        );
        byte[] expectedPdf = "%PDF-1.4 mock-pdf-bytes".getBytes();
        when(facturaRepository.findById(facturaId)).thenReturn(Optional.of(factura));
        when(facturaPdfPort.generarPdf(factura)).thenReturn(expectedPdf);

        byte[] actualPdf = facturaService.generarPdf(facturaId);

        assertThat(actualPdf).isEqualTo(expectedPdf);
        verify(facturaPdfPort).generarPdf(factura);
    }

    @Test
    @DisplayName("listarPorUsuario para ADMIN retorna todas las facturas")
    void listarPorUsuarioAdminRetornaTodas() {
        Factura f1 = Factura.original(UUID.randomUUID(), "F-001", Dinero.cop(BigDecimal.valueOf(100)), Dinero.cero(), Dinero.cop(BigDecimal.valueOf(100)), BigDecimal.ZERO, false, "{}", "{}");
        Factura f2 = Factura.original(UUID.randomUUID(), "F-002", Dinero.cop(BigDecimal.valueOf(200)), Dinero.cero(), Dinero.cop(BigDecimal.valueOf(200)), BigDecimal.ZERO, false, "{}", "{}");
        when(facturaRepository.findAll()).thenReturn(java.util.List.of(f1, f2));

        var res = facturaService.listarPorUsuario(UUID.randomUUID(), UserRol.ADMIN);
        assertThat(res).hasSize(2);
    }

    @Test
    @DisplayName("listarPorUsuario para CLIENTE retorna solo las facturas de sus reservas")
    void listarPorUsuarioClienteRetornaSoloPropias() {
        UUID clienteId = UUID.randomUUID();
        UUID miReservaId = UUID.randomUUID();
        UUID otraReservaId = UUID.randomUUID();

        Reserva miReserva = new Reserva(miReservaId, clienteId, ReservaEstado.CONFIRMADA, LocalDate.now(), LocalDate.now().plusDays(2), Instant.now(), null);
        when(reservaRepository.findByClienteId(clienteId)).thenReturn(java.util.List.of(miReserva));

        Factura miFactura = Factura.original(miReservaId, "F-001", Dinero.cop(BigDecimal.valueOf(100)), Dinero.cero(), Dinero.cop(BigDecimal.valueOf(100)), BigDecimal.ZERO, false, "{}", "{}");
        Factura otraFactura = Factura.original(otraReservaId, "F-002", Dinero.cop(BigDecimal.valueOf(200)), Dinero.cero(), Dinero.cop(BigDecimal.valueOf(200)), BigDecimal.ZERO, false, "{}", "{}");

        when(facturaRepository.findAll()).thenReturn(java.util.List.of(miFactura, otraFactura));

        var res = facturaService.listarPorUsuario(clienteId, UserRol.CLIENTE);
        assertThat(res).hasSize(1);
        assertThat(res.get(0).getNumeroFactura()).isEqualTo("F-001");
    }

    @Test
    @DisplayName("tieneAcceso deniega a CLIENTE ajeno descargar PDF de factura - Prevención IDOR")
    void tieneAccesoDeniegaClienteAjeno() {
        UUID facturaId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();
        UUID clienteTitularId = UUID.randomUUID();
        UUID atacanteId = UUID.randomUUID();

        Factura factura = Factura.original(reservaId, "F-999", Dinero.cop(BigDecimal.valueOf(500)), Dinero.cero(), Dinero.cop(BigDecimal.valueOf(500)), BigDecimal.ZERO, false, "{}", "{}");
        Reserva reserva = new Reserva(reservaId, clienteTitularId, ReservaEstado.CONFIRMADA, LocalDate.now(), LocalDate.now().plusDays(1), Instant.now(), null);

        when(facturaRepository.findById(facturaId)).thenReturn(Optional.of(factura));
        when(reservaRepository.findById(reservaId)).thenReturn(Optional.of(reserva));

        boolean permitido = facturaService.tieneAcceso(facturaId, atacanteId, UserRol.CLIENTE);
        assertThat(permitido).isFalse();
    }

    @Test
    @DisplayName("tieneAcceso permite a CLIENTE titular descargar su propia factura")
    void tieneAccesoPermiteClienteTitular() {
        UUID facturaId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();
        UUID clienteTitularId = UUID.randomUUID();

        Factura factura = Factura.original(reservaId, "F-999", Dinero.cop(BigDecimal.valueOf(500)), Dinero.cero(), Dinero.cop(BigDecimal.valueOf(500)), BigDecimal.ZERO, false, "{}", "{}");
        Reserva reserva = new Reserva(reservaId, clienteTitularId, ReservaEstado.CONFIRMADA, LocalDate.now(), LocalDate.now().plusDays(1), Instant.now(), null);

        when(facturaRepository.findById(facturaId)).thenReturn(Optional.of(factura));
        when(reservaRepository.findById(reservaId)).thenReturn(Optional.of(reserva));

        boolean permitido = facturaService.tieneAcceso(facturaId, clienteTitularId, UserRol.CLIENTE);
        assertThat(permitido).isTrue();
    }

    @Test
    @DisplayName("emitirFacturaReparacion crea factura pendiente con descripcion del dano")
    void emitirFacturaReparacionCreaFacturaPendiente() {
        UUID reservaId = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        UUID herramientaId = UUID.randomUUID();
        UUID propietarioId = UUID.randomUUID();

        Reserva reserva = new Reserva(
                reservaId, clienteId, ReservaEstado.FINALIZADA,
                LocalDate.now(), LocalDate.now().plusDays(1),
                Instant.now(), null
        );
        reserva.agregarDetalle(DetalleReserva.crear(
                reservaId, herramientaId, 1,
                Dinero.cop(new BigDecimal("50000")),
                LocalDate.now(), LocalDate.now().plusDays(1),
                new BigDecimal("19.00"), true
        ));

        Herramienta herramienta = new Herramienta(
                herramientaId, UUID.randomUUID(), propietarioId,
                "Taladro Percutor", "Desc", "Bosch", "GSB-13",
                Dinero.cop(new BigDecimal("50000")), 5,
                HerramientaPubEstado.PUBLICADA, new BigDecimal("19.00"), true,
                Instant.now(), Instant.now()
        );

        Propietario propietario = new Propietario(
                propietarioId, Email.of("propietario@construccion.com"),
                "Equipos del Norte SAS", "Cra 10 # 20-30", "3101234567", "COMUN", "Bogota", "Cundinamarca", Instant.now()
        );

        Cliente cliente = new Cliente(
                clienteId, Email.of("cliente@empresa.com"), "Carlos", "Gómez", "3009876543",
                "Calle 100 # 15-20", "{}"
        );

        Dinero costo = Dinero.cop(BigDecimal.valueOf(100000));
        String descripcion = "Motor quemado por sobrecarga";

        when(facturaRepository.findReparacionByReservaId(reservaId)).thenReturn(Optional.empty());
        when(herramientaRepository.findById(herramientaId)).thenReturn(Optional.of(herramienta));
        when(propietarioRepository.findById(propietarioId)).thenReturn(Optional.of(propietario));
        when(clienteRepository.findByUsuarioId(clienteId)).thenReturn(Optional.of(cliente));
        when(facturaRepository.save(any(Factura.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Factura factura = facturaService.emitirFacturaReparacion(reserva, costo, descripcion);

        assertThat(factura).isNotNull();
        assertThat(factura.getTipo()).isEqualTo(FacturaTipo.REPARACION);
        assertThat(factura.getEstado()).isEqualTo("PENDIENTE");
        assertThat(factura.getDescripcionDano()).isEqualTo(descripcion);
        assertThat(factura.getSubtotal().getMonto()).isEqualByComparingTo(BigDecimal.valueOf(100000));
        assertThat(factura.getTotal().getMonto()).isEqualByComparingTo(BigDecimal.valueOf(100000));
        assertThat(factura.getNumeroFactura()).startsWith("REP-");
    }

    @Test
    @DisplayName("emitirFacturaReparacion es idempotente y retorna factura existente sin duplicar")
    void emitirFacturaReparacionEsIdempotente() {
        UUID reservaId = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        Reserva reserva = new Reserva(reservaId, clienteId, ReservaEstado.FINALIZADA, LocalDate.now(), LocalDate.now().plusDays(1), Instant.now(), null);
        Dinero costo = Dinero.cop(BigDecimal.valueOf(100000));
        String descripcion = "Motor quemado por sobrecarga";

        Factura existente = Factura.reparacion(reservaId, "REP-EXISTENTE", costo, Dinero.cero(), costo, BigDecimal.ZERO, false, descripcion, "{}", "{}");
        when(facturaRepository.findReparacionByReservaId(reservaId)).thenReturn(Optional.of(existente));

        Factura resultado = facturaService.emitirFacturaReparacion(reserva, costo, descripcion);

        assertThat(resultado).isSameAs(existente);
        verify(facturaRepository, never()).save(any(Factura.class));
    }
}
