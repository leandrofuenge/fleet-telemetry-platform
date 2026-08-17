-- Pipeline confiável de telemetria: idempotência, ordenação e transactional outbox.

ALTER TABLE telemetria ADD COLUMN IF NOT EXISTS event_id VARCHAR(128);
ALTER TABLE telemetria ADD COLUMN IF NOT EXISTS sequence_number BIGINT;
ALTER TABLE telemetria ADD COLUMN IF NOT EXISTS fora_de_ordem BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE telemetria ADD COLUMN IF NOT EXISTS sequence_gap BIGINT NOT NULL DEFAULT 0;
ALTER TABLE telemetria ADD COLUMN IF NOT EXISTS qualidade_dados SMALLINT NOT NULL DEFAULT 100;

UPDATE telemetria
SET event_id = 'legacy-' || id::text
WHERE event_id IS NULL;

ALTER TABLE telemetria ALTER COLUMN event_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_telemetria_tenant_event
    ON telemetria (tenant_id, event_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_telemetria_device_sequence
    ON telemetria (tenant_id, device_id, sequence_number)
    WHERE device_id IS NOT NULL AND sequence_number IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_telemetria_vehicle_sequence_without_device
    ON telemetria (tenant_id, veiculo_id, sequence_number)
    WHERE device_id IS NULL AND sequence_number IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_telemetria_sequence
    ON telemetria (veiculo_id, device_id, sequence_number DESC);

ALTER TABLE telemetria DROP CONSTRAINT IF EXISTS ck_telemetria_qualidade_dados;
ALTER TABLE telemetria ADD CONSTRAINT ck_telemetria_qualidade_dados
    CHECK (qualidade_dados BETWEEN 0 AND 100);

ALTER TABLE telemetria DROP CONSTRAINT IF EXISTS ck_telemetria_sequence_number;
ALTER TABLE telemetria ADD CONSTRAINT ck_telemetria_sequence_number
    CHECK (sequence_number IS NULL OR sequence_number >= 0);

ALTER TABLE telemetria DROP CONSTRAINT IF EXISTS ck_telemetria_sequence_gap;
ALTER TABLE telemetria ADD CONSTRAINT ck_telemetria_sequence_gap CHECK (sequence_gap >= 0);

CREATE TABLE IF NOT EXISTS telemetria_outbox (
    id                      VARCHAR(36) PRIMARY KEY,
    telemetria_id           BIGINT NOT NULL,
    tenant_id               BIGINT NOT NULL,
    veiculo_id              BIGINT NOT NULL,
    event_id                VARCHAR(128) NOT NULL,
    event_type              VARCHAR(80) NOT NULL,
    payload                 TEXT NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    tentativas              INT NOT NULL DEFAULT 0,
    criado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    publicado_em            TIMESTAMP,
    proxima_tentativa_em    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_erro             TEXT,
    CONSTRAINT uk_outbox_telemetria UNIQUE (telemetria_id),
    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDENTE', 'PUBLICADO', 'FALHOU')),
    CONSTRAINT ck_outbox_tentativas CHECK (tentativas >= 0)
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_retry
    ON telemetria_outbox (status, proxima_tentativa_em);
CREATE INDEX IF NOT EXISTS idx_outbox_veiculo
    ON telemetria_outbox (veiculo_id, criado_em);
