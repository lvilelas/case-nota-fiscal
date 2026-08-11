package br.com.itau.geradornotafiscal.service.frete;

import br.com.itau.geradornotafiscal.model.Regiao;
import br.com.itau.geradornotafiscal.service.frete.tabela.CatalogoFreteRegional;
import org.springframework.stereotype.Component;

@Component
public class CalculadorFrete {
    private final CatalogoFreteRegional catalogoFreteRegional;

    public CalculadorFrete(CatalogoFreteRegional catalogoFreteRegional) {
        this.catalogoFreteRegional = catalogoFreteRegional;
    }

    public double calcular(double valorFrete, Regiao regiao) {
        double multiplicador = catalogoFreteRegional.buscar(regiao);
        return valorFrete * multiplicador;
    }
}
