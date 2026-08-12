package br.com.itau.geradornotafiscal.adapter.in.web.dto;

import br.com.itau.geradornotafiscal.domain.model.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DocumentoRequest(
        @NotBlank(message = "é obrigatório")
        String numero,
        @NotNull(message = "é obrigatório")
        TipoDocumento tipo) {
}
