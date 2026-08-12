ALTER TABLE outbox_evento
    ADD COLUMN lock_token UUID;
