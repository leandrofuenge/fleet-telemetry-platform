-- =============================================================================
-- VEHICLE SERVICE — vehicle_db
-- Responsável: Veículos, motoristas, frota, documentos, TPMS, histórico
-- Porta: 8084 / 8085 (driver_service compartilha o mesmo schema ou banco separado)
-- =============================================================================

CREATE DATABASE IF NOT EXISTS vehicle_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE vehicle_db;

-- ---------------------------------------------------------------------------
-- TABELA: veiculos
-- Cadastro completo dos veículos da frota por tenant
-- ---------------------------------------------------------------------------
CREATE TABLE veiculos (
    id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid                  CHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    tenant_id             BIGINT UNSIGNED NOT NULL,
    placa                 VARCHAR(10) NOT NULL,
    chassi                VARCHAR(50),
    renavam               VARCHAR(20),
    modelo                VARCHAR(255) NOT NULL,
    marca                 VARCHAR(100) NOT NULL,
    ano_fabricacao        SMALLINT,
    ano_modelo            SMALLINT,
    cor                   VARCHAR(50),
    tipo_veiculo          ENUM('CAMINHAO_LEVE','CAMINHAO_PESADO','CARRETA','BITREM','RODOTREM','VAN','UTILITARIO') NOT NULL DEFAULT 'CAMINHAO_PESADO',
    tipo_carroceria       VARCHAR(50) COMMENT 'BAU, GRANELEIRO, TANQUE, FRIGORÍFICO, PLATAFORMA',
    capacidade_carga_kg   DOUBLE NOT NULL COMMENT 'Capacidade máxima em kg',
    tara_kg               DOUBLE COMMENT 'Peso do veículo vazio em kg',
    eixos                 TINYINT DEFAULT 2,
    pbt_kg                DOUBLE COMMENT 'Peso Bruto Total em kg — para regra tacógrafo >4536kg',
    cmt_kg                DOUBLE COMMENT 'Capacidade Máxima de Tração em kg',
    consumo_medio         DOUBLE COMMENT 'Consumo médio em L/100km',
    capacidade_tanque_l   DOUBLE COMMENT 'Capacidade do tanque em litros',
    odometro_atual_km     DOUBLE NOT NULL DEFAULT 0,
    horas_motor           DOUBLE NOT NULL DEFAULT 0 COMMENT 'Horas totais de motor',
    status                ENUM('ATIVO','MANUTENCAO','INATIVO','VENDIDO','SINISTRO') NOT NULL DEFAULT 'ATIVO',
    -- Documentação
    rntrc                 VARCHAR(20) COMMENT 'Registro Nacional de Transportadores',
    data_venc_crlv        DATE,
    data_venc_seguro      DATE,
    data_venc_tacografo   DATE,
    data_venc_vistoria    DATE,
    -- Dispositivo IoT vinculado
    device_id             VARCHAR(64) UNIQUE COMMENT 'Serial do rastreador GPS vinculado',
    device_imei           VARCHAR(20),
    firmware_versao       VARCHAR(30),
    -- Flags
    tacografo_obrigatorio BOOLEAN NOT NULL DEFAULT FALSE,
    ativo                 BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_veiculo_placa_tenant (tenant_id, placa),
    INDEX idx_veiculo_tenant  (tenant_id),
    INDEX idx_veiculo_status  (status),
    INDEX idx_veiculo_device  (device_id),
    INDEX idx_veiculo_tipo    (tipo_veiculo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: motoristas
-- Cadastro completo dos motoristas por tenant
-- ---------------------------------------------------------------------------
CREATE TABLE motoristas (
    id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid                  CHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    tenant_id             BIGINT UNSIGNED NOT NULL,
    usuario_uuid          CHAR(36) COMMENT 'UUID do usuário no auth-service (para app mobile)',
    nome                  VARCHAR(255) NOT NULL,
    cpf                   VARCHAR(14) NOT NULL,
    cnh                   VARCHAR(50) NOT NULL,
    categoria_cnh         VARCHAR(10) NOT NULL COMMENT 'A, B, C, D, E',
    data_validade_cnh     DATE,
    data_nascimento       DATE,
    telefone              VARCHAR(20),
    email                 VARCHAR(255),
    mopp                  BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Habilitação para produtos perigosos',
    data_venc_mopp        DATE,
    aso_valido            BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Atestado de Saúde Ocupacional',
    data_venc_aso         DATE,
    -- Dados bancários (para pagamento, opcional)
    banco_codigo          VARCHAR(10),
    banco_agencia         VARCHAR(10),
    banco_conta           VARCHAR(20),
    pix_chave             VARCHAR(100),
    -- Métricas
    score_comportamento   INT NOT NULL DEFAULT 1000 COMMENT 'Escala 0-1000, quanto maior melhor',
    km_total_rodados      DOUBLE NOT NULL DEFAULT 0,
    viagens_realizadas    INT NOT NULL DEFAULT 0,
    status                ENUM('ATIVO','FERIAS','AFASTADO','INATIVO') NOT NULL DEFAULT 'ATIVO',
    ativo                 BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_motorista_cpf_tenant (tenant_id, cpf),
    INDEX idx_motorista_tenant  (tenant_id),
    INDEX idx_motorista_cnh     (cnh),
    INDEX idx_motorista_status  (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: veiculo_motorista
-- Histórico de vinculação motorista <-> veículo ao longo do tempo
-- ---------------------------------------------------------------------------
CREATE TABLE veiculo_motorista (
    id           BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id    BIGINT UNSIGNED NOT NULL,
    veiculo_id   BIGINT UNSIGNED NOT NULL,
    motorista_id BIGINT UNSIGNED NOT NULL,
    data_inicio  DATETIME NOT NULL,
    data_fim     DATETIME COMMENT 'NULL = vínculo ativo atual',
    ativo        BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_vm_veiculo   (veiculo_id),
    INDEX idx_vm_motorista (motorista_id),
    INDEX idx_vm_ativo     (ativo),
    CONSTRAINT fk_vm_veiculo   FOREIGN KEY (veiculo_id)   REFERENCES veiculos(id)   ON DELETE CASCADE,
    CONSTRAINT fk_vm_motorista FOREIGN KEY (motorista_id) REFERENCES motoristas(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: documentos_veiculo
-- Documentos com controle de vencimento e alertas
-- ---------------------------------------------------------------------------
CREATE TABLE documentos_veiculo (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT UNSIGNED NOT NULL,
    veiculo_id      BIGINT UNSIGNED NOT NULL,
    tipo            ENUM('CRLV','SEGURO_DPVAT','SEGURO_RCF','TACOGRAFO','VISTORIA','RNTRC','OUTROS') NOT NULL,
    numero          VARCHAR(50),
    data_emissao    DATE,
    data_vencimento DATE NOT NULL,
    arquivo_path    VARCHAR(500) COMMENT 'Caminho no MinIO',
    observacoes     TEXT,
    notificado_30d  BOOLEAN NOT NULL DEFAULT FALSE,
    notificado_7d   BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_docv_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculos(id) ON DELETE CASCADE,
    INDEX idx_docv_veiculo    (veiculo_id),
    INDEX idx_docv_vencimento (data_vencimento),
    INDEX idx_docv_tipo       (tipo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: documentos_motorista
-- CNH, MOPP, ASO e outros documentos do motorista
-- ---------------------------------------------------------------------------
CREATE TABLE documentos_motorista (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT UNSIGNED NOT NULL,
    motorista_id    BIGINT UNSIGNED NOT NULL,
    tipo            ENUM('CNH','MOPP','ASO','TREINAMENTO','OUTROS') NOT NULL,
    numero          VARCHAR(50),
    data_emissao    DATE,
    data_vencimento DATE NOT NULL,
    arquivo_path    VARCHAR(500),
    observacoes     TEXT,
    notificado_30d  BOOLEAN NOT NULL DEFAULT FALSE,
    notificado_7d   BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_docm_motorista FOREIGN KEY (motorista_id) REFERENCES motoristas(id) ON DELETE CASCADE,
    INDEX idx_docm_motorista  (motorista_id),
    INDEX idx_docm_vencimento (data_vencimento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: pneus
-- Gestão individual de pneus por veículo (integração TPMS)
-- ---------------------------------------------------------------------------
CREATE TABLE pneus (
    id                   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id            BIGINT UNSIGNED NOT NULL,
    veiculo_id           BIGINT UNSIGNED NOT NULL,
    posicao              VARCHAR(10) NOT NULL COMMENT 'ex: D1, D2, T1E, T1D — eixo+posição',
    marca                VARCHAR(50),
    modelo               VARCHAR(100),
    dimensao             VARCHAR(30) COMMENT 'ex: 295/80R22.5',
    dot                  VARCHAR(20) COMMENT 'Data de fabricação do pneu',
    km_instalado         DOUBLE COMMENT 'Odômetro na instalação',
    km_vida_util_est     DOUBLE NOT NULL DEFAULT 80000 COMMENT 'Vida útil estimada em km',
    pressao_recomendada  DOUBLE NOT NULL COMMENT 'PSI recomendado',
    pressao_atual        DOUBLE COMMENT 'Última leitura do TPMS',
    temperatura_atual    DOUBLE COMMENT 'Última leitura do TPMS em °C',
    sensor_id_tpms       VARCHAR(30) COMMENT 'ID do sensor BLE/433MHz no pneu',
    status               ENUM('ATIVO','RECAPADO','RESERVA','DESCARTADO') NOT NULL DEFAULT 'ATIVO',
    instalado_em         DATETIME,
    criado_em            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pneu_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculos(id) ON DELETE CASCADE,
    INDEX idx_pneu_veiculo (veiculo_id),
    INDEX idx_pneu_sensor  (sensor_id_tpms)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: historico_score_motorista
-- Série temporal do score de comportamento (para gráficos e UBI)
-- ---------------------------------------------------------------------------
CREATE TABLE historico_score_motorista (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT UNSIGNED NOT NULL,
    motorista_id    BIGINT UNSIGNED NOT NULL,
    periodo         DATE NOT NULL COMMENT 'Primeiro dia do mês de referência',
    score           INT NOT NULL,
    frenagens       INT NOT NULL DEFAULT 0,
    aceleracoes     INT NOT NULL DEFAULT 0,
    excessos_vel    INT NOT NULL DEFAULT 0,
    uso_celular     INT NOT NULL DEFAULT 0 COMMENT 'Detecções DMS',
    fadiga          INT NOT NULL DEFAULT 0 COMMENT 'Detecções DMS',
    km_periodo      DOUBLE NOT NULL DEFAULT 0,
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_score_motorista_periodo (tenant_id, motorista_id, periodo),
    CONSTRAINT fk_hsc_motorista FOREIGN KEY (motorista_id) REFERENCES motoristas(id) ON DELETE CASCADE,
    INDEX idx_hsc_motorista (motorista_id),
    INDEX idx_hsc_periodo   (periodo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: jornadas
-- Controle de jornada Lei 12.619 — integrando com tacógrafo
-- ---------------------------------------------------------------------------
CREATE TABLE jornadas (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT UNSIGNED NOT NULL,
    motorista_id        BIGINT UNSIGNED NOT NULL,
    veiculo_id          BIGINT UNSIGNED,
    data_inicio         DATETIME NOT NULL,
    data_fim            DATETIME,
    horas_direcao       DOUBLE NOT NULL DEFAULT 0 COMMENT 'Horas efetivas de direção',
    horas_disponivel    DOUBLE NOT NULL DEFAULT 0,
    horas_repouso       DOUBLE NOT NULL DEFAULT 0,
    horas_extras        DOUBLE NOT NULL DEFAULT 0,
    pausas_realizadas   INT NOT NULL DEFAULT 0 COMMENT 'Pausas de 30min a cada 4h',
    status              ENUM('ABERTA','FECHADA','IRREGULAR') NOT NULL DEFAULT 'ABERTA',
    origem_dado         ENUM('TACOGRAFO','APP','MANUAL') NOT NULL DEFAULT 'APP',
    alerta_enviado      BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Alerta de limite próximo enviado',
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_jornada_motorista (motorista_id),
    INDEX idx_jornada_data      (data_inicio),
    INDEX idx_jornada_status    (status),
    CONSTRAINT fk_jornada_motorista FOREIGN KEY (motorista_id) REFERENCES motoristas(id) ON DELETE CASCADE,
    CONSTRAINT fk_jornada_veiculo   FOREIGN KEY (veiculo_id)   REFERENCES veiculos(id)   ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: geofences
-- Zonas geográficas configuráveis por tenant (círculo ou polígono)
-- ---------------------------------------------------------------------------
CREATE TABLE geofences (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid             CHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    tenant_id        BIGINT UNSIGNED NOT NULL,
    nome             VARCHAR(255) NOT NULL,
    tipo             ENUM('CIRCULO','POLIGONO') NOT NULL DEFAULT 'CIRCULO',
    latitude_centro  DOUBLE COMMENT 'Para círculo',
    longitude_centro DOUBLE COMMENT 'Para círculo',
    raio_metros      DOUBLE COMMENT 'Para círculo',
    vertices         JSON COMMENT 'Array [{lat,lng}] para polígono',
    tipo_alerta      ENUM('ENTRADA','SAIDA','AMBOS') NOT NULL DEFAULT 'AMBOS',
    ativo            BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_gf_tenant (tenant_id),
    INDEX idx_gf_ativo  (ativo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: veiculo_geofence
-- N:N — quais geofences monitoram quais veículos
-- ---------------------------------------------------------------------------
CREATE TABLE veiculo_geofence (
    veiculo_id   BIGINT UNSIGNED NOT NULL,
    geofence_id  BIGINT UNSIGNED NOT NULL,
    ativo        BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (veiculo_id, geofence_id),
    CONSTRAINT fk_vgf_veiculo   FOREIGN KEY (veiculo_id)  REFERENCES veiculos(id)  ON DELETE CASCADE,
    CONSTRAINT fk_vgf_geofence  FOREIGN KEY (geofence_id) REFERENCES geofences(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: clientes_frete
-- Embarcadores / clientes das transportadoras
-- ---------------------------------------------------------------------------
CREATE TABLE clientes_frete (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id      BIGINT UNSIGNED NOT NULL,
    nome           VARCHAR(255) NOT NULL,
    cnpj           VARCHAR(20),
    telefone       VARCHAR(20),
    email          VARCHAR(255),
    endereco       VARCHAR(500),
    contato_nome   VARCHAR(100),
    ativo          BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cf_tenant (tenant_id),
    INDEX idx_cf_cnpj   (cnpj)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: cargas
-- Cargas associadas às viagens
-- ---------------------------------------------------------------------------
CREATE TABLE cargas (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid              CHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    tenant_id         BIGINT UNSIGNED NOT NULL,
    cliente_id        BIGINT UNSIGNED,
    descricao         VARCHAR(255) NOT NULL,
    tipo_carga        ENUM('GERAL','REFRIGERADA','CONGELADA','PERIGOSA','VIVA','GRANEL','LIQUIDO','VALORES','FRAGIL') NOT NULL DEFAULT 'GERAL',
    peso_kg           DOUBLE NOT NULL,
    volume_m3         DOUBLE,
    valor_declarado   DECIMAL(15,2),
    temp_min_c        DOUBLE COMMENT 'Para carga refrigerada/congelada',
    temp_max_c        DOUBLE COMMENT 'Para carga refrigerada/congelada',
    nfe_chave         VARCHAR(50) COMMENT 'Chave de acesso da NF-e',
    cte_chave         VARCHAR(50) COMMENT 'Chave de acesso do CT-e',
    mdfe_chave        VARCHAR(50) COMMENT 'Chave do MDF-e',
    status            ENUM('PENDENTE','EM_TRANSITO','ENTREGUE','CANCELADA','AVARIADA') NOT NULL DEFAULT 'PENDENTE',
    criado_em         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_carga_tenant   (tenant_id),
    INDEX idx_carga_cliente  (cliente_id),
    INDEX idx_carga_status   (status),
    CONSTRAINT fk_carga_cliente FOREIGN KEY (cliente_id) REFERENCES clientes_frete(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
