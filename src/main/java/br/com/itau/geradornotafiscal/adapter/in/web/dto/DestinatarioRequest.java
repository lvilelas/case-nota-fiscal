package br.com.itau.geradornotafiscal.adapter.in.web.dto;

import br.com.itau.geradornotafiscal.domain.model.RegimeTributacaoPJ;
import br.com.itau.geradornotafiscal.domain.model.TipoPessoa;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DestinatarioRequest(
        @NotBlank(message = "é obrigatório")
        String nome,
        @NotNull(message = "é obrigatório")
        TipoPessoa tipoPessoa,
        RegimeTributacaoPJ regimeTributacao,
        @NotEmpty(message = "deve possuir ao menos um documento")
        List<@Valid DocumentoRequest> documentos,
        @NotEmpty(message = "deve possuir ao menos um endereço")
        List<@Valid EnderecoRequest> enderecos) {
}
