package br.com.itau.geradornotafiscal.adapter.out.integration;

import br.com.itau.geradornotafiscal.application.port.out.BaixarEstoquePort;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import org.springframework.stereotype.Component;

@Component
public class EstoqueIntegrationAdapter implements BaixarEstoquePort {
    @Override
    public void baixarEstoque(NotaFiscal notaFiscal) {
        try {
            //Simula envio de nota fiscal para baixa de estoque
            Thread.sleep(380);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
