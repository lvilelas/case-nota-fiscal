package br.com.itau.geradornotafiscal.application.event;

public record RegistroOutbox(NotaFiscalGeradaEvent evento, int tentativas, String tokenReserva) {
}
