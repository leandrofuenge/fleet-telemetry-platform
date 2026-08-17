-- =============================================================================
-- ALERT SERVICE — alert_db
-- Responsável: Motor de regras, alertas em tempo real, notificações
-- Porta: 8086
-- =============================================================================

CREATE DATABASE IF NOT EXISTS alert_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE alert_db;

-- ---------------------------------------------------------------------------
-- TABELA: regras_alerta
-- Motor de regras configurável por tenant (no-code para gestores)
-- ---------------------------------------------------------------------------
CREATE TABLE regras_alerta (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid             CHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    tenant_id        BIGINT UNSIGNED NOT NULL,
    nome             VARCHAR(255) NOT NULL,
    descricao        TEXT,
    -- Trigger
    tipo_evento      VARCHAR(50) NOT NULL COMMENT 'sensor_reading, geofence_event, route_event, device_event',
    condicoes        JSON NOT NULL COMMENT '[{field, operator, value}] — motor de regras',
    logica           ENUM('AND','OR') NOT NULL DEFAULT 'AND',
    duracao_seg      INT NOT NULL DEFAULT 0 COMMENT 'Condição deve persistir X segundos antes de disparar',
    -- Ação
    severidade       ENUM('BAIXO','MEDIO','ALTO','CRITICO') NOT NULL DEFAULT 'MEDIO',
    acoes            JSON NOT NULL COMMENT '[{type: push/sms/email/webhook, target/url}]',
    cooldown_min     INT NOT NULL DEFAULT 15 COMMENT 'Intervalo mínimo entre disparos iguais',
    -- Escopo
    aplica_todos     BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Aplica a todos os veículos do tenant',
    veiculos_uuid    JSON COMMENT 'Lista de UUIDs de veículos específicos',
    tipo_carga       JSON COMMENT 'Lista de tipos de carga que ativam a regra',
    -- Controle
    ativo            BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_regra_tenant (tenant_id),
    INDEX idx_regra_ativo  (ativo),
    INDEX idx_regra_tipo   (tipo_evento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: alertas
-- Alertas gerados — cada disparo do motor de regras cria um registro
-- ---------------------------------------------------------------------------
CREATE TABLE alertas (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid             CHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    tenant_id        BIGINT UNSIGNED NOT NULL,
    regra_id         BIGINT UNSIGNED,
    veiculo_uuid     CHAR(36) NOT NULL,
    motorista_uuid   CHAR(36),
    viagem_uuid      CHAR(36),

    tipo             VARCHAR(80) NOT NULL COMMENT 'EXCESSO_VELOCIDADE, DESVIO_ROTA, GEOFENCE, TEMPERATURA_CARGA...',
    severidade       ENUM('BAIXO','MEDIO','ALTO','CRITICO') NOT NULL DEFAULT 'MEDIO',
    mensagem         TEXT NOT NULL,
    dados_contexto   JSON COMMENT 'Snapshot dos dados que dispararam o alerta',

    -- Localização no momento do alerta
    latitude         DOUBLE,
    longitude        DOUBLE,
    velocidade_kmh   DOUBLE,
    odometro_km      DOUBLE,

    data_hora        DATETIME NOT NULL,

    -- Gestão
    lido             BOOLEAN NOT NULL DEFAULT FALSE,
    lido_por         VARCHAR(255),
    data_hora_leitura DATETIME,
    resolvido        BOOLEAN NOT NULL DEFAULT FALSE,
    resolvido_por    VARCHAR(255),
    data_hora_resolucao DATETIME,
    observacao_resolucao TEXT,

    criado_em        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_alerta_tenant   (tenant_id),
    INDEX idx_alerta_veiculo  (veiculo_uuid),
    INDEX idx_alerta_tipo     (tipo),
    INDEX idx_alerta_sev      (severidade),
    INDEX idx_alerta_data     (data_hora),
    INDEX idx_alerta_lido     (lido),
    INDEX idx_alerta_resolv   (resolvido),
    CONSTRAINT fk_alerta_regra FOREIGN KEY (regra_id) REFERENCES regras_alerta(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: notificacoes
-- Fila de notificações enviadas para cada canal
-- ---------------------------------------------------------------------------
CREATE TABLE notificacoes (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id        BIGINT UNSIGNED NOT NULL,
    alerta_id        BIGINT UNSIGNED,
    canal            ENUM('EMAIL','SMS','WHATSAPP','PUSH_FCM','WEBHOOK','INTERNO') NOT NULL,
    destinatario     VARCHAR(255) NOT NULL COMMENT 'email, telefone, FCM token ou URL',
    assunto          VARCHAR(255),
    corpo            TEXT NOT NULL,
    status           ENUM('PENDENTE','ENVIADO','ENTREGUE','FALHOU') NOT NULL DEFAULT 'PENDENTE',
    tentativas       TINYINT UNSIGNED NOT NULL DEFAULT 0,
    proximo_retry    DATETIME,
    id_externo       VARCHAR(100) COMMENT 'ID retornado pela API do canal (ex: SID do Twilio)',
    erro_detalhe     TEXT,
    enviado_em       DATETIME,
    criado_em        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notif_tenant    (tenant_id),
    INDEX idx_notif_alerta    (alerta_id),
    INDEX idx_notif_status    (status),
    INDEX idx_notif_retry     (proximo_retry, status),
    CONSTRAINT fk_notif_alerta FOREIGN KEY (alerta_id) REFERENCES alertas(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: webhook_deliveries
-- Histórico de entregas de webhooks para integrações externas
-- ---------------------------------------------------------------------------
CREATE TABLE webhook_deliveries (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT UNSIGNED NOT NULL,
    alerta_uuid     CHAR(36),
    url_destino     VARCHAR(500) NOT NULL,
    payload         JSON NOT NULL,
    http_status     SMALLINT,
    resposta        TEXT,
    tentativa       TINYINT UNSIGNED NOT NULL DEFAULT 1,
    sucesso         BOOLEAN NOT NULL DEFAULT FALSE,
    enviado_em      DATETIME NOT NULL,
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_whd_tenant  (tenant_id),
    INDEX idx_whd_sucesso (sucesso),
    INDEX idx_whd_enviado (enviado_em)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =============================================================================
-- MAINTENANCE SERVICE — maintenance_db
-- Responsável: Manutenções preventivas/corretivas, manutenção preditiva IA, RUL
-- Porta: integrado ao vehicle-service ou separado
-- =============================================================================

CREATE DATABASE IF NOT EXISTS maintenance_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE maintenance_db;

-- ---------------------------------------------------------------------------
-- TABELA: manutencoes
-- Registro de manutenções realizadas e agendadas
-- ---------------------------------------------------------------------------
CREATE TABLE manutencoes (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid             CHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    tenant_id        BIGINT UNSIGNED NOT NULL,
    veiculo_uuid     CHAR(36) NOT NULL,
    tipo             ENUM('PREVENTIVA','CORRETIVA','PREDITIVA','REVISAO','SINISTRO') NOT NULL DEFAULT 'PREVENTIVA',
    categoria        VARCHAR(100) COMMENT 'FREIOS, MOTOR, PNEUS, ELETRICA, SUSPENSAO, FILTROS, LUBRIFICACAO',
    descricao        VARCHAR(255) NOT NULL,
    observacoes      TEXT,
    status           ENUM('AGENDADA','EM_ANDAMENTO','CONCLUIDA','CANCELADA') NOT NULL DEFAULT 'AGENDADA',
    data_agendada    DATE,
    data_realizada   DATE,
    km_agendado      DOUBLE COMMENT 'Odômetro alvo para manutenção por km',
    km_realizado     DOUBLE,
    horas_agendado   DOUBLE COMMENT 'Horas de motor alvo',
    custo_pecas      DECIMAL(10,2) NOT NULL DEFAULT 0,
    custo_mao_obra   DECIMAL(10,2) NOT NULL DEFAULT 0,
    custo_total      DECIMAL(10,2) NOT NULL DEFAULT 0,
    fornecedor       VARCHAR(255),
    nota_fiscal      VARCHAR(50),
    alerta_enviado   BOOLEAN NOT NULL DEFAULT FALSE,
    -- Origem preditiva
    originada_ml     BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Gerada pelo modelo de IA',
    modelo_ml_versao VARCHAR(30) COMMENT 'Versão do modelo que gerou a predição',
    confianca_ml     DOUBLE COMMENT 'Score de confiança da predição 0-1',
    criado_em        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_man_tenant   (tenant_id),
    INDEX idx_man_veiculo  (veiculo_uuid),
    INDEX idx_man_status   (status),
    INDEX idx_man_data     (data_agendada),
    INDEX idx_man_tipo     (tipo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: componentes
-- Componentes monitorados individualmente por veículo (para RUL)
-- ---------------------------------------------------------------------------
CREATE TABLE componentes (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id         BIGINT UNSIGNED NOT NULL,
    veiculo_uuid      CHAR(36) NOT NULL,
    nome              VARCHAR(100) NOT NULL COMMENT 'ROLAMENTO_D1, FREIO_DIANTEIRO, FILTRO_OLEO...',
    fabricante        VARCHAR(100),
    modelo_peca       VARCHAR(100),
    numero_serie      VARCHAR(50),
    km_instalado      DOUBLE,
    horas_instalado   DOUBLE,
    vida_util_km_est  DOUBLE NOT NULL DEFAULT 100000,
    -- RUL calculado pelo ML
    rul_km_estimado   DOUBLE COMMENT 'Remaining Useful Life em km',
    rul_dias_estimado INT    COMMENT 'Remaining Useful Life em dias',
    anomaly_score     DOUBLE NOT NULL DEFAULT 0 COMMENT '0-100: quanto maior, mais suspeito',
    status            ENUM('BOM','ATENCAO','CRITICO','SUBSTITUIDO') NOT NULL DEFAULT 'BOM',
    ultimo_modelo_ml  VARCHAR(30),
    ultima_pred_em    DATETIME,
    instalado_em      DATE,
    substituido_em    DATE,
    criado_em         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_comp_veiculo (veiculo_uuid),
    INDEX idx_comp_status  (status),
    INDEX idx_comp_rul     (rul_dias_estimado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: predicoes_ml
-- Histórico de predições de manutenção preditiva geradas pelos modelos IA
-- ---------------------------------------------------------------------------
CREATE TABLE predicoes_ml (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT UNSIGNED NOT NULL,
    veiculo_uuid    CHAR(36) NOT NULL,
    componente_id   BIGINT UNSIGNED,
    modelo          VARCHAR(50) NOT NULL COMMENT 'LSTM_RUL, ISOLATION_FOREST, RANDOM_FOREST',
    versao_modelo   VARCHAR(30) NOT NULL,
    tipo_predicao   ENUM('ANOMALIA','RUL','FALHA_IMINENTE','FRAUDE_ODOMETRO','CONSUMO_ANORMAL') NOT NULL,
    score           DOUBLE NOT NULL COMMENT 'Score bruto do modelo',
    probabilidade   DOUBLE NOT NULL DEFAULT 0 COMMENT '0.0 a 1.0',
    rul_km          DOUBLE COMMENT 'Vida útil restante estimada em km',
    rul_dias        INT    COMMENT 'Vida útil restante em dias',
    features_input  JSON   COMMENT 'Features usadas na predição (para explicabilidade)',
    acao_recomendada TEXT,
    acionada        BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Se gerou ordem de manutenção',
    manutencao_id   BIGINT UNSIGNED,
    predito_em      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_pred_veiculo   (veiculo_uuid),
    INDEX idx_pred_tipo      (tipo_predicao),
    INDEX idx_pred_em        (predito_em),
    CONSTRAINT fk_pred_comp FOREIGN KEY (componente_id) REFERENCES componentes(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: abastecimentos
-- Registro de abastecimentos com detecção de fraude
-- ---------------------------------------------------------------------------
CREATE TABLE abastecimentos (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT UNSIGNED NOT NULL,
    veiculo_uuid        CHAR(36) NOT NULL,
    motorista_uuid      CHAR(36),
    viagem_uuid         CHAR(36),
    data_hora           DATETIME NOT NULL,
    posto_nome          VARCHAR(255),
    posto_cnpj          VARCHAR(20),
    latitude            DOUBLE,
    longitude           DOUBLE,
    litros_declarados   DOUBLE NOT NULL,
    litros_sensor       DOUBLE COMMENT 'Calculado pelo delta do sensor de nível',
    tipo_combustivel    ENUM('DIESEL','DIESEL_S10','GNV','GASOLINA','ETANOL') NOT NULL DEFAULT 'DIESEL',
    valor_litro         DECIMAL(6,3),
    valor_total         DECIMAL(10,2),
    odometro_km         DOUBLE,
    km_desde_ultimo     DOUBLE,
    consumo_kmpl        DOUBLE COMMENT 'Consumo calculado desde último abastecimento',
    cupom_foto_path     VARCHAR(500),
    -- Fraude
    fraude_score        DOUBLE NOT NULL DEFAULT 0 COMMENT '0-100',
    fraude_tipo         VARCHAR(100) COMMENT 'VOLUME_DIVERGENTE, POSTO_NAO_AUTORIZADO, CONSUMO_EXCESSIVO',
    alerta_fraude       BOOLEAN NOT NULL DEFAULT FALSE,
    posto_autorizado    BOOLEAN NOT NULL DEFAULT TRUE,
    revisado            BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_abas_tenant  (tenant_id),
    INDEX idx_abas_veiculo (veiculo_uuid),
    INDEX idx_abas_data    (data_hora),
    INDEX idx_abas_fraude  (alerta_fraude)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =============================================================================
-- BILLING SERVICE — billing_db
-- Responsável: Planos, cobranças, uso, métricas para faturamento
-- Porta: 8089
-- =============================================================================

CREATE DATABASE IF NOT EXISTS billing_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE billing_db;

-- ---------------------------------------------------------------------------
-- TABELA: planos
-- Definição dos planos SaaS
-- ---------------------------------------------------------------------------
CREATE TABLE planos (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nome                ENUM('STARTER','PRO','ENTERPRISE','CUSTOM') NOT NULL UNIQUE,
    preco_veiculo_mes   DECIMAL(8,2) NOT NULL,
    min_veiculos        INT NOT NULL DEFAULT 1,
    max_veiculos        INT COMMENT 'NULL = ilimitado',
    features            JSON NOT NULL COMMENT 'Mapa de features habilitadas por plano',
    sla_uptime          DECIMAL(5,2) NOT NULL COMMENT 'ex: 99.90',
    ativo               BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO planos (nome, preco_veiculo_mes, min_veiculos, max_veiculos, features, sla_uptime) VALUES
    ('STARTER',    49.00,  1,  9,   '{"app_motorista":false,"osrm":false,"ia_preditiva":false,"satelite":false,"api":false}', 99.00),
    ('PRO',        99.00,  10, 49,  '{"app_motorista":true,"osrm":true,"ia_preditiva":false,"satelite":false,"api":false}',   99.50),
    ('ENTERPRISE', 159.00, 50, 499, '{"app_motorista":true,"osrm":true,"ia_preditiva":true,"satelite":"addon","api":true}',   99.90),
    ('CUSTOM',     0.00,   500, NULL,'{"app_motorista":true,"osrm":true,"ia_preditiva":true,"satelite":true,"api":true}',     99.99);

-- ---------------------------------------------------------------------------
-- TABELA: assinaturas
-- Assinatura ativa de cada tenant
-- ---------------------------------------------------------------------------
CREATE TABLE assinaturas (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_uuid         CHAR(36) NOT NULL UNIQUE,
    plano_id            BIGINT UNSIGNED NOT NULL,
    status              ENUM('TRIAL','ATIVA','INADIMPLENTE','CANCELADA','SUSPENSA') NOT NULL DEFAULT 'TRIAL',
    veiculos_contratados INT NOT NULL DEFAULT 1,
    data_inicio         DATE NOT NULL,
    data_fim            DATE COMMENT 'NULL = recorrente',
    trial_expira        DATE,
    -- Gateway de pagamento
    gateway             ENUM('STRIPE','PAGSEGURO','MANUAL') NOT NULL DEFAULT 'PAGSEGURO',
    customer_id_gw      VARCHAR(100) COMMENT 'ID do cliente no gateway (ex: cus_xxx Stripe)',
    subscription_id_gw  VARCHAR(100) COMMENT 'ID da assinatura no gateway',
    -- Desconto
    desconto_pct        DECIMAL(5,2) NOT NULL DEFAULT 0,
    motivo_desconto     VARCHAR(255),
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_assin_plano FOREIGN KEY (plano_id) REFERENCES planos(id),
    INDEX idx_assin_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: faturas
-- Faturas mensais geradas para cada tenant
-- ---------------------------------------------------------------------------
CREATE TABLE faturas (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid                CHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    tenant_uuid         CHAR(36) NOT NULL,
    assinatura_id       BIGINT UNSIGNED NOT NULL,
    periodo_ref         DATE NOT NULL COMMENT 'Primeiro dia do mês de referência',
    veiculos_faturados  INT NOT NULL,
    preco_unitario      DECIMAL(8,2) NOT NULL,
    subtotal            DECIMAL(12,2) NOT NULL,
    desconto            DECIMAL(12,2) NOT NULL DEFAULT 0,
    total               DECIMAL(12,2) NOT NULL,
    status              ENUM('PENDENTE','PAGA','ATRASADA','CANCELADA','ESTORNADA') NOT NULL DEFAULT 'PENDENTE',
    data_vencimento     DATE NOT NULL,
    data_pagamento      DATE,
    linha_digitavel     VARCHAR(80) COMMENT 'Boleto',
    pix_copia_cola      TEXT,
    invoice_id_gw       VARCHAR(100) COMMENT 'ID da invoice no gateway',
    pdf_path            VARCHAR(500) COMMENT 'PDF da fatura no MinIO',
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_fatura_assin FOREIGN KEY (assinatura_id) REFERENCES assinaturas(id),
    INDEX idx_fatura_tenant  (tenant_uuid),
    INDEX idx_fatura_status  (status),
    INDEX idx_fatura_periodo (periodo_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: uso_mensal
-- Métricas de uso por tenant para billing e análise
-- ---------------------------------------------------------------------------
CREATE TABLE uso_mensal (
    id                   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_uuid          CHAR(36) NOT NULL,
    periodo              DATE NOT NULL,
    veiculos_ativos      INT NOT NULL DEFAULT 0,
    total_eventos        BIGINT NOT NULL DEFAULT 0,
    total_km             DOUBLE NOT NULL DEFAULT 0,
    total_viagens        INT NOT NULL DEFAULT 0,
    total_alertas        INT NOT NULL DEFAULT 0,
    requests_api         BIGINT NOT NULL DEFAULT 0,
    storage_mb           DOUBLE NOT NULL DEFAULT 0,
    chamadas_osrm        INT NOT NULL DEFAULT 0,
    chamadas_satelite    INT NOT NULL DEFAULT 0,
    criado_em            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_uso_tenant_periodo (tenant_uuid, periodo),
    INDEX idx_uso_periodo (periodo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- =============================================================================
-- OTA SERVICE — ota_db
-- Responsável: Firmware, distribuição OTA, rollout canário, audit trail
-- Porta: 8091
-- =============================================================================

CREATE DATABASE IF NOT EXISTS ota_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE ota_db;

-- ---------------------------------------------------------------------------
-- TABELA: firmware_versoes
-- Catálogo de versões de firmware dos rastreadores
-- ---------------------------------------------------------------------------
CREATE TABLE firmware_versoes (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    versao           VARCHAR(30) NOT NULL UNIQUE COMMENT 'SemVer ex: 2.4.1',
    versao_anterior  VARCHAR(30) COMMENT 'Versão base para delta update',
    arquivo_path     VARCHAR(500) NOT NULL COMMENT 'Caminho no MinIO',
    delta_path       VARCHAR(500) COMMENT 'Patch bsdiff em relação à versão anterior',
    tamanho_bytes    BIGINT NOT NULL,
    delta_bytes      BIGINT COMMENT 'Tamanho do delta',
    hash_sha256      VARCHAR(64) NOT NULL COMMENT 'Hash do firmware completo',
    hash_delta_sha256 VARCHAR(64) COMMENT 'Hash do delta',
    assinatura_hsm   TEXT NOT NULL COMMENT 'Assinatura criptográfica via HSM',
    changelog        TEXT,
    tipo             ENUM('MAJOR','MINOR','PATCH','HOTFIX','SECURITY') NOT NULL DEFAULT 'PATCH',
    status           ENUM('EM_TESTE','CANARY','PRODUCAO','DEPRECIADO','REVOGADO') NOT NULL DEFAULT 'EM_TESTE',
    modelo_hardware  VARCHAR(50) COMMENT 'Compatibilidade com modelo de hardware',
    criado_em        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_fw_status (status),
    INDEX idx_fw_versao (versao)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: ota_jobs
-- Jobs de atualização OTA por tenant/versão com estratégia de rollout
-- ---------------------------------------------------------------------------
CREATE TABLE ota_jobs (
    id                BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid              CHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    tenant_id         BIGINT UNSIGNED NOT NULL,
    firmware_id       BIGINT UNSIGNED NOT NULL,
    estrategia        ENUM('CANARY_1PCT','CANARY_5PCT','ROLLOUT_20PCT','FULL','ESPECIFICO') NOT NULL DEFAULT 'CANARY_1PCT',
    dispositivos_alvo JSON COMMENT 'Lista de device_ids para ESPECIFICO',
    pct_rollout       DECIMAL(5,2) NOT NULL DEFAULT 1.0 COMMENT 'Percentual atual',
    total_dispositivos INT NOT NULL DEFAULT 0,
    enviados          INT NOT NULL DEFAULT 0,
    sucessos          INT NOT NULL DEFAULT 0,
    falhas            INT NOT NULL DEFAULT 0,
    rollbacks         INT NOT NULL DEFAULT 0,
    status            ENUM('AGENDADO','EM_ANDAMENTO','PAUSADO','CONCLUIDO','REVERTIDO','FALHOU') NOT NULL DEFAULT 'AGENDADO',
    auto_rollback     BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Reverter se error rate > 2%',
    erro_rate_limite  DECIMAL(5,2) NOT NULL DEFAULT 2.0 COMMENT 'Percentual para acionar rollback automático',
    iniciado_em       DATETIME,
    concluido_em      DATETIME,
    criado_por        VARCHAR(255),
    criado_em         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ota_job_fw FOREIGN KEY (firmware_id) REFERENCES firmware_versoes(id),
    INDEX idx_ota_job_tenant (tenant_id),
    INDEX idx_ota_job_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: ota_dispositivos
-- Status de atualização por dispositivo individual
-- ---------------------------------------------------------------------------
CREATE TABLE ota_dispositivos (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    job_id          BIGINT UNSIGNED NOT NULL,
    device_id       VARCHAR(64) NOT NULL,
    veiculo_uuid    CHAR(36),
    tenant_id       BIGINT UNSIGNED NOT NULL,
    versao_atual    VARCHAR(30) NOT NULL,
    versao_alvo     VARCHAR(30) NOT NULL,
    usa_delta       BOOLEAN NOT NULL DEFAULT TRUE,
    status          ENUM('PENDENTE','DOWNLOAD','VERIFICANDO','INSTALANDO','SUCESSO','FALHOU','ROLLBACK') NOT NULL DEFAULT 'PENDENTE',
    token_unico     VARCHAR(64) NOT NULL UNIQUE COMMENT 'Token único e não reutilizável por update',
    tentativas      TINYINT UNSIGNED NOT NULL DEFAULT 0,
    progresso_pct   TINYINT UNSIGNED NOT NULL DEFAULT 0,
    erro_detalhe    TEXT,
    iniciado_em     DATETIME,
    concluido_em    DATETIME,
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ota_disp_job FOREIGN KEY (job_id) REFERENCES ota_jobs(id) ON DELETE CASCADE,
    INDEX idx_ota_disp_job    (job_id),
    INDEX idx_ota_disp_device (device_id),
    INDEX idx_ota_disp_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: ota_audit_log
-- Log imutável de todas as atualizações OTA
-- ---------------------------------------------------------------------------
CREATE TABLE ota_audit_log (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id       VARCHAR(64) NOT NULL,
    job_id          BIGINT UNSIGNED,
    versao_anterior VARCHAR(30),
    versao_nova     VARCHAR(30),
    evento          VARCHAR(50) NOT NULL COMMENT 'DOWNLOAD_STARTED, INSTALL_OK, ROLLBACK, VERIFY_FAIL',
    detalhes        JSON,
    sucesso         BOOLEAN NOT NULL,
    registrado_em   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ota_audit_device (device_id),
    INDEX idx_ota_audit_job    (job_id),
    INDEX idx_ota_audit_em     (registrado_em)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
