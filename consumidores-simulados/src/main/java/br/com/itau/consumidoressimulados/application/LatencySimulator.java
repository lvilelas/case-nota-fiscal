package br.com.itau.consumidoressimulados.application;

import org.springframework.stereotype.Component;

@Component
public class LatencySimulator {

    public void waitFor(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ProcessingInterruptedException("Processamento simulado interrompido", exception);
        }
    }
}
