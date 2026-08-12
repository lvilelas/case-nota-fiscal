package br.com.itau.geradornotafiscal.application.exception;

public class PersistenciaNotaFiscalException extends RuntimeException {
    public PersistenciaNotaFiscalException(String message, Throwable cause) {
        super(message, cause);
    }
}
