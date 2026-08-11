package br.com.itau.geradornotafiscal;

import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import br.com.itau.geradornotafiscal.domain.model.Pedido;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

@SpringBootTest
class GeradorNotaFiscalApplicationTests {
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void deveIniciarAplicacaoSpring() {
        String[] argumentos = {"--spring.main.web-application-type=none"};

        new GeradorNotaFiscalApplication();
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            GeradorNotaFiscalApplication.main(argumentos);

            springApplication.verify(() -> SpringApplication.run(
                    GeradorNotaFiscalApplication.class,
                    argumentos));
        }
    }

    @Test
    void deveManterContratoHttpEmSnakeCase() throws Exception {
        String jsonPedido = """
                {
                  "id_pedido": 123,
                  "valor_total_itens": 100.0,
                  "valor_frete": 10.0,
                  "itens": []
                }
                """;

        Pedido pedido = objectMapper.readValue(jsonPedido, Pedido.class);
        NotaFiscal notaFiscal = NotaFiscal.builder()
                .idNotaFiscal("nf-123")
                .valorTotalItens(100)
                .valorFrete(10)
                .build();
        JsonNode jsonNotaFiscal = objectMapper.readTree(objectMapper.writeValueAsString(notaFiscal));

        assertEquals(123, pedido.getIdPedido());
        assertEquals(100, pedido.getValorTotalItens());
        assertTrue(jsonNotaFiscal.has("id_nota_fiscal"));
        assertTrue(jsonNotaFiscal.has("valor_total_itens"));
        assertFalse(jsonNotaFiscal.has("idNotaFiscal"));
    }
}
