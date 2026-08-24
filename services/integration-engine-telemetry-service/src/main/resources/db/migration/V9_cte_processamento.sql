CREATE SCHEMA IF NOT EXISTS telemetria;

CREATE TABLE IF NOT EXISTS telemetria.cte_processamento (

    id                  BIGSERIAL PRIMARY KEY,

    chave               VARCHAR(44) NOT NULL,

    numero              VARCHAR(20) NOT NULL,

    serie               VARCHAR(10) NOT NULL,

    modelo              VARCHAR(10),

    versao              VARCHAR(20),

    status              VARCHAR(40) NOT NULL,

    xml_hash            CHAR(64) NOT NULL,

    xml_tamanho_bytes   INTEGER NOT NULL,

    xml_normalizado     TEXT,

    tentativa           INTEGER NOT NULL DEFAULT 1,

    codigo_sefaz        VARCHAR(20),

    motivo_sefaz        TEXT,

    protocolo           VARCHAR(100),

    erro_codigo         VARCHAR(100),

    erro_mensagem       TEXT,

    criado_em           TIMESTAMPTZ NOT NULL
                        DEFAULT CURRENT_TIMESTAMP,

    atualizado_em       TIMESTAMPTZ NOT NULL
                        DEFAULT CURRENT_TIMESTAMP,

    processado_em       TIMESTAMPTZ,

    autorizado_em       TIMESTAMPTZ,

    CONSTRAINT uk_cte_chave
        UNIQUE (chave)
);

CREATE INDEX IF NOT EXISTS idx_cte_status
    ON telemetria.cte_processamento(status);

CREATE INDEX IF NOT EXISTS idx_cte_criado_em
    ON telemetria.cte_processamento(criado_em);

CREATE INDEX IF NOT EXISTS idx_cte_atualizado_em
    ON telemetria.cte_processamento(atualizado_em);

CREATE INDEX IF NOT EXISTS idx_cte_protocolo
    ON telemetria.cte_processamento(protocolo);

CREATE TABLE IF NOT EXISTS telemetria.cte_processamento_historico (

    id                  BIGSERIAL PRIMARY KEY,

    cte_id              BIGINT,

    chave               VARCHAR(44) NOT NULL,

    status_anterior     VARCHAR(40),

    status_novo         VARCHAR(40) NOT NULL,

    etapa               VARCHAR(50) NOT NULL,

    codigo              VARCHAR(30),

    mensagem            TEXT,

    criado_em           TIMESTAMPTZ NOT NULL
                        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cte_historico
        FOREIGN KEY (cte_id)
        REFERENCES telemetria.cte_processamento(id)
);

CREATE INDEX IF NOT EXISTS idx_cte_hist_chave
    ON telemetria.cte_processamento_historico(chave);

CREATE INDEX IF NOT EXISTS idx_cte_hist_cte_id
    ON telemetria.cte_processamento_historico(cte_id);

CREATE INDEX IF NOT EXISTS idx_cte_hist_criado
    ON telemetria.cte_processamento_historico(criado_em);