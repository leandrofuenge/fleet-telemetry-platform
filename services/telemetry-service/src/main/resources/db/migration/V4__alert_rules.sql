CREATE TABLE IF NOT EXISTS regras_alerta (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT UNSIGNED NOT NULL,
    nome VARCHAR(120) NOT NULL,
    tipo VARCHAR(80) NOT NULL,
    severidade VARCHAR(20) NOT NULL,
    campo VARCHAR(80) NOT NULL,
    operador VARCHAR(10) NOT NULL,
    valor_limite DOUBLE NOT NULL,
    cooldown_minutos INT NOT NULL DEFAULT 5,
    canais JSON NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_regra_alerta_tenant_ativa (tenant_id, ativo)
);
