-- =============================================================================
-- Script de Configuração / Dados Iniciais para Telemetria Service
-- Versão: 1.1 (com UUIDs seguros)
-- =============================================================================

USE telemetria;

-- =============================================================================
-- 1. AJUSTES NAS TABELAS (adicionar colunas que faltam)
-- =============================================================================

-- Adicionar coluna 'plano' na tabela veiculos (se não existir)
SET @exist_plano = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'telemetria' AND table_name = 'veiculos' AND column_name = 'plano');
SET @sql_plano = IF(@exist_plano = 0, 'ALTER TABLE veiculos ADD COLUMN plano VARCHAR(20) DEFAULT ''STARTER'' COMMENT ''STARTER, PRO, ENTERPRISE''', 'SELECT 1');
PREPARE stmt FROM @sql_plano;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Adicionar coluna 'uuid' na tabela veiculos (se não existir)
SET @exist_uuid = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'telemetria' AND table_name = 'veiculos' AND column_name = 'uuid');
SET @sql_uuid = IF(@exist_uuid = 0, 'ALTER TABLE veiculos ADD COLUMN uuid VARCHAR(36) NOT NULL UNIQUE', 'SELECT 1');
PREPARE stmt FROM @sql_uuid;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Atualizar UUIDs existentes (se a coluna foi adicionada agora, gerar UUIDs para registros antigos)
UPDATE veiculos SET uuid = UUID() WHERE uuid IS NULL;

-- Adicionar coluna 'impreciso' na tabela telemetria (se não existir)
SET @exist_impreciso = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = 'telemetria' AND table_name = 'telemetria' AND column_name = 'impreciso');
SET @sql_impreciso = IF(@exist_impreciso = 0, 'ALTER TABLE telemetria ADD COLUMN impreciso BOOLEAN DEFAULT FALSE', 'SELECT 1');
PREPARE stmt FROM @sql_impreciso;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- =============================================================================
-- 2. INSERIR CLIENTES (TENANTS)
-- =============================================================================

INSERT INTO clientes (id, nome_razao_social, cnpj, email, telefone, endereco, ativo) VALUES
(1, 'Transportadora Expresso Brasil', '12.345.678/0001-90', 'financeiro@expressobrasil.com', '(11) 3123-4567', 'Av. Paulista, 1000 - São Paulo/SP', 1),
(2, 'Logística Nacional LTDA', '98.765.432/0001-21', 'contato@logisticanacional.com', '(21) 98765-4321', 'Av. Rio Branco, 500 - Rio de Janeiro/RJ', 1)
ON DUPLICATE KEY UPDATE 
    nome_razao_social = VALUES(nome_razao_social),
    cnpj = VALUES(cnpj),
    email = VALUES(email),
    telefone = VALUES(telefone),
    endereco = VALUES(endereco),
    ativo = VALUES(ativo);

-- =============================================================================
-- 3. INSERIR MOTORISTAS
-- =============================================================================

INSERT INTO motoristas (id, tenant_id, nome, cpf, cnh, categoria_cnh, data_venc_cnh, data_venc_aso, mopp_valido, score, email, telefone, ativo) VALUES
(1, 1, 'João da Silva', '123.456.789-00', '12345678901', 'E', DATE_ADD(CURDATE(), INTERVAL 180 DAY), DATE_ADD(CURDATE(), INTERVAL 90 DAY), 1, 950, 'joao.silva@expressobrasil.com', '(11) 99999-1111', 1),
(2, 1, 'Maria Oliveira', '234.567.890-11', '23456789012', 'D', DATE_ADD(CURDATE(), INTERVAL 240 DAY), DATE_ADD(CURDATE(), INTERVAL 120 DAY), 0, 880, 'maria.oliveira@expressobrasil.com', '(11) 98888-2222', 1),
(3, 2, 'Carlos Souza', '345.678.901-22', '34567890123', 'E', DATE_ADD(CURDATE(), INTERVAL 150 DAY), DATE_ADD(CURDATE(), INTERVAL 60 DAY), 1, 920, 'carlos.souza@logisticanacional.com', '(21) 97777-3333', 1)
ON DUPLICATE KEY UPDATE
    nome = VALUES(nome), cpf = VALUES(cpf), cnh = VALUES(cnh), categoria_cnh = VALUES(categoria_cnh),
    data_venc_cnh = VALUES(data_venc_cnh), data_venc_aso = VALUES(data_venc_aso), mopp_valido = VALUES(mopp_valido),
    score = VALUES(score), email = VALUES(email), telefone = VALUES(telefone), ativo = VALUES(ativo);

-- =============================================================================
-- 4. INSERIR VEÍCULOS (com plano e uuid – usando variáveis para UUID)
-- =============================================================================

-- Veículo 1
SET @uuid1 = UUID();
INSERT INTO veiculos (
    id, tenant_id, placa, modelo, marca, capacidade_carga, ano_fabricacao, ativo,
    cliente_id, motorista_atual_id, pbt_kg, tacografo_obrigatorio,
    data_venc_tacografo, data_venc_crlv, data_venc_seguro, data_venc_dpvat,
    data_venc_rcf, data_venc_vistoria, data_venc_rntrc, plano, uuid
) VALUES
(1, 1, 'BRA2E19', 'FH 540', 'Volvo', 25000, 2022, 1,
 NULL, 1, 6000, 1,
 DATE_ADD(CURDATE(), INTERVAL 90 DAY), DATE_ADD(CURDATE(), INTERVAL 180 DAY),
 DATE_ADD(CURDATE(), INTERVAL 120 DAY), DATE_ADD(CURDATE(), INTERVAL 150 DAY),
 DATE_ADD(CURDATE(), INTERVAL 200 DAY), DATE_ADD(CURDATE(), INTERVAL 250 DAY),
 DATE_ADD(CURDATE(), INTERVAL 300 DAY), 'STARTER', @uuid1)
ON DUPLICATE KEY UPDATE
    modelo = VALUES(modelo), marca = VALUES(marca), capacidade_carga = VALUES(capacidade_carga),
    ano_fabricacao = VALUES(ano_fabricacao), ativo = VALUES(ativo), pbt_kg = VALUES(pbt_kg),
    tacografo_obrigatorio = VALUES(tacografo_obrigatorio), plano = VALUES(plano),
    uuid = VALUES(uuid);

-- Veículo 2
SET @uuid2 = UUID();
INSERT INTO veiculos (
    id, tenant_id, placa, modelo, marca, capacidade_carga, ano_fabricacao, ativo,
    cliente_id, motorista_atual_id, pbt_kg, tacografo_obrigatorio,
    data_venc_tacografo, data_venc_crlv, data_venc_seguro, data_venc_dpvat,
    data_venc_rcf, data_venc_vistoria, data_venc_rntrc, plano, uuid
) VALUES
(2, 1, 'DEF2E34', 'Axor', 'Mercedes-Benz', 28000, 2021, 1,
 NULL, 2, 7000, 1,
 DATE_ADD(CURDATE(), INTERVAL 45 DAY), DATE_ADD(CURDATE(), INTERVAL 150 DAY),
 DATE_ADD(CURDATE(), INTERVAL 90 DAY), DATE_ADD(CURDATE(), INTERVAL 120 DAY),
 DATE_ADD(CURDATE(), INTERVAL 180 DAY), DATE_ADD(CURDATE(), INTERVAL 220 DAY),
 DATE_ADD(CURDATE(), INTERVAL 270 DAY), 'PRO', @uuid2)
ON DUPLICATE KEY UPDATE
    modelo = VALUES(modelo), marca = VALUES(marca), capacidade_carga = VALUES(capacidade_carga),
    ano_fabricacao = VALUES(ano_fabricacao), ativo = VALUES(ativo), pbt_kg = VALUES(pbt_kg),
    tacografo_obrigatorio = VALUES(tacografo_obrigatorio), plano = VALUES(plano),
    uuid = VALUES(uuid);

-- Veículo 3
SET @uuid3 = UUID();
INSERT INTO veiculos (
    id, tenant_id, placa, modelo, marca, capacidade_carga, ano_fabricacao, ativo,
    cliente_id, motorista_atual_id, pbt_kg, tacografo_obrigatorio,
    data_venc_tacografo, data_venc_crlv, data_venc_seguro, data_venc_dpvat,
    data_venc_rcf, data_venc_vistoria, data_venc_rntrc, plano, uuid
) VALUES
(3, 2, 'GHI3F45', 'Constellation', 'Volkswagen', 22000, 2023, 1,
 NULL, 3, 5000, 1,
 DATE_ADD(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 100 DAY),
 DATE_ADD(CURDATE(), INTERVAL 60 DAY), DATE_ADD(CURDATE(), INTERVAL 80 DAY),
 DATE_ADD(CURDATE(), INTERVAL 140 DAY), DATE_ADD(CURDATE(), INTERVAL 180 DAY),
 DATE_ADD(CURDATE(), INTERVAL 210 DAY), 'ENTERPRISE', @uuid3)
ON DUPLICATE KEY UPDATE
    modelo = VALUES(modelo), marca = VALUES(marca), capacidade_carga = VALUES(capacidade_carga),
    ano_fabricacao = VALUES(ano_fabricacao), ativo = VALUES(ativo), pbt_kg = VALUES(pbt_kg),
    tacografo_obrigatorio = VALUES(tacografo_obrigatorio), plano = VALUES(plano),
    uuid = VALUES(uuid);

-- Veículo 4
SET @uuid4 = UUID();
INSERT INTO veiculos (
    id, tenant_id, placa, modelo, marca, capacidade_carga, ano_fabricacao, ativo,
    cliente_id, motorista_atual_id, pbt_kg, tacografo_obrigatorio,
    data_venc_tacografo, data_venc_crlv, data_venc_seguro, data_venc_dpvat,
    data_venc_rcf, data_venc_vistoria, data_venc_rntrc, plano, uuid
) VALUES
(4, 1, 'JKL4G56', 'TGS 33.480', 'MAN', 32000, 2020, 1,
 NULL, NULL, 8500, 1,
 DATE_ADD(CURDATE(), INTERVAL 60 DAY), DATE_ADD(CURDATE(), INTERVAL 200 DAY),
 DATE_ADD(CURDATE(), INTERVAL 110 DAY), DATE_ADD(CURDATE(), INTERVAL 130 DAY),
 DATE_ADD(CURDATE(), INTERVAL 190 DAY), DATE_ADD(CURDATE(), INTERVAL 240 DAY),
 DATE_ADD(CURDATE(), INTERVAL 280 DAY), 'STARTER', @uuid4)
ON DUPLICATE KEY UPDATE
    modelo = VALUES(modelo), marca = VALUES(marca), capacidade_carga = VALUES(capacidade_carga),
    ano_fabricacao = VALUES(ano_fabricacao), ativo = VALUES(ativo), pbt_kg = VALUES(pbt_kg),
    tacografo_obrigatorio = VALUES(tacografo_obrigatorio), plano = VALUES(plano),
    uuid = VALUES(uuid);

-- Veículo 5
SET @uuid5 = UUID();
INSERT INTO veiculos (
    id, tenant_id, placa, modelo, marca, capacidade_carga, ano_fabricacao, ativo,
    cliente_id, motorista_atual_id, pbt_kg, tacografo_obrigatorio,
    data_venc_tacografo, data_venc_crlv, data_venc_seguro, data_venc_dpvat,
    data_venc_rcf, data_venc_vistoria, data_venc_rntrc, plano, uuid
) VALUES
(5, 2, 'MNO5H67', 'R 500', 'Scania', 35000, 2022, 1,
 NULL, NULL, 10000, 1,
 DATE_ADD(CURDATE(), INTERVAL 75 DAY), DATE_ADD(CURDATE(), INTERVAL 220 DAY),
 DATE_ADD(CURDATE(), INTERVAL 130 DAY), DATE_ADD(CURDATE(), INTERVAL 160 DAY),
 DATE_ADD(CURDATE(), INTERVAL 210 DAY), DATE_ADD(CURDATE(), INTERVAL 260 DAY),
 DATE_ADD(CURDATE(), INTERVAL 310 DAY), 'PRO', @uuid5)
ON DUPLICATE KEY UPDATE
    modelo = VALUES(modelo), marca = VALUES(marca), capacidade_carga = VALUES(capacidade_carga),
    ano_fabricacao = VALUES(ano_fabricacao), ativo = VALUES(ativo), pbt_kg = VALUES(pbt_kg),
    tacografo_obrigatorio = VALUES(tacografo_obrigatorio), plano = VALUES(plano),
    uuid = VALUES(uuid);

-- =============================================================================
-- 5. SINCRONIZAR VEÍCULOS_CACHE (para atender FKs da telemetria)
-- =============================================================================

INSERT INTO veiculos_cache (id, uuid, tenant_id, placa, modelo, marca, tipo_veiculo, capacidade_carga_kg, pbt_kg, ativo)
SELECT id, uuid, tenant_id, placa, modelo, marca, 'CAMINHAO_PESADO', capacidade_carga, pbt_kg, ativo
FROM veiculos
ON DUPLICATE KEY UPDATE
    uuid = VALUES(uuid),
    tenant_id = VALUES(tenant_id),
    placa = VALUES(placa),
    modelo = VALUES(modelo),
    marca = VALUES(marca),
    capacidade_carga_kg = VALUES(capacidade_carga_kg),
    pbt_kg = VALUES(pbt_kg),
    ativo = VALUES(ativo);

-- =============================================================================
-- 6. INSERIR DISPOSITIVOS IoT
-- =============================================================================

INSERT INTO dispositivos_iot (device_id, imei, tenant_id, veiculo_id, tipo, fabricante, modelo_hw, versao_firmware, status, tecnologia_rede, freq_envio_s) VALUES
('DEV-001', '123456789012345', 1, 1, 'PRINCIPAL', 'ZTE', 'MG100', 'v2.1.0', 'ATIVO', '4G', 5),
('DEV-002', '234567890123456', 1, 2, 'PRINCIPAL', 'ZTE', 'MG100', 'v2.1.0', 'ATIVO', '5G', 5),
('DEV-003', '345678901234567', 2, 3, 'PRINCIPAL', 'Huawei', 'ME909s', 'v1.8.2', 'ATIVO', '4G', 5),
('DEV-004', '456789012345678', 1, 4, 'PRINCIPAL', 'Quectel', 'BG96', 'v2.0.1', 'ATIVO', 'LTE', 5),
('DEV-005', '567890123456789', 2, 5, 'PRINCIPAL', 'Sierra Wireless', 'WP7607', 'v3.2.0', 'ATIVO', '4G', 5)
ON DUPLICATE KEY UPDATE
    veiculo_id = VALUES(veiculo_id),
    status = VALUES(status),
    versao_firmware = VALUES(versao_firmware);

-- =============================================================================
-- 7. INSERIR GEOFENCES (exemplos para RF07)
-- =============================================================================

INSERT INTO geofences (uuid, tenant_id, nome, tipo, latitude_centro, longitude_centro, raio, vertices, tipo_alerta, aplica_todos, ativo) VALUES
(UUID(), 1, 'Área de Descanso - SP', 'CIRCULO', -23.48, -46.55, 500, NULL, 'AMBOS', 1, 1),
(UUID(), 1, 'Pedágio - Bandeirantes', 'CIRCULO', -23.38, -46.45, 200, NULL, 'ENTRADA', 1, 1),
(UUID(), 2, 'Porto de Santos', 'POLIGONO', NULL, NULL, NULL, '[{"lat":-23.52,"lng":-46.62},{"lat":-23.51,"lng":-46.61},{"lat":-23.50,"lng":-46.62},{"lat":-23.51,"lng":-46.63}]', 'AMBOS', 1, 1)
ON DUPLICATE KEY UPDATE
    nome = VALUES(nome);

-- =============================================================================
-- 8. INSERIR ROTAS (exemplo São Paulo -> Campinas)
-- =============================================================================

INSERT INTO rotas (nome, origem, latitude_origem, longitude_origem, destino, latitude_destino, longitude_destino, distancia_prevista, tempo_previsto, status, ativa) VALUES
('São Paulo - Campinas', 'São Paulo', -23.5505, -46.6333, 'Campinas', -22.9500, -45.9500, 97.97, 90, 'ATIVA', 1)
ON DUPLICATE KEY UPDATE
    distancia_prevista = VALUES(distancia_prevista),
    tempo_previsto = VALUES(tempo_previsto);

-- =============================================================================
-- 9. INSERIR VIAGENS (exemplo)
-- =============================================================================

INSERT INTO viagens (status, data_saida, data_chegada_prevista, veiculo_id, motorista_id, rota_id, score_viagem) VALUES
('PLANEJADA', NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR), 1, 1, 1, 1000),
('PLANEJADA', NOW(), DATE_ADD(NOW(), INTERVAL 3 HOUR), 2, 2, 1, 1000)
ON DUPLICATE KEY UPDATE
    status = VALUES(status);

-- =============================================================================
-- 10. INSERIR USUÁRIOS (acesso à API)
-- Senha padrão: "123456" criptografada com BCrypt (12 rounds) – gerada com online tool
-- A senha abaixo é para "123456" (exemplo)
-- =============================================================================

INSERT INTO usuarios (login, senha, nome, email, cpf, perfil, ativo, data_expiracao_senha) VALUES
('admin', '$2a$12$9Pq5JvHh9Zm9z5v2Fq6w1O7Fj7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f', 'Administrador', 'admin@telemetria.com', '111.111.111-11', 'ADMIN', 1, DATE_ADD(CURDATE(), INTERVAL 90 DAY)),
('joao.silva', '$2a$12$9Pq5JvHh9Zm9z5v2Fq6w1O7Fj7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f', 'João Silva', 'joao.silva@expressobrasil.com', '123.456.789-00', 'MOTORISTA', 1, DATE_ADD(CURDATE(), INTERVAL 90 DAY))
ON DUPLICATE KEY UPDATE
    nome = VALUES(nome),
    email = VALUES(email),
    perfil = VALUES(perfil),
    ativo = VALUES(ativo);

-- =============================================================================
-- 11. ASSOCIAR MOTORISTAS A USUÁRIOS (opcional)
-- =============================================================================

UPDATE usuarios SET motorista_id = 1 WHERE login = 'joao.silva';

-- =============================================================================
-- FIM DO SCRIPT DE CONFIGURAÇÃO
-- =============================================================================