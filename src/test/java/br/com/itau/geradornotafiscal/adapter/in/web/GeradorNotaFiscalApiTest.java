package br.com.itau.geradornotafiscal.adapter.in.web;

import br.com.itau.geradornotafiscal.adapter.in.web.error.ApiExceptionHandler;
import br.com.itau.geradornotafiscal.adapter.in.web.filter.RastreabilidadeHttpFilter;
import br.com.itau.geradornotafiscal.adapter.in.web.mapper.PedidoWebMapper;
import br.com.itau.geradornotafiscal.application.port.in.GerarNotaFiscalUseCase;
import br.com.itau.geradornotafiscal.domain.exception.TipoPessoaNaoSuportadoException;
import br.com.itau.geradornotafiscal.domain.model.NotaFiscal;
import br.com.itau.geradornotafiscal.domain.model.Pedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GeradorNotaFiscalApiTest {
    private static final String URI = "/api/pedido/gerarNotaFiscal";
    private GerarNotaFiscalUseCase useCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        useCase = mock(GerarNotaFiscalUseCase.class);
        JsonMapper jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .build();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new GeradorNotaFiscalController(useCase, new PedidoWebMapper()))
                .setControllerAdvice(new ApiExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
                .addFilters(new RastreabilidadeHttpFilter())
                .build();
    }

    @Test
    void deveAceitarContratoSnakeCaseExistente() throws Exception {
        NotaFiscal notaFiscal = NotaFiscal.builder()
                .idNotaFiscal("nf-1")
                .itens(List.of())
                .build();
        when(useCase.gerarNotaFiscal(any(Pedido.class))).thenReturn(notaFiscal);

        mockMvc.perform(post(URI)
                        .header("X-Correlation-Id", "correlation-api-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadValido()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "correlation-api-test"))
                .andExpect(header().exists("X-Flow-Id"))
                .andExpect(jsonPath("$.id_nota_fiscal").value("nf-1"));
    }

    @Test
    void deveResponder400QuandoPayloadViolaValidacoes() throws Exception {
        mockMvc.perform(post(URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "id_pedido": 0,
                                  "data": null,
                                  "valor_total_itens": -1,
                                  "valor_frete": -1,
                                  "itens": [],
                                  "destinatario": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PAYLOAD_INVALIDO"))
                .andExpect(jsonPath("$.campos_invalidos", hasSize(6)))
                .andExpect(jsonPath("$.correlation_id").isNotEmpty())
                .andExpect(jsonPath("$.flow_id").isNotEmpty());
    }

    @Test
    void deveResponder400QuandoEnumNaoPodeSerInterpretado() throws Exception {
        mockMvc.perform(post(URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadValido().replace("FISICA", "TIPO_INEXISTENTE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("PAYLOAD_ILEGIVEL"));
    }

    @Test
    void deveResponder422QuandoRegraDeDominioNaoForSuportada() throws Exception {
        when(useCase.gerarNotaFiscal(any(Pedido.class)))
                .thenThrow(new TipoPessoaNaoSuportadoException(null));

        mockMvc.perform(post(URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadValido()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("REGRA_NEGOCIO_INVALIDA"));
    }

    private String payloadValido() {
        return """
                {
                  "id_pedido": 1,
                  "data": "2026-08-11",
                  "valor_total_itens": 100.0,
                  "valor_frete": 10.0,
                  "itens": [{
                    "id_item": 1,
                    "descricao": "Teclado USB",
                    "valor_unitario": 50,
                    "quantidade": 2
                  }],
                  "destinatario": {
                    "nome": "John Doe",
                    "tipo_pessoa": "FISICA",
                    "documentos": [{"tipo": "CPF", "numero": "88740347095"}],
                    "enderecos": [{
                      "logradouro": "Av do Estado",
                      "numero": "5533",
                      "complemento": "4 andar",
                      "bairro": "Mooca",
                      "cidade": "Sao Paulo",
                      "estado": "SP",
                      "pais": "Brasil",
                      "cep": "03105003",
                      "finalidade": "ENTREGA",
                      "regiao": "SUDESTE"
                    }]
                  }
                }
                """;
    }
}
