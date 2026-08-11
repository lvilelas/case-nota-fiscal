package br.com.itau.geradornotafiscal.domain.service.tributacao;

import br.com.itau.geradornotafiscal.domain.exception.RegimeTributacaoNaoSuportadoException;
import br.com.itau.geradornotafiscal.domain.exception.TipoPessoaNaoSuportadoException;
import br.com.itau.geradornotafiscal.domain.model.RegimeTributacaoPJ;
import br.com.itau.geradornotafiscal.domain.model.TipoPessoa;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CatalogoTributario {
    private static final List<FaixaAliquota> FAIXAS_PESSOA_FISICA = List.of(
            new FaixaAliquota(500, false, 0),
            new FaixaAliquota(2000, true, 0.12),
            new FaixaAliquota(3500, true, 0.15),
            new FaixaAliquota(Double.POSITIVE_INFINITY, true, 0.17));

    private static final Map<RegimeTributacaoPJ, List<FaixaAliquota>> TABELAS_PJ = Map.of(
            RegimeTributacaoPJ.SIMPLES_NACIONAL, List.of(
                    new FaixaAliquota(1000, false, 0.03),
                    new FaixaAliquota(2000, true, 0.07),
                    new FaixaAliquota(5000, true, 0.13),
                    new FaixaAliquota(Double.POSITIVE_INFINITY, true, 0.19)),
            RegimeTributacaoPJ.LUCRO_REAL, List.of(
                    new FaixaAliquota(1000, false, 0.03),
                    new FaixaAliquota(2000, true, 0.09),
                    new FaixaAliquota(5000, true, 0.15),
                    new FaixaAliquota(Double.POSITIVE_INFINITY, true, 0.20)),
            RegimeTributacaoPJ.LUCRO_PRESUMIDO, List.of(
                    new FaixaAliquota(1000, false, 0.03),
                    new FaixaAliquota(2000, true, 0.09),
                    new FaixaAliquota(5000, true, 0.16),
                    new FaixaAliquota(Double.POSITIVE_INFINITY, true, 0.20)));

    public List<FaixaAliquota> buscar(
            TipoPessoa tipoPessoa,
            RegimeTributacaoPJ regimeTributacao) {
        if (tipoPessoa == null) {
            throw new TipoPessoaNaoSuportadoException(null);
        }

        return switch (tipoPessoa) {
            case FISICA -> FAIXAS_PESSOA_FISICA;
            case JURIDICA -> buscarTabelaPessoaJuridica(regimeTributacao);
        };
    }

    private List<FaixaAliquota> buscarTabelaPessoaJuridica(
            RegimeTributacaoPJ regimeTributacao) {
        return Optional.ofNullable(regimeTributacao)
                .map(TABELAS_PJ::get)
                .orElseThrow(() -> new RegimeTributacaoNaoSuportadoException(regimeTributacao));
    }
}
