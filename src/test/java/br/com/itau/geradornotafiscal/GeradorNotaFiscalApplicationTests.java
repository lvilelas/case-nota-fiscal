package br.com.itau.geradornotafiscal;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

import static org.mockito.Mockito.mockStatic;

@SpringBootTest
class GeradorNotaFiscalApplicationTests {

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
}
