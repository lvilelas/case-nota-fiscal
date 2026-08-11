CREATE TABLE nota_fiscal_processada (
    pedido_id INTEGER PRIMARY KEY,
    nota_fiscal_id UUID NOT NULL UNIQUE,
    resposta JSONB NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE outbox_evento (
    event_id UUID PRIMARY KEY,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_version INTEGER NOT NULL,
    idempotency_key VARCHAR(200) NOT NULL UNIQUE,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    tentativas INTEGER NOT NULL DEFAULT 0,
    proxima_tentativa_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    bloqueado_ate TIMESTAMPTZ,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    publicado_em TIMESTAMPTZ,
    ultimo_erro VARCHAR(1000),
    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDENTE', 'PROCESSANDO', 'PUBLICADO', 'FALHA'))
);

CREATE INDEX idx_outbox_publicacao
    ON outbox_evento (status, proxima_tentativa_em, criado_em);
