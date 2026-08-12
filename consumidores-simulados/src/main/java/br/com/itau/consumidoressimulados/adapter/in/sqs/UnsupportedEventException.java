package br.com.itau.consumidoressimulados.adapter.in.sqs;

public class UnsupportedEventException extends RuntimeException {

    public UnsupportedEventException(String message) {
        super(message);
    }
}
