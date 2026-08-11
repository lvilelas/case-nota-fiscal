package br.com.itau.geradornotafiscal.adapter.in.web.dto;

import br.com.itau.geradornotafiscal.domain.model.Finalidade;
import br.com.itau.geradornotafiscal.domain.model.Regiao;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EnderecoRequest(
        @NotBlank(message = "é obrigatório")
        String cep,
        @NotBlank(message = "é obrigatório")
        String logradouro,
        @NotBlank(message = "é obrigatório")
        String numero,
        @NotBlank(message = "é obrigatório")
        String estado,
        String complemento,
        @NotNull(message = "é obrigatória")
        Finalidade finalidade,
        @NotNull(message = "é obrigatória")
        Regiao regiao) {
}
