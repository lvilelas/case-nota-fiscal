package br.com.itau.geradornotafiscal.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.util.List;

public record PedidoRequest(
        @Positive(message = "deve ser maior que zero")
        int idPedido,
        @NotNull(message = "é obrigatória")
        LocalDate data,
        @PositiveOrZero(message = "não pode ser negativo")
        double valorTotalItens,
        @PositiveOrZero(message = "não pode ser negativo")
        double valorFrete,
        @NotEmpty(message = "deve possuir ao menos um item")
        List<@Valid ItemRequest> itens,
        @NotNull(message = "é obrigatório")
        @Valid
        DestinatarioRequest destinatario) {
}
