package br.com.itau.geradornotafiscal.application.exception;

public class AcessoSegredoException extends RuntimeException {
    public AcessoSegredoException(String message, Throwable cause) {
        super(message, cause);
    }
}
