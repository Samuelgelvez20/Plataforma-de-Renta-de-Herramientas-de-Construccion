package com.plataforma.renta.application.service;

import com.plataforma.renta.domain.exception.OperacionInvalidaException;
import com.plataforma.renta.domain.model.Categoria;
import com.plataforma.renta.domain.model.Herramienta;
import com.plataforma.renta.domain.model.HerramientaPubEstado;
import com.plataforma.renta.domain.model.Propietario;
import com.plataforma.renta.domain.model.UserRol;
import com.plataforma.renta.domain.port.out.CategoriaRepository;
import com.plataforma.renta.domain.port.out.HerramientaRepository;
import com.plataforma.renta.domain.port.out.PropietarioRepository;
import com.plataforma.renta.domain.port.out.DisponibilidadRepository;
import com.plataforma.renta.domain.exception.EntidadNoEncontradaException;
import java.time.LocalDate;
import com.plataforma.renta.domain.value.Dinero;
import com.plataforma.renta.domain.value.Email;
import com.plataforma.renta.domain.value.Nit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HerramientaServiceTest {

    @Mock
    private HerramientaRepository herramientaRepository;
    @Mock
    private CategoriaRepository categoriaRepository;
    @Mock
    private PropietarioRepository propietarioRepository;
    @Mock
    private DisponibilidadRepository disponibilidadRepository;

    private HerramientaService herramientaService;

    @BeforeEach
    void setUp() {
        herramientaService = new HerramientaService(
                herramientaRepository,
                categoriaRepository,
                propietarioRepository,
                disponibilidadRepository
        );
    }

    @Test
    @DisplayName("PROPIETARIO puede crear herramientas para su propia cuenta")
    void propietarioPuedeCrearHerramientaPropia() {
        UUID propietarioId = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();

        Propietario propietario = new Propietario(
                propietarioId, Email.of("prop@test.com"), Nit.of("900123450-4"),
                "Razón Social", "Calle 1", "3001234567", "COMUN", "Bogota", "Cundinamarca", Instant.now()
        );
        Categoria categoria = new Categoria(categoriaId, "Herramientas Eléctricas", "Desc", Instant.now());

        when(propietarioRepository.findById(propietarioId)).thenReturn(Optional.of(propietario));
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(herramientaRepository.save(any(Herramienta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Herramienta h = herramientaService.crearHerramienta(
                propietarioId, UserRol.PROPIETARIO, propietarioId, categoriaId,
                "Taladro", "Desc", "DeWalt", "DCD777",
                Dinero.cop(new BigDecimal("45000")), 3,
                new BigDecimal("19.00"), true
        );

        assertThat(h).isNotNull();
        assertThat(h.getPropietarioId()).isEqualTo(propietarioId);
        assertThat(h.getNombre()).isEqualTo("Taladro");
        verify(herramientaRepository).save(any(Herramienta.class));
    }

    @Test
    @DisplayName("CLIENTE no puede crear herramientas")
    void clienteNoPuedeCrearHerramientas() {
        UUID clienteId = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();

        assertThatThrownBy(() -> herramientaService.crearHerramienta(
                clienteId, UserRol.CLIENTE, clienteId, categoriaId,
                "Taladro", "Desc", "DeWalt", "DCD777",
                Dinero.cop(new BigDecimal("45000")), 3,
                new BigDecimal("19.00"), true
        )).isInstanceOf(OperacionInvalidaException.class)
          .hasMessageContaining("No autorizado: solo el propietario titular o un administrador pueden crear herramientas");

        verifyNoInteractions(propietarioRepository);
    }

    @Test
    @DisplayName("PROPIETARIO no puede crear herramientas para otro propietario")
    void propietarioNoPuedeCrearParaOtro() {
        UUID actorId = UUID.randomUUID();
        UUID otroPropietarioId = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();

        assertThatThrownBy(() -> herramientaService.crearHerramienta(
                actorId, UserRol.PROPIETARIO, otroPropietarioId, categoriaId,
                "Taladro", "Desc", "DeWalt", "DCD777",
                Dinero.cop(new BigDecimal("45000")), 3,
                new BigDecimal("19.00"), true
        )).isInstanceOf(OperacionInvalidaException.class)
          .hasMessageContaining("No autorizado");
    }

    @Test
    @DisplayName("PROPIETARIO no puede modificar herramientas de otro")
    void propietarioNoPuedeModificarHerramientaAjena() {
        UUID actorId = UUID.randomUUID();
        UUID propietarioRealId = UUID.randomUUID();
        UUID herramientaId = UUID.randomUUID();

        Herramienta h = new Herramienta(
                herramientaId, UUID.randomUUID(), propietarioRealId,
                "Taladro", "Desc", "DeWalt", "DCD777",
                Dinero.cop(new BigDecimal("45000")), 3,
                HerramientaPubEstado.PUBLICADA, new BigDecimal("19.00"), true,
                Instant.now(), Instant.now()
        );

        when(herramientaRepository.findById(herramientaId)).thenReturn(Optional.of(h));

        assertThatThrownBy(() -> herramientaService.actualizarEstado(herramientaId, HerramientaPubEstado.PAUSADA, actorId, UserRol.PROPIETARIO))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("No autorizado");
    }

    @Test
    @DisplayName("PROPIETARIO titular puede actualizar estado y precio de su herramienta")
    void propietarioTitularPuedeActualizarEstadoYPrecio() {
        UUID propietarioId = UUID.randomUUID();
        UUID herramientaId = UUID.randomUUID();

        Herramienta h = new Herramienta(
                herramientaId, UUID.randomUUID(), propietarioId,
                "Taladro", "Desc", "DeWalt", "DCD777",
                Dinero.cop(new BigDecimal("45000")), 3,
                HerramientaPubEstado.PUBLICADA, new BigDecimal("19.00"), true,
                Instant.now(), Instant.now()
        );

        when(herramientaRepository.findById(herramientaId)).thenReturn(Optional.of(h));
        when(herramientaRepository.save(any(Herramienta.class))).thenAnswer(inv -> inv.getArgument(0));

        Herramienta actualizadaEstado = herramientaService.actualizarEstado(herramientaId, HerramientaPubEstado.PAUSADA, propietarioId, UserRol.PROPIETARIO);
        assertThat(actualizadaEstado.getEstadoPublicacion()).isEqualTo(HerramientaPubEstado.PAUSADA);

        Herramienta actualizadaPrecio = herramientaService.actualizarPrecio(herramientaId, Dinero.cop(new BigDecimal("52000")), propietarioId, UserRol.PROPIETARIO);
        assertThat(actualizadaPrecio.getPrecioDia().getMonto()).isEqualByComparingTo(new BigDecimal("52000"));
    }

    @Test
    @DisplayName("ADMIN puede actualizar estado y precio de cualquier herramienta")
    void adminPuedeActualizarEstadoYPrecio() {
        UUID adminId = UUID.randomUUID();
        UUID propietarioId = UUID.randomUUID();
        UUID herramientaId = UUID.randomUUID();

        Herramienta h = new Herramienta(
                herramientaId, UUID.randomUUID(), propietarioId,
                "Generador", "Desc", "Yamaha", "EF2000",
                Dinero.cop(new BigDecimal("80000")), 2,
                HerramientaPubEstado.PUBLICADA, new BigDecimal("19.00"), true,
                Instant.now(), Instant.now()
        );

        when(herramientaRepository.findById(herramientaId)).thenReturn(Optional.of(h));
        when(herramientaRepository.save(any(Herramienta.class))).thenAnswer(inv -> inv.getArgument(0));

        Herramienta actualizada = herramientaService.actualizarEstado(herramientaId, HerramientaPubEstado.RETIRADA, adminId, UserRol.ADMIN);
        assertThat(actualizada.getEstadoPublicacion()).isEqualTo(HerramientaPubEstado.RETIRADA);

        Herramienta conNuevoPrecio = herramientaService.actualizarPrecio(herramientaId, Dinero.cop(new BigDecimal("85000")), adminId, UserRol.ADMIN);
        assertThat(conNuevoPrecio.getPrecioDia().getMonto()).isEqualByComparingTo(new BigDecimal("85000"));
    }

    @Test
    @DisplayName("CLIENTE no puede actualizar estado ni precio de herramientas")
    void clienteNoPuedeActualizarEstadoNiPrecio() {
        UUID clienteId = UUID.randomUUID();
        UUID propietarioId = UUID.randomUUID();
        UUID herramientaId = UUID.randomUUID();

        Herramienta h = new Herramienta(
                herramientaId, UUID.randomUUID(), propietarioId,
                "Mezcladora", "Desc", "Imer", "Syntesi 350",
                Dinero.cop(new BigDecimal("70000")), 1,
                HerramientaPubEstado.PUBLICADA, new BigDecimal("19.00"), true,
                Instant.now(), Instant.now()
        );

        when(herramientaRepository.findById(herramientaId)).thenReturn(Optional.of(h));

        assertThatThrownBy(() -> herramientaService.actualizarEstado(herramientaId, HerramientaPubEstado.PAUSADA, clienteId, UserRol.CLIENTE))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("No autorizado");

        assertThatThrownBy(() -> herramientaService.actualizarPrecio(herramientaId, Dinero.cop(new BigDecimal("75000")), clienteId, UserRol.CLIENTE))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("No autorizado");
    }
    @Test
    @DisplayName("calcularDisponibilidad retorna stock disponible cuando no hay reservas")
    void calcularDisponibilidadSinReservas() {
        UUID herramientaId = UUID.randomUUID();
        LocalDate inicio = LocalDate.now().plusDays(2);
        LocalDate fin = LocalDate.now().plusDays(5);
        Herramienta h = new Herramienta(
                herramientaId, UUID.randomUUID(), UUID.randomUUID(),
                "Taladro Percutor", "Desc", "DeWalt", "DWD024",
                Dinero.cop(new BigDecimal("35000")), 5,
                HerramientaPubEstado.PUBLICADA, new BigDecimal("19.00"), true,
                Instant.now(), Instant.now()
        );
        when(herramientaRepository.findById(herramientaId)).thenReturn(Optional.of(h));
        when(disponibilidadRepository.calcularDisponibilidad(herramientaId, inicio, fin, null)).thenReturn(5);
        int disponible = herramientaService.calcularDisponibilidad(herramientaId, inicio, fin);
        assertThat(disponible).isEqualTo(5);
        verify(disponibilidadRepository).calcularDisponibilidad(herramientaId, inicio, fin, null);
    }

    @Test
    @DisplayName("calcularDisponibilidad retorna stock reducido cuando hay reservas que solapan")
    void calcularDisponibilidadConReservasSolapadas() {
        UUID herramientaId = UUID.randomUUID();
        LocalDate inicio = LocalDate.now().plusDays(1);
        LocalDate fin = LocalDate.now().plusDays(4);
        Herramienta h = new Herramienta(
                herramientaId, UUID.randomUUID(), UUID.randomUUID(),
                "Generador", "Desc", "Honda", "EU22i",
                Dinero.cop(new BigDecimal("90000")), 3,
                HerramientaPubEstado.PUBLICADA, new BigDecimal("19.00"), true,
                Instant.now(), Instant.now()
        );
        when(herramientaRepository.findById(herramientaId)).thenReturn(Optional.of(h));
        when(disponibilidadRepository.calcularDisponibilidad(herramientaId, inicio, fin, null)).thenReturn(1);
        int disponible = herramientaService.calcularDisponibilidad(herramientaId, inicio, fin);
        assertThat(disponible).isEqualTo(1);
    }

    @Test
    @DisplayName("calcularDisponibilidad retorna 0 si la herramienta no está publicada")
    void calcularDisponibilidadHerramientaNoPublicada() {
        UUID herramientaId = UUID.randomUUID();
        LocalDate inicio = LocalDate.now().plusDays(1);
        LocalDate fin = LocalDate.now().plusDays(3);
        Herramienta h = new Herramienta(
                herramientaId, UUID.randomUUID(), UUID.randomUUID(),
                "Generador", "Desc", "Honda", "EU22i",
                Dinero.cop(new BigDecimal("90000")), 3,
                HerramientaPubEstado.PAUSADA, new BigDecimal("19.00"), true,
                Instant.now(), Instant.now()
        );
        when(herramientaRepository.findById(herramientaId)).thenReturn(Optional.of(h));
        int disponible = herramientaService.calcularDisponibilidad(herramientaId, inicio, fin);
        assertThat(disponible).isEqualTo(0);
        verifyNoInteractions(disponibilidadRepository);
    }

    @Test
    @DisplayName("calcularDisponibilidad rechaza fechas nulas")
    void calcularDisponibilidadFechasNulas() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> herramientaService.calcularDisponibilidad(id, null, LocalDate.now().plusDays(1)))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("obligatorias");
        assertThatThrownBy(() -> herramientaService.calcularDisponibilidad(id, LocalDate.now().plusDays(1), null))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("obligatorias");
    }

    @Test
    @DisplayName("calcularDisponibilidad rechaza rango de fechas invertido")
    void calcularDisponibilidadFechasInvertidas() {
        UUID id = UUID.randomUUID();
        LocalDate inicio = LocalDate.now().plusDays(5);
        LocalDate fin = LocalDate.now().plusDays(2);
        assertThatThrownBy(() -> herramientaService.calcularDisponibilidad(id, inicio, fin))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("no puede ser posterior");
    }

    @Test
    @DisplayName("calcularDisponibilidad rechaza fecha de inicio en el pasado")
    void calcularDisponibilidadFechaPasada() {
        UUID id = UUID.randomUUID();
        LocalDate inicio = LocalDate.now().minusDays(1);
        LocalDate fin = LocalDate.now().plusDays(2);
        assertThatThrownBy(() -> herramientaService.calcularDisponibilidad(id, inicio, fin))
                .isInstanceOf(OperacionInvalidaException.class)
                .hasMessageContaining("no puede ser anterior a la fecha actual");
    }

    @Test
    @DisplayName("calcularDisponibilidad lanza EntidadNoEncontradaException si la herramienta no existe")
    void calcularDisponibilidadHerramientaInexistente() {
        UUID id = UUID.randomUUID();
        LocalDate inicio = LocalDate.now().plusDays(1);
        LocalDate fin = LocalDate.now().plusDays(2);
        when(herramientaRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> herramientaService.calcularDisponibilidad(id, inicio, fin))
                .isInstanceOf(EntidadNoEncontradaException.class);
    }

    @Test
    @DisplayName("listarPorPropietario retorna solo las herramientas del propietario especificado")
    void listarPorPropietarioRetornaSoloHerramientasPropias() {
        UUID propietarioId = UUID.randomUUID();
        Herramienta h1 = new Herramienta(UUID.randomUUID(), UUID.randomUUID(), propietarioId, "Taladro", "Desc", "Bosch", "GSB", Dinero.cop(BigDecimal.valueOf(50000)), 3, HerramientaPubEstado.PUBLICADA, BigDecimal.valueOf(19), true, Instant.now(), Instant.now());
        when(herramientaRepository.findByPropietarioId(propietarioId)).thenReturn(java.util.List.of(h1));

        java.util.List<Herramienta> resultado = herramientaService.listarPorPropietario(propietarioId);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Taladro");
        verify(herramientaRepository).findByPropietarioId(propietarioId);
    }
}
