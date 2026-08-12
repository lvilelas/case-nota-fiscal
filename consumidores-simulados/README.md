# Consumidores simulados

Aplicação Spring Boot independente usada para demonstrar o fan-out do evento
`NotaFiscalGerada`. A mesma imagem atende dois modos:

- `CONSUMER_TYPE=ALL` (padrão): quatro listeners SQS em virtual threads no mesmo container;
- `CONSUMER_TYPE=ESTOQUE|REGISTRO|ENTREGA|FINANCEIRO`: somente um listener, permitindo
  executar e escalar a mesma imagem separadamente por consumidor.

Cada listener lê sua fila, desserializa e valida `event_type=NotaFiscalGerada` e
`event_version=1`, adiciona `correlationId`, `flowId` e `eventId` ao MDC, chama o
handler e somente então remove a mensagem. Uma exceção preserva a mensagem para o
retry da SQS e posterior envio à DLQ.

O long polling busca uma mensagem por vez. Isso mantém o pior fluxo simulado de
entrega (5,35 s) dentro do visibility timeout padrão de 30 s, inclusive quando a
mesma imagem é escalada horizontalmente.

Os handlers reproduzem apenas as latências do case: estoque 380 ms, registro 500 ms,
financeiro 250 ms e entrega 350 ms, acrescidos de 5 s a partir de seis itens. Eles
não representam os sistemas reais nem possuem efeitos de negócio persistentes. Em
produção, cada sistema consumidor deve persistir a `idempotency_key` na mesma
transação do próprio efeito.

## Testes

```bash
./mvnw -f consumidores-simulados/pom.xml verify
```

## Imagem

O contexto do build deve ser a raiz do repositório:

```bash
docker build -f consumidores-simulados/Dockerfile \
  -t case-nota-fiscal-consumidores:local .
```
