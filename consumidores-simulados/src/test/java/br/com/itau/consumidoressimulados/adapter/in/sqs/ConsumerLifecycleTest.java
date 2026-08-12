package br.com.itau.consumidoressimulados.adapter.in.sqs;

import br.com.itau.consumidoressimulados.config.ConsumerProperties;
import br.com.itau.consumidoressimulados.domain.ConsumerType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConsumerLifecycleTest {

    @Test
    void shouldStartOnlyTheConfiguredListenerAndStopSafely() {
        NotaFiscalSqsListener stock = mock(NotaFiscalSqsListener.class);
        when(stock.type()).thenReturn(ConsumerType.ESTOQUE);
        var properties = new ConsumerProperties(
                ConsumerType.ESTOQUE, "prefix-", 20, 30, 10, Duration.ZERO);
        var lifecycle = new ConsumerLifecycle(List.of(stock), properties);

        lifecycle.start();
        lifecycle.start();

        assertThat(lifecycle.isRunning()).isTrue();
        verify(stock, timeout(1_000)).listen(any());

        AtomicBoolean callback = new AtomicBoolean();
        lifecycle.stop(() -> callback.set(true));
        lifecycle.stop();

        assertThat(lifecycle.isRunning()).isFalse();
        assertThat(callback).isTrue();
    }

    @Test
    void healthShouldReflectLifecycleState() {
        ConsumerLifecycle lifecycle = mock(ConsumerLifecycle.class);
        ConsumerHealthIndicator indicator = new ConsumerHealthIndicator(lifecycle);

        when(lifecycle.isRunning()).thenReturn(true, false);

        var up = indicator.health();
        var down = indicator.health();

        assertThat(up.getStatus().getCode()).isEqualTo("UP");
        assertThat(up.getDetails()).containsEntry("listeners", "running");
        assertThat(down.getStatus().getCode()).isEqualTo("DOWN");
        assertThat(down.getDetails()).containsEntry("listeners", "stopped");
    }

    @Test
    void shouldFailFastWhenASelectedListenerWasNotImplemented() {
        var properties = new ConsumerProperties(
                ConsumerType.REGISTRO, "prefix-", 20, 30, 1, Duration.ZERO);
        var lifecycle = new ConsumerLifecycle(List.of(), properties);

        assertThatThrownBy(lifecycle::start)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Listener nao implementado para REGISTRO");
        lifecycle.stop();
    }
}
