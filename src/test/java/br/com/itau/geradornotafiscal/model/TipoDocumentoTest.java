package br.com.itau.geradornotafiscal.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TipoDocumentoTest {

    @Test
    void deveDisponibilizarTiposDeDocumento() {
        assertArrayEquals(
                new TipoDocumento[]{TipoDocumento.CPF, TipoDocumento.CNPJ},
                TipoDocumento.values());
        assertEquals(TipoDocumento.CPF, TipoDocumento.valueOf("CPF"));
    }
}
