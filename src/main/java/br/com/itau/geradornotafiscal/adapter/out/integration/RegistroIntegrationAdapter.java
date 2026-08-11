package br.com.itau.geradornotafiscal.adapter.out.integration;

import br.com.itau.geradornotafiscal.application.port.out.RegistrarNotaFiscalPort;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import org.springframework.stereotype.Component;

@Component
public class RegistroIntegrationAdapter implements RegistrarNotaFiscalPort {
    @Override
    public void registrar(NotaFiscal notaFiscal) {

        try {
            //Simula o registro da nota fiscal
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
