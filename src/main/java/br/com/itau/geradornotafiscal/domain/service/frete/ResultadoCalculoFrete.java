package br.com.itau.geradornotafiscal.domain.service.frete;

import br.com.itau.geradornotafiscal.domain.model.Regiao;

public record ResultadoCalculoFrete(
        double valorOriginal,
        Regiao regiao,
        double multiplicador,
        double valorCalculado) {
}
