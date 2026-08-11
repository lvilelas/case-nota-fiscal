package br.com.itau.geradornotafiscal.domain.service.tributacao;

public record FaixaAliquota(double limiteSuperior, boolean inclusivo, double aliquota) {
    boolean aceita(double valorTotalItens) {
        return inclusivo
                ? valorTotalItens <= limiteSuperior
                : valorTotalItens < limiteSuperior;
    }
}
