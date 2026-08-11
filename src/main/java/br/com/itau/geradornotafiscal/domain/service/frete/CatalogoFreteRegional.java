package br.com.itau.geradornotafiscal.domain.service.frete;

import br.com.itau.geradornotafiscal.domain.model.Regiao;

import java.util.EnumMap;

public class CatalogoFreteRegional {
    private final EnumMap<Regiao, Double> multiplicadores = new EnumMap<>(Regiao.class);

    public CatalogoFreteRegional() {
        multiplicadores.put(Regiao.NORTE, 1.08);
        multiplicadores.put(Regiao.NORDESTE, 1.085);
        multiplicadores.put(Regiao.CENTRO_OESTE, 1.07);
        multiplicadores.put(Regiao.SUDESTE, 1.048);
        multiplicadores.put(Regiao.SUL, 1.06);
    }

    public double buscar(Regiao regiao) {
        return multiplicadores.getOrDefault(regiao, 0.0);
    }
}
