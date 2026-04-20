-- =============================================================================
-- ROUTING SERVICE — routing_db
-- Responsável: Rotas planejadas, viagens, desvios, pontos de entrega,
--              cache OSRM, geocoding, ETA dinâmico
-- Porta: 8083
-- =============================================================================

CREATE DATABASE IF NOT EXISTS routing_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE routing_db;

-- =============================================================================
-- SEÇÃO 1 — CACHES LOCAIS (replicados via Kafka)
-- =============================================================================

CREATE TABLE veiculos_cache (
    id              BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    uuid            CHAR(36) NOT NULL UNIQUE,
    tenant_id       BIGINT UNSIGNED NOT NULL,
    placa           VARCHAR(10) NOT NULL,
    modelo          VARCHAR(255),
    tipo_veiculo    VARCHAR(50) NOT NULL DEFAULT 'CAMINHAO_PESADO',
    consumo_medio   DOUBLE COMMENT 'km/L para estimativa de custo',
    pbt_kg          DOUBLE COMMENT 'Peso Bruto Total para perfil OSRM',
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    sincronizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_vc_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE motoristas_cache (
    id              BIGINT UNSIGNED NOT NULL PRIMARY KEY,
    uuid            CHAR(36) NOT NULL UNIQUE,
    tenant_id       BIGINT UNSIGNED NOT NULL,
    nome            VARCHAR(255) NOT NULL,
    cpf             VARCHAR(14) NOT NULL,
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    sincronizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_mc_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- SEÇÃO 2 — ROTAS
-- =============================================================================

-- ---------------------------------------------------------------------------
-- TABELA: rotas
-- Rota planejada com dados OSRM completos, paradas, thresholds e status.
-- Esta é a tabela central do routing-service.
-- ---------------------------------------------------------------------------
CREATE TABLE rotas (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid                    CHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    tenant_id               BIGINT UNSIGNED NOT NULL,
    nome                    VARCHAR(255) NOT NULL,

    -- ▸ ORIGEM
    origem_nome             VARCHAR(255) NOT NULL,
    origem_latitude         DOUBLE NOT NULL,
    origem_longitude        DOUBLE NOT NULL,
    origem_endereco         VARCHAR(500),
    origem_cep              VARCHAR(10),

    -- ▸ DESTINO
    destino_nome            VARCHAR(255) NOT NULL,
    destino_latitude        DOUBLE NOT NULL,
    destino_longitude       DOUBLE NOT NULL,
    destino_endereco        VARCHAR(500),
    destino_cep             VARCHAR(10),

    -- ▸ DADOS CALCULADOS PELO OSRM
    distancia_km            DOUBLE COMMENT 'Distância total calculada (OSRM /route)',
    tempo_estimado_min      INT    COMMENT 'Tempo estimado em minutos (OSRM)',
    polyline_encoded        TEXT   COMMENT 'Encoded Polyline (Google format) para frontend',
    rota_geojson            JSON   COMMENT 'GeoJSON LineString completo da rota',
    pontos_rota             JSON   COMMENT 'Array de coordenadas [{lat,lng}] para cálculo de desvio',
    pontos_snap             JSON   COMMENT 'Pontos ajustados via OSRM /match snap-to-road',
    osrm_request_id         VARCHAR(36) COMMENT 'ID da requisição OSRM para debug',

    -- ▸ PARADAS INTERMEDIÁRIAS
    paradas                 JSON COMMENT '[{seq:1, nome:"CD São Paulo", lat:-23.5, lng:-46.6, janela_inicio:"08:00", janela_fim:"12:00", tipo:"ENTREGA"}]',
    sequencia_otimizada     JSON COMMENT 'Ordem reordenada pelo OSRM /trip (TSP solver)',
    total_paradas           INT NOT NULL DEFAULT 0,

    -- ▸ ESTIMATIVAS DE CUSTO
    custo_pedagio_est       DECIMAL(10,2) COMMENT 'Valor estimado de pedágios',
    custo_combustivel_est   DECIMAL(10,2) COMMENT 'Baseado no consumo médio do veículo',
    custo_total_est         DECIMAL(10,2),

    -- ▸ CONFIGURAÇÕES DE DESVIO
    tolerancia_desvio_m     DOUBLE NOT NULL DEFAULT 100.0 COMMENT 'Metros: distância máxima da via',
    threshold_alerta_m      DOUBLE NOT NULL DEFAULT 50.0  COMMENT 'Metros para disparar alerta',
    max_km_extras_alerta    DOUBLE NOT NULL DEFAULT 2.0   COMMENT 'km extras acumulados para alerta',

    -- ▸ VÍNCULOS
    veiculo_id              BIGINT UNSIGNED COMMENT 'FK local para veiculos_cache',
    veiculo_uuid            CHAR(36),
    motorista_id            BIGINT UNSIGNED COMMENT 'FK local para motoristas_cache',
    motorista_uuid          CHAR(36),
    carga_uuid              CHAR(36) COMMENT 'UUID da carga no vehicle-service',

    -- ▸ DATAS
    data_inicio_plan        DATETIME COMMENT 'Saída planejada',
    data_fim_plan           DATETIME COMMENT 'Chegada planejada',
    data_inicio_real        DATETIME,
    data_fim_real           DATETIME,

    -- ▸ DOCUMENTAÇÃO FISCAL
    mdfe_chave              VARCHAR(50) COMMENT 'Chave do MDF-e vinculado',
    cte_chave               VARCHAR(50) COMMENT 'Chave do CT-e vinculado',
    ciot_codigo             VARCHAR(30) COMMENT 'Código CIOT da operação',

    -- ▸ PERFIL OSRM
    perfil_osrm             ENUM('CAMINHAO','CARRO') NOT NULL DEFAULT 'CAMINHAO',

    -- ▸ STATUS
    status                  ENUM('PLANEJADA','EM_ANDAMENTO','PAUSADA','DESVIO_DETECTADO','FINALIZADA','CANCELADA') NOT NULL DEFAULT 'PLANEJADA',
    motivo_cancelamento     TEXT,

    -- ▸ CONTROLE
    ativa                   BOOLEAN NOT NULL DEFAULT TRUE,
    criado_por              VARCHAR(255) COMMENT 'UUID do usuário que criou a rota',
    criado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_rota_tenant   (tenant_id),
    INDEX idx_rota_status   (status),
    INDEX idx_rota_veiculo  (veiculo_id),
    INDEX idx_rota_motorista (motorista_id),
    INDEX idx_rota_ativa    (ativa),
    INDEX idx_rota_datas    (data_inicio_plan, data_fim_plan),
    INDEX idx_rota_uuid     (uuid),
    CONSTRAINT fk_rota_veiculo   FOREIGN KEY (veiculo_id)   REFERENCES veiculos_cache(id)  ON DELETE SET NULL,
    CONSTRAINT fk_rota_motorista FOREIGN KEY (motorista_id) REFERENCES motoristas_cache(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- SEÇÃO 3 — VIAGENS
-- =============================================================================

-- ---------------------------------------------------------------------------
-- TABELA: viagens
-- Execução real de uma rota. Contém o estado em tempo real da viagem ativa.
-- ---------------------------------------------------------------------------
CREATE TABLE viagens (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid                    CHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    tenant_id               BIGINT UNSIGNED NOT NULL,
    rota_id                 BIGINT UNSIGNED,

    -- Vínculos
    veiculo_id              BIGINT UNSIGNED,
    veiculo_uuid            CHAR(36) NOT NULL,
    motorista_id            BIGINT UNSIGNED,
    motorista_uuid          CHAR(36),
    carga_uuid              CHAR(36),

    -- Datas
    data_saida_plan         DATETIME,
    data_saida_real         DATETIME,
    data_chegada_plan       DATETIME,
    data_chegada_real       DATETIME,
    eta_atual               DATETIME COMMENT 'ETA recalculado em tempo real',
    eta_atualizado_em       DATETIME COMMENT 'Última atualização do ETA',

    -- Métricas reais
    distancia_real_km       DOUBLE NOT NULL DEFAULT 0,
    consumo_real_l          DOUBLE NOT NULL DEFAULT 0,
    km_fora_rota            DOUBLE NOT NULL DEFAULT 0 COMMENT 'km extras fora da rota planejada',
    velocidade_media        DOUBLE,
    velocidade_maxima       DOUBLE,

    -- Última posição conhecida
    ultima_lat              DOUBLE,
    ultima_lng              DOUBLE,
    ultima_posicao_em       DATETIME,

    -- Resumo de ocorrências
    total_desvios           INT NOT NULL DEFAULT 0,
    total_alertas           INT NOT NULL DEFAULT 0,
    total_paradas           INT NOT NULL DEFAULT 0,
    tempo_ocioso_min        INT NOT NULL DEFAULT 0,
    frenagens_bruscas       INT NOT NULL DEFAULT 0,
    excessos_velocidade     INT NOT NULL DEFAULT 0,

    -- Score
    score_viagem            INT NOT NULL DEFAULT 1000 COMMENT '0-1000: quanto maior, melhor',

    -- Status
    status                  ENUM('PLANEJADA','EM_ANDAMENTO','PAUSADA','FINALIZADA','CANCELADA') NOT NULL DEFAULT 'PLANEJADA',
    motivo_cancelamento     TEXT,

    criado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_viagem_tenant     (tenant_id),
    INDEX idx_viagem_rota       (rota_id),
    INDEX idx_viagem_veiculo    (veiculo_id),
    INDEX idx_viagem_motorista  (motorista_id),
    INDEX idx_viagem_status     (status),
    INDEX idx_viagem_datas      (data_saida_real, data_chegada_real),
    INDEX idx_viagem_uuid       (uuid),
    CONSTRAINT fk_viagem_rota      FOREIGN KEY (rota_id)    REFERENCES rotas(id)             ON DELETE SET NULL,
    CONSTRAINT fk_viagem_veiculo   FOREIGN KEY (veiculo_id) REFERENCES veiculos_cache(id)    ON DELETE SET NULL,
    CONSTRAINT fk_viagem_motorista FOREIGN KEY (motorista_id) REFERENCES motoristas_cache(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- SEÇÃO 4 — DESVIOS E PONTOS DE ENTREGA
-- =============================================================================

-- ---------------------------------------------------------------------------
-- TABELA: desvios_rota
-- Cada desvio detectado durante a viagem.
-- O telemetry-service envia eventos via Kafka; este serviço persiste o histórico.
-- ---------------------------------------------------------------------------
CREATE TABLE desvios_rota (
    id                          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id                   BIGINT UNSIGNED NOT NULL,
    viagem_id                   BIGINT UNSIGNED NOT NULL,
    rota_id                     BIGINT UNSIGNED NOT NULL,
    veiculo_uuid                CHAR(36) NOT NULL,

    -- Posição do desvio
    latitude_desvio             DOUBLE NOT NULL,
    longitude_desvio            DOUBLE NOT NULL,
    velocidade_kmh              DOUBLE,
    distancia_metros            DOUBLE NOT NULL COMMENT 'Distância do ponto mais próximo na rota',

    -- Ponto de referência na rota
    lat_ponto_mais_proximo      DOUBLE,
    lng_ponto_mais_proximo      DOUBLE,
    nome_via_desvio             VARCHAR(255),
    nome_via_rota               VARCHAR(255) COMMENT 'Via que deveria estar seguindo',

    -- Período do desvio
    data_hora_desvio            DATETIME NOT NULL,
    data_hora_retorno           DATETIME COMMENT 'NULL = ainda fora da rota',
    duracao_min                 INT,
    km_extras                   DOUBLE NOT NULL DEFAULT 0,

    -- Gestão
    alerta_enviado              BOOLEAN NOT NULL DEFAULT FALSE,
    alerta_id                   BIGINT UNSIGNED COMMENT 'ID do alerta no alert-service',
    resolvido                   BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_motorista            VARCHAR(255) COMMENT 'Justificativa do motorista',
    aprovado_gestor             BOOLEAN COMMENT 'NULL = não revisado',

    criado_em                   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_dr_viagem     (viagem_id),
    INDEX idx_dr_rota       (rota_id),
    INDEX idx_dr_data       (data_hora_desvio),
    INDEX idx_dr_resolvido  (resolvido),
    CONSTRAINT fk_dr_viagem FOREIGN KEY (viagem_id) REFERENCES viagens(id) ON DELETE CASCADE,
    CONSTRAINT fk_dr_rota   FOREIGN KEY (rota_id)   REFERENCES rotas(id)   ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: pontos_entrega
-- Cada parada/entrega realizada na viagem com proof of delivery.
-- ---------------------------------------------------------------------------
CREATE TABLE pontos_entrega (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id           BIGINT UNSIGNED NOT NULL,
    viagem_id           BIGINT UNSIGNED NOT NULL,
    sequencia           INT NOT NULL COMMENT 'Ordem na rota (1, 2, 3...)',

    -- Dados do ponto
    nome                VARCHAR(255),
    endereco            VARCHAR(500),
    latitude            DOUBLE NOT NULL,
    longitude           DOUBLE NOT NULL,
    tipo                ENUM('COLETA','ENTREGA','PARADA','ABASTECIMENTO','PERNOITE') NOT NULL DEFAULT 'ENTREGA',

    -- Janela de tempo
    janela_inicio       DATETIME COMMENT 'Início da janela de entrega',
    janela_fim          DATETIME COMMENT 'Fim da janela de entrega',

    -- Execução real
    eta_calculado       DATETIME,
    chegada_real        DATETIME,
    saida_real          DATETIME,
    dentro_janela       BOOLEAN COMMENT 'Chegou dentro da janela? NULL = pendente',

    -- Proof of Delivery
    status              ENUM('PENDENTE','CHEGOU','ENTREGUE','FALHOU','PULADO') NOT NULL DEFAULT 'PENDENTE',
    destinatario_nome   VARCHAR(255),
    assinatura_path     VARCHAR(500) COMMENT 'Foto da assinatura no MinIO',
    foto_entrega_path   VARCHAR(500) COMMENT 'Foto da entrega no MinIO',
    ocorrencia          TEXT COMMENT 'Ocorrência registrada (recusa, ausência, etc.)',
    codigo_rastreio     VARCHAR(50) COMMENT 'Código de rastreio da encomenda',
    nfe_chave           VARCHAR(50) COMMENT 'NF-e da entrega',

    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_pe_viagem (viagem_id),
    INDEX idx_pe_seq    (viagem_id, sequencia),
    INDEX idx_pe_status (status),
    CONSTRAINT fk_pe_viagem FOREIGN KEY (viagem_id) REFERENCES viagens(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- SEÇÃO 5 — CACHE OSRM E GEOCODING
-- =============================================================================

-- ---------------------------------------------------------------------------
-- TABELA: osrm_route_cache
-- Cache de rotas calculadas para evitar chamadas repetidas ao OSRM.
-- Key = MD5(origem_lat:origem_lng:destino_lat:destino_lng:perfil)
-- TTL recomendado: 7 dias (rotas mudam com obras/updates do mapa)
-- ---------------------------------------------------------------------------
CREATE TABLE osrm_route_cache (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    cache_key       VARCHAR(64) NOT NULL UNIQUE COMMENT 'MD5(coords+perfil)',
    perfil          ENUM('CAMINHAO','CARRO') NOT NULL DEFAULT 'CAMINHAO',

    -- Origem e destino (para debug e recalcular se necessário)
    lat_origem      DOUBLE NOT NULL,
    lng_origem      DOUBLE NOT NULL,
    lat_destino     DOUBLE NOT NULL,
    lng_destino     DOUBLE NOT NULL,

    -- Resultado OSRM
    distancia_km    DOUBLE NOT NULL,
    duracao_min     INT NOT NULL,
    polyline        TEXT COMMENT 'Encoded Polyline',
    geojson         JSON COMMENT 'GeoJSON da rota',
    instrucoes      JSON COMMENT 'Turn-by-turn navigation instructions',

    -- Metadados
    hits            INT NOT NULL DEFAULT 1 COMMENT 'Número de vezes que o cache foi utilizado',
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expira_em       TIMESTAMP NOT NULL,

    INDEX idx_osrm_key    (cache_key),
    INDEX idx_osrm_expira (expira_em)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: geocoding_cache
-- Cache de geocoding direto e reverso para evitar chamadas externas.
-- ---------------------------------------------------------------------------
CREATE TABLE geocoding_cache (
    id              BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    lat_arred       DECIMAL(7,4) NOT NULL COMMENT 'Latitude arredondada 4 casas (~11m)',
    lng_arred       DECIMAL(7,4) NOT NULL COMMENT 'Longitude arredondada 4 casas',
    pais            VARCHAR(100),
    estado          VARCHAR(100),
    cidade          VARCHAR(200),
    bairro          VARCHAR(200),
    logradouro      VARCHAR(300),
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
-- SEÇÃO 6 — HISTÓRICO E ANÁLISE
-- =============================================================================

-- ---------------------------------------------------------------------------
-- TABELA: historico_trajeto_viagem
-- Trajetória real simplificada da viagem para relatório pós-viagem.
-- Versão comprimida (Douglas-Peucker) — dados completos no telemetry-service.
-- ---------------------------------------------------------------------------
CREATE TABLE historico_trajeto_viagem (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id   BIGINT UNSIGNED NOT NULL,
    viagem_id   BIGINT UNSIGNED NOT NULL,
    segmento    INT NOT NULL COMMENT 'Segmento do trajeto (1h de dados por segmento)',
    trajeto     JSON NOT NULL COMMENT 'Array [{lat,lng,ts,vel,ignicao}] simplificado',
    km_segmento DOUBLE NOT NULL DEFAULT 0,
    criado_em   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_htv_viagem FOREIGN KEY (viagem_id) REFERENCES viagens(id) ON DELETE CASCADE,
    INDEX idx_htv_viagem   (viagem_id),
    INDEX idx_htv_segmento (viagem_id, segmento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: relatorio_viagem
-- Relatório pós-viagem consolidado gerado ao finalizar a viagem.
-- ---------------------------------------------------------------------------
CREATE TABLE relatorio_viagem (
    id                      BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id               BIGINT UNSIGNED NOT NULL,
    viagem_id               BIGINT UNSIGNED NOT NULL UNIQUE,
    rota_id                 BIGINT UNSIGNED,
    veiculo_uuid            CHAR(36) NOT NULL,
    motorista_uuid          CHAR(36),

    -- Aderência à rota
    aderencia_pct           DOUBLE COMMENT 'Percentual do trajeto dentro da rota planejada',
    desvios_count           INT NOT NULL DEFAULT 0,
    km_extras_total         DOUBLE NOT NULL DEFAULT 0,

    -- Tempo
    duracao_total_min       INT,
    atraso_chegada_min      INT COMMENT 'Positivo = atrasado, negativo = adiantado',
    tempo_paradas_min       INT,
    tempo_ocioso_min        INT,

    -- Distância
    km_total                DOUBLE,
    km_planejado            DOUBLE,
    diferenca_km            DOUBLE,

    -- Combustível
    litros_consumidos       DOUBLE,
    custo_combustivel       DECIMAL(10,2),
    consumo_medio           DOUBLE COMMENT 'km/L real da viagem',

    -- Comportamento
    score_final             INT,
    frenagens_bruscas       INT,
    excessos_velocidade     INT,
    aceleracoes_bruscas     INT,

    -- Entregas
    entregas_total          INT,
    entregas_sucesso        INT,
    entregas_falhas         INT,
    entregas_no_prazo       INT,

    -- PDF gerado
    pdf_path                VARCHAR(500) COMMENT 'Relatório PDF no MinIO',
    gerado_em               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_rv_tenant  (tenant_id),
    INDEX idx_rv_veiculo (veiculo_uuid),
    CONSTRAINT fk_rv_viagem FOREIGN KEY (viagem_id) REFERENCES viagens(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================================================
-- SEÇÃO 7 — VIEWS ÚTEIS
-- =============================================================================

-- Viagens ativas com dados de veículo e motorista
CREATE OR REPLACE VIEW vw_viagens_ativas AS
SELECT
    v.*,
    vc.placa,
    vc.modelo,
    vc.tipo_veiculo,
    mc.nome AS motorista_nome,
    r.origem_nome,
    r.destino_nome,
    r.distancia_km AS distancia_planejada_km
FROM viagens v
LEFT JOIN veiculos_cache  vc ON v.veiculo_id   = vc.id
LEFT JOIN motoristas_cache mc ON v.motorista_id = mc.id
LEFT JOIN rotas r              ON v.rota_id      = r.id
WHERE v.status IN ('PLANEJADA','EM_ANDAMENTO','PAUSADA');

-- Resumo de desvios por rota
CREATE OR REPLACE VIEW vw_desvios_pendentes AS
SELECT
    d.*,
    v.uuid AS viagem_uuid,
    r.nome AS rota_nome
FROM desvios_rota d
LEFT JOIN viagens v ON d.viagem_id = v.id
LEFT JOIN rotas   r ON d.rota_id   = r.id
WHERE d.resolvido = FALSE
ORDER BY d.data_hora_desvio DESC;
