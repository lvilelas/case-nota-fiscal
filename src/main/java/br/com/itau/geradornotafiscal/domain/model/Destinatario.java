package br.com.itau.geradornotafiscal.domain.model;

import java.util.List;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Destinatario {
	private String nome;

	private TipoPessoa tipoPessoa;

	private RegimeTributacaoPJ regimeTributacao;

	private List<Documento> documentos;

	private List<Endereco> enderecos;

}




