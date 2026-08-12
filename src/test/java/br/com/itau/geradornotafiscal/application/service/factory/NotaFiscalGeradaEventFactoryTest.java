package br.com.itau.geradornotafiscal.application.service.factory;

import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class NotaFiscalGeradaEventFactoryTest {
    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    @Test
    void deveCriarEventoVersionadoComContextoEChaveIdempotente() {
        MDC.put("correlationId", "correlation-1");
        NotaFiscal notaFiscal = NotaFiscal.builder().idNotaFiscal("nf-1").itens(List.of()).build();

        var evento = new NotaFiscalGeradaEventFactory().criar(42, notaFiscal);

        assertNotNull(evento.eventId());
        assertEquals("NotaFiscalGerada", evento.eventType());
        assertEquals(1, evento.eventVersion());
        assertNotNull(evento.occurredAt());
        assertEquals("correlation-1", evento.correlationId());
        assertEquals("not-provided", evento.flowId());
        assertEquals("nota-fiscal-gerada:pedido:42", evento.idempotencyKey());
        assertEquals(42, evento.pedidoId());
        assertSame(notaFiscal, evento.notaFiscal());
    }
}
