package br.com.itau.geradornotafiscal.service.tributacao.exception;

import br.com.itau.geradornotafiscal.model.TipoPessoa;

public class TipoPessoaNaoSuportadoException extends RuntimeException {
    public TipoPessoaNaoSuportadoException(TipoPessoa tipoPessoa) {
        super("Tipo de pessoa nao suportado: " + tipoPessoa);
    }
}
