package br.com.itau.geradornotafiscal.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Documento {

    private String numero;
    private TipoDocumento tipo;

}
