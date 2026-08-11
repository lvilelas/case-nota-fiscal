package br.com.itau.geradornotafiscal.domain.service.tributacao;

import br.com.itau.geradornotafiscal.domain.model.ItemNotaFiscal;

import java.util.List;

public record ResultadoCalculoTributario(
        List<ItemNotaFiscal> itens,
        FaixaAliquota faixaAplicada) {
}
