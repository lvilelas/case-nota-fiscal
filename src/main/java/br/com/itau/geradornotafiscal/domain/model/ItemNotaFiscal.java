package br.com.itau.geradornotafiscal.domain.model;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ItemNotaFiscal {
    private String idItem;

    private String descricao;

    private double valorUnitario;

    private int quantidade;

    private double valorTributoItem;

}
