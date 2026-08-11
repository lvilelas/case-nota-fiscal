package br.com.itau.geradornotafiscal.service.tributacao.faixa;

public record FaixaAliquota(double limiteSuperior, boolean inclusivo, double aliquota) {
    boolean aceita(double valorTotalItens) {
        return inclusivo
                ? valorTotalItens <= limiteSuperior
                : valorTotalItens < limiteSuperior;
    }
}
