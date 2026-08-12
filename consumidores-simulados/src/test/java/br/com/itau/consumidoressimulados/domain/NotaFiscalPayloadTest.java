package br.com.itau.consumidoressimulados.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotaFiscalPayloadTest {

    @Test
    void shouldCountItemsAndAcceptLegacyNullList() {
        assertThat(new NotaFiscalPayload("nf-1", List.of(
                new NotaFiscalPayload.ItemPayload("1"),
                new NotaFiscalPayload.ItemPayload("2"))).quantidadeItens()).isEqualTo(2);
        assertThat(new NotaFiscalPayload("nf-2", null).quantidadeItens()).isZero();
    }
}
