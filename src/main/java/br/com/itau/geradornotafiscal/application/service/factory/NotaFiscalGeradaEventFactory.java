package br.com.itau.geradornotafiscal.application.service.factory;

import br.com.itau.geradornotafiscal.application.event.NotaFiscalGeradaEvent;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import org.slf4j.MDC;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

public class NotaFiscalGeradaEventFactory {
    static final String EVENT_TYPE = "NotaFiscalGerada";
    static final int EVENT_VERSION = 1;

    public NotaFiscalGeradaEvent criar(int pedidoId, NotaFiscal notaFiscal) {
        return new NotaFiscalGeradaEvent(
                UUID.randomUUID().toString(),
                EVENT_TYPE,
                EVENT_VERSION,
                OffsetDateTime.now(ZoneOffset.UTC),
                contexto("correlationId"),
                contexto("flowId"),
                "nota-fiscal-gerada:pedido:" + pedidoId,
                pedidoId,
                notaFiscal);
    }

    private String contexto(String nome) {
        return Objects.requireNonNullElse(MDC.get(nome), "not-provided");
    }
}
