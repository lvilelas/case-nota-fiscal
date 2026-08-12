package br.com.itau.geradornotafiscal.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ItemRequest(
        @NotBlank(message = "é obrigatório")
        String idItem,
        @NotBlank(message = "é obrigatória")
        String descricao,
        @Positive(message = "deve ser maior que zero")
        double valorUnitario,
        @Positive(message = "deve ser maior que zero")
        int quantidade) {
}
