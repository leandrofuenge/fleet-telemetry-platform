-- =============================================================================
-- V1__create_all_tables.sql
-- Script completo com todas as tabelas necessárias
-- =============================================================================

USE telemetria;

-- Tabela alertas
CREATE TABLE IF NOT EXISTS alertas (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid                    VARCHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    tenant_id               BIGINT UNSIGNED NOT NULL,
    veiculo_id              BIGINT UNSIGNED,
    veiculo_uuid            VARCHAR(36),
    motorista_id            BIGINT UNSIGNED,
    viagem_id               BIGINT UNSIGNED,
    telemetria_id           BIGINT UNSIGNED,
    regra_id                BIGINT UNSIGNED,
    tipo                    VARCHAR(80) NOT NULL,
    severidade              ENUM('BAIXO','MEDIO','ALTO','CRITICO') NOT NULL DEFAULT 'MEDIO',
    categoria               VARCHAR(50),
    mensagem                TEXT NOT NULL,
    latitude                DOUBLE,
    longitude               DOUBLE,
    velocidade_kmh          DOUBLE,
    odometro_km             DOUBLE,
    nome_local              VARCHAR(255),
    dados_contexto          JSON,
    data_hora               DATETIME NOT NULL,
    lido                    BOOLEAN NOT NULL DEFAULT FALSE,
    lido_por                VARCHAR(255),
    data_hora_leitura       DATETIME,
    resolvido               BOOLEAN NOT NULL DEFAULT FALSE,
    resolvido_por           VARCHAR(255),
    data_hora_resolucao     DATETIME,
    observacao_resolucao    TEXT,
    notificacao_enviada     BOOLEAN NOT NULL DEFAULT FALSE,
    canais_notificados      JSON,
    criado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_alerta_tenant     (tenant_id),
    INDEX idx_alerta_veiculo    (veiculo_id),
    INDEX idx_alerta_motorista  (motorista_id),
    INDEX idx_alerta_viagem     (viagem_id),
    INDEX idx_alerta_tipo       (tipo),
    INDEX idx_alerta_severidade (severidade),
    INDEX idx_alerta_data       (data_hora),
    INDEX idx_alerta_lido       (lido),
    INDEX idx_alerta_resolvido  (resolvido)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela dispositivos_iot (com a coluna tipo)
CREATE TABLE IF NOT EXISTS dispositivos_iot (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id           VARCHAR(64) NOT NULL UNIQUE,
    imei                VARCHAR(20),
    iccid               VARCHAR(25),
    tenant_id           BIGINT UNSIGNED,
    veiculo_id          BIGINT UNSIGNED,
    tipo                ENUM('PRINCIPAL','BACKUP') DEFAULT 'PRINCIPAL',
    fabricante          VARCHAR(100),
    modelo_hw           VARCHAR(100),
    versao_firmware     VARCHAR(30),
    versao_alvo         VARCHAR(30),
    certificado_cn      VARCHAR(100),
    certificado_expira  DATE,
    status_cert         ENUM('ATIVO','EXPIRANDO','EXPIRADO','REVOGADO') NOT NULL DEFAULT 'ATIVO',
    status              ENUM('PENDENTE','ATIVO','INATIVO','MANUTENCAO','REVOGADO') NOT NULL DEFAULT 'PENDENTE',
    ultima_conexao      DATETIME,
    ultimo_heartbeat    DATETIME,
    ip_ultima_conexao   VARCHAR(45),
    tecnologia_rede     VARCHAR(10),
    rssi                DOUBLE,
    freq_envio_s        INT NOT NULL DEFAULT 5,
    buffer_horas        INT NOT NULL DEFAULT 72,
    tem_satelite        BOOLEAN NOT NULL DEFAULT FALSE,
    iridium_imei        VARCHAR(20),
    satelite_ativo      BOOLEAN NOT NULL DEFAULT FALSE,
    observacoes         TEXT,
    instalado_em        DATE,
    instalado_por       VARCHAR(100),
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_disp_tenant  (tenant_id),
    INDEX idx_disp_veiculo (veiculo_id),
    INDEX idx_disp_status  (status),
    INDEX idx_disp_imei    (imei)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela historico_senhas
CREATE TABLE IF NOT EXISTS historico_senhas (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    usuario_id  BIGINT UNSIGNED NOT NULL,
    senha_hash  VARCHAR(255) NOT NULL,
    criado_em   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_hs_usuario (usuario_id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela sessoes_ativas
CREATE TABLE IF NOT EXISTS sessoes_ativas (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    usuario_id      BIGINT UNSIGNED NOT NULL,
    token_jwt       VARCHAR(500) NOT NULL,
    ip              VARCHAR(45),
    user_agent      TEXT,
    data_criacao    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acesso   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    data_expiracao  TIMESTAMP NOT NULL,
    INDEX idx_sa_usuario (usuario_id),
    INDEX idx_sa_token (token_jwt(255)),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela historico_score_motorista
CREATE TABLE IF NOT EXISTS historico_score_motorista (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    motorista_id        BIGINT UNSIGNED NOT NULL,
    data                DATE NOT NULL,
    score_anterior      INT NOT NULL,
    score_novo          INT NOT NULL,
    diferenca           INT NOT NULL,
    motivo              VARCHAR(100),
    viagem_id           BIGINT UNSIGNED,
    evento_tipo         VARCHAR(50),
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_hsm_motorista (motorista_id),
    INDEX idx_hsm_data (data),
    FOREIGN KEY (motorista_id) REFERENCES motoristas(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela usuarios (com todos os campos)
CREATE TABLE IF NOT EXISTS usuarios (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    login                   VARCHAR(100) NOT NULL UNIQUE,
    senha                   VARCHAR(255) NOT NULL,
    nome                    VARCHAR(255) NOT NULL,
    email                   VARCHAR(200) UNIQUE,
    cpf                     VARCHAR(14) NOT NULL UNIQUE,
    ativo                   BOOLEAN NOT NULL DEFAULT TRUE,
    perfil                  VARCHAR(20) NOT NULL,
    ultimo_acesso           DATETIME,
    motorista_id            BIGINT UNSIGNED UNIQUE,
    data_expiracao_senha    DATE,
    tentativas_falha        INT DEFAULT 0,
    ultima_tentativa_falha  DATETIME,
    bloqueado_ate           DATETIME,
    mfa_secret              VARCHAR(32),
    mfa_ativado             BOOLEAN DEFAULT FALSE,
    criado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_usr_login (login),
    INDEX idx_usr_cpf (cpf),
    INDEX idx_usr_email (email),
    FOREIGN KEY (motorista_id) REFERENCES motoristas(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela motoristas (com todos os campos)
CREATE TABLE IF NOT EXISTS motoristas (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT UNSIGNED NOT NULL,
    nome                VARCHAR(255) NOT NULL,
    cpf                 VARCHAR(14) NOT NULL,
    cnh                 VARCHAR(20) NOT NULL,
    categoria_cnh       VARCHAR(5) NOT NULL,
    data_venc_cnh       DATE,
    data_venc_aso       DATE,
    mopp_valido         BOOLEAN DEFAULT FALSE,
    score               INT NOT NULL DEFAULT 1000,
    email               VARCHAR(200) UNIQUE,
    telefone            VARCHAR(20),
    ativo               BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_motorista_cpf_tenant (cpf, tenant_id),
    UNIQUE KEY uk_motorista_cnh (cnh),
    INDEX idx_mot_cpf   (cpf),
    INDEX idx_mot_cnh   (cnh),
    INDEX idx_mot_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabela veiculos (com todos os campos)
CREATE TABLE IF NOT EXISTS veiculos (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id               BIGINT UNSIGNED NOT NULL,
    placa                   VARCHAR(10) NOT NULL,
    modelo                  VARCHAR(255),
    marca                   VARCHAR(100),
    capacidade_carga        DOUBLE,
    ano_fabricacao          INT,
    ativo                   BOOLEAN NOT NULL DEFAULT TRUE,
    cliente_id              BIGINT UNSIGNED,
    motorista_atual_id      BIGINT UNSIGNED,
    pbt_kg                  DOUBLE,
    tacografo_obrigatorio   BOOLEAN DEFAULT FALSE,
    data_venc_tacografo     DATE,
    data_venc_crlv          DATE,
    data_venc_seguro        DATE,
    data_venc_dpvat         DATE,
    data_venc_rcf           DATE,
    data_venc_vistoria      DATE,
    data_venc_rntrc         DATE,
    criado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_veiculo_placa_tenant (placa, tenant_id),
    INDEX idx_vei_placa     (placa),
    INDEX idx_vei_tenant    (tenant_id),
    FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE SET NULL,
    FOREIGN KEY (motorista_atual_id) REFERENCES motoristas(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;