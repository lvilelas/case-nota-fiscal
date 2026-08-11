package br.com.itau.geradornotafiscal.domain.service.frete;

import br.com.itau.geradornotafiscal.domain.model.Regiao;

public class CalculadorFrete {
    private final CatalogoFreteRegional catalogoFreteRegional;

    public CalculadorFrete(CatalogoFreteRegional catalogoFreteRegional) {
        this.catalogoFreteRegional = catalogoFreteRegional;
    }

    public ResultadoCalculoFrete calcular(double valorFrete, Regiao regiao) {
        double multiplicador = catalogoFreteRegional.buscar(regiao);
        return new ResultadoCalculoFrete(
                valorFrete,
                regiao,
                multiplicador,
                valorFrete * multiplicador);
    }
}
