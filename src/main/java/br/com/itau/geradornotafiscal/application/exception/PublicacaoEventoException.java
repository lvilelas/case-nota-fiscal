package br.com.itau.geradornotafiscal.application.exception;

public class PublicacaoEventoException extends RuntimeException {
    public PublicacaoEventoException(String message, Throwable cause) {
        super(message, cause);
    }
}
