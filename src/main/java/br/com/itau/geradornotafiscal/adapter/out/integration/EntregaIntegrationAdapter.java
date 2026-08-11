package br.com.itau.geradornotafiscal.adapter.out.integration;

import br.com.itau.geradornotafiscal.application.port.out.AgendarEntregaPort;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import org.springframework.stereotype.Component;

@Component
public class EntregaIntegrationAdapter implements AgendarEntregaPort {
    @Override
    public void agendar(NotaFiscal notaFiscal) {
        try {
            Thread.sleep(150);
            if (notaFiscal.getItens().size() > 5) {
                Thread.sleep(5000);
            }
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
