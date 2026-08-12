# Instrucoes de revisao do case Nota Fiscal

Ao revisar pull requests, responda em portugues e reporte somente achados acionaveis. Classifique cada achado por severidade, indique arquivo e linha, explique o impacto observavel e proponha a menor correcao segura. Nao considere a revisao de IA substituta da revisao humana.

Preserve integralmente o contrato HTTP `snake_case`: nao adicione, remova, renomeie, reordene nem altere tipos ou obrigatoriedade dos campos de entrada e saida. Verifique validacao de entrada e respostas 400/422/503 sem tornar o schema mais restritivo.

Proteja as regras centrais: o total da nota vem de `valor_unitario * quantidade`; itens de execucoes diferentes nunca compartilham estado; as faixas tributarias mantem seus limites inclusivos/exclusivos; o frete mantem os multiplicadores e o fallback zero do contrato original.

Respeite a arquitetura hexagonal. Dominio e aplicacao nao devem depender de Spring, AWS, banco ou HTTP. Integracoes devem entrar por portas e adaptadores. Evite estado mutavel global, acoplamento entre camadas e criacao direta de clientes externos nas regras de negocio.

No fluxo de eventos, preserve a gravacao atomica da nota e da outbox, contrato versionado, rastreabilidade, semantica `at-least-once`, retry/DLQ e idempotencia. Um evento `NotaFiscalGerada` deve ser publicado no SNS e distribuido para as quatro filas SQS.

Revise concorrencia, transacoes, tratamento de interrupcao, fechamento de recursos, exposicao de segredos e configuracoes inseguras. Nunca aceite credenciais reais, tokens ou senhas de ambientes compartilhados no repositorio.

Exija testes de regressao para alteracoes funcionais. Os comandos de validacao sao `./mvnw verify` e `./mvnw -f consumidores-simulados/pom.xml verify`; o gate JaCoCo exige 100% de linhas e branches nas classes medidas. Nao recomende deploy automatico: a GitHub Action deste case apenas compila, testa, verifica seguranca e constroi imagens localmente sem publica-las.

Ignore artefatos gerados em `target/`, `performance/report/`, arquivos JTL e estado local do Terraform.
