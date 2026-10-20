package com.plataforma.renta.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plataforma.renta.api.controller.ReembolsoController;
import com.plataforma.renta.api.dto.ReembolsoDtos.ReembolsoResponse;
import com.plataforma.renta.api.dto.ReembolsoDtos.ResolverReembolsoRequest;
import com.plataforma.renta.application.service.AuthService;
import com.plataforma.renta.application.service.PagoService;
import com.plataforma.renta.domain.model.Pago;
import com.plataforma.renta.domain.model.PagoEstado;
import com.plataforma.renta.domain.model.UserRol;
import com.plataforma.renta.domain.model.Usuario;
import com.plataforma.renta.domain.value.Dinero;
import com.plataforma.renta.domain.value.Email;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class ReembolsoControllerTest {

    @Mock
    private PagoService pagoService;

    @Mock
    private AuthService authService;

    @Mock
    private Authentication authentication;

    private ReembolsoController controller;

    private final UUID actorId = UUID.randomUUID();
    private final UUID reembolsoId = UUID.randomUUID();
    private final UUID reservaId = UUID.randomUUID();
    private final UUID pagoOriginalId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        controller = new ReembolsoController(pagoService, authService);
    }

    @Test
    @DisplayName("SEC-02: resolver con req.aprobar() == false invoca pagoService.rechazarReembolso y retorna estado actualizado")
    void resolverReembolso_rechazar_invocaServicioYRetornaActualizado() {
        when(authentication.getName()).thenReturn("propietario@test.com");

        Usuario actor = new Usuario(
                actorId, Email.of("propietario@test.com"), "hash", UserRol.PROPIETARIO,
                "Carlos", "Gomez", "3001234567", Instant.now(), Instant.now()
        );
        when(authService.obtenerUsuarioPorEmail("propietario@test.com")).thenReturn(actor);

        Pago pagoRechazado = new Pago(
                reembolsoId, reservaId, pagoOriginalId,
                PagoEstado.FALLIDO, "SIMULADO", null,
                Dinero.cop(new BigDecimal("75000")), "Reembolso | Rechazo: Daño por mal uso",
                Instant.now(), Instant.now()
        );
        when(pagoService.obtenerPorId(reembolsoId)).thenReturn(pagoRechazado);

        ResolverReembolsoRequest req = new ResolverReembolsoRequest(false, "Daño por mal uso");

        ResponseEntity<ReembolsoResponse> response = controller.resolver(reembolsoId, req, authentication);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().estado()).isEqualTo("RECHAZADO");
        assertThat(response.getBody().motivo()).contains("Daño por mal uso");
        assertThat(response.getBody().resolucion()).isEqualTo("Daño por mal uso");

        verify(pagoService).rechazarReembolso(eq(reembolsoId), eq("Daño por mal uso"), eq(actorId), eq(UserRol.PROPIETARIO));
    }

    @Test
    @DisplayName("SEC-02B: resolver con req.aprobar() == true invoca pagoService.aprobarReembolso y retorna estado normalizado APROBADO")
    void resolverReembolso_aprobar_invocaServicioYRetornaActualizado() {
        when(authentication.getName()).thenReturn("admin@test.com");

        Usuario actor = new Usuario(
                actorId, Email.of("admin@test.com"), "hash", UserRol.ADMIN,
                "Admin", "User", "3001234567", Instant.now(), Instant.now()
        );
        when(authService.obtenerUsuarioPorEmail("admin@test.com")).thenReturn(actor);

        Pago pagoAprobado = new Pago(
                reembolsoId, reservaId, pagoOriginalId,
                PagoEstado.REEMBOLSADO, "SIMULADO", null,
                Dinero.cop(new BigDecimal("75000")), "Reembolso aprobado",
                Instant.now(), Instant.now()
        );
        when(pagoService.obtenerPorId(reembolsoId)).thenReturn(pagoAprobado);

        ResolverReembolsoRequest req = new ResolverReembolsoRequest(true, "Aprobado conforme");

        ResponseEntity<ReembolsoResponse> response = controller.resolver(reembolsoId, req, authentication);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().estado()).isEqualTo("APROBADO");

        verify(pagoService).aprobarReembolso(eq(reembolsoId), eq(actorId), eq(UserRol.ADMIN));
    }

    @Test
    @DisplayName("SEC-02B: Pago sin pagoOriginalId no se mapea a RECHAZADO incluso si estado es FALLIDO")
    void pagoSinPagoOriginalIdMantieneEstadoOriginal() {
        when(authentication.getName()).thenReturn("cliente@test.com");
        Usuario actor = new Usuario(
                actorId, Email.of("cliente@test.com"), "hash", UserRol.CLIENTE,
                "Cliente", "Test", "3001234567", Instant.now(), Instant.now()
        );
        when(authService.obtenerUsuarioPorEmail("cliente@test.com")).thenReturn(actor);

        Pago pagoNormalFallido = new Pago(
                reembolsoId, reservaId, null, // Sin pago original
                PagoEstado.FALLIDO, "SIMULADO", null,
                Dinero.cop(new BigDecimal("75000")), "Fallo en pasarela",
                Instant.now(), Instant.now()
        );
        when(pagoService.listarReembolsosPorUsuario(actorId, UserRol.CLIENTE)).thenReturn(List.of(pagoNormalFallido));

        ResponseEntity<java.util.List<ReembolsoResponse>> list = controller.listar(authentication);

        assertThat(list.getBody()).isNotNull();
        assertThat(list.getBody().get(0).estado()).isEqualTo("FALLIDO");
    }
}
