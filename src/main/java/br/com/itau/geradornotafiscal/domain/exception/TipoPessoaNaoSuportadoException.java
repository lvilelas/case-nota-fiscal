package br.com.itau.geradornotafiscal.domain.exception;

import br.com.itau.geradornotafiscal.domain.model.TipoPessoa;

public class TipoPessoaNaoSuportadoException extends RuntimeException {
    public TipoPessoaNaoSuportadoException(TipoPessoa tipoPessoa) {
        super("Tipo de pessoa nao suportado: " + tipoPessoa);
    }
}
