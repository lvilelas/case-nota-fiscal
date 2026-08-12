package br.com.itau.geradornotafiscal.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Endereco {
    private String cep;

    private String logradouro;

    private String numero;

    private String estado;

    private String complemento;

    private Finalidade finalidade;

    private Regiao regiao;
}
