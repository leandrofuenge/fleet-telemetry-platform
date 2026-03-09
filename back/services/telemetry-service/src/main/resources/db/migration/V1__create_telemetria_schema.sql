-- =========================================
-- CRIAÇÃO DO BANCO (opcional)
-- =========================================
-- CREATE DATABASE telemetria CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- USE telemetria;

-- =========================================
-- TABELA: clientes
-- =========================================
CREATE TABLE clientes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome_razao_social VARCHAR(255) NOT NULL,
    cnpj VARCHAR(20) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ativo BOOLEAN DEFAULT TRUE,
    INDEX idx_cliente_cnpj (cnpj)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABELA: veiculos
-- =========================================
CREATE TABLE veiculos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    placa VARCHAR(10) NOT NULL UNIQUE,
    modelo VARCHAR(255) NOT NULL,
    marca VARCHAR(100),
    ano_fabricacao INT,
    capacidade_carga DOUBLE NOT NULL,
    tipo_veiculo VARCHAR(50) DEFAULT 'CAMINHAO',
    consumo_medio DOUBLE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ativo BOOLEAN DEFAULT TRUE,
    INDEX idx_veiculo_placa (placa),
    INDEX idx_veiculo_tipo (tipo_veiculo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABELA: motoristas
-- =========================================
CREATE TABLE motoristas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    cpf VARCHAR(14) UNIQUE NOT NULL,
    cnh VARCHAR(50) NOT NULL,
    categoria_cnh VARCHAR(10) NOT NULL,
    data_validade_cnh DATE,
    telefone VARCHAR(20),
    email VARCHAR(255) UNIQUE,
    data_nascimento DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ativo BOOLEAN DEFAULT TRUE,
    INDEX idx_motorista_cpf (cpf),
    INDEX idx_motorista_cnh (cnh)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABELA: usuarios
-- =========================================
CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    login VARCHAR(255) NOT NULL UNIQUE,
    senha VARCHAR(255) NOT NULL,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    cpf VARCHAR(14) UNIQUE,
    ativo BOOLEAN DEFAULT TRUE,
    perfil VARCHAR(50) DEFAULT 'OPERADOR',
    ultimo_acesso DATETIME,
    tentativas_login INT DEFAULT 0,
    bloqueado BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_usuario_login (login),
    INDEX idx_usuario_perfil (perfil),
    INDEX idx_usuario_cpf (cpf)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABELA: cargas
-- =========================================
CREATE TABLE cargas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    descricao VARCHAR(255) NOT NULL,
    peso DOUBLE NOT NULL,
    tipo_carga VARCHAR(100),
    valor FLOAT,
    cliente_id BIGINT,
    status VARCHAR(50) DEFAULT 'PENDENTE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_carga_cliente
        FOREIGN KEY (cliente_id)
        REFERENCES clientes(id)
        ON DELETE SET NULL,
    INDEX idx_carga_cliente (cliente_id),
    INDEX idx_carga_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABELA: rotas (COMPLETA)
-- =========================================
CREATE TABLE rotas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    origem VARCHAR(255),
    destino VARCHAR(255),
    latitude_origem DOUBLE,
    longitude_origem DOUBLE,
    latitude_destino DOUBLE,
    longitude_destino DOUBLE,
    distancia_prevista DOUBLE,
    tempo_previsto INT COMMENT 'Em minutos',
    ativa BOOLEAN DEFAULT TRUE,
    data_inicio DATETIME,
    data_fim DATETIME,
    veiculo_id BIGINT,
    
    -- STATUS DA ROTA
    status VARCHAR(50) NOT NULL DEFAULT 'PLANEJADA' 
        COMMENT 'PLANEJADA, EM_ANDAMENTO, FINALIZADA, CANCELADA',
    
    -- CONFIGURAÇÕES DE DESVIO
    tolerancia_desvio DOUBLE DEFAULT 100.0 
        COMMENT 'Tolerância em metros antes de considerar desvio',
    threshold_desvio DOUBLE DEFAULT 50.0 
        COMMENT 'Distância para disparar alerta de desvio',
    
    -- DADOS GEOGRÁFICOS DA ROTA
    rota_geojson JSON 
        COMMENT 'Rota completa em formato GeoJSON (polyline)',
    pontos_rota JSON 
        COMMENT 'Lista de pontos que formam a rota para cálculos de desvio',
    
    -- TIMESTAMPS
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- CHAVES ESTRANGEIRAS
    CONSTRAINT fk_rota_veiculo
        FOREIGN KEY (veiculo_id)
        REFERENCES veiculos(id)
        ON DELETE SET NULL,
    
    -- ÍNDICES
    INDEX idx_rota_veiculo (veiculo_id),
    INDEX idx_rota_ativa (ativa),
    INDEX idx_rota_status (status),
    INDEX idx_rota_datas (data_inicio, data_fim)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABELA: manutencoes
-- =========================================
CREATE TABLE manutencoes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    veiculo_id BIGINT NOT NULL,
    data_manutencao DATE NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    custo FLOAT,
    tipo VARCHAR(50) DEFAULT 'PREVENTIVA',
    status VARCHAR(50) DEFAULT 'AGENDADA',
    observacoes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_manutencao_veiculo
        FOREIGN KEY (veiculo_id)
        REFERENCES veiculos(id)
        ON DELETE CASCADE,
    INDEX idx_manutencao_veiculo (veiculo_id),
    INDEX idx_manutencao_data (data_manutencao),
    INDEX idx_manutencao_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABELA: cache geocoding
-- =========================================
CREATE TABLE geocoding_cache (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    is_urbano BOOLEAN NOT NULL,
    nome_local VARCHAR(255),
    tipo_local VARCHAR(50),
    pais VARCHAR(100),
    estado VARCHAR(100),
    cidade VARCHAR(200),
    data_consulta DATETIME NOT NULL,
    precisao_metros INT,
    INDEX idx_coordenadas (latitude, longitude),
    INDEX idx_data_consulta (data_consulta)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABELA: viagens
-- =========================================
CREATE TABLE viagens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    veiculo_id BIGINT,
    motorista_id BIGINT,
    carga_id BIGINT,
    rota_id BIGINT,
    data_saida DATETIME,
    data_inicio DATETIME COMMENT 'Momento efetivo em que a viagem começou',
    data_chegada_prevista DATETIME,
    data_chegada_real DATETIME,
    status VARCHAR(50) DEFAULT 'PLANEJADA' 
        COMMENT 'PLANEJADA, EM_ANDAMENTO, FINALIZADA, CANCELADA',
    observacoes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_viagem_veiculo
        FOREIGN KEY (veiculo_id)
        REFERENCES veiculos(id)
        ON DELETE SET NULL,
        
    CONSTRAINT fk_viagem_motorista
        FOREIGN KEY (motorista_id)
        REFERENCES motoristas(id)
        ON DELETE SET NULL,
        
    CONSTRAINT fk_viagem_carga
        FOREIGN KEY (carga_id)
        REFERENCES cargas(id)
        ON DELETE SET NULL,
        
    CONSTRAINT fk_viagem_rota
        FOREIGN KEY (rota_id)
        REFERENCES rotas(id)
        ON DELETE SET NULL,
        
    INDEX idx_viagem_veiculo (veiculo_id),
    INDEX idx_viagem_motorista (motorista_id),
    INDEX idx_viagem_status (status),
    INDEX idx_viagem_datas (data_saida, data_chegada_prevista),
    INDEX idx_viagem_inicio (data_inicio)
    
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABELA: telemetria
-- =========================================
CREATE TABLE telemetria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    veiculo_id BIGINT NOT NULL,
    viagem_id BIGINT,

    -- Localização
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    velocidade DOUBLE DEFAULT 0,
    odometro DOUBLE DEFAULT 0,
    altitude DOUBLE,
    precisao_gps DOUBLE,
    satelites INT,
    direcao DOUBLE,
    aceleracao DOUBLE,
    inclinacao DOUBLE,

    -- Sensores
    ignicao BOOLEAN DEFAULT FALSE,
    nivel_combustivel DOUBLE,
    temperatura_motor DOUBLE,
    pressao_oleo DOUBLE,
    consumo_combustivel DOUBLE,
    tensao_bateria DOUBLE,
    carga_motor DOUBLE,
    torque_motor DOUBLE,

    -- Comportamento
    rpm DOUBLE DEFAULT 0,
    frenagem_brusca BOOLEAN DEFAULT FALSE,
    numero_frenagens INT DEFAULT 0,
    numero_aceleracoes_bruscas INT DEFAULT 0,
    pontuacao_motorista INT,
    tempo_motor_ligado INT COMMENT 'Em segundos',
    tempo_ocioso INT COMMENT 'Em segundos',

    -- Eventos
    colisao_detectada BOOLEAN DEFAULT FALSE,
    excesso_velocidade BOOLEAN DEFAULT FALSE,
    geofence_violada BOOLEAN DEFAULT FALSE,
    cinto_seguranca BOOLEAN DEFAULT TRUE,
    porta_aberta BOOLEAN DEFAULT FALSE,

    -- Ambiente
    temperatura_externa DOUBLE,
    umidade_externa DOUBLE,
    chuva_detectada BOOLEAN DEFAULT FALSE,

    -- Dispositivo
    sinal_gsm DOUBLE,
    sinal_gps DOUBLE,
    firmware_versao VARCHAR(100),
    imei_dispositivo VARCHAR(50),

    -- Manutenção
    manutencao_pendente BOOLEAN DEFAULT FALSE,
    proxima_revisao DATETIME,
    horas_motor DOUBLE COMMENT 'Horas totais de motor',
    desgaste_freio DOUBLE COMMENT 'Percentual de desgaste',

    data_hora DATETIME NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_telemetria_veiculo
        FOREIGN KEY (veiculo_id)
        REFERENCES veiculos(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_telemetria_viagem
        FOREIGN KEY (viagem_id)
        REFERENCES viagens(id)
        ON DELETE SET NULL,

    INDEX idx_telemetria_veiculo (veiculo_id),
    INDEX idx_telemetria_viagem (viagem_id),
    INDEX idx_telemetria_datahora (data_hora),
    INDEX idx_telemetria_localizacao (latitude, longitude),
    INDEX idx_telemetria_eventos (colisao_detectada, excesso_velocidade, geofence_violada)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABELA: alertas
-- =========================================
CREATE TABLE alertas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    veiculo_id BIGINT,
    motorista_id BIGINT,
    viagem_id BIGINT,
    tipo VARCHAR(50) NOT NULL,
    gravidade VARCHAR(20) NOT NULL,
    mensagem TEXT,
    latitude DOUBLE,
    longitude DOUBLE,
    velocidade DOUBLE,
    odometro DOUBLE,
    data_hora DATETIME NOT NULL,
    lido BOOLEAN DEFAULT FALSE,
    data_hora_leitura DATETIME,
    resolvido BOOLEAN DEFAULT FALSE,
    data_hora_resolucao DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_alerta_veiculo
        FOREIGN KEY (veiculo_id)
        REFERENCES veiculos(id)
        ON DELETE SET NULL,
        
    CONSTRAINT fk_alerta_motorista
        FOREIGN KEY (motorista_id)
        REFERENCES motoristas(id)
        ON DELETE SET NULL,
        
    CONSTRAINT fk_alerta_viagem
        FOREIGN KEY (viagem_id)
        REFERENCES viagens(id)
        ON DELETE SET NULL,
        
    INDEX idx_alerta_veiculo (veiculo_id),
    INDEX idx_alerta_motorista (motorista_id),
    INDEX idx_alerta_viagem (viagem_id),
    INDEX idx_alerta_data (data_hora),
    INDEX idx_alerta_lido (lido),
    INDEX idx_alerta_resolvido (resolvido),
    INDEX idx_alerta_gravidade (gravidade)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABELA: geofences
-- =========================================
CREATE TABLE geofences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    latitude_centro DOUBLE,
    longitude_centro DOUBLE,
    raio DOUBLE COMMENT 'Para círculo',
    vertices JSON COMMENT 'Para polígonos',
    ativo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_geofence_localizacao (latitude_centro, longitude_centro)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABELA: desvios_rota
-- =========================================
CREATE TABLE desvios_rota (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rota_id BIGINT NOT NULL,
    veiculo_id BIGINT NOT NULL,
    latitude_desvio DOUBLE NOT NULL,
    longitude_desvio DOUBLE NOT NULL,
    distancia_desvio DOUBLE NOT NULL COMMENT 'Distância do desvio em metros',
    data_hora_desvio DATETIME NOT NULL,
    resolvido BOOLEAN DEFAULT FALSE,
    data_hora_retorno DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_desvio_rota
        FOREIGN KEY (rota_id)
        REFERENCES rotas(id)
        ON DELETE CASCADE,
        
    CONSTRAINT fk_desvio_veiculo
        FOREIGN KEY (veiculo_id)
        REFERENCES veiculos(id)
        ON DELETE CASCADE,
        
    INDEX idx_desvio_rota (rota_id),
    INDEX idx_desvio_veiculo (veiculo_id),
    INDEX idx_desvio_resolvido (resolvido),
    INDEX idx_desvio_data (data_hora_desvio)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =========================================
-- TABELAS DE RELACIONAMENTO
-- =========================================

-- Veículos x Geofences (muitos-para-muitos)
CREATE TABLE veiculo_geofence (
    veiculo_id BIGINT,
    geofence_id BIGINT,
    ativo BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (veiculo_id, geofence_id),
    FOREIGN KEY (veiculo_id) REFERENCES veiculos(id) ON DELETE CASCADE,
    FOREIGN KEY (geofence_id) REFERENCES geofences(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- =========================================
-- ÍNDICES COMPOSTOS PARA PERFORMANCE
-- =========================================
CREATE INDEX idx_telemetria_veiculo_data ON telemetria(veiculo_id, data_hora);
CREATE INDEX idx_viagens_completo ON viagens(veiculo_id, motorista_id, status);
CREATE INDEX idx_alertas_nao_lidos ON alertas(lido, resolvido, data_hora);

-- =========================================
-- VIEWS ÚTEIS
-- =========================================

-- View de último status dos veículos
CREATE VIEW vw_ultima_telemetria AS
SELECT t1.*
FROM telemetria t1
INNER JOIN (
    SELECT veiculo_id, MAX(data_hora) as ultima_data
    FROM telemetria
    GROUP BY veiculo_id
) t2 ON t1.veiculo_id = t2.veiculo_id AND t1.data_hora = t2.ultima_data;

-- View de viagens ativas
CREATE VIEW vw_viagens_ativas AS
SELECT v.*, 
       ve.placa, 
       m.nome as motorista_nome,
       c.descricao as carga_descricao
FROM viagens v
LEFT JOIN veiculos ve ON v.veiculo_id = ve.id
LEFT JOIN motoristas m ON v.motorista_id = m.id
LEFT JOIN cargas c ON v.carga_id = c.id
WHERE v.status IN ('PLANEJADA', 'EM_ANDAMENTO');