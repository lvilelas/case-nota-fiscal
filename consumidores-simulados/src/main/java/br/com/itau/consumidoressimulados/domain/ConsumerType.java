package br.com.itau.consumidoressimulados.domain;

import java.util.EnumSet;
import java.util.Set;

public enum ConsumerType {
    ALL,
    ESTOQUE,
    REGISTRO,
    ENTREGA,
    FINANCEIRO;

    public Set<ConsumerType> selectedConsumers() {
        if (this == ALL) {
            return EnumSet.complementOf(EnumSet.of(ALL));
        }
        return EnumSet.of(this);
    }

    public String queueSuffix() {
        if (this == ALL) {
            throw new IllegalStateException("ALL nao representa uma fila SQS");
        }
        return name().toLowerCase();
    }
}
