# Runbook operacional

## Endpoints de diagnóstico

- Local: liveness `/actuator/health/liveness`, readiness `/actuator/health/readiness`, métricas `/actuator/prometheus` e OpenAPI `/v3/api-docs` na porta 8080.
- AWS: o ALB acessa somente `/livez` e `/readyz` na porta 8080. Actuator e Prometheus ficam na porta de gerenciamento 9090, acessível apenas dentro da task pelo sidecar ADOT. OpenAPI/Swagger ficam desabilitados por padrão em produção.

O ALB remove uma task do tráfego quando a readiness falha. A liveness deve indicar apenas se o processo precisa ser reiniciado; indisponibilidade temporária de SNS não derruba a API porque a outbox é durável.

## Alarme de CPU ou memória

1. Verifique o dashboard CloudWatch e correlacione CPU, memória, latência e quantidade de tasks.
2. Consulte Container Insights e logs da task pelo mesmo intervalo.
3. Confirme se o autoscaling aumentou `DesiredCount` e se novas tasks ficaram `RUNNING`.
4. Para memória crescente, consulte `jvm_memory_used_bytes`, GC e heap no namespace `CaseNotaFiscal/Application` antes de aumentar o limite.
5. Ajustes de CPU/memória devem passar pelo Terraform; não altere manualmente a task definition.

## Latência ou respostas 5xx

1. Localize `traceId` no log e abra o trace correspondente no X-Ray.
2. Verifique `http_server_requests_seconds`, pool Hikari, CPU, GC e conexões Aurora.
3. Consulte eventos do ECS, saúde do target group e deployments recentes.
4. Se começou após deploy, faça rollback para uma task definition anterior:

```bash
aws ecs update-service --cluster <cluster> --service <service> --task-definition <family:revision-anterior>
aws ecs wait services-stable --cluster <cluster> --services <service>
```

## Falha definitiva da outbox

1. Localize no log `Evento da outbox esgotou tentativas` e capture `eventId`, `pedidoId`, `traceId`, `correlationId` e causa.
2. Confirme se SNS e IAM estão saudáveis antes do replay.
3. Inspecione o registro sem modificar o payload:

```sql
SELECT event_id, aggregate_id, status, tentativas, ultimo_erro, criado_em
FROM outbox_evento
WHERE event_id = '<event-id>';
```

4. Após corrigir a causa, reative somente o evento aprovado:

```sql
UPDATE outbox_evento
SET status = 'PENDENTE', tentativas = 0,
    proxima_tentativa_em = CURRENT_TIMESTAMP,
    bloqueado_ate = NULL, lock_token = NULL, ultimo_erro = NULL
WHERE event_id = '<event-id>' AND status = 'FALHA';
```

O replay pode entregar novamente um evento que já chegou ao consumidor. Os consumidores devem tratar a `idempotency_key` atomicamente.

## Mensagem na DLQ

1. Não faça redrive antes de corrigir a causa do consumidor.
2. Inspecione body, `event_id`, `event_version`, `idempotency_key` e `ApproximateReceiveCount`.
3. Compare o contrato com a versão suportada pelo consumidor.
4. Após a correção, use o redrive da DLQ para a fila original e acompanhe backlog/erros.
5. Nunca edite manualmente valores fiscais no evento.

## Aurora indisponível

1. Verifique eventos/failover do cluster, conexões, CPU e `DatabaseConnections`.
2. Confirme readiness das tasks e erros `PERSISTENCIA_INDISPONIVEL`.
3. Valide rotação do secret e reinicie tasks somente se as credenciais injetadas estiverem obsoletas.
4. Durante falha do writer, não remova a idempotência nem direcione gravações ao reader endpoint.

## Graceful shutdown

O Spring recebe SIGTERM, interrompe novas requisições e possui 30 segundos para finalizar requests. O ECS usa `stopTimeout=30` e o container local `stop_grace_period=35s`. Investigue requests acima desse prazo antes de aumentá-lo.
