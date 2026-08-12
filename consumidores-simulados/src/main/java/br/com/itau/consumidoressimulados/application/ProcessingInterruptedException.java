package br.com.itau.consumidoressimulados.application;

public class ProcessingInterruptedException extends RuntimeException {

    public ProcessingInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }
}
