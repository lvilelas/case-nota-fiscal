# Teste de performance com JMeter

O plano envia pedidos únicos com sete itens e mede a API completa, incluindo cálculo, persistência idempotente e criação da outbox. Além do HTTP 200, cada amostra valida que a resposta preserva os sete itens e o total de R$ 700,00. O critério padrão é taxa de erro igual a zero, cada amostra abaixo de 2 segundos e p95 global abaixo de 1 segundo.

Com o Compose em execução:

```bash
docker run --rm --network case-nota-fiscal_default \
  -v "$PWD/performance:/tests" \
  alpine/jmeter:5.6.3 \
  -n -t /tests/nota-fiscal-mais-de-seis-itens.jmx \
  -l /tests/results.jtl -e -o /tests/report \
  -Jhost=app -Jport=8080 -Jthreads=10 -Jiterations=10

python performance/assert_results.py performance/results.jtl
```

Para comparar commits, execute o mesmo plano, volume e limites em cada revisão e preserve os relatórios como artefatos do pipeline. O resultado não deve ser comparado entre máquinas diferentes sem normalizar CPU, memória e banco.

Validação local desta entrega (Docker Desktop, 2026-08-11): 100 pedidos com sete itens, 0% de erro, p95 de 39 ms e máximo de 85 ms. Esse número demonstra o atendimento do gate neste ambiente; o relatório de cada execução do GitHub Actions é a evidência reproduzível e não deve ser tratado como SLA de produção.
