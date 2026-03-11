-- =============================================================================
-- ROUTE SERVICE — route_db
-- Responsável: Rotas, viagens, desvios, geocoding cache, OSRM integração
-- Porta: 8083
-- =============================================================================

CREATE DATABASE IF NOT EXISTS route_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE route_db;

-- ---------------------------------------------------------------------------
-- TABELA: rotas
-- Rota planejada com dados OSRM, pontos de parada e configurações de desvio
-- ---------------------------------------------------------------------------
CREATE TABLE rotas (
    id                    BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid                  CHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    tenant_id             BIGINT UNSIGNED NOT NULL,
    nome                  VARCHAR(255) NOT NULL,

    -- Origem
    origem_nome           VARCHAR(255) NOT NULL,
    origem_latitude       DOUBLE NOT NULL,
    origem_longitude      DOUBLE NOT NULL,
    origem_endereco       VARCHAR(500),

    -- Destino
    destino_nome          VARCHAR(255) NOT NULL,
    destino_latitude      DOUBLE NOT NULL,
    destino_longitude     DOUBLE NOT NULL,
    destino_endereco      VARCHAR(500),

    -- Dados calculados pelo OSRM
    distancia_km          DOUBLE COMMENT 'Distância calculada pelo OSRM',
    tempo_estimado_min    INT    COMMENT 'Tempo estimado em minutos (OSRM)',
    polyline_encoded      TEXT   COMMENT 'Polyline Google Encoded para exibição no mapa',
    rota_geojson          JSON   COMMENT 'GeoJSON LineString da rota',
    pontos_snap           JSON   COMMENT 'Pontos após snap-to-road do OSRM /match',

    -- Paradas intermediárias
    paradas               JSON   COMMENT '[{nome, lat, lng, janela_inicio, janela_fim, seq}]',
    sequencia_otimizada   JSON   COMMENT 'Paradas reordenadas pelo OSRM /trip (TSP)',

    -- Custo estimado
    custo_pedagio_est     DECIMAL(10,2),
    custo_combustivel_est DECIMAL(10,2) COMMENT 'Calculado com consumo médio do veículo',

    -- Configurações de desvio
    tolerancia_desvio_m   DOUBLE NOT NULL DEFAULT 100.0 COMMENT 'Metros antes de considerar desvio',
    threshold_alerta_m    DOUBLE NOT NULL DEFAULT 50.0  COMMENT 'Metros para disparar alerta',

    -- Status e vínculos
    status                ENUM('PLANEJADA','EM_ANDAMENTO','PAUSADA','DESVIO_DETECTADO','FINALIZADA','CANCELADA') NOT NULL DEFAULT 'PLANEJADA',
    veiculo_uuid          CHAR(36) COMMENT 'UUID do veículo (referência ao vehicle-service)',
    motorista_uuid        CHAR(36) COMMENT 'UUID do motorista',
    carga_uuid            CHAR(36) COMMENT 'UUID da carga',

    -- Datas
    data_inicio_plan      DATETIME,
    data_fim_plan         DATETIME,
    data_inicio_real      DATETIME,
    data_fim_real         DATETIME,

    -- Conformidade
    mdfe_chave            VARCHAR(50),
    ciot_codigo           VARCHAR(30) COMMENT 'Código CIOT da operação de transporte',

    -- Flags
    ativa                 BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em             TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_rota_tenant  (tenant_id),
    INDEX idx_rota_status  (status),
    INDEX idx_rota_veiculo (veiculo_uuid),
    INDEX idx_rota_ativa   (ativa),
    INDEX idx_rota_datas   (data_inicio_plan, data_fim_plan)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: viagens
-- Execução real de uma rota (pode haver múltiplas tentativas por rota)
-- ---------------------------------------------------------------------------
CREATE TABLE viagens (
    id                     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid                   CHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    tenant_id              BIGINT UNSIGNED NOT NULL,
    rota_id                BIGINT UNSIGNED,
    veiculo_uuid           CHAR(36) NOT NULL,
    motorista_uuid         CHAR(36),
    carga_uuid             CHAR(36),

    -- Datas
    data_saida_plan        DATETIME,
    data_saida_real        DATETIME,
    data_chegada_plan      DATETIME,
    data_chegada_real      DATETIME,
    eta_atual              DATETIME COMMENT 'ETA recalculado em tempo real',

    -- Distância e consumo reais
    distancia_real_km      DOUBLE NOT NULL DEFAULT 0,
    consumo_real_l         DOUBLE NOT NULL DEFAULT 0,
    km_fora_rota           DOUBLE NOT NULL DEFAULT 0 COMMENT 'Km acumulados fora da rota planejada',

    -- Status
    status                 ENUM('PLANEJADA','EM_ANDAMENTO','PAUSADA','FINALIZADA','CANCELADA') NOT NULL DEFAULT 'PLANEJADA',
    motivo_cancelamento    TEXT,

    -- Resumo de ocorrências
    total_desvios          INT NOT NULL DEFAULT 0,
    total_alertas          INT NOT NULL DEFAULT 0,
    total_paradas          INT NOT NULL DEFAULT 0,
    tempo_ocioso_min       INT NOT NULL DEFAULT 0 COMMENT 'Minutos com motor ligado parado',

    -- Score da viagem
    score_viagem           INT NOT NULL DEFAULT 1000 COMMENT '0-1000',

    criado_em              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_viagem_tenant    (tenant_id),
    INDEX idx_viagem_rota      (rota_id),
    INDEX idx_viagem_veiculo   (veiculo_uuid),
    INDEX idx_viagem_motorista (motorista_uuid),
    INDEX idx_viagem_status    (status),
    INDEX idx_viagem_datas     (data_saida_real, data_chegada_real),
    CONSTRAINT fk_viagem_rota FOREIGN KEY (rota_id) REFERENCES rotas(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: desvios_rota
-- Registro de cada desvio detectado durante uma viagem
-- ---------------------------------------------------------------------------
CREATE TABLE desvios_rota (
    id                 BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id          BIGINT UNSIGNED NOT NULL,
    viagem_id          BIGINT UNSIGNED NOT NULL,
    rota_id            BIGINT UNSIGNED NOT NULL,
    veiculo_uuid       CHAR(36) NOT NULL,

    latitude_desvio    DOUBLE NOT NULL,
    longitude_desvio   DOUBLE NOT NULL,
    distancia_metros   DOUBLE NOT NULL COMMENT 'Distância do ponto mais próximo da rota planejada',
    velocidade_kmh     DOUBLE,

    -- Ponto mais próximo na rota planejada
    lat_ponto_mais_proximo  DOUBLE,
    lng_ponto_mais_proximo  DOUBLE,

    data_hora_desvio   DATETIME NOT NULL,
    data_hora_retorno  DATETIME COMMENT 'Quando voltou à rota',
    duracao_min        INT COMMENT 'Minutos fora da rota',
    km_extras          DOUBLE NOT NULL DEFAULT 0,

    alerta_enviado     BOOLEAN NOT NULL DEFAULT FALSE,
    resolvido          BOOLEAN NOT NULL DEFAULT FALSE,
    motivo             VARCHAR(255) COMMENT 'Razão informada pelo motorista',

    criado_em          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_desvio_viagem  (viagem_id),
    INDEX idx_desvio_rota    (rota_id),
    INDEX idx_desvio_data    (data_hora_desvio),
    INDEX idx_desvio_resolv  (resolvido),
    CONSTRAINT fk_desvio_viagem FOREIGN KEY (viagem_id) REFERENCES viagens(id) ON DELETE CASCADE,
    CONSTRAINT fk_desvio_rota   FOREIGN KEY (rota_id)   REFERENCES rotas(id)   ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: pontos_entrega
-- Registro de cada ponto de parada/entrega realizado na viagem
-- ---------------------------------------------------------------------------
CREATE TABLE pontos_entrega (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id        BIGINT UNSIGNED NOT NULL,
    viagem_id        BIGINT UNSIGNED NOT NULL,
    sequencia        INT NOT NULL,
    nome             VARCHAR(255),
    latitude         DOUBLE NOT NULL,
    longitude        DOUBLE NOT NULL,
    eta_plan         DATETIME,
    chegada_real     DATETIME,
    saida_real       DATETIME,
    status           ENUM('PENDENTE','CHEGOU','ENTREGUE','FALHOU') NOT NULL DEFAULT 'PENDENTE',
    assinatura_path  VARCHAR(500) COMMENT 'Foto da assinatura no MinIO',
    foto_entrega_path VARCHAR(500),
    observacoes      TEXT,
    criado_em        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pe_viagem FOREIGN KEY (viagem_id) REFERENCES viagens(id) ON DELETE CASCADE,
    INDEX idx_pe_viagem (viagem_id),
    INDEX idx_pe_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: osrm_route_cache
-- Cache de rotas calculadas pelo OSRM para evitar recalcular rotas iguais
-- Complementa o cache Redis (persiste rotas para análise e auditoria)
-- ---------------------------------------------------------------------------
CREATE TABLE osrm_route_cache (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    cache_key       VARCHAR(64) NOT NULL UNIQUE COMMENT 'Hash MD5 das coordenadas + perfil',
    perfil          ENUM('CAMINHAO','CARRO') NOT NULL DEFAULT 'CAMINHAO',
    lat_origem      DOUBLE NOT NULL,
    lng_origem      DOUBLE NOT NULL,
    lat_destino     DOUBLE NOT NULL,
    lng_destino     DOUBLE NOT NULL,
    distancia_km    DOUBLE NOT NULL,
    duracao_min     INT NOT NULL,
    polyline        TEXT,
    geojson         JSON,
    hits            INT NOT NULL DEFAULT 1 COMMENT 'Quantas vezes este cache foi utilizado',
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expira_em       TIMESTAMP NOT NULL,
    INDEX idx_osrm_cache_key (cache_key),
    INDEX idx_osrm_expira    (expira_em)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: geocoding_cache
-- Cache de reverse geocoding para evitar chamadas repetidas a APIs externas
-- ---------------------------------------------------------------------------
CREATE TABLE geocoding_cache (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    lat_arred        DECIMAL(7,4) NOT NULL COMMENT 'Latitude arredondada 4 casas (~11m precisão)',
    lng_arred        DECIMAL(7,4) NOT NULL COMMENT 'Longitude arredondada 4 casas',
    pais            VARCHAR(100),
    estado          VARCHAR(100),
    cidade          VARCHAR(200),
    bairro          VARCHAR(200),
    logradouro      VARCHAR(300),
    nome_local      VARCHAR(255),
    tipo_local      VARCHAR(50),
    is_urbano       BOOLEAN,
    precisao_metros INT,
    consulta_em     DATETIME NOT NULL,
    expira_em       DATETIME NOT NULL,
    UNIQUE KEY uk_geocoding_coords (lat_arred, lng_arred),
    INDEX idx_geocoding_expira (expira_em)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: historico_posicao_rota
-- Snapshot diário/semanal da trajetória real vs planejada (para relatórios)
-- Dados em tempo real ficam no TimescaleDB do telemetry-service
-- ---------------------------------------------------------------------------
CREATE TABLE historico_posicao_rota (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id   BIGINT UNSIGNED NOT NULL,
    viagem_id   BIGINT UNSIGNED NOT NULL,
    periodo     DATETIME NOT NULL COMMENT 'Timestamp do bucket',
    trajeto     JSON NOT NULL COMMENT 'Array [{lat,lng,ts,vel}] simplificado (Douglas-Peucker)',
    criado_em   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_hpr_viagem FOREIGN KEY (viagem_id) REFERENCES viagens(id) ON DELETE CASCADE,
    INDEX idx_hpr_viagem  (viagem_id),
    INDEX idx_hpr_periodo (periodo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
