package br.com.itau.geradornotafiscal.adapter.out.integration;

import br.com.itau.geradornotafiscal.application.port.out.EnviarNotaFiscalFinanceiroPort;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import org.springframework.stereotype.Component;

@Component
public class FinanceiroIntegrationAdapter implements EnviarNotaFiscalFinanceiroPort {
    @Override
    public void enviar(NotaFiscal notaFiscal) {

        try {
            //Simula o envio da nota fiscal para o contas a receber
            Thread.sleep(250);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
