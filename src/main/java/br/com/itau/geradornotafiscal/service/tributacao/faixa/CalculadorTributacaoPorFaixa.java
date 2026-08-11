package br.com.itau.geradornotafiscal.service.tributacao.faixa;

import br.com.itau.geradornotafiscal.model.ItemNotaFiscal;
import br.com.itau.geradornotafiscal.model.Item;
import br.com.itau.geradornotafiscal.service.CalculadoraAliquotaProduto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
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
