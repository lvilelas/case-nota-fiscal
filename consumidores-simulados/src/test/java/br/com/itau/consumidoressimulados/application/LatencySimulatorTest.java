package br.com.itau.consumidoressimulados.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LatencySimulatorTest {

    @Test
    void shouldWaitNormally() {
        new LatencySimulator().waitFor(0);
    }

    @Test
    void shouldPreserveInterruptFlag() {
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> new LatencySimulator().waitFor(1))
                    .isInstanceOf(ProcessingInterruptedException.class)
                    .hasMessage("Processamento simulado interrompido")
                    .hasCauseInstanceOf(InterruptedException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }
}
