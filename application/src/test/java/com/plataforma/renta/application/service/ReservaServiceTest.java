package com.plataforma.renta.application.service;

import com.plataforma.renta.domain.port.out.PagoRepository;

import com.plataforma.renta.application.command.CancelarReservaCommand;
import com.plataforma.renta.application.command.CrearReservaCommand;
import com.plataforma.renta.domain.exception.OperacionInvalidaException;
import com.plataforma.renta.domain.model.DetalleReserva;
import com.plataforma.renta.domain.model.Herramienta;
import com.plataforma.renta.domain.model.HerramientaPubEstado;
import com.plataforma.renta.domain.model.Reserva;
import com.plataforma.renta.domain.model.ReservaEstado;
import com.plataforma.renta.domain.model.UserRol;
import com.plataforma.renta.domain.port.out.AdvisoryLockService;
import com.plataforma.renta.domain.port.out.ConfiguracionFiscalRepository;
import com.plataforma.renta.domain.port.out.DisponibilidadRepository;
import com.plataforma.renta.domain.port.out.HerramientaRepository;
import com.plataforma.renta.domain.port.out.ReservaRepository;
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
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private HerramientaRepository herramientaRepository;
    @Mock
    private AdvisoryLockService advisoryLockService;
    @Mock
    private ConfiguracionFiscalRepository configuracionFiscalRepository;
    @Mock
    private DisponibilidadRepository disponibilidadRepository;
    @Mock
    private PagoRepository pagoRepository;

    private ReservaService reservaService;

    @BeforeEach
    void setUp() {
        reservaService = new ReservaService(
                reservaRepository,
                herramientaRepository,
                advisoryLockService,
                configuracionFiscalRepository,
                disponibilidadRepository,
                pagoRepository
        );
    }

    @Test
    @DisplayName("CLIENTE creando una reserva para sí mismo tiene éxito")
    void clienteCreaReservaParaSiMismoExito() {
        UUID clienteId = UUID.randomUUID();
        UUID herramientaId = UUID.randomUUID();
        LocalDate inicio = LocalDate.now().plusDays(1);
        LocalDate fin = LocalDate.now().plusDays(3);

        CrearReservaCommand cmd = new CrearReservaCommand(
                clienteId, inicio, fin,
                List.of(new CrearReservaCommand.DetalleCommand(herramientaId, 2))
        );

        Herramienta herramienta = new Herramienta(
                herramientaId, UUID.randomUUID(), UUID.randomUUID(),
                "Taladro", "Desc", "Marca", "Modelo",
                Dinero.cop(new BigDecimal("50000")), 5,
                HerramientaPubEstado.PUBLICADA, new BigDecimal("19.00"), true,
                Instant.now(), Instant.now()
        );

        when(herramientaRepository.findByIdsForUpdate(List.of(herramientaId))).thenReturn(List.of(herramienta));
        when(disponibilidadRepository.calcularDisponibilidad(herramientaId, inicio, fin, null)).thenReturn(5);
        when(configuracionFiscalRepository.findValor("reserva_pendiente_expiracion_minutos")).thenReturn(Optional.of("30"));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reserva reserva = reservaService.crearReserva(cmd, clienteId, UserRol.CLIENTE);

        assertThat(reserva).isNotNull();
        assertThat(reserva.getClienteId()).isEqualTo(clienteId);
        assertThat(reserva.getEstado()).isEqualTo(ReservaEstado.PENDIENTE);
        verify(advisoryLockService).acquireLocks(any());
        verify(advisoryLockService).releaseLocks();
    }

    @Test
    @DisplayName("CLIENTE intentando crear una reserva para otro usuario falla con OperacionInvalidaException")
    void clienteNoPuedeCrearReservaParaOtroUsuario() {
        UUID titularId = UUID.randomUUID();
        UUID otroUsuarioId = UUID.randomUUID();

        CrearReservaCommand cmd = new CrearReservaCommand(
                titularId, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                List.of(new CrearReservaCommand.DetalleCommand(UUID.randomUUID(), 1))
        );

        assertThatThrownBy(() -> reservaService.crearReserva(cmd, otroUsuarioId, UserRol.CLIENTE))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("No autorizado: solo el cliente titular puede crear reservas");

        verifyNoInteractions(herramientaRepository);
    }

    @Test
    @DisplayName("CLIENTE titular puede cancelar su propia reserva")
    void clienteTitularPuedeCancelarReserva() {
        UUID clienteId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();
        UUID herramientaId = UUID.randomUUID();

        Reserva reserva = new Reserva(
                reservaId, clienteId, ReservaEstado.PENDIENTE,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                Instant.now(), Instant.now().plusSeconds(1800)
        );
        reserva.agregarDetalle(DetalleReserva.crear(
                reservaId, herramientaId, 1,
                Dinero.cop(new BigDecimal("10000")),
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                new BigDecimal("19.00"), true
        ));

        when(reservaRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CancelarReservaCommand cmd = new CancelarReservaCommand(reservaId, "Motivo test", clienteId);
        reservaService.cancelarReserva(cmd, clienteId, UserRol.CLIENTE);

        assertThat(reserva.getEstado()).isEqualTo(ReservaEstado.CANCELADA);
        verify(reservaRepository).save(reserva);
    }

    @Test
    @DisplayName("CLIENTE no titular no puede cancelar reserva de otro")
    void clienteNoTitularNoPuedeCancelar() {
        UUID titularId = UUID.randomUUID();
        UUID otroClienteId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();

        Reserva reserva = new Reserva(
                reservaId, titularId, ReservaEstado.PENDIENTE,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                Instant.now(), Instant.now().plusSeconds(1800)
        );

        when(reservaRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));

        CancelarReservaCommand cmd = new CancelarReservaCommand(reservaId, "Motivo", otroClienteId);

        assertThatThrownBy(() -> reservaService.cancelarReserva(cmd, otroClienteId, UserRol.CLIENTE))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("No autorizado: solo el cliente titular o un administrador pueden cancelar esta reserva");
    }

    @Test
    @DisplayName("PROPIETARIO no puede cancelar reserva aunque sea dueño de herramienta")
    void propietarioNoPuedeCancelarReserva() {
        UUID titularId = UUID.randomUUID();
        UUID propietarioId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();

        Reserva reserva = new Reserva(
                reservaId, titularId, ReservaEstado.PENDIENTE,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                Instant.now(), Instant.now().plusSeconds(1800)
        );

        when(reservaRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));

        CancelarReservaCommand cmd = new CancelarReservaCommand(reservaId, "Motivo", propietarioId);

        assertThatThrownBy(() -> reservaService.cancelarReserva(cmd, propietarioId, UserRol.PROPIETARIO))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("No autorizado");
    }

    @Test
    @DisplayName("ADMIN puede cancelar cualquier reserva")
    void adminPuedeCancelarCualquierReserva() {
        UUID titularId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID reservaId = UUID.randomUUID();
        UUID herramientaId = UUID.randomUUID();

        Reserva reserva = new Reserva(
                reservaId, titularId, ReservaEstado.PENDIENTE,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                Instant.now(), Instant.now().plusSeconds(1800)
        );
        reserva.agregarDetalle(DetalleReserva.crear(
                reservaId, herramientaId, 1,
                Dinero.cop(new BigDecimal("10000")),
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(2),
                new BigDecimal("19.00"), true
        ));

        when(reservaRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CancelarReservaCommand cmd = new CancelarReservaCommand(reservaId, "Cancelación admin", adminId);
        reservaService.cancelarReserva(cmd, adminId, UserRol.ADMIN);

        assertThat(reserva.getEstado()).isEqualTo(ReservaEstado.CANCELADA);
        verify(reservaRepository).save(reserva);
    }

    @Test
    @DisplayName("Entregar equipo sin pago completado lanza OperacionInvalidaException")
    void entregarEquipoSinPagoLanzaExcepcion() {
        UUID reservaId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID toolId = UUID.randomUUID();

        Reserva reserva = new Reserva(
                reservaId, UUID.randomUUID(), ReservaEstado.CONFIRMADA,
                LocalDate.now(), LocalDate.now().plusDays(2),
                Instant.now(), Instant.now().plusSeconds(1800)
        );
        reserva.agregarDetalle(DetalleReserva.crear(
                reservaId, toolId, 1, Dinero.cop(new BigDecimal("100000")),
                LocalDate.now(), LocalDate.now().plusDays(2), new BigDecimal("19.00"), true
        ));

        Herramienta tool = new Herramienta(
                toolId, UUID.randomUUID(), ownerId, "Excavadora", "Desc", "CAT", "320",
                Dinero.cop(new BigDecimal("100000")), 2, HerramientaPubEstado.PUBLICADA,
                new BigDecimal("19.00"), true, Instant.now(), Instant.now()
        );

        when(reservaRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
        when(herramientaRepository.findByPropietarioId(ownerId)).thenReturn(List.of(tool));
        when(pagoRepository.existsCompletadoByReservaId(reservaId)).thenReturn(false);

        assertThatThrownBy(() -> reservaService.entregarEquipo(reservaId, ownerId, UserRol.PROPIETARIO))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("el pago requerido no se ha completado");
    }

    @Test
    @DisplayName("Entregar equipo con pago completado transiciona reserva a EN_CURSO")
    void entregarEquipoConPagoCompletadoPasaAEnCurso() {
        UUID reservaId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID toolId = UUID.randomUUID();

        Reserva reserva = new Reserva(
                reservaId, UUID.randomUUID(), ReservaEstado.CONFIRMADA,
                LocalDate.now(), LocalDate.now().plusDays(2),
                Instant.now(), Instant.now().plusSeconds(1800)
        );
        reserva.agregarDetalle(DetalleReserva.crear(
                reservaId, toolId, 1, Dinero.cop(new BigDecimal("100000")),
                LocalDate.now(), LocalDate.now().plusDays(2), new BigDecimal("19.00"), true
        ));

        Herramienta tool = new Herramienta(
                toolId, UUID.randomUUID(), ownerId, "Excavadora", "Desc", "CAT", "320",
                Dinero.cop(new BigDecimal("100000")), 2, HerramientaPubEstado.PUBLICADA,
                new BigDecimal("19.00"), true, Instant.now(), Instant.now()
        );

        when(reservaRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
        when(herramientaRepository.findByPropietarioId(ownerId)).thenReturn(List.of(tool));
        when(pagoRepository.existsCompletadoByReservaId(reservaId)).thenReturn(true);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva resultado = reservaService.entregarEquipo(reservaId, ownerId, UserRol.PROPIETARIO);

        assertThat(resultado.getEstado()).isEqualTo(ReservaEstado.EN_CURSO);
        verify(reservaRepository).save(reserva);
    }

    @Test
    @DisplayName("Usuario no propietario de la herramienta no puede entregar equipo")
    void usuarioNoPropietarioNoPuedeEntregar() {
        UUID reservaId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otroUsuarioId = UUID.randomUUID();
        UUID toolId = UUID.randomUUID();

        Reserva reserva = new Reserva(
                reservaId, UUID.randomUUID(), ReservaEstado.CONFIRMADA,
                LocalDate.now(), LocalDate.now().plusDays(2),
                Instant.now(), Instant.now().plusSeconds(1800)
        );
        reserva.agregarDetalle(DetalleReserva.crear(
                reservaId, toolId, 1, Dinero.cop(new BigDecimal("100000")),
                LocalDate.now(), LocalDate.now().plusDays(2), new BigDecimal("19.00"), true
        ));

        when(reservaRepository.findByIdForUpdate(reservaId)).thenReturn(Optional.of(reserva));
        when(herramientaRepository.findByPropietarioId(otroUsuarioId)).thenReturn(List.of());

        assertThatThrownBy(() -> reservaService.entregarEquipo(reservaId, otroUsuarioId, UserRol.PROPIETARIO))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("no es el propietario");
    }
}
