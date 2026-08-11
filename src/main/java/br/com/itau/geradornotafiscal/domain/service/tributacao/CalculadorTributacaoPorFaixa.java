package br.com.itau.geradornotafiscal.domain.service.tributacao;

import br.com.itau.geradornotafiscal.domain.model.Item;

import java.util.List;

public class CalculadorTributacaoPorFaixa {
    private final CalculadoraAliquotaProduto calculadoraAliquotaProduto;

    public CalculadorTributacaoPorFaixa(CalculadoraAliquotaProduto calculadoraAliquotaProduto) {
        this.calculadoraAliquotaProduto = calculadoraAliquotaProduto;
    }

    public ResultadoCalculoTributario calcular(
            List<Item> itens,
            double valorTotalItens,
            List<FaixaAliquota> faixas) {
        FaixaAliquota faixaAplicada = faixas.stream()
                .filter(faixa -> faixa.aceita(valorTotalItens))
                .findFirst()
                .orElseThrow();

        return new ResultadoCalculoTributario(
                calculadoraAliquotaProduto.calcularAliquota(itens, faixaAplicada.aliquota()),
                faixaAplicada);
    }
}
