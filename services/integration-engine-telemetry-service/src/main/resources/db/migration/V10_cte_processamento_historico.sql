CREATE TABLE IF NOT EXISTS telemetria.cte_processamento_historico (

    id                  BIGSERIAL PRIMARY KEY,

    cte_id              BIGINT NOT NULL,

    chave               VARCHAR(44) NOT NULL,

    status_anterior     VARCHAR(30),

    status_novo         VARCHAR(30) NOT NULL,

    etapa               VARCHAR(50),

    codigo              VARCHAR(30),

    mensagem            TEXT,

    criado_em           TIMESTAMPTZ NOT NULL
                        DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cte_historico
        FOREIGN KEY (cte_id)
        REFERENCES telemetria.cte_processamento(id)
);

CREATE INDEX IF NOT EXISTS ix_cte_historico_chave
    ON telemetria.cte_processamento_historico(chave);

CREATE INDEX IF NOT EXISTS ix_cte_historico_cte_id
    ON telemetria.cte_processamento_historico(cte_id);

CREATE INDEX IF NOT EXISTS ix_cte_historico_data
    ON telemetria.cte_processamento_historico(criado_em);