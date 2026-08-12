package br.com.itau.consumidoressimulados.application;

import br.com.itau.consumidoressimulados.domain.ConsumerType;
import br.com.itau.consumidoressimulados.domain.NotaFiscalGeradaEvent;
import br.com.itau.consumidoressimulados.domain.NotaFiscalPayload;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class HandlersTest {

    @Test
    void shouldSimulateStock() {
        LatencySimulator latency = mock(LatencySimulator.class);
        EstoqueHandler handler = new EstoqueHandler(latency);

        handler.handle(eventWithItems(1));

        assertThat(handler.type()).isEqualTo(ConsumerType.ESTOQUE);
        verify(latency).waitFor(380);
    }

    @Test
    void shouldSimulateRegistration() {
        LatencySimulator latency = mock(LatencySimulator.class);
        RegistroHandler handler = new RegistroHandler(latency);

        handler.handle(eventWithItems(1));

        assertThat(handler.type()).isEqualTo(ConsumerType.REGISTRO);
        verify(latency).waitFor(500);
    }

    @Test
    void shouldSimulateFinancialProcessing() {
        LatencySimulator latency = mock(LatencySimulator.class);
        FinanceiroHandler handler = new FinanceiroHandler(latency);

        handler.handle(eventWithItems(1));

        assertThat(handler.type()).isEqualTo(ConsumerType.FINANCEIRO);
        verify(latency).waitFor(250);
    }

    @Test
    void shouldUseTheRegularDeliveryFlowForUpToFiveItems() {
        LatencySimulator latency = mock(LatencySimulator.class);
        EntregaHandler handler = new EntregaHandler(latency);

        handler.handle(eventWithItems(5));

        assertThat(handler.type()).isEqualTo(ConsumerType.ENTREGA);
        var order = inOrder(latency);
        order.verify(latency).waitFor(150);
        order.verify(latency).waitFor(200);
        order.verifyNoMoreInteractions();
    }

    @Test
    void shouldUseTheSlowDeliveryFlowFromSixItems() {
        LatencySimulator latency = mock(LatencySimulator.class);
        EntregaHandler handler = new EntregaHandler(latency);

        handler.handle(eventWithItems(6));

        var order = inOrder(latency);
        order.verify(latency).waitFor(150);
        order.verify(latency).waitFor(5_000);
        order.verify(latency).waitFor(200);
    }

    private NotaFiscalGeradaEvent eventWithItems(int itemCount) {
        List<NotaFiscalPayload.ItemPayload> items = java.util.stream.IntStream.range(0, itemCount)
                .mapToObj(index -> new NotaFiscalPayload.ItemPayload(String.valueOf(index)))
                .toList();
        return new NotaFiscalGeradaEvent(
                "event-1", "NotaFiscalGerada", 1, OffsetDateTime.now(),
                "correlation-1", "flow-1", "pedido:42", 42,
                new NotaFiscalPayload("nf-1", items));
    }
}
