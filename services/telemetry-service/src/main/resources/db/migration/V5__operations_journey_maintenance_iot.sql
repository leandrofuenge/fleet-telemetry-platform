CREATE TABLE IF NOT EXISTS escalas_motorista (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, motorista_id BIGINT NOT NULL,
    veiculo_id BIGINT NOT NULL, rota_id BIGINT NULL, data_inicio_turno DATETIME NOT NULL,
    data_fim_turno DATETIME NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PLANEJADA',
    confirmado_motorista BOOLEAN NOT NULL DEFAULT FALSE, motivo_cancelamento TEXT NULL, criado_por VARCHAR(36) NULL,
    INDEX idx_esc_motorista_inicio (motorista_id, data_inicio_turno), INDEX idx_esc_veiculo_inicio (veiculo_id, data_inicio_turno)
);
CREATE TABLE IF NOT EXISTS abastecimentos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, veiculo_id BIGINT NOT NULL, motorista_id BIGINT NULL,
    data_hora DATETIME NOT NULL, litros DOUBLE NOT NULL, valor_total DOUBLE NOT NULL, odometro DOUBLE NULL,
    posto_cnpj VARCHAR(14) NULL, tipo_combustivel VARCHAR(30) NULL, tipo_origem VARCHAR(20) NOT NULL,
    litros_sensor DOUBLE NULL, fraude_score INT NOT NULL DEFAULT 0, posto_autorizado BOOLEAN NULL,
    status_conciliacao VARCHAR(20) NOT NULL DEFAULT 'PENDENTE', INDEX idx_aba_tenant_data (tenant_id, data_hora), INDEX idx_aba_veiculo (veiculo_id)
);
CREATE TABLE IF NOT EXISTS ota_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, tenant_id BIGINT NOT NULL, device_id VARCHAR(64) NOT NULL,
    versao_alvo VARCHAR(30) NOT NULL, sha256 VARCHAR(64) NOT NULL, assinatura_hsm TEXT NOT NULL,
    fase VARCHAR(30) NOT NULL, status VARCHAR(30) NOT NULL, token_hash VARCHAR(64) NOT NULL,
    token_consumido BOOLEAN NOT NULL DEFAULT FALSE, erro TEXT NULL, criado_em DATETIME NOT NULL, atualizado_em DATETIME NULL,
    INDEX idx_ota_tenant_status (tenant_id, status), INDEX idx_ota_device (device_id)
);
ALTER TABLE manutencoes ADD COLUMN IF NOT EXISTS custo_pecas DOUBLE NULL;
ALTER TABLE manutencoes ADD COLUMN IF NOT EXISTS custo_mao_obra DOUBLE NULL;
ALTER TABLE manutencoes ADD COLUMN IF NOT EXISTS nota_fiscal_path VARCHAR(500) NULL;
ALTER TABLE manutencoes ADD COLUMN IF NOT EXISTS data_agendada DATE NULL;
ALTER TABLE manutencoes ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'AGENDADA';
ALTER TABLE manutencoes ADD COLUMN IF NOT EXISTS anomaly_score DOUBLE NULL;
ALTER TABLE manutencoes ADD COLUMN IF NOT EXISTS rul_dias_estimado INT NULL;
ALTER TABLE manutencoes ADD COLUMN IF NOT EXISTS probabilidade_falha DOUBLE NULL;
