package br.com.itau.geradornotafiscal;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(
        title = "Gerador de Nota Fiscal API",
        version = "v1",
        description = "Gera notas fiscais de forma idempotente e publica eventos por transactional outbox."))
public class GeradorNotaFiscalApplication {

	public static void main(String[] args) {
		SpringApplication.run(GeradorNotaFiscalApplication.class, args);
	}

}
