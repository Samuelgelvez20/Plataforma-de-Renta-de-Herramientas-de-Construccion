package com.plataforma.renta.application.service;

import com.plataforma.renta.application.command.CrearReembolsoCommand;
import com.plataforma.renta.domain.exception.EntidadNoEncontradaException;
import com.plataforma.renta.domain.exception.EstadoInvalidoException;
import com.plataforma.renta.domain.exception.OperacionInvalidaException;
import com.plataforma.renta.domain.model.*;
import com.plataforma.renta.domain.port.out.*;
import com.plataforma.renta.domain.value.Dinero;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private AdvisoryLockService advisoryLockService;
    @Mock
    private FacturaRepository facturaRepository;
    @Mock
    private ConfiguracionFiscalRepository configuracionFiscalRepository;
    @Mock
    private HerramientaRepository herramientaRepository;
    @Mock
    private FacturaService facturaService;
    @Mock
    private DisponibilidadRepository disponibilidadRepository;

    private PagoService pagoService;

    @BeforeEach
    void setUp() {
        pagoService = new PagoService(
                pagoRepository,
                reservaRepository,
                facturaRepository,
                herramientaRepository,
                advisoryLockService,
                configuracionFiscalRepository,
                facturaService,
                disponibilidadRepository
        );
    }

    @Test
    @DisplayName("CLIENTE titular puede solicitar reembolso de su reserva confirmada")
    void clienteTitularSolicitaReembolsoExito() {
        UUID clienteId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();
        UUID pagoOriginalId = UUID.randomUUID();

        Reserva reserva = new Reserva(
                reservaId, clienteId, ReservaEstado.CONFIRMADA,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                Instant.now(), null
        );

        Pago pagoOriginal = new Pago(
                pagoOriginalId, reservaId, null,
                PagoEstado.COMPLETADO, "SIMULADO", "TX-1",
                Dinero.cop(new BigDecimal("100000")), "Pago inicial",
                Instant.now(), Instant.now()
        );

        when(reservaRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
        when(pagoRepository.findCompletadoByReservaIdForUpdate(reservaId)).thenReturn(Optional.of(pagoOriginal));
        when(pagoRepository.sumMontoByPagoOriginalIdAndEstado(pagoOriginalId, PagoEstado.REEMBOLSADO)).thenReturn(BigDecimal.ZERO);
        when(pagoRepository.existsByPagoOriginalIdAndEstado(pagoOriginalId, PagoEstado.PENDIENTE)).thenReturn(false);

        CrearReembolsoCommand cmd = new CrearReembolsoCommand(reservaId, Dinero.cop(new BigDecimal("50000")), "Devolución acordada");
        Pago reembolso = pagoService.crearReembolso(cmd, clienteId, UserRol.CLIENTE);

        assertThat(reembolso).isNotNull();
        assertThat(reembolso.getEstado()).isEqualTo(PagoEstado.PENDIENTE);
        assertThat(reembolso.getPagoOriginalId()).isEqualTo(pagoOriginalId);
        verify(pagoRepository).save(any(Pago.class));
    }

    @Test
    @DisplayName("Otro CLIENTE no puede solicitar reembolso de una reserva ajena")
    void otroClienteNoPuedeSolicitarReembolso() {
        UUID titularId = UUID.randomUUID();
        UUID intrusoId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();

        Reserva reserva = new Reserva(
                reservaId, titularId, ReservaEstado.CONFIRMADA,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                Instant.now(), null
        );

        when(reservaRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));

        CrearReembolsoCommand cmd = new CrearReembolsoCommand(reservaId, Dinero.cop(new BigDecimal("50000")), "Devolución");

        assertThatThrownBy(() -> pagoService.crearReembolso(cmd, intrusoId, UserRol.CLIENTE))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("No autorizado: solo el cliente titular puede solicitar un reembolso");
    }

    @Test
    @DisplayName("PROPIETARIO no puede solicitar reembolso")
    void propietarioNoPuedeSolicitarReembolso() {
        UUID propietarioId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();

        Reserva reserva = new Reserva(
                reservaId, UUID.randomUUID(), ReservaEstado.CONFIRMADA,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                Instant.now(), null
        );

        when(reservaRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));

        CrearReembolsoCommand cmd = new CrearReembolsoCommand(reservaId, Dinero.cop(new BigDecimal("50000")), "Devolución");

        assertThatThrownBy(() -> pagoService.crearReembolso(cmd, propietarioId, UserRol.PROPIETARIO))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("No autorizado: solo el cliente titular puede solicitar un reembolso");
    }

    @Test
    @DisplayName("No se permite solicitar reembolso si ya existe una solicitud PENDIENTE para el mismo pago")
    void rechazaReembolsoDuplicadoSiExistePendiente() {
        UUID clienteId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();
        UUID pagoOriginalId = UUID.randomUUID();

        Reserva reserva = new Reserva(
                reservaId, clienteId, ReservaEstado.CONFIRMADA,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                Instant.now(), null
        );

        Pago pagoOriginal = new Pago(
                pagoOriginalId, reservaId, null,
                PagoEstado.COMPLETADO, "SIMULADO", "TX-1",
                Dinero.cop(new BigDecimal("100000")), "Pago inicial",
                Instant.now(), Instant.now()
        );

        when(reservaRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
        when(pagoRepository.findCompletadoByReservaIdForUpdate(reservaId)).thenReturn(Optional.of(pagoOriginal));
        when(pagoRepository.sumMontoByPagoOriginalIdAndEstado(pagoOriginalId, PagoEstado.REEMBOLSADO)).thenReturn(BigDecimal.ZERO);
        when(pagoRepository.existsByPagoOriginalIdAndEstado(pagoOriginalId, PagoEstado.PENDIENTE)).thenReturn(true);

        CrearReembolsoCommand cmd = new CrearReembolsoCommand(reservaId, Dinero.cop(new BigDecimal("50000")), "Devolución");

        assertThatThrownBy(() -> pagoService.crearReembolso(cmd, clienteId, UserRol.CLIENTE))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("Ya existe una solicitud de reembolso pendiente");
    }

    @Test
    @DisplayName("CLIENTE nunca puede aprobar reembolsos")
    void clienteNuncaPuedeAprobarReembolsos() {
        UUID clienteId = UUID.randomUUID();
        UUID reembolsoId = UUID.randomUUID();

        assertThatThrownBy(() -> pagoService.aprobarReembolso(reembolsoId, clienteId, UserRol.CLIENTE))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("No autorizado: el cliente no puede aprobar reembolsos");

        verifyNoInteractions(pagoRepository);
    }

    @Test
    @DisplayName("ADMIN puede aprobar un reembolso pendiente")
    void adminPuedeAprobarReembolsoPendiente() {
        UUID adminId = UUID.randomUUID();
        UUID reembolsoId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();
        UUID pagoOriginalId = UUID.randomUUID();

        Pago reembolso = new Pago(
                reembolsoId, reservaId, pagoOriginalId,
                PagoEstado.PENDIENTE, "SIMULADO", null,
                Dinero.cop(new BigDecimal("50000")), "Reembolso",
                Instant.now(), Instant.now()
        );

        Reserva reserva = new Reserva(
                reservaId, UUID.randomUUID(), ReservaEstado.CONFIRMADA,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                Instant.now(), null
        );

        when(pagoRepository.findByIdForUpdate(reembolsoId)).thenReturn(Optional.of(reembolso));
        when(reservaRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));

        pagoService.aprobarReembolso(reembolsoId, adminId, UserRol.ADMIN);

        assertThat(reembolso.getEstado()).isEqualTo(PagoEstado.REEMBOLSADO);
        verify(pagoRepository).save(reembolso);
        verify(facturaService).emitirNotaCredito(eq(reserva), eq(reembolso.getMonto()), any(UUID.class));
    }

    @Test
    @DisplayName("PROPIETARIO relacionado puede aprobar un reembolso")
    void propietarioRelacionadoPuedeAprobarReembolso() {
        UUID propietarioId = UUID.randomUUID();
        UUID reembolsoId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();
        UUID herramientaId = UUID.randomUUID();
        UUID pagoOriginalId = UUID.randomUUID();

        Pago reembolso = new Pago(
                reembolsoId, reservaId, pagoOriginalId,
                PagoEstado.PENDIENTE, "SIMULADO", null,
                Dinero.cop(new BigDecimal("50000")), "Reembolso",
                Instant.now(), Instant.now()
        );

        Reserva reserva = new Reserva(
                reservaId, UUID.randomUUID(), ReservaEstado.CONFIRMADA,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                Instant.now(), null
        );
        reserva.agregarDetalle(DetalleReserva.crear(
                reservaId, herramientaId, 1,
                Dinero.cop(new BigDecimal("50000")),
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                new BigDecimal("19.00"), true
        ));

        Herramienta herramienta = new Herramienta(
                herramientaId, UUID.randomUUID(), propietarioId,
                "Taladro", "Desc", "Marca", "Modelo",
                Dinero.cop(new BigDecimal("50000")), 1,
                HerramientaPubEstado.PUBLICADA, new BigDecimal("19.00"), true,
                Instant.now(), Instant.now()
        );

        when(pagoRepository.findByIdForUpdate(reembolsoId)).thenReturn(Optional.of(reembolso));
        when(reservaRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
        when(herramientaRepository.findByIdsForUpdate(List.of(herramientaId))).thenReturn(List.of(herramienta));

        pagoService.aprobarReembolso(reembolsoId, propietarioId, UserRol.PROPIETARIO);

        assertThat(reembolso.getEstado()).isEqualTo(PagoEstado.REEMBOLSADO);
        verify(pagoRepository).save(reembolso);
    }

    @Test
    @DisplayName("PROPIETARIO no relacionado no puede aprobar reembolso")
    void propietarioNoRelacionadoNoPuedeAprobarReembolso() {
        UUID propietarioAjenoId = UUID.randomUUID();
        UUID propietarioRealId = UUID.randomUUID();
        UUID reembolsoId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();
        UUID herramientaId = UUID.randomUUID();

        Pago reembolso = new Pago(
                reembolsoId, reservaId, UUID.randomUUID(),
                PagoEstado.PENDIENTE, "SIMULADO", null,
                Dinero.cop(new BigDecimal("50000")), "Reembolso",
                Instant.now(), Instant.now()
        );

        Reserva reserva = new Reserva(
                reservaId, UUID.randomUUID(), ReservaEstado.CONFIRMADA,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                Instant.now(), null
        );
        reserva.agregarDetalle(DetalleReserva.crear(
                reservaId, herramientaId, 1,
                Dinero.cop(new BigDecimal("50000")),
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                new BigDecimal("19.00"), true
        ));

        Herramienta herramienta = new Herramienta(
                herramientaId, UUID.randomUUID(), propietarioRealId,
                "Taladro", "Desc", "Marca", "Modelo",
                Dinero.cop(new BigDecimal("50000")), 1,
                HerramientaPubEstado.PUBLICADA, new BigDecimal("19.00"), true,
                Instant.now(), Instant.now()
        );

        when(pagoRepository.findByIdForUpdate(reembolsoId)).thenReturn(Optional.of(reembolso));
        when(reservaRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
        when(herramientaRepository.findByIdsForUpdate(List.of(herramientaId))).thenReturn(List.of(herramienta));

        assertThatThrownBy(() -> pagoService.aprobarReembolso(reembolsoId, propietarioAjenoId, UserRol.PROPIETARIO))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("No autorizado");
    }

    @Test
    @DisplayName("listarReembolsosPorUsuario para ADMIN retorna todos los reembolsos")
    void listarReembolsosPorUsuarioAdminRetornaTodos() {
        Pago r1 = new Pago(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), PagoEstado.PENDIENTE, "SIMULADO", null, Dinero.cop(BigDecimal.valueOf(100)), "Motivo 1", Instant.now(), Instant.now());
        Pago r2 = new Pago(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), PagoEstado.REEMBOLSADO, "SIMULADO", null, Dinero.cop(BigDecimal.valueOf(200)), "Motivo 2", Instant.now(), Instant.now());
        when(pagoRepository.findAll()).thenReturn(List.of(r1, r2));

        var res = pagoService.listarReembolsosPorUsuario(UUID.randomUUID(), UserRol.ADMIN);
        assertThat(res).hasSize(2);
    }

    @Test
    @DisplayName("listarReembolsosPorUsuario para CLIENTE retorna solo los de sus reservas")
    void listarReembolsosPorUsuarioClienteRetornaSoloPropios() {
        UUID clienteId = UUID.randomUUID();
        UUID miReservaId = UUID.randomUUID();
        UUID otraReservaId = UUID.randomUUID();

        Reserva miReserva = new Reserva(miReservaId, clienteId, ReservaEstado.CONFIRMADA, LocalDate.now(), LocalDate.now().plusDays(2), Instant.now(), null);
        when(reservaRepository.findByClienteId(clienteId)).thenReturn(List.of(miReserva));

        Pago miReembolso = new Pago(UUID.randomUUID(), miReservaId, UUID.randomUUID(), PagoEstado.PENDIENTE, "SIMULADO", null, Dinero.cop(BigDecimal.valueOf(100)), "Motivo 1", Instant.now(), Instant.now());
        Pago otroReembolso = new Pago(UUID.randomUUID(), otraReservaId, UUID.randomUUID(), PagoEstado.PENDIENTE, "SIMULADO", null, Dinero.cop(BigDecimal.valueOf(200)), "Motivo 2", Instant.now(), Instant.now());

        when(pagoRepository.findAll()).thenReturn(List.of(miReembolso, otroReembolso));

        var res = pagoService.listarReembolsosPorUsuario(clienteId, UserRol.CLIENTE);
        assertThat(res).hasSize(1);
        assertThat(res.get(0).getReservaId()).isEqualTo(miReservaId);
    }

    @Test
    @DisplayName("SEC-02: CLIENTE nunca puede rechazar reembolsos")
    void clienteNuncaPuedeRechazarReembolsos() {
        UUID clienteId = UUID.randomUUID();
        UUID reembolsoId = UUID.randomUUID();

        assertThatThrownBy(() -> pagoService.rechazarReembolso(reembolsoId, clienteId, UserRol.CLIENTE))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("No autorizado: el cliente no puede rechazar reembolsos");

        verifyNoInteractions(pagoRepository);
    }

    @Test
    @DisplayName("SEC-02: ADMIN puede rechazar un reembolso pendiente y persiste estado FALLIDO")
    void adminPuedeRechazarReembolsoPendiente() {
        UUID adminId = UUID.randomUUID();
        UUID reembolsoId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();
        UUID pagoOriginalId = UUID.randomUUID();

        Pago reembolso = new Pago(
                reembolsoId, reservaId, pagoOriginalId,
                PagoEstado.PENDIENTE, "SIMULADO", null,
                Dinero.cop(new BigDecimal("50000")), "Motivo inicial",
                Instant.now(), Instant.now()
        );

        Reserva reserva = new Reserva(
                reservaId, UUID.randomUUID(), ReservaEstado.CONFIRMADA,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                Instant.now(), null
        );

        when(pagoRepository.findByIdForUpdate(reembolsoId)).thenReturn(Optional.of(reembolso));
        when(reservaRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));

        pagoService.rechazarReembolso(reembolsoId, "No cumple con la política de garantías", adminId, UserRol.ADMIN);

        assertThat(reembolso.getEstado()).isEqualTo(PagoEstado.FALLIDO);
        assertThat(reembolso.getMotivo()).contains("No cumple con la política de garantías");
        verify(pagoRepository).save(reembolso);
        verifyNoInteractions(facturaService); // No emite nota de crédito si es rechazado
    }

    @Test
    @DisplayName("SEC-02: PROPIETARIO relacionado puede rechazar reembolso de sus herramientas")
    void propietarioRelacionadoPuedeRechazarReembolso() {
        UUID propietarioId = UUID.randomUUID();
        UUID reembolsoId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();
        UUID herramientaId = UUID.randomUUID();
        UUID pagoOriginalId = UUID.randomUUID();

        Pago reembolso = new Pago(
                reembolsoId, reservaId, pagoOriginalId,
                PagoEstado.PENDIENTE, "SIMULADO", null,
                Dinero.cop(new BigDecimal("50000")), "Reembolso",
                Instant.now(), Instant.now()
        );

        Reserva reserva = new Reserva(
                reservaId, UUID.randomUUID(), ReservaEstado.CONFIRMADA,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                Instant.now(), null
        );
        reserva.agregarDetalle(DetalleReserva.crear(
                reservaId, herramientaId, 1,
                Dinero.cop(new BigDecimal("50000")),
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                new BigDecimal("19.00"), true
        ));

        Herramienta herramienta = new Herramienta(
                herramientaId, UUID.randomUUID(), propietarioId,
                "Taladro", "Desc", "Marca", "Modelo",
                Dinero.cop(new BigDecimal("50000")), 1,
                HerramientaPubEstado.PUBLICADA, new BigDecimal("19.00"), true,
                Instant.now(), Instant.now()
        );

        when(pagoRepository.findByIdForUpdate(reembolsoId)).thenReturn(Optional.of(reembolso));
        when(reservaRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
        when(herramientaRepository.findByIdsForUpdate(List.of(herramientaId))).thenReturn(List.of(herramienta));

        pagoService.rechazarReembolso(reembolsoId, "Herramienta ya usada", propietarioId, UserRol.PROPIETARIO);

        assertThat(reembolso.getEstado()).isEqualTo(PagoEstado.FALLIDO);
        verify(pagoRepository).save(reembolso);
    }

    @Test
    @DisplayName("SEC-02: PROPIETARIO no relacionado no puede rechazar reembolso ajeno")
    void propietarioNoRelacionadoNoPuedeRechazarReembolso() {
        UUID propietarioAjenoId = UUID.randomUUID();
        UUID propietarioRealId = UUID.randomUUID();
        UUID reembolsoId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();
        UUID herramientaId = UUID.randomUUID();

        Pago reembolso = new Pago(
                reembolsoId, reservaId, UUID.randomUUID(),
                PagoEstado.PENDIENTE, "SIMULADO", null,
                Dinero.cop(new BigDecimal("50000")), "Reembolso",
                Instant.now(), Instant.now()
        );

        Reserva reserva = new Reserva(
                reservaId, UUID.randomUUID(), ReservaEstado.CONFIRMADA,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                Instant.now(), null
        );
        reserva.agregarDetalle(DetalleReserva.crear(
                reservaId, herramientaId, 1,
                Dinero.cop(new BigDecimal("50000")),
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                new BigDecimal("19.00"), true
        ));

        Herramienta herramienta = new Herramienta(
                herramientaId, UUID.randomUUID(), propietarioRealId,
                "Taladro", "Desc", "Marca", "Modelo",
                Dinero.cop(new BigDecimal("50000")), 1,
                HerramientaPubEstado.PUBLICADA, new BigDecimal("19.00"), true,
                Instant.now(), Instant.now()
        );

        when(pagoRepository.findByIdForUpdate(reembolsoId)).thenReturn(Optional.of(reembolso));
        when(reservaRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
        when(herramientaRepository.findByIdsForUpdate(List.of(herramientaId))).thenReturn(List.of(herramienta));

        assertThatThrownBy(() -> pagoService.rechazarReembolso(reembolsoId, propietarioAjenoId, UserRol.PROPIETARIO))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("No autorizado");
    }

    @Test
    @DisplayName("SEC-02: Rechazar reembolso inexistente lanza EntidadNoEncontradaException")
    void rechazarReembolsoInexistenteLanzaExcepcion() {
        UUID noExisteId = UUID.randomUUID();
        when(pagoRepository.findByIdForUpdate(noExisteId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pagoService.rechazarReembolso(noExisteId, UUID.randomUUID(), UserRol.ADMIN))
                .isInstanceOf(EntidadNoEncontradaException.class)
                .hasMessageContaining("Reembolso no encontrado");
    }

    @Test
    @DisplayName("SEC-02: No se puede rechazar un reembolso que ya fue resuelto")
    void noSePuedeRechazarReembolsoYaResuelto() {
        UUID reembolsoId = UUID.randomUUID();
        Pago reembolsoYaResuelto = new Pago(
                reembolsoId, UUID.randomUUID(), UUID.randomUUID(),
                PagoEstado.REEMBOLSADO, "SIMULADO", null,
                Dinero.cop(new BigDecimal("50000")), "Reembolso",
                Instant.now(), Instant.now()
        );

        when(pagoRepository.findByIdForUpdate(reembolsoId)).thenReturn(Optional.of(reembolsoYaResuelto));

        assertThatThrownBy(() -> pagoService.rechazarReembolso(reembolsoId, UUID.randomUUID(), UserRol.ADMIN))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("el reembolso no está en estado pendiente o ya ha sido procesado");
    }

    @Test
    @DisplayName("SEC-02A: Reembolso previo REEMBOLSADO descuenta del saldo disponible y permite solicitud por el remanente")
    void crearReembolso_conReembolsoPrevioAprobado_descuentaSaldoDisponible() {
        UUID clienteId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();
        UUID pagoOriginalId = UUID.randomUUID();

        Reserva reserva = new Reserva(
                reservaId, clienteId, ReservaEstado.CONFIRMADA,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                Instant.now(), null
        );

        Pago pagoOriginal = new Pago(
                pagoOriginalId, reservaId, null,
                PagoEstado.COMPLETADO, "SIMULADO", "TX-1",
                Dinero.cop(new BigDecimal("100000")), "Pago inicial",
                Instant.now(), Instant.now()
        );

        when(reservaRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
        when(pagoRepository.findCompletadoByReservaIdForUpdate(reservaId)).thenReturn(Optional.of(pagoOriginal));
        // Ya se reembolsaron 40.000 COP aprobados
        when(pagoRepository.sumMontoByPagoOriginalIdAndEstado(pagoOriginalId, PagoEstado.REEMBOLSADO))
                .thenReturn(new BigDecimal("40000"));
        when(pagoRepository.existsByPagoOriginalIdAndEstado(pagoOriginalId, PagoEstado.PENDIENTE)).thenReturn(false);

        // Solicitar los 60.000 COP restantes -> DEBE SER PERMITIDO
        CrearReembolsoCommand cmd = new CrearReembolsoCommand(reservaId, Dinero.cop(new BigDecimal("60000")), "Saldo restante");
        Pago nuevoReembolso = pagoService.crearReembolso(cmd, clienteId, UserRol.CLIENTE);

        assertThat(nuevoReembolso).isNotNull();
        assertThat(nuevoReembolso.getMonto().getMonto()).isEqualByComparingTo(new BigDecimal("60000"));
        verify(pagoRepository).save(any(Pago.class));
    }

    @Test
    @DisplayName("SEC-02A: Reembolso previo FALLIDO (rechazado) NO reduce el saldo disponible")
    void crearReembolso_conReembolsoPrevioRechazado_noReduceSaldoDisponible() {
        UUID clienteId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();
        UUID pagoOriginalId = UUID.randomUUID();

        Reserva reserva = new Reserva(
                reservaId, clienteId, ReservaEstado.CONFIRMADA,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                Instant.now(), null
        );

        Pago pagoOriginal = new Pago(
                pagoOriginalId, reservaId, null,
                PagoEstado.COMPLETADO, "SIMULADO", "TX-1",
                Dinero.cop(new BigDecimal("100000")), "Pago inicial",
                Instant.now(), Instant.now()
        );

        when(reservaRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
        when(pagoRepository.findCompletadoByReservaIdForUpdate(reservaId)).thenReturn(Optional.of(pagoOriginal));
        // Como los rechazos quedan en FALLIDO, la suma de REEMBOLSADO retorna CERO
        when(pagoRepository.sumMontoByPagoOriginalIdAndEstado(pagoOriginalId, PagoEstado.REEMBOLSADO))
                .thenReturn(BigDecimal.ZERO);
        when(pagoRepository.existsByPagoOriginalIdAndEstado(pagoOriginalId, PagoEstado.PENDIENTE)).thenReturn(false);

        // Puede solicitar nuevamente el valor completo de 100.000 COP
        CrearReembolsoCommand cmd = new CrearReembolsoCommand(reservaId, Dinero.cop(new BigDecimal("100000")), "Nueva solicitud tras rechazo");
        Pago nuevoReembolso = pagoService.crearReembolso(cmd, clienteId, UserRol.CLIENTE);

        assertThat(nuevoReembolso).isNotNull();
        assertThat(nuevoReembolso.getMonto().getMonto()).isEqualByComparingTo(new BigDecimal("100000"));
        verify(pagoRepository).save(any(Pago.class));
    }

    @Test
    @DisplayName("SEC-02A: Intentar solicitar reembolso por monto mayor al saldo disponible lanza MontoReembolsoInvalidoException")
    void crearReembolso_montoSuperiorAlDisponible_lanzaExcepcion() {
        UUID clienteId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();
        UUID pagoOriginalId = UUID.randomUUID();

        Reserva reserva = new Reserva(
                reservaId, clienteId, ReservaEstado.CONFIRMADA,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                Instant.now(), null
        );

        Pago pagoOriginal = new Pago(
                pagoOriginalId, reservaId, null,
                PagoEstado.COMPLETADO, "SIMULADO", "TX-1",
                Dinero.cop(new BigDecimal("100000")), "Pago inicial",
                Instant.now(), Instant.now()
        );

        when(reservaRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
        when(pagoRepository.findCompletadoByReservaIdForUpdate(reservaId)).thenReturn(Optional.of(pagoOriginal));
        when(pagoRepository.sumMontoByPagoOriginalIdAndEstado(pagoOriginalId, PagoEstado.REEMBOLSADO))
                .thenReturn(new BigDecimal("80000")); // Ya se devolvieron 80.000, solo quedan 20.000

        CrearReembolsoCommand cmd = new CrearReembolsoCommand(reservaId, Dinero.cop(new BigDecimal("20001")), "Exceso");

        assertThatThrownBy(() -> pagoService.crearReembolso(cmd, clienteId, UserRol.CLIENTE))
                .isInstanceOf(com.plataforma.renta.domain.exception.MontoReembolsoInvalidoException.class)
                .hasMessageContaining("Monto inválido. Disponible");
    }
}
