package br.com.itau.geradornotafiscal.domain.exception;

import br.com.itau.geradornotafiscal.domain.model.RegimeTributacaoPJ;

public class RegimeTributacaoNaoSuportadoException extends RuntimeException {
    public RegimeTributacaoNaoSuportadoException(RegimeTributacaoPJ regimeTributacao) {
        super("Regime tributario nao suportado: " + regimeTributacao);
    }
}
