package br.com.itau.consumidoressimulados.domain;

import java.util.List;

public record NotaFiscalPayload(
        String idNotaFiscal,
        List<ItemPayload> itens) {

    public int quantidadeItens() {
        return itens == null ? 0 : itens.size();
    }

    public record ItemPayload(String idItem) {
    }
}
