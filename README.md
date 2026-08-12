# Desafio Técnico — Nota Fiscal

## Contexto

A aplicação responsável pelo processamento de notas fiscais apresenta atualmente uma série de desafios técnicos, funcionais e de manutenção.

O objetivo deste desafio é analisar o cenário existente, identificar as causas dos problemas e propor uma evolução da solução considerando não apenas a correção dos problemas atuais, mas também qualidade de código, arquitetura, performance, observabilidade, testes e todo o ciclo de desenvolvimento e entrega de software.

O candidato tem liberdade para aplicar quaisquer boas práticas, padrões ou melhorias que considerar pertinentes. Espera-se uma visão que vá além da simples correção dos problemas apresentados.

---

## Problemas conhecidos

### Manutenibilidade e qualidade de código

* O código atual apresenta alta complexidade, tornando alterações e evoluções difíceis de realizar com segurança.
* Existem diversas regras de cálculo e fluxos de processamento diferentes concentrados na aplicação.
* A classe responsável pelo fluxo principal sofre alterações frequentes e apresenta alto grau de instabilidade.
* A cobertura de testes é baixa.
* Parte dos testes existentes encontra-se quebrada ou apresenta comportamento inconsistente.

### Problemas funcionais

* A primeira execução de um processamento ocorre corretamente.
* A partir das execuções seguintes, ocorre um problema na devolução dos itens: dados processados anteriormente permanecem acumulados e passam a fazer parte das novas respostas.
* Existem relatos de sistemas consumidores recebendo informações inconsistentes relacionadas:

  * aos valores da nota fiscal;
  * ao valor total calculado;
  * à quantidade de itens processados.

### Problemas de performance

* Pedidos contendo mais de **6 itens** apresentam aumento significativo no tempo de processamento.
* Após sucessivas execuções da aplicação, o tempo de resposta tende a aumentar consideravelmente.
* Algumas integrações externas possuem latência propositalmente simulada para representar chamadas reais e esse comportamento faz parte do cenário do desafio.

---

# Objetivo do desafio

A solução proposta deve:

* Corrigir os problemas funcionais identificados.
* Melhorar a performance da aplicação.
* Aumentar a confiabilidade do processamento.
* Facilitar futuras alterações e inclusão de novas regras.
* Melhorar a experiência de desenvolvimento e manutenção da aplicação.
* Demonstrar boas práticas de engenharia de software durante todo o ciclo de desenvolvimento e entrega.

O candidato é livre para realizar quaisquer melhorias adicionais que considerar relevantes. A capacidade de identificar problemas não explicitamente descritos e propor soluções adequadas também será considerada na avaliação.

---

# Premissas e restrições

## Contrato da API

O **payload de entrada não deve ser modificado**.

A solução deve manter compatibilidade com o contrato atual da aplicação.

## Integrações externas

Algumas integrações possuem tempos de resposta simulados para representar chamadas externas reais.

Essas esperas fazem parte do comportamento esperado do cenário e **não devem simplesmente ser removidas** como forma de otimização.

## Modernização tecnológica

A aplicação deverá ser atualizada para:

* **Java 21**
* versão estável mais recente do **Spring / Spring Boot** compatível com a solução proposta.

Sempre que fizer sentido, o candidato poderá utilizar recursos disponíveis nas versões mais recentes do Java e do ecossistema Spring.

A utilização dessas funcionalidades deve ser justificada pelo benefício proporcionado à solução, evitando seu uso apenas por serem recursos novos.

---

# Visão arquitetural

Além das alterações no código da aplicação, espera-se uma proposta arquitetural representando o contexto no qual o sistema estaria executando em um ambiente produtivo.

O desenho deverá considerar, quando aplicável:

* API Gateway;
* autenticação e autorização;
* serviços/APIs consumidos pela aplicação;
* integrações externas;
* comunicação entre serviços;
* infraestrutura AWS;
* rede e segurança;
* escalabilidade;
* alta disponibilidade;
* observabilidade;
* persistência de dados;
* mecanismos de resiliência.

Não é necessário implementar toda a infraestrutura apresentada no desenho, mas as decisões arquiteturais deverão estar coerentes com a solução proposta.

---

# Engenharia e ciclo de entrega

A avaliação não estará restrita apenas ao código produzido.

Uma entrega de alta qualidade deve considerar o **ciclo completo de desenvolvimento e entrega de software**, incluindo aspectos como:

* organização e qualidade do código;
* arquitetura e separação de responsabilidades;
* estratégia de testes;
* tratamento de falhas;
* performance;
* concorrência;
* segurança;
* observabilidade;
* documentação;
* decisões arquiteturais e seus trade-offs;
* estratégia de build;
* integração contínua;
* estratégia de deployment;
* rollback;
* configuração por ambiente;
* monitoramento da aplicação em produção.

A profundidade aplicada em cada aspecto fica a critério do candidato.

O principal objetivo é demonstrar **como você pensa, investiga problemas, toma decisões técnicas e estrutura uma solução sustentável para produção**.

---

## Documentação da solução

- [Arquitetura hexagonal, eventos e diagrama AWS](ARCHITECTURE.md)
- [Execução local com Docker Compose](docs/EXECUCAO-LOCAL.md)
- [Worker didático com quatro consumidores SQS](consumidores-simulados/README.md)
- [Infraestrutura AWS proposta em Terraform](infra/terraform/README.md)
- [Runbook operacional](docs/RUNBOOK.md)
- [Teste de performance com JMeter](performance/README.md)
- [Configuração da revisão automática com GitHub Copilot](docs/COPILOT-CODE-REVIEW.md)

Com o perfil `local`, a especificação OpenAPI fica disponível em `/v3/api-docs` e a interface Swagger em `/swagger-ui.html`.

---

## Como os problemas do case foram tratados

O contrato HTTP original foi preservado: os nomes `snake_case`, os tipos e a estrutura de entrada e saída não foram alterados. Campos adicionais presentes nos arquivos de exemplo continuam sendo tolerados como no código inicial.

| Problema apresentado | Tratamento aplicado | Evidência principal |
| --- | --- | --- |
| Classe central extensa, instável e difícil de evoluir | O fluxo foi separado em arquitetura hexagonal: controller e scheduler como adaptadores de entrada, casos de uso na aplicação, regras no domínio e JDBC/AWS nos adaptadores de saída. | [`ARCHITECTURE.md`](ARCHITECTURE.md) e `src/main/java/.../application` |
| Muitos condicionais para tributação e frete | O algoritmo tributário único passou a consultar um catálogo de faixas por tipo/regime; o frete usa `EnumMap<Regiao, Double>`. Novas tabelas não exigem alterar o fluxo principal. | `CatalogoTributario`, `CalculadorTributacaoPorFaixa` e `CatalogoFreteRegional` |
| Baixa cobertura e testes quebrados/inconsistentes | Há testes unitários, MVC, persistência PostgreSQL, concorrência/idempotência, LocalStack, contrato de evento e consumidores. O JaCoCo bloqueia o build se qualquer classe medida ficar abaixo de 100% de linhas ou branches. | `src/test`, `consumidores-simulados/src/test` e `pom.xml` |
| Itens de execuções anteriores acumulavam nas respostas seguintes | A lista `static` foi removida. Cada cálculo cria sua própria lista, sem estado mutável compartilhado entre requisições. | `CalculadoraAliquotaProduto` e teste `naoDeveMisturarItensDeExecucoesDiferentes` |
| Valor total, valores da nota e quantidade de itens inconsistentes | O total informado não é usado como fonte de verdade: ele é recalculado pela soma de `valor_unitario * quantidade`. A nota usa somente os itens da requisição corrente e o teste de carga valida sete itens e total de R$ 700,00 em todas as respostas. | `CalculadorTotalPedido`, `NotaFiscalFactory` e plano JMeter |
| Pedidos com mais de seis itens bloqueavam a API por mais de cinco segundos | As quatro integrações deixaram o caminho síncrono. A transação grava a nota e um evento de outbox; o relay publica um único `NotaFiscalGerada` no SNS, que faz fan-out para quatro filas SQS com DLQ. A latência simulada foi mantida nos consumidores didáticos. | `PostgresNotaFiscalAdapter`, `PublicadorOutboxNotaFiscal`, Terraform e `consumidores-simulados` |
| Tempo aumentava após execuções sucessivas | O estado global foi eliminado e o processamento passou a ser idempotente por `pedido_id`, persistido no PostgreSQL. Repetições recuperam a mesma nota sem recalcular nem criar outro evento. | `GerarNotaFiscalService` e `nota_fiscal_processada` |
| Falhas concorrentes ou entre banco e mensageria podiam duplicar/perder processamento | `pedido_id` e a chave de idempotência possuem restrições únicas; nota e outbox são gravadas na mesma transação; a reserva usa `FOR UPDATE SKIP LOCKED`, fencing token e renovação de lease; publicação segue `at-least-once`, retry exponencial e DLQ. | migrations Flyway, adapter PostgreSQL e testes Testcontainers |
| Exceções de entrada/regra viravam erro 500 | DTOs validam o contrato sem alterar sua forma. O `RestControllerAdvice` devolve 400 para payload inválido/ilegível, 422 para regra não suportada e 503 para indisponibilidade de infraestrutura. | `ApiExceptionHandler` e testes MVC |
| Secrets e configurações misturados entre ambientes | Existem perfis `local`, `dev`, `homol` e `prod`; valores sensíveis chegam por variáveis de ambiente/Secrets Manager e apenas credenciais locais descartáveis possuem defaults. As properties falham cedo para limites inválidos. | `application-*.properties`, `AwsProperties` e Terraform |
| Ausência de rastreabilidade e operação | Logs incluem `correlationId`, `flowId`, `traceId` e `spanId`; identificadores recebidos são preservados quando seguros. Actuator, Prometheus, OpenTelemetry/Jaeger, health probes, graceful shutdown, alarmes e dashboard foram adicionados. | filtro de rastreabilidade, Compose, Terraform e runbook |
| Ciclo de entrega não demonstrado | A GitHub Action é propositalmente educacional: compila, testa, aplica gate JaCoCo, procura secrets/vulnerabilidades, constrói e escaneia as imagens, mas não autentica na AWS, não publica no ECR e não executa deploy. | `.github/workflows/ci-cd.yml` |

### Limites assumidos

- A idempotência considera `pedido_id` como chave do comando. Reutilizar o mesmo identificador para um pedido semanticamente diferente é erro do cliente; em uma evolução de contrato, um hash do comando poderia permitir a detecção explícita desse conflito.
- Os consumidores incluídos são demonstrativos e não produzem efeitos reais. Em produção, cada consumidor precisa registrar sua própria chave idempotente antes/depois do efeito externo conforme a garantia oferecida pela integração.
- PostgreSQL local valida protocolo, SQL e transações, mas não reproduz failover, rede multi-AZ nem elasticidade do Aurora.

---

## Resultado do teste de performance

O relatório em `performance/report` foi gerado em **12/08/2026 02:45 UTC** (11/08/2026 no horário de Brasília), usando Docker Desktop e o plano [`nota-fiscal-mais-de-seis-itens.jmx`](performance/nota-fiscal-mais-de-seis-itens.jmx). A carga utilizou 50 usuários virtuais, 20 iterações por usuário, ramp-up de 10 segundos e pedidos únicos com sete itens.

| Métrica | Resultado |
| --- | ---: |
| Requisições | 1.000 |
| Sucesso HTTP 200 | 1.000 (100%) |
| Erros | 0 (0%) |
| Vazão | 109,55 requisições/s |
| Média | 55,13 ms |
| Mediana | 34,50 ms |
| p90 | 125,00 ms |
| p95 | 156,90 ms |
| p99 | 220,97 ms |
| Mínimo / máximo | 5 ms / 503 ms |

Todas as amostras também passaram nas asserções funcionais do JMeter: HTTP 200, sete itens devolvidos e `valor_total_itens` igual a 700. O resultado mostra que a latência proposital de mais de cinco segundos permanece no consumidor de entrega, fora do tempo de resposta da API.

Esse ensaio local demonstra a melhoria e atende ao gate configurado, mas não representa SLA de produção. Comparações futuras devem reutilizar o mesmo plano, limites, hardware e configuração de banco. O relatório HTML e o JTL são artefatos gerados e permanecem ignorados pelo Git; a forma de reprodução está em [`performance/README.md`](performance/README.md).
