CREATE TABLE IF NOT EXISTS tenants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome_razao_social VARCHAR(255) NOT NULL,
    cnpj VARCHAR(14) NOT NULL,
    plano VARCHAR(20) NOT NULL DEFAULT 'STARTER',
    status VARCHAR(20) NOT NULL DEFAULT 'TRIAL',
    trial_inicio DATE NOT NULL,
    trial_expira_em DATE NOT NULL,
    dados_preservados_ate DATE NULL,
    email VARCHAR(200) NULL,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenants_cnpj UNIQUE (cnpj),
    INDEX idx_tenant_status (status)
);

ALTER TABLE usuarios ADD COLUMN tenant_id BIGINT NULL;
CREATE INDEX idx_usr_tenant ON usuarios (tenant_id);
