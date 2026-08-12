package br.com.itau.consumidoressimulados.adapter.in.sqs;

import br.com.itau.consumidoressimulados.application.EntregaHandler;
import br.com.itau.consumidoressimulados.application.EstoqueHandler;
import br.com.itau.consumidoressimulados.application.FinanceiroHandler;
import br.com.itau.consumidoressimulados.application.RegistroHandler;
import br.com.itau.consumidoressimulados.domain.ConsumerType;
import org.junit.jupiter.api.Test;

import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ListenerDelegationTest {
    private final SqsMessageProcessor processor = mock(SqsMessageProcessor.class);
    private final BooleanSupplier running = () -> true;

    @Test
    void shouldDelegateStockQueue() {
        EstoqueHandler handler = mock(EstoqueHandler.class);
        var listener = new EstoqueSqsListener(processor, handler);

        listener.listen(running);

        assertThat(listener.type()).isEqualTo(ConsumerType.ESTOQUE);
        verify(processor).listen(ConsumerType.ESTOQUE, handler, running);
    }

    @Test
    void shouldDelegateRegistrationQueue() {
        RegistroHandler handler = mock(RegistroHandler.class);
        var listener = new RegistroSqsListener(processor, handler);

        listener.listen(running);

        assertThat(listener.type()).isEqualTo(ConsumerType.REGISTRO);
        verify(processor).listen(ConsumerType.REGISTRO, handler, running);
    }

    @Test
    void shouldDelegateDeliveryQueue() {
        EntregaHandler handler = mock(EntregaHandler.class);
        var listener = new EntregaSqsListener(processor, handler);

        listener.listen(running);

        assertThat(listener.type()).isEqualTo(ConsumerType.ENTREGA);
        verify(processor).listen(ConsumerType.ENTREGA, handler, running);
    }

    @Test
    void shouldDelegateFinancialQueue() {
        FinanceiroHandler handler = mock(FinanceiroHandler.class);
        var listener = new FinanceiroSqsListener(processor, handler);

        listener.listen(running);

        assertThat(listener.type()).isEqualTo(ConsumerType.FINANCEIRO);
        verify(processor).listen(ConsumerType.FINANCEIRO, handler, running);
    }
}
