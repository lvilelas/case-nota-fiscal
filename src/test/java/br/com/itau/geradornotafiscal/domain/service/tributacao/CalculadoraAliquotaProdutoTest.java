package br.com.itau.geradornotafiscal.domain.service.tributacao;

import br.com.itau.geradornotafiscal.domain.model.Item;
import br.com.itau.geradornotafiscal.domain.model.ItemNotaFiscal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class CalculadoraAliquotaProdutoTest {

    private final CalculadoraAliquotaProduto calculadora = new CalculadoraAliquotaProduto();

    @Test
    void naoDeveAcumularItensEntreCalculos() {
        List<ItemNotaFiscal> primeiroResultado = calculadora.calcularAliquota(
                List.of(item("primeiro", 100, 1)), 0.10);

        List<ItemNotaFiscal> segundoResultado = calculadora.calcularAliquota(
                List.of(item("segundo", 200, 2)), 0.10);

        assertNotSame(primeiroResultado, segundoResultado);
        assertEquals(1, primeiroResultado.size());
        assertEquals("primeiro", primeiroResultado.get(0).getIdItem());
        assertEquals(1, segundoResultado.size());
        assertEquals("segundo", segundoResultado.get(0).getIdItem());
    }

    private Item item(String id, double valorUnitario, int quantidade) {
        Item item = new Item();
        item.setIdItem(id);
        item.setValorUnitario(valorUnitario);
        item.setQuantidade(quantidade);
        return item;
    }
}
