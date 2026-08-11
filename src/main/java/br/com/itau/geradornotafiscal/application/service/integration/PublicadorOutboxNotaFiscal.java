package br.com.itau.geradornotafiscal.application.service.integration;

import br.com.itau.geradornotafiscal.application.event.RegistroOutbox;
import br.com.itau.geradornotafiscal.application.port.out.OutboxNotaFiscalPort;
import br.com.itau.geradornotafiscal.application.port.out.PublicarNotaFiscalGeradaPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class PublicadorOutboxNotaFiscal {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublicadorOutboxNotaFiscal.class);

    private final OutboxNotaFiscalPort outboxPort;
    private final PublicarNotaFiscalGeradaPort publicadorPort;
    private final int maxTentativas;
    private final Duration atrasoBase;
    private final Duration atrasoMaximo;

    public PublicadorOutboxNotaFiscal(
            OutboxNotaFiscalPort outboxPort,
            PublicarNotaFiscalGeradaPort publicadorPort,
            int maxTentativas,
            Duration atrasoBase,
            Duration atrasoMaximo) {
        this.outboxPort = outboxPort;
        this.publicadorPort = publicadorPort;
        this.maxTentativas = maxTentativas;
        this.atrasoBase = atrasoBase;
        this.atrasoMaximo = atrasoMaximo;
    }

    public void publicarPendentes(int limite, Duration duracaoBloqueio) {
        for (RegistroOutbox registro : outboxPort.reservarPendentes(limite, duracaoBloqueio)) {
            publicar(registro);
        }
    }

    private void publicar(RegistroOutbox registro) {
        var evento = registro.evento();
        try (var correlationContext = MDC.putCloseable("correlationId", evento.correlationId());
             var flowContext = MDC.putCloseable("flowId", evento.flowId())) {
            publicadorPort.publicar(evento);
            outboxPort.marcarPublicado(evento.eventId());
            LOGGER.info(
                    "Evento da outbox publicado: eventId={}, pedidoId={}, tentativa={}",
                    evento.eventId(),
                    evento.pedidoId(),
                    registro.tentativas() + 1);
        } catch (RuntimeException exception) {
            tratarFalha(registro, exception);
        }
    }

    private void tratarFalha(RegistroOutbox registro, RuntimeException exception) {
        int tentativa = registro.tentativas() + 1;
        String motivo = exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
        if (tentativa >= maxTentativas) {
            outboxPort.marcarFalhaDefinitiva(registro.evento().eventId(), motivo);
            LOGGER.error(
                    "Evento da outbox esgotou tentativas: eventId={}, pedidoId={}, tentativas={}",
                    registro.evento().eventId(),
                    registro.evento().pedidoId(),
                    tentativa,
                    exception);
            return;
        }

        Duration atraso = calcularAtraso(registro.tentativas());
        OffsetDateTime proximaTentativa = OffsetDateTime.now(ZoneOffset.UTC).plus(atraso);
        outboxPort.reagendar(registro.evento().eventId(), proximaTentativa, motivo);
        LOGGER.warn(
                "Evento da outbox reagendado: eventId={}, pedidoId={}, tentativa={}, atrasoMs={}",
                registro.evento().eventId(),
                registro.evento().pedidoId(),
                tentativa,
                atraso.toMillis());
    }

    private Duration calcularAtraso(int tentativasRealizadas) {
        long multiplicador = 1L << Math.min(tentativasRealizadas, 20);
        Duration atraso = atrasoBase.multipliedBy(multiplicador);
        return atraso.compareTo(atrasoMaximo) > 0 ? atrasoMaximo : atraso;
    }
}
