package br.com.itau.consumidoressimulados;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class ConsumidoresSimuladosApplicationTest {

    @Test
    void shouldDelegateBootstrapToSpring() {
        new ConsumidoresSimuladosApplication();
        String[] arguments = {"--spring.main.web-application-type=none"};
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            ConsumidoresSimuladosApplication.main(arguments);

            springApplication.verify(() -> SpringApplication.run(
                    ConsumidoresSimuladosApplication.class, arguments));
        }
    }
}
