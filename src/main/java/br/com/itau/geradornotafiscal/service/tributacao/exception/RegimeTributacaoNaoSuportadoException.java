package br.com.itau.geradornotafiscal.service.tributacao.exception;

import br.com.itau.geradornotafiscal.model.RegimeTributacaoPJ;

public class RegimeTributacaoNaoSuportadoException extends RuntimeException {
    public RegimeTributacaoNaoSuportadoException(RegimeTributacaoPJ regimeTributacao) {
        super("Regime tributario nao suportado: " + regimeTributacao);
    }
}
