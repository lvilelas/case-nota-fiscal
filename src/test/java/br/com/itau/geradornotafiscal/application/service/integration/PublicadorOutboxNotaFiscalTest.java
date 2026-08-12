package br.com.itau.geradornotafiscal.application.service.integration;

import br.com.itau.geradornotafiscal.application.event.NotaFiscalGeradaEvent;
import br.com.itau.geradornotafiscal.application.event.RegistroOutbox;
import br.com.itau.geradornotafiscal.application.port.out.OutboxNotaFiscalPort;
import br.com.itau.geradornotafiscal.application.port.out.PublicarNotaFiscalGeradaPort;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicadorOutboxNotaFiscalTest {
    private static final String TOKEN_RESERVA = "00000000-0000-0000-0000-000000000099";

    @Mock
    private OutboxNotaFiscalPort outboxPort;
    @Mock
    private PublicarNotaFiscalGeradaPort publicadorPort;

    private PublicadorOutboxNotaFiscal publicador;

    @BeforeEach
    void setup() {
        publicador = new PublicadorOutboxNotaFiscal(
                outboxPort,
                publicadorPort,
                3,
                Duration.ofSeconds(1),
                Duration.ofSeconds(5));
    }

    @Test
    void devePublicarEConfirmarEventoReservado() {
        NotaFiscalGeradaEvent evento = evento("00000000-0000-0000-0000-000000000001");
        when(outboxPort.reservarPendentes(10, Duration.ofSeconds(30)))
                .thenReturn(List.of(registro(evento, 0)));
        when(outboxPort.renovarReserva(evento.eventId(), TOKEN_RESERVA, Duration.ofSeconds(30)))
                .thenReturn(true);
        when(outboxPort.marcarPublicado(evento.eventId(), TOKEN_RESERVA)).thenReturn(true);

        publicador.publicarPendentes(10, Duration.ofSeconds(30));

        verify(publicadorPort).publicar(evento);
        verify(outboxPort).marcarPublicado(evento.eventId(), TOKEN_RESERVA);
    }

    @Test
    void deveReagendarComBackoffQuandoPublicacaoFalhar() {
        NotaFiscalGeradaEvent evento = evento("00000000-0000-0000-0000-000000000002");
        when(outboxPort.reservarPendentes(10, Duration.ofSeconds(30)))
                .thenReturn(List.of(registro(evento, 0)));
        when(outboxPort.renovarReserva(evento.eventId(), TOKEN_RESERVA, Duration.ofSeconds(30)))
                .thenReturn(true);
        when(outboxPort.reagendar(
                eq(evento.eventId()),
                eq(TOKEN_RESERVA),
                org.mockito.ArgumentMatchers.any(),
                eq("SNS indisponivel"))).thenReturn(true);
        doThrow(new RuntimeException("SNS indisponivel")).when(publicadorPort).publicar(evento);
        ArgumentCaptor<OffsetDateTime> dataCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);

        OffsetDateTime antes = OffsetDateTime.now().plusNanos(500_000_000);
        publicador.publicarPendentes(10, Duration.ofSeconds(30));

        verify(outboxPort).reagendar(
                eq(evento.eventId()),
                eq(TOKEN_RESERVA),
                dataCaptor.capture(),
                eq("SNS indisponivel"));
        assertTrue(dataCaptor.getValue().isAfter(antes));
        verify(outboxPort, never()).marcarPublicado(evento.eventId(), TOKEN_RESERVA);
    }

    @Test
    void deveMarcarFalhaDefinitivaAoEsgotarTentativas() {
        NotaFiscalGeradaEvent evento = evento("00000000-0000-0000-0000-000000000003");
        when(outboxPort.reservarPendentes(10, Duration.ofSeconds(30)))
                .thenReturn(List.of(registro(evento, 2)));
        when(outboxPort.renovarReserva(evento.eventId(), TOKEN_RESERVA, Duration.ofSeconds(30)))
                .thenReturn(true);
        when(outboxPort.marcarFalhaDefinitiva(evento.eventId(), TOKEN_RESERVA, "RuntimeException"))
                .thenReturn(true);
        doThrow(new RuntimeException()).when(publicadorPort).publicar(evento);

        publicador.publicarPendentes(10, Duration.ofSeconds(30));

        verify(outboxPort).marcarFalhaDefinitiva(evento.eventId(), TOKEN_RESERVA, "RuntimeException");
        verify(outboxPort, never()).reagendar(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void deveLimitarBackoffAoMaximoConfigurado() {
        NotaFiscalGeradaEvent evento = evento("00000000-0000-0000-0000-000000000004");
        when(outboxPort.reservarPendentes(10, Duration.ofSeconds(30)))
                .thenReturn(List.of(registro(evento, 21)));
        when(outboxPort.renovarReserva(evento.eventId(), TOKEN_RESERVA, Duration.ofSeconds(30)))
                .thenReturn(true);
        when(outboxPort.reagendar(
                eq(evento.eventId()),
                eq(TOKEN_RESERVA),
                org.mockito.ArgumentMatchers.any(),
                eq("falha"))).thenReturn(true);
        doThrow(new RuntimeException("falha")).when(publicadorPort).publicar(evento);
        publicador = new PublicadorOutboxNotaFiscal(
                outboxPort,
                publicadorPort,
                30,
                Duration.ofSeconds(1),
                Duration.ofSeconds(5));
        ArgumentCaptor<OffsetDateTime> dataCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);

        OffsetDateTime antes = OffsetDateTime.now().plusSeconds(4);
        OffsetDateTime depois = OffsetDateTime.now().plusSeconds(6);
        publicador.publicarPendentes(10, Duration.ofSeconds(30));

        verify(outboxPort).reagendar(
                eq(evento.eventId()),
                eq(TOKEN_RESERVA),
                dataCaptor.capture(),
                eq("falha"));
        assertTrue(dataCaptor.getValue().isAfter(antes));
        assertTrue(dataCaptor.getValue().isBefore(depois));
    }

    @Test
    void deveIgnorarEventoQuandoReservaFoiAssumidaPorOutroWorker() {
        NotaFiscalGeradaEvent evento = evento("00000000-0000-0000-0000-000000000005");
        when(outboxPort.reservarPendentes(10, Duration.ofSeconds(30)))
                .thenReturn(List.of(registro(evento, 0)));

        publicador.publicarPendentes(10, Duration.ofSeconds(30));

        verify(publicadorPort, never()).publicar(evento);
        verify(outboxPort, never()).marcarPublicado(evento.eventId(), TOKEN_RESERVA);
    }

    @Test
    void deveIgnorarConfirmacaoQuandoReservaExpiraDurantePublicacao() {
        NotaFiscalGeradaEvent evento = evento("00000000-0000-0000-0000-000000000006");
        when(outboxPort.reservarPendentes(10, Duration.ofSeconds(30)))
                .thenReturn(List.of(registro(evento, 0)));
        when(outboxPort.renovarReserva(evento.eventId(), TOKEN_RESERVA, Duration.ofSeconds(30)))
                .thenReturn(true);

        publicador.publicarPendentes(10, Duration.ofSeconds(30));

        verify(publicadorPort).publicar(evento);
        verify(outboxPort).marcarPublicado(evento.eventId(), TOKEN_RESERVA);
    }

    @Test
    void deveIgnorarReagendamentoDeWorkerQuePerdeuAReserva() {
        NotaFiscalGeradaEvent evento = evento("00000000-0000-0000-0000-000000000007");
        when(outboxPort.reservarPendentes(10, Duration.ofSeconds(30)))
                .thenReturn(List.of(registro(evento, 0)));
        when(outboxPort.renovarReserva(evento.eventId(), TOKEN_RESERVA, Duration.ofSeconds(30)))
                .thenReturn(true);
        doThrow(new RuntimeException("SNS indisponivel")).when(publicadorPort).publicar(evento);

        publicador.publicarPendentes(10, Duration.ofSeconds(30));

        verify(outboxPort).reagendar(
                eq(evento.eventId()),
                eq(TOKEN_RESERVA),
                org.mockito.ArgumentMatchers.any(),
                eq("SNS indisponivel"));
    }

    @Test
    void deveIgnorarFalhaDefinitivaDeWorkerQuePerdeuAReserva() {
        NotaFiscalGeradaEvent evento = evento("00000000-0000-0000-0000-000000000008");
        when(outboxPort.reservarPendentes(10, Duration.ofSeconds(30)))
                .thenReturn(List.of(registro(evento, 2)));
        when(outboxPort.renovarReserva(evento.eventId(), TOKEN_RESERVA, Duration.ofSeconds(30)))
                .thenReturn(true);
        doThrow(new RuntimeException()).when(publicadorPort).publicar(evento);

        publicador.publicarPendentes(10, Duration.ofSeconds(30));

        verify(outboxPort).marcarFalhaDefinitiva(evento.eventId(), TOKEN_RESERVA, "RuntimeException");
    }

    private RegistroOutbox registro(NotaFiscalGeradaEvent evento, int tentativas) {
        return new RegistroOutbox(evento, tentativas, TOKEN_RESERVA);
    }

    private NotaFiscalGeradaEvent evento(String eventId) {
        return new NotaFiscalGeradaEvent(
                eventId,
                "NotaFiscalGerada",
                1,
                OffsetDateTime.parse("2026-08-11T20:00:00Z"),
                "correlation",
                "flow",
                "nota-fiscal-gerada:pedido:1",
                1,
                NotaFiscal.builder()
                        .idNotaFiscal("00000000-0000-0000-0000-000000000010")
                        .itens(List.of())
                        .build());
    }
}
