package br.com.itau.geradornotafiscal.domain.service.tributacao;

import br.com.itau.geradornotafiscal.domain.model.ItemNotaFiscal;
import br.com.itau.geradornotafiscal.domain.model.Item;

import java.util.List;

public class CalculadorTributacaoPorFaixa {
    private final CalculadoraAliquotaProduto calculadoraAliquotaProduto;

    public CalculadorTributacaoPorFaixa(CalculadoraAliquotaProduto calculadoraAliquotaProduto) {
        this.calculadoraAliquotaProduto = calculadoraAliquotaProduto;
    }

    public List<ItemNotaFiscal> calcular(
            List<Item> itens,
            double valorTotalItens,
            List<FaixaAliquota> faixas) {
        double aliquota = faixas.stream()
                .filter(faixa -> faixa.aceita(valorTotalItens))
                .findFirst()
                .orElseThrow()
                .aliquota();

        return calculadoraAliquotaProduto.calcularAliquota(itens, aliquota);
    }
}
