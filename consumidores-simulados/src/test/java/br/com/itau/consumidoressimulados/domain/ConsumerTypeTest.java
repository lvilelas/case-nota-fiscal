package br.com.itau.consumidoressimulados.domain;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsumerTypeTest {

    @Test
    void shouldSelectEveryConcreteConsumerForAll() {
        assertThat(ConsumerType.ALL.selectedConsumers())
                .containsExactlyInAnyOrder(
                        ConsumerType.ESTOQUE,
                        ConsumerType.REGISTRO,
                        ConsumerType.ENTREGA,
                        ConsumerType.FINANCEIRO);
    }

    @Test
    void shouldSelectOnlyTheRequestedConsumer() {
        assertThat(ConsumerType.ESTOQUE.selectedConsumers())
                .isEqualTo(Set.of(ConsumerType.ESTOQUE));
        assertThat(ConsumerType.ESTOQUE.queueSuffix()).isEqualTo("estoque");
    }

    @Test
    void allShouldNotResolveToAQueue() {
        assertThatThrownBy(ConsumerType.ALL::queueSuffix)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ALL nao representa uma fila SQS");
    }
}
