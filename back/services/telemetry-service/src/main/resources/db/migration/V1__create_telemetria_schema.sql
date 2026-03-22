-- =============================================================================
-- TELEMETRY SERVICE — telemetry_db
-- Script completo para MySQL Workbench
-- =============================================================================

-- Cria o banco de dados se não existir
CREATE DATABASE IF NOT EXISTS telemetria
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE telemetria;

-- =============================================================================
-- SEÇÃO 1 — CADASTROS BASE (caches locais)
-- =============================================================================

CREATE TABLE IF NOT EXISTS veiculos_cache (
    id                  BIGINT UNSIGNED NOT NULL PRIMARY KEY COMMENT 'Mesmo ID do vehicle-service',
    uuid                VARCHAR(36) NOT NULL UNIQUE,
    tenant_id           BIGINT UNSIGNED NOT NULL,
    placa               VARCHAR(10) NOT NULL,
    modelo              VARCHAR(255) NOT NULL,
    marca               VARCHAR(100),
    tipo_veiculo        VARCHAR(50) NOT NULL DEFAULT 'CAMINHAO_PESADO',
    consumo_medio       DOUBLE,
    capacidade_carga_kg DOUBLE,
    device_id           VARCHAR(64) COMMENT 'IMEI / serial do rastreador GPS',
    device_imei         VARCHAR(20),
    pbt_kg              DOUBLE COMMENT 'Peso Bruto Total — regra tacógrafo >4536kg',
    ativo               BOOLEAN NOT NULL DEFAULT TRUE,
    sincronizado_em     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_vc_tenant (tenant_id),
    INDEX idx_vc_device (device_id),
    INDEX idx_vc_placa  (placa)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS motoristas_cache (
    id              BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    uuid            VARCHAR(36) NOT NULL UNIQUE,
    tenant_id       BIGINT UNSIGNED NOT NULL,
    nome            VARCHAR(255) NOT NULL,
    cpf             VARCHAR(14) NOT NULL,
    cnh             VARCHAR(50) NOT NULL,
    categoria_cnh   VARCHAR(10) NOT NULL,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    sincronizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_mc_tenant (tenant_id),
    INDEX idx_mc_cpf    (cpf)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- SEÇÃO 1.5 — CLIENTES E CARGAS
-- =============================================================================

CREATE TABLE IF NOT EXISTS clientes (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nome_razao_social   VARCHAR(255) NOT NULL,
    cnpj                VARCHAR(18) NOT NULL UNIQUE,
    email               VARCHAR(200),
    telefone            VARCHAR(20),
    endereco            VARCHAR(500),
    ativo               BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_cli_cnpj  (cnpj)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS cargas (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    descricao           VARCHAR(500) NOT NULL,
    peso_kg             DOUBLE,
    tipo                VARCHAR(50),
    volume_m3           DOUBLE,
    nfe_chave           VARCHAR(50),
    cte_chave           VARCHAR(50),
    cliente_id          BIGINT UNSIGNED,
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_car_cliente (cliente_id),
    INDEX idx_car_tipo    (tipo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- SEÇÃO 8 — MOTORISTAS E VEÍCULOS (entidades principais)
-- =============================================================================

CREATE TABLE IF NOT EXISTS motoristas (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nome                VARCHAR(255) NOT NULL,
    cpf                 VARCHAR(14) NOT NULL UNIQUE,
    cnh                 VARCHAR(20) NOT NULL UNIQUE,
    categoria_cnh       VARCHAR(5) NOT NULL,
    email               VARCHAR(200) UNIQUE,
    telefone            VARCHAR(20),
    ativo               BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_mot_cpf   (cpf),
    INDEX idx_mot_cnh   (cnh),
    INDEX idx_mot_ativo (ativo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS veiculos (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id               BIGINT UNSIGNED NOT NULL COMMENT 'ID do cliente/tenant proprietário',
    placa                   VARCHAR(10) NOT NULL COMMENT 'Placa no formato Mercosul (ABC1D23) ou antigo (ABC-1234)',
    modelo                  VARCHAR(255),
    marca                   VARCHAR(100),
    capacidade_carga        DOUBLE,
    ano_fabricacao          INT,
    ativo                   BOOLEAN NOT NULL DEFAULT TRUE,
    cliente_id              BIGINT UNSIGNED,
    motorista_atual_id      BIGINT UNSIGNED,
    -- RN-VEI-002: Tacógrafo
    pbt_kg                  DOUBLE COMMENT 'Peso Bruto Total em kg - regra tacógrafo >4536kg',
    tacografo_obrigatorio   BOOLEAN DEFAULT FALSE COMMENT 'TRUE se PBT > 4.536kg',
    data_venc_tacografo     DATE COMMENT 'Data de vencimento do tacógrafo',
    -- RN-VEI-003: Documentos com Vencimento
    data_venc_crlv          DATE COMMENT 'Vencimento do CRLV',
    data_venc_seguro        DATE COMMENT 'Vencimento do Seguro',
    data_venc_dpvat         DATE COMMENT 'Vencimento do DPVAT',
    data_venc_rcf           DATE COMMENT 'Vencimento do RCF (Responsabilidade Civil Facultativa)',
    data_venc_vistoria      DATE COMMENT 'Vencimento da Vistoria',
    data_venc_rntrc         DATE COMMENT 'Vencimento do RNTRC',
    criado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    -- Constraints
    UNIQUE KEY uk_veiculo_placa_tenant (placa, tenant_id) COMMENT 'Unicidade de placa por tenant (RN-VEI-001)',
    INDEX idx_vei_placa     (placa),
    INDEX idx_vei_tenant    (tenant_id),
    INDEX idx_vei_ativo     (ativo),
    INDEX idx_vei_tacografo_venc (data_venc_tacografo),
    INDEX idx_vei_crlv_venc (data_venc_crlv),
    INDEX idx_vei_seguro_venc (data_venc_seguro)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Veículos com todas as regras do RF02: unicidade por tenant, tacógrafo, documentos';

-- =============================================================================
-- SEÇÃO 6 — ROTAS
-- =============================================================================

CREATE TABLE IF NOT EXISTS rotas (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nome                    VARCHAR(255) NOT NULL,
    origem                  VARCHAR(255),
    latitude_origem         DOUBLE,
    longitude_origem        DOUBLE,
    destino                 VARCHAR(255),
    latitude_destino        DOUBLE,
    longitude_destino       DOUBLE,
    distancia_prevista      DOUBLE,
    tempo_previsto          INT COMMENT 'minutos',
    tolerancia_desvio_m     DOUBLE DEFAULT 100.0,
    threshold_alerta_m      DOUBLE DEFAULT 50.0,
    status                  VARCHAR(30) NOT NULL DEFAULT 'PLANEJADA',
    ativa                   BOOLEAN NOT NULL DEFAULT TRUE,
    data_inicio             DATETIME,
    data_fim                DATETIME,
    pontos_rota             JSON,
    veiculo_id              BIGINT UNSIGNED,
    motorista_id            BIGINT UNSIGNED,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rot_veiculo    (veiculo_id),
    INDEX idx_rot_motorista  (motorista_id),
    INDEX idx_rot_status     (status),
    INDEX idx_rot_ativa      (ativa)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- SEÇÃO 7 — VIAGENS
-- =============================================================================

CREATE TABLE IF NOT EXISTS viagens (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PLANEJADA',
    observacoes             TEXT,
    data_saida              DATETIME,
    data_chegada_prevista   DATETIME,
    data_chegada_real       DATETIME,
    data_inicio             DATETIME,
    distancia_real_km       DOUBLE DEFAULT 0.0,
    km_fora_rota            DOUBLE DEFAULT 0.0,
    score_viagem            INT DEFAULT 1000,
    veiculo_id              BIGINT UNSIGNED,
    motorista_id            BIGINT UNSIGNED,
    carga_id                BIGINT UNSIGNED,
    rota_id                 BIGINT UNSIGNED,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_via_veiculo   (veiculo_id),
    INDEX idx_via_motorista (motorista_id),
    INDEX idx_via_rota      (rota_id),
    INDEX idx_via_status    (status),
    INDEX idx_via_saida     (data_saida)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- SEÇÃO 2 — TELEMETRIA PRINCIPAL
-- =============================================================================

CREATE TABLE IF NOT EXISTS telemetria (
    id                          BIGINT UNSIGNED AUTO_INCREMENT,
    -- Identificação
    tenant_id                   BIGINT UNSIGNED NOT NULL,
    veiculo_id                  BIGINT UNSIGNED NOT NULL,
    veiculo_uuid                VARCHAR(36) NOT NULL,
    motorista_id                BIGINT UNSIGNED,
    viagem_id                   BIGINT UNSIGNED,
    device_id                   VARCHAR(64) COMMENT 'IMEI/serial do rastreador',
    imei_dispositivo            VARCHAR(20),

    -- LOCALIZAÇÃO GPS
    latitude                    DOUBLE NOT NULL,
    longitude                   DOUBLE NOT NULL,
    altitude                    DOUBLE,
    velocidade                  DOUBLE NOT NULL DEFAULT 0 COMMENT 'km/h',
    direcao                     DOUBLE COMMENT 'Heading em graus (0-359)',
    hdop                        DOUBLE COMMENT 'Horizontal Dilution of Precision',
    satelites                   INT,
    precisao_gps                DOUBLE,
    lat_snap                    DOUBLE COMMENT 'Latitude após snap-to-road OSRM',
    lng_snap                    DOUBLE COMMENT 'Longitude após snap-to-road OSRM',
    nome_via                    VARCHAR(255) COMMENT 'Nome da via mais próxima',

    -- MOTOR E DESEMPENHO
    ignicao                     BOOLEAN NOT NULL DEFAULT FALSE,
    rpm                         DOUBLE NOT NULL DEFAULT 0,
    carga_motor                 DOUBLE,
    torque_motor                DOUBLE,
    temperatura_motor           DOUBLE,
    pressao_oleo                DOUBLE,
    tensao_bateria              DOUBLE,
    odometro                    DOUBLE NOT NULL DEFAULT 0 COMMENT 'km',
    horas_motor                 DOUBLE,
    aceleracao                  DOUBLE COMMENT 'm/s²',
    inclinacao                  DOUBLE,

    -- COMBUSTÍVEL
    nivel_combustivel           DOUBLE COMMENT 'Percentual do tanque 0-100',
    consumo_combustivel         DOUBLE COMMENT 'L/100km instantâneo',
    consumo_acumulado           DOUBLE,
    tempo_ocioso                INT COMMENT 'Segundos com motor ligado parado',
    tempo_motor_ligado          INT,

    -- COMPORTAMENTO DO MOTORISTA
    frenagem_brusca             BOOLEAN NOT NULL DEFAULT FALSE,
    numero_frenagens            INT NOT NULL DEFAULT 0,
    numero_aceleracoes_bruscas  INT NOT NULL DEFAULT 0,
    excesso_velocidade          BOOLEAN NOT NULL DEFAULT FALSE,
    velocidade_limite_via       DOUBLE,
    curva_brusca                BOOLEAN NOT NULL DEFAULT FALSE,
    pontuacao_motorista         INT,

    -- EVENTOS DE SEGURANÇA
    colisao_detectada           BOOLEAN NOT NULL DEFAULT FALSE,
    geofence_violada            BOOLEAN NOT NULL DEFAULT FALSE,
    geofence_id                 BIGINT UNSIGNED,
    cinto_seguranca             BOOLEAN NOT NULL DEFAULT TRUE,
    porta_aberta                BOOLEAN NOT NULL DEFAULT FALSE,
    botao_panico                BOOLEAN NOT NULL DEFAULT FALSE,
    adulteracao_gps             BOOLEAN NOT NULL DEFAULT FALSE,

    -- CARGA
    temperatura_carga           DOUBLE,
    umidade_carga               DOUBLE,
    peso_carga_kg               DOUBLE,
    porta_bau_aberta            BOOLEAN NOT NULL DEFAULT FALSE,
    impacto_carga               BOOLEAN NOT NULL DEFAULT FALSE,
    g_force_impacto             DOUBLE,

    -- PNEUS TPMS
    pressao_pneus_json          JSON COMMENT '[{pos:"D1",psi:120.5,temp_c:45.2,sensor_id:"ABC"}]',
    alerta_pneu                 BOOLEAN NOT NULL DEFAULT FALSE,

    -- CÂMERA DMS
    fadiga_detectada            BOOLEAN NOT NULL DEFAULT FALSE,
    distracao_detectada         BOOLEAN NOT NULL DEFAULT FALSE,
    uso_celular_detectado       BOOLEAN NOT NULL DEFAULT FALSE,
    cigarro_detectado           BOOLEAN NOT NULL DEFAULT FALSE,
    ausencia_cinto_dms          BOOLEAN NOT NULL DEFAULT FALSE,
    score_dms                   INT,

    -- AMBIENTE
    temperatura_externa         DOUBLE,
    umidade_externa             DOUBLE,
    chuva_detectada             BOOLEAN NOT NULL DEFAULT FALSE,
    condicao_pista              VARCHAR(30),

    -- CONECTIVIDADE
    sinal_gsm                   DOUBLE,
    sinal_gps                   DOUBLE,
    tecnologia_rede             VARCHAR(10),
    firmware_versao             VARCHAR(30),
    modo_offline                BOOLEAN NOT NULL DEFAULT FALSE,
    delay_sincronizacao_s       INT,

    -- TACÓGRAFO
    tacografo_status            VARCHAR(20),
    tacografo_velocidade        DOUBLE,
    tacografo_distancia         DOUBLE,
    horas_direcao_acumuladas    DOUBLE,

    -- DESGASTE / MANUTENÇÃO
    manutencao_pendente         BOOLEAN NOT NULL DEFAULT FALSE,
    proxima_revisao             DATETIME,
    desgaste_freio              DOUBLE,
    dtc_codes                   JSON COMMENT 'Diagnostic Trouble Codes OBD-II',

    -- PAYLOAD EXTENSÍVEL
    payload                     JSON,

    -- TIMESTAMPS
    data_hora                   DATETIME NOT NULL COMMENT 'Timestamp do dispositivo',
    recebido_em                 TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processado_em               TIMESTAMP,

    PRIMARY KEY (id, data_hora),
    INDEX idx_tel_veiculo_data  (veiculo_id, data_hora DESC),
    INDEX idx_tel_tenant_data   (tenant_id, data_hora DESC),
    INDEX idx_tel_viagem        (viagem_id),
    INDEX idx_tel_device        (device_id),
    INDEX idx_tel_data_hora     (data_hora),
    INDEX idx_tel_eventos       (colisao_detectada, botao_panico, adulteracao_gps),
    INDEX idx_tel_alertas       (excesso_velocidade, frenagem_brusca, geofence_violada),
    INDEX idx_tel_localizacao   (latitude, longitude),
    INDEX idx_tel_dms           (fadiga_detectada, uso_celular_detectado)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Eventos IoT de telemetria';

-- =============================================================================
-- SEÇÃO 3 — ALERTAS E GEOFENCES
-- =============================================================================

CREATE TABLE IF NOT EXISTS geofences (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid             VARCHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    tenant_id        BIGINT UNSIGNED NOT NULL,
    nome             VARCHAR(255) NOT NULL,
    tipo             ENUM('CIRCULO','POLIGONO') NOT NULL DEFAULT 'CIRCULO',
    latitude_centro  DOUBLE,
    longitude_centro DOUBLE,
    raio             DOUBLE COMMENT 'Raio em metros',
    vertices         JSON COMMENT '[{lat, lng}]',
    tipo_alerta      ENUM('ENTRADA','SAIDA','AMBOS') NOT NULL DEFAULT 'AMBOS',
    aplica_todos     BOOLEAN NOT NULL DEFAULT TRUE,
    veiculos_uuid    JSON,
    ativo            BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_gf_tenant (tenant_id),
    INDEX idx_gf_ativo  (ativo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS veiculo_geofence (
    veiculo_id  BIGINT UNSIGNED NOT NULL,
    geofence_id BIGINT UNSIGNED NOT NULL,
    ativo       BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (veiculo_id, geofence_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS alertas (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid                    VARCHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    tenant_id               BIGINT UNSIGNED NOT NULL,
    veiculo_id              BIGINT UNSIGNED,
    veiculo_uuid            VARCHAR(36),
    motorista_id            BIGINT UNSIGNED,
    viagem_id               BIGINT UNSIGNED,
    telemetria_id           BIGINT UNSIGNED COMMENT 'Evento que originou o alerta',
    regra_id                BIGINT UNSIGNED COMMENT 'Regra do motor que disparou',
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
    canais_notificados      JSON COMMENT '["PUSH","SMS","EMAIL"]',
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

-- =============================================================================
-- SEÇÃO 4 — DESVIOS DE ROTA
-- =============================================================================

CREATE TABLE IF NOT EXISTS desvios_rota (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id               BIGINT UNSIGNED NOT NULL,
    rota_id                 BIGINT UNSIGNED NOT NULL COMMENT 'ID da rota no routing-service',
    veiculo_id              BIGINT UNSIGNED NOT NULL,
    veiculo_uuid            VARCHAR(36) NOT NULL,
    viagem_id               BIGINT UNSIGNED,
    latitude_desvio         DOUBLE NOT NULL,
    longitude_desvio        DOUBLE NOT NULL,
    velocidade_kmh          DOUBLE,
    distancia_metros        DOUBLE NOT NULL,
    lat_ponto_mais_proximo  DOUBLE,
    lng_ponto_mais_proximo  DOUBLE,
    nome_via_desvio         VARCHAR(255),
    data_hora_desvio        DATETIME NOT NULL,
    data_hora_retorno       DATETIME,
    duracao_min             INT,
    km_extras               DOUBLE NOT NULL DEFAULT 0,
    alerta_enviado          BOOLEAN NOT NULL DEFAULT FALSE,
    resolvido               BOOLEAN NOT NULL DEFAULT FALSE,
    motivo                  VARCHAR(255),
    criado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_desvio_rota      (rota_id),
    INDEX idx_desvio_veiculo   (veiculo_id),
    INDEX idx_desvio_viagem    (viagem_id),
    INDEX idx_desvio_data      (data_hora_desvio),
    INDEX idx_desvio_resolvido (resolvido)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- SEÇÃO 5 — GEOCODING CACHE
-- =============================================================================

CREATE TABLE IF NOT EXISTS geocoding_cache (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    lat_arred       DECIMAL(7,4) NOT NULL COMMENT 'Latitude arredondada 4 casas (~11m)',
    lng_arred       DECIMAL(7,4) NOT NULL COMMENT 'Longitude arredondada 4 casas',
    pais            VARCHAR(100),
    estado          VARCHAR(100),
    cidade          VARCHAR(200),
    bairro          VARCHAR(200),
    logradouro      VARCHAR(300),
    numero          VARCHAR(20),
    cep             VARCHAR(10),
    nome_local      VARCHAR(255),
    tipo_local      VARCHAR(50),
    is_urbano       BOOLEAN,
    precisao_metros INT,
    fonte           VARCHAR(50) NOT NULL DEFAULT 'NOMINATIM',
    consulta_em     DATETIME NOT NULL,
    expira_em       DATETIME NOT NULL,
    UNIQUE KEY uk_geocoding_coords (lat_arred, lng_arred),
    INDEX idx_geo_expira (expira_em)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- SEÇÃO 9 — MANUTENÇÕES
-- =============================================================================

CREATE TABLE IF NOT EXISTS manutencoes (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    data_manutencao         DATE NOT NULL,
    descricao               TEXT NOT NULL,
    custo                   DOUBLE,
    tipo                    VARCHAR(30) NOT NULL,
    oficina                 VARCHAR(100),
    km_realizacao           DOUBLE,
    proxima_manutencao_km   DOUBLE,
    proxima_manutencao_data DATE,
    observacoes             VARCHAR(500),
    veiculo_id              BIGINT UNSIGNED NOT NULL,
    motorista_id            BIGINT UNSIGNED,
    criado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_man_veiculo   (veiculo_id),
    INDEX idx_man_data      (data_manutencao),
    INDEX idx_man_tipo      (tipo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- SEÇÃO 10 — USUÁRIOS
-- =============================================================================

CREATE TABLE IF NOT EXISTS usuarios (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    login               VARCHAR(100) NOT NULL UNIQUE,
    senha               VARCHAR(255) NOT NULL,
    nome                VARCHAR(255) NOT NULL,
    email               VARCHAR(200) UNIQUE,
    cpf                 VARCHAR(14) NOT NULL UNIQUE,
    ativo               BOOLEAN NOT NULL DEFAULT TRUE,
    perfil              VARCHAR(20) NOT NULL,
    ultimo_acesso       DATETIME,
    motorista_id        BIGINT UNSIGNED UNIQUE,
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_usr_login (login),
    INDEX idx_usr_cpf   (cpf),
    INDEX idx_usr_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- SEÇÃO 11 — AGREGAÇÕES E POSIÇÃO ATUAL
-- =============================================================================

CREATE TABLE IF NOT EXISTS resumo_diario_veiculo (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT UNSIGNED NOT NULL,
    veiculo_id          BIGINT UNSIGNED NOT NULL,
    data                DATE NOT NULL,
    km_total            DOUBLE NOT NULL DEFAULT 0,
    horas_uso           DOUBLE NOT NULL DEFAULT 0,
    horas_ocioso        DOUBLE NOT NULL DEFAULT 0,
    litros_consumidos   DOUBLE NOT NULL DEFAULT 0,
    consumo_medio       DOUBLE,
    velocidade_media    DOUBLE,
    velocidade_maxima   DOUBLE,
    frenagens_bruscas   INT NOT NULL DEFAULT 0,
    aceleracoes_bruscas INT NOT NULL DEFAULT 0,
    excessos_velocidade INT NOT NULL DEFAULT 0,
    curvas_bruscas      INT NOT NULL DEFAULT 0,
    total_alertas       INT NOT NULL DEFAULT 0,
    alertas_criticos    INT NOT NULL DEFAULT 0,
    total_viagens       INT NOT NULL DEFAULT 0,
    alertas_fadiga      INT NOT NULL DEFAULT 0,
    alertas_celular     INT NOT NULL DEFAULT 0,
    score_dia           INT NOT NULL DEFAULT 1000,
    total_eventos       INT NOT NULL DEFAULT 0,
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_resumo_veiculo_data (tenant_id, veiculo_id, data),
    INDEX idx_rdv_tenant  (tenant_id),
    INDEX idx_rdv_veiculo (veiculo_id),
    INDEX idx_rdv_data    (data)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS posicao_atual (
    veiculo_id          BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    tenant_id           BIGINT UNSIGNED NOT NULL,
    veiculo_uuid        VARCHAR(36) NOT NULL,
    latitude            DOUBLE NOT NULL,
    longitude           DOUBLE NOT NULL,
    velocidade          DOUBLE NOT NULL DEFAULT 0,
    direcao             DOUBLE,
    ignicao             BOOLEAN NOT NULL DEFAULT FALSE,
    status_veiculo      VARCHAR(30) NOT NULL DEFAULT 'DESCONHECIDO',
    motorista_id        BIGINT UNSIGNED,
    viagem_id           BIGINT UNSIGNED,
    odometro            DOUBLE,
    nivel_combustivel   DOUBLE,
    bateria_v           DOUBLE,
    ultima_telemetria   DATETIME NOT NULL,
    ultima_atualizacao  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    nome_local          VARCHAR(255),
    alertas_ativos      INT NOT NULL DEFAULT 0,
    INDEX idx_pa_tenant (tenant_id),
    INDEX idx_pa_status (status_veiculo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS historico_posicao (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id   BIGINT UNSIGNED NOT NULL,
    veiculo_id  BIGINT UNSIGNED NOT NULL,
    data_hora   DATETIME NOT NULL,
    latitude    DOUBLE NOT NULL,
    longitude   DOUBLE NOT NULL,
    velocidade  DOUBLE,
    ignicao     BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_hp_veiculo   (veiculo_id, data_hora DESC),
    INDEX idx_hp_tenant    (tenant_id),
    INDEX idx_hp_data_hora (data_hora)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- SEÇÃO 12 — DISPOSITIVOS IoT
-- =============================================================================

CREATE TABLE IF NOT EXISTS dispositivos_iot (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id           VARCHAR(64) NOT NULL UNIQUE,
    imei                VARCHAR(20),
    iccid               VARCHAR(25),
    tenant_id           BIGINT UNSIGNED,
    veiculo_id          BIGINT UNSIGNED,
    tipo                ENUM('PRINCIPAL','BACKUP') DEFAULT 'PRINCIPAL' COMMENT 'RN-VEI-005: Principal (GSM) ou Backup (Satelital)',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Dispositivos IoT com suporte a principal/backup (RN-VEI-004, RN-VEI-005)';

CREATE TABLE IF NOT EXISTS heartbeat_log (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    device_id     VARCHAR(64) NOT NULL,
    tenant_id     BIGINT UNSIGNED NOT NULL,
    tipo          ENUM('CONNECT','DISCONNECT','HEARTBEAT','RECONNECT') NOT NULL,
    ip            VARCHAR(45),
    tecnologia    VARCHAR(10),
    rssi          DOUBLE,
    firmware      VARCHAR(30),
    registrado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_hb_device     (device_id),
    INDEX idx_hb_tenant     (tenant_id),
    INDEX idx_hb_registrado (registrado_em)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- SEÇÃO 13 — HISTÓRICO DE ODÔMETRO (RN-VEI-006)
-- =============================================================================

CREATE TABLE IF NOT EXISTS historico_odometro (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    veiculo_id              BIGINT UNSIGNED NOT NULL COMMENT 'Veículo que sofreu a troca',
    dispositivo_origem_id   BIGINT UNSIGNED COMMENT 'Dispositivo antigo desvinculado',
    dispositivo_destino_id  BIGINT UNSIGNED COMMENT 'Novo dispositivo vinculado',
    odometro_anterior_km    DOUBLE NOT NULL COMMENT 'Último odômetro registrado pelo dispositivo antigo',
    odometro_novo_km        DOUBLE NOT NULL COMMENT 'Odômetro informado no momento da troca',
    delta_km                DOUBLE NOT NULL COMMENT 'Diferença entre odometro_novo e odometro_anterior',
    data_troca              DATETIME NOT NULL COMMENT 'Data e hora da troca',
    usuario_id              BIGINT UNSIGNED COMMENT 'Usuário que realizou a troca',
    observacao              TEXT COMMENT 'Observação sobre a troca',
    alerta_inconsistencia   BOOLEAN DEFAULT FALSE COMMENT 'TRUE se delta > 500 km (RN-VEI-006)',
    criado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ho_veiculo    (veiculo_id),
    INDEX idx_ho_data       (data_troca),
    INDEX idx_ho_delta      (delta_km),
    FOREIGN KEY (veiculo_id) REFERENCES veiculos(id) ON DELETE CASCADE,
    FOREIGN KEY (dispositivo_origem_id) REFERENCES dispositivos_iot(id) ON DELETE SET NULL,
    FOREIGN KEY (dispositivo_destino_id) REFERENCES dispositivos_iot(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Histórico de trocas de dispositivo com calibração de odômetro (RN-VEI-006)';

-- =============================================================================
-- SEÇÃO 14 — JORNADA (Lei 12.619/2012)
-- =============================================================================

CREATE TABLE IF NOT EXISTS jornadas (
    id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id             BIGINT UNSIGNED NOT NULL,
    motorista_id          BIGINT UNSIGNED NOT NULL,
    veiculo_id            BIGINT UNSIGNED,
    viagem_id             BIGINT UNSIGNED,
    data_inicio           DATETIME NOT NULL,
    data_fim              DATETIME,
    horas_direcao         DOUBLE NOT NULL DEFAULT 0,
    horas_disponivel      DOUBLE NOT NULL DEFAULT 0,
    horas_repouso         DOUBLE NOT NULL DEFAULT 0,
    horas_extras          DOUBLE NOT NULL DEFAULT 0,
    pausas_realizadas     INT NOT NULL DEFAULT 0,
    km_rodados            DOUBLE NOT NULL DEFAULT 0,
    limite_direcao_h      DOUBLE NOT NULL DEFAULT 8.0,
    limite_extra_h        DOUBLE NOT NULL DEFAULT 2.0,
    alertas_enviados      INT NOT NULL DEFAULT 0,
    alerta_limite_30min   BOOLEAN NOT NULL DEFAULT FALSE,
    status                ENUM('ABERTA','FECHADA','IRREGULAR') NOT NULL DEFAULT 'ABERTA',
    origem_dado           ENUM('TACOGRAFO','APP','TELEMETRIA','MANUAL') NOT NULL DEFAULT 'TELEMETRIA',
    arquivo_tacografo     VARCHAR(200),
    irregular             BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_irregularidade TEXT,
    criado_em             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_jornada_motorista (motorista_id),
    INDEX idx_jornada_tenant    (tenant_id),
    INDEX idx_jornada_data      (data_inicio),
    INDEX idx_jornada_status    (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- ADIÇÃO DAS CONSTRAINTS (FOREIGN KEYS) APÓS TODAS AS TABELAS CRIADAS
-- =============================================================================

ALTER TABLE cargas ADD CONSTRAINT fk_carga_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE SET NULL;

ALTER TABLE veiculos ADD CONSTRAINT fk_veiculo_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id) ON DELETE SET NULL;
ALTER TABLE veiculos ADD CONSTRAINT fk_veiculo_motorista_atual FOREIGN KEY (motorista_atual_id) REFERENCES motoristas(id) ON DELETE SET NULL;

ALTER TABLE rotas ADD CONSTRAINT fk_rota_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculos(id) ON DELETE SET NULL;
ALTER TABLE rotas ADD CONSTRAINT fk_rota_motorista FOREIGN KEY (motorista_id) REFERENCES motoristas(id) ON DELETE SET NULL;

ALTER TABLE viagens ADD CONSTRAINT fk_viagem_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculos(id) ON DELETE SET NULL;
ALTER TABLE viagens ADD CONSTRAINT fk_viagem_motorista FOREIGN KEY (motorista_id) REFERENCES motoristas(id) ON DELETE SET NULL;
ALTER TABLE viagens ADD CONSTRAINT fk_viagem_carga FOREIGN KEY (carga_id) REFERENCES cargas(id) ON DELETE SET NULL;
ALTER TABLE viagens ADD CONSTRAINT fk_viagem_rota FOREIGN KEY (rota_id) REFERENCES rotas(id) ON DELETE SET NULL;

ALTER TABLE telemetria ADD CONSTRAINT fk_tel_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculos_cache(id) ON DELETE CASCADE;
ALTER TABLE telemetria ADD CONSTRAINT fk_tel_motorista FOREIGN KEY (motorista_id) REFERENCES motoristas_cache(id) ON DELETE SET NULL;

ALTER TABLE veiculo_geofence ADD CONSTRAINT fk_vgf_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculos_cache(id) ON DELETE CASCADE;
ALTER TABLE veiculo_geofence ADD CONSTRAINT fk_vgf_geofence FOREIGN KEY (geofence_id) REFERENCES geofences(id) ON DELETE CASCADE;

ALTER TABLE alertas ADD CONSTRAINT fk_alert_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculos_cache(id) ON DELETE SET NULL;
ALTER TABLE alertas ADD CONSTRAINT fk_alert_motorista FOREIGN KEY (motorista_id) REFERENCES motoristas_cache(id) ON DELETE SET NULL;

ALTER TABLE desvios_rota ADD CONSTRAINT fk_desvio_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculos_cache(id) ON DELETE CASCADE;

ALTER TABLE posicao_atual ADD CONSTRAINT fk_pa_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculos_cache(id) ON DELETE CASCADE;

ALTER TABLE historico_posicao ADD CONSTRAINT fk_hp_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculos_cache(id) ON DELETE CASCADE;

ALTER TABLE manutencoes ADD CONSTRAINT fk_manutencao_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculos(id) ON DELETE CASCADE;
ALTER TABLE manutencoes ADD CONSTRAINT fk_manutencao_motorista FOREIGN KEY (motorista_id) REFERENCES motoristas(id) ON DELETE SET NULL;

ALTER TABLE usuarios ADD CONSTRAINT fk_usuario_motorista FOREIGN KEY (motorista_id) REFERENCES motoristas(id) ON DELETE SET NULL;

ALTER TABLE jornadas ADD CONSTRAINT fk_jornada_motorista FOREIGN KEY (motorista_id) REFERENCES motoristas_cache(id) ON DELETE CASCADE;
ALTER TABLE jornadas ADD CONSTRAINT fk_jornada_veiculo FOREIGN KEY (veiculo_id) REFERENCES veiculos_cache(id) ON DELETE SET NULL;

-- =============================================================================
-- VIEWS ÚTEIS
-- =============================================================================

CREATE OR REPLACE VIEW vw_ultima_telemetria AS
SELECT t.*
FROM telemetria t
INNER JOIN (
    SELECT veiculo_id, MAX(data_hora) AS ultima_data
    FROM telemetria
    GROUP BY veiculo_id
) sub ON t.veiculo_id = sub.veiculo_id AND t.data_hora = sub.ultima_data;

CREATE OR REPLACE VIEW vw_frota_ao_vivo AS
SELECT
    pa.veiculo_id,
    pa.veiculo_uuid,
    pa.tenant_id,
    vc.placa,
    vc.modelo,
    vc.tipo_veiculo,
    pa.latitude,
    pa.longitude,
    pa.velocidade,
    pa.direcao,
    pa.ignicao,
    pa.status_veiculo,
    pa.odometro,
    pa.nivel_combustivel,
    pa.bateria_v,
    pa.ultima_telemetria,
    pa.alertas_ativos,
    pa.nome_local,
    mc.nome AS motorista_nome,
    pa.viagem_id
FROM posicao_atual pa
LEFT JOIN veiculos_cache   vc ON pa.veiculo_id  = vc.id
LEFT JOIN motoristas_cache mc ON pa.motorista_id = mc.id;

CREATE OR REPLACE VIEW vw_alertas_pendentes AS
SELECT
    a.*,
    vc.placa,
    mc.nome AS motorista_nome
FROM alertas a
LEFT JOIN veiculos_cache   vc ON a.veiculo_id   = vc.id
LEFT JOIN motoristas_cache mc ON a.motorista_id = mc.id
WHERE a.lido = FALSE
ORDER BY
    CASE a.severidade
        WHEN 'CRITICO' THEN 1
        WHEN 'ALTO'    THEN 2
        WHEN 'MEDIO'   THEN 3
        WHEN 'BAIXO'   THEN 4
    END,
    a.data_hora DESC;

-- =============================================================================
-- VIEW DE VEÍCULOS COM DOCUMENTOS VENCIDOS (RN-VEI-003)
-- =============================================================================

CREATE OR REPLACE VIEW vw_veiculos_documentos_vencidos AS
SELECT
    v.id,
    v.tenant_id,
    v.placa,
    v.modelo,
    v.marca,
    v.pbt_kg,
    v.tacografo_obrigatorio,
    v.data_venc_tacografo,
    v.data_venc_crlv,
    v.data_venc_seguro,
    v.data_venc_dpvat,
    v.data_venc_rcf,
    v.data_venc_vistoria,
    v.data_venc_rntrc,
    c.nome_razao_social AS tenant_nome,
    CASE 
        WHEN v.data_venc_crlv < CURDATE() THEN 'CRLV'
        WHEN v.data_venc_seguro < CURDATE() THEN 'Seguro'
        WHEN v.data_venc_dpvat < CURDATE() THEN 'DPVAT'
        WHEN v.data_venc_rcf < CURDATE() THEN 'RCF'
        WHEN v.data_venc_vistoria < CURDATE() THEN 'Vistoria'
        WHEN v.data_venc_rntrc < CURDATE() THEN 'RNTRC'
        WHEN v.tacografo_obrigatorio = TRUE AND v.data_venc_tacografo < CURDATE() THEN 'Tacógrafo'
        ELSE NULL
    END AS documento_vencido,
    LEAST(
        COALESCE(v.data_venc_crlv, DATE_ADD(CURDATE(), INTERVAL 999 DAY)),
        COALESCE(v.data_venc_seguro, DATE_ADD(CURDATE(), INTERVAL 999 DAY)),
        COALESCE(v.data_venc_dpvat, DATE_ADD(CURDATE(), INTERVAL 999 DAY)),
        COALESCE(v.data_venc_rcf, DATE_ADD(CURDATE(), INTERVAL 999 DAY)),
        COALESCE(v.data_venc_vistoria, DATE_ADD(CURDATE(), INTERVAL 999 DAY)),
        COALESCE(v.data_venc_rntrc, DATE_ADD(CURDATE(), INTERVAL 999 DAY)),
        COALESCE(v.data_venc_tacografo, DATE_ADD(CURDATE(), INTERVAL 999 DAY))
    ) AS proximo_vencimento
FROM veiculos v
INNER JOIN clientes c ON v.tenant_id = c.id
WHERE v.ativo = TRUE
  AND (
      v.data_venc_crlv < CURDATE() OR
      v.data_venc_seguro < CURDATE() OR
      v.data_venc_dpvat < CURDATE() OR
      v.data_venc_rcf < CURDATE() OR
      v.data_venc_vistoria < CURDATE() OR
      v.data_venc_rntrc < CURDATE() OR
      (v.tacografo_obrigatorio = TRUE AND v.data_venc_tacografo < CURDATE())
  );

-- =============================================================================
-- VIEW DE VEÍCULOS COM TACÓGRAFO VENCENDO EM 30/7 DIAS (RN-VEI-002)
-- =============================================================================

CREATE OR REPLACE VIEW vw_tacografo_proximo_vencimento AS
SELECT
    v.id,
    v.tenant_id,
    v.placa,
    v.modelo,
    v.pbt_kg,
    v.data_venc_tacografo,
    DATEDIFF(v.data_venc_tacografo, CURDATE()) AS dias_para_vencimento,
    c.nome_razao_social AS tenant_nome,
    CASE
        WHEN DATEDIFF(v.data_venc_tacografo, CURDATE()) <= 7 THEN 'CRITICO_7_DIAS'
        WHEN DATEDIFF(v.data_venc_tacografo, CURDATE()) <= 30 THEN 'ALERTA_30_DIAS'
        ELSE 'OK'
    END AS status_alerta
FROM veiculos v
INNER JOIN clientes c ON v.tenant_id = c.id
WHERE v.tacografo_obrigatorio = TRUE
  AND v.data_venc_tacografo IS NOT NULL
  AND v.data_venc_tacografo > CURDATE()
  AND DATEDIFF(v.data_venc_tacografo, CURDATE()) <= 30;