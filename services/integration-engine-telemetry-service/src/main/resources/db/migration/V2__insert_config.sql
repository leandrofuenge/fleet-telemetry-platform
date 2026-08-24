-- Dados iniciais e ajustes incrementais para PostgreSQL 16.

ALTER TABLE veiculos ADD COLUMN IF NOT EXISTS plano VARCHAR(20) DEFAULT 'STARTER';
ALTER TABLE veiculos ADD COLUMN IF NOT EXISTS uuid VARCHAR(36);
UPDATE veiculos SET uuid = gen_random_uuid()::text WHERE uuid IS NULL;
ALTER TABLE veiculos ALTER COLUMN uuid SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_veiculos_uuid ON veiculos (uuid);

ALTER TABLE telemetria ADD COLUMN IF NOT EXISTS impreciso BOOLEAN DEFAULT FALSE;

INSERT INTO clientes (id, nome_razao_social, cnpj, email, telefone, endereco, ativo) VALUES
    (1, 'Transportadora Expresso Brasil', '12.345.678/0001-90', 'financeiro@expressobrasil.com', '(11) 3123-4567', 'Av. Paulista, 1000 - São Paulo/SP', TRUE),
    (2, 'Logística Nacional LTDA', '98.765.432/0001-21', 'contato@logisticanacional.com', '(21) 98765-4321', 'Av. Rio Branco, 500 - Rio de Janeiro/RJ', TRUE)
ON CONFLICT (id) DO UPDATE SET
    nome_razao_social = EXCLUDED.nome_razao_social,
    cnpj = EXCLUDED.cnpj,
    email = EXCLUDED.email,
    telefone = EXCLUDED.telefone,
    endereco = EXCLUDED.endereco,
    ativo = EXCLUDED.ativo;

INSERT INTO motoristas (
    id, tenant_id, nome, cpf, cnh, categoria_cnh, data_venc_cnh,
    data_venc_aso, mopp_valido, score, email, telefone, ativo
) VALUES
    (1, 1, 'João da Silva', '123.456.789-00', '12345678901', 'E', CURRENT_DATE + 180, CURRENT_DATE + 90, TRUE, 950, 'joao.silva@expressobrasil.com', '(11) 99999-1111', TRUE),
    (2, 1, 'Maria Oliveira', '234.567.890-11', '23456789012', 'D', CURRENT_DATE + 240, CURRENT_DATE + 120, FALSE, 880, 'maria.oliveira@expressobrasil.com', '(11) 98888-2222', TRUE),
    (3, 2, 'Carlos Souza', '345.678.901-22', '34567890123', 'E', CURRENT_DATE + 150, CURRENT_DATE + 60, TRUE, 920, 'carlos.souza@logisticanacional.com', '(21) 97777-3333', TRUE)
ON CONFLICT (id) DO UPDATE SET
    nome = EXCLUDED.nome,
    cpf = EXCLUDED.cpf,
    cnh = EXCLUDED.cnh,
    categoria_cnh = EXCLUDED.categoria_cnh,
    data_venc_cnh = EXCLUDED.data_venc_cnh,
    data_venc_aso = EXCLUDED.data_venc_aso,
    mopp_valido = EXCLUDED.mopp_valido,
    score = EXCLUDED.score,
    email = EXCLUDED.email,
    telefone = EXCLUDED.telefone,
    ativo = EXCLUDED.ativo;

INSERT INTO veiculos (
    id, tenant_id, placa, modelo, marca, capacidade_carga, ano_fabricacao, ativo,
    cliente_id, motorista_atual_id, pbt_kg, tacografo_obrigatorio,
    data_venc_tacografo, data_venc_crlv, data_venc_seguro, data_venc_dpvat,
    data_venc_rcf, data_venc_vistoria, data_venc_rntrc, plano, uuid
) VALUES
    (1, 1, 'BRA2E19', 'FH 540', 'Volvo', 25000, 2022, TRUE, NULL, 1, 6000, TRUE,
     CURRENT_DATE + 90, CURRENT_DATE + 180, CURRENT_DATE + 120, CURRENT_DATE + 150,
     CURRENT_DATE + 200, CURRENT_DATE + 250, CURRENT_DATE + 300, 'STARTER', '00000000-0000-0000-0000-000000000001'),
    (2, 1, 'DEF2E34', 'Axor', 'Mercedes-Benz', 28000, 2021, TRUE, NULL, 2, 7000, TRUE,
     CURRENT_DATE + 45, CURRENT_DATE + 150, CURRENT_DATE + 90, CURRENT_DATE + 120,
     CURRENT_DATE + 180, CURRENT_DATE + 220, CURRENT_DATE + 270, 'PRO', '00000000-0000-0000-0000-000000000002'),
    (3, 2, 'GHI3F45', 'Constellation', 'Volkswagen', 22000, 2023, TRUE, NULL, 3, 5000, TRUE,
     CURRENT_DATE + 30, CURRENT_DATE + 100, CURRENT_DATE + 60, CURRENT_DATE + 80,
     CURRENT_DATE + 140, CURRENT_DATE + 180, CURRENT_DATE + 210, 'ENTERPRISE', '00000000-0000-0000-0000-000000000003'),
    (4, 1, 'JKL4G56', 'TGS 33.480', 'MAN', 32000, 2020, TRUE, NULL, NULL, 8500, TRUE,
     CURRENT_DATE + 60, CURRENT_DATE + 200, CURRENT_DATE + 110, CURRENT_DATE + 130,
     CURRENT_DATE + 190, CURRENT_DATE + 240, CURRENT_DATE + 280, 'STARTER', '00000000-0000-0000-0000-000000000004'),
    (5, 2, 'MNO5H67', 'R 500', 'Scania', 35000, 2022, TRUE, NULL, NULL, 10000, TRUE,
     CURRENT_DATE + 75, CURRENT_DATE + 220, CURRENT_DATE + 130, CURRENT_DATE + 160,
     CURRENT_DATE + 210, CURRENT_DATE + 260, CURRENT_DATE + 310, 'PRO', '00000000-0000-0000-0000-000000000005')
ON CONFLICT (id) DO UPDATE SET
    modelo = EXCLUDED.modelo,
    marca = EXCLUDED.marca,
    capacidade_carga = EXCLUDED.capacidade_carga,
    ano_fabricacao = EXCLUDED.ano_fabricacao,
    ativo = EXCLUDED.ativo,
    pbt_kg = EXCLUDED.pbt_kg,
    tacografo_obrigatorio = EXCLUDED.tacografo_obrigatorio,
    plano = EXCLUDED.plano,
    uuid = EXCLUDED.uuid;

INSERT INTO veiculos_cache (
    id, uuid, tenant_id, placa, modelo, marca, tipo_veiculo,
    capacidade_carga_kg, pbt_kg, ativo
)
SELECT
    id, uuid, tenant_id, placa, modelo, marca, 'CAMINHAO_PESADO',
    capacidade_carga, pbt_kg, ativo
FROM veiculos
ON CONFLICT (id) DO UPDATE SET
    uuid = EXCLUDED.uuid,
    tenant_id = EXCLUDED.tenant_id,
    placa = EXCLUDED.placa,
    modelo = EXCLUDED.modelo,
    marca = EXCLUDED.marca,
    capacidade_carga_kg = EXCLUDED.capacidade_carga_kg,
    pbt_kg = EXCLUDED.pbt_kg,
    ativo = EXCLUDED.ativo;

INSERT INTO dispositivos_iot (
    device_id, imei, tenant_id, veiculo_id, tipo, fabricante, modelo_hw,
    versao_firmware, status, tecnologia_rede, freq_envio_s
) VALUES
    ('DEV-001', '123456789012345', 1, 1, 'PRINCIPAL', 'ZTE', 'MG100', 'v2.1.0', 'ATIVO', '4G', 5),
    ('DEV-002', '234567890123456', 1, 2, 'PRINCIPAL', 'ZTE', 'MG100', 'v2.1.0', 'ATIVO', '5G', 5),
    ('DEV-003', '345678901234567', 2, 3, 'PRINCIPAL', 'Huawei', 'ME909s', 'v1.8.2', 'ATIVO', '4G', 5),
    ('DEV-004', '456789012345678', 1, 4, 'PRINCIPAL', 'Quectel', 'BG96', 'v2.0.1', 'ATIVO', 'LTE', 5),
    ('DEV-005', '567890123456789', 2, 5, 'PRINCIPAL', 'Sierra Wireless', 'WP7607', 'v3.2.0', 'ATIVO', '4G', 5)
ON CONFLICT (device_id) DO UPDATE SET
    veiculo_id = EXCLUDED.veiculo_id,
    status = EXCLUDED.status,
    versao_firmware = EXCLUDED.versao_firmware;

INSERT INTO geofences (
    uuid, tenant_id, nome, tipo, latitude_centro, longitude_centro,
    raio, vertices, tipo_alerta, aplica_todos, ativo
) VALUES
    ('10000000-0000-0000-0000-000000000001', 1, 'Área de Descanso - SP', 'CIRCULO', -23.48, -46.55, 500, NULL, 'AMBOS', TRUE, TRUE),
    ('10000000-0000-0000-0000-000000000002', 1, 'Pedágio - Bandeirantes', 'CIRCULO', -23.38, -46.45, 200, NULL, 'ENTRADA', TRUE, TRUE),
    ('10000000-0000-0000-0000-000000000003', 2, 'Porto de Santos', 'POLIGONO', NULL, NULL, NULL, '[{"lat":-23.52,"lng":-46.62},{"lat":-23.51,"lng":-46.61},{"lat":-23.50,"lng":-46.62},{"lat":-23.51,"lng":-46.63}]', 'AMBOS', TRUE, TRUE)
ON CONFLICT (uuid) DO UPDATE SET nome = EXCLUDED.nome;

INSERT INTO rotas (
    nome, origem, latitude_origem, longitude_origem, destino,
    latitude_destino, longitude_destino, distancia_prevista,
    tempo_previsto, status, ativa
) VALUES
    ('São Paulo - Campinas', 'São Paulo', -23.5505, -46.6333, 'Campinas', -22.9500, -45.9500, 97.97, 90, 'ATIVA', TRUE);

INSERT INTO viagens (
    status, data_saida, data_chegada_prevista, veiculo_id,
    motorista_id, rota_id, score_viagem
) VALUES
    ('PLANEJADA', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '2 hours', 1, 1, 1, 1000),
    ('PLANEJADA', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '3 hours', 2, 2, 1, 1000);

INSERT INTO usuarios (
    login, senha, nome, email, cpf, perfil, ativo, data_expiracao_senha
) VALUES
    ('admin', '$2a$12$9Pq5JvHh9Zm9z5v2Fq6w1O7Fj7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f', 'Administrador', 'admin@telemetria.com', '111.111.111-11', 'ADMIN', TRUE, CURRENT_DATE + 90),
    ('joao.silva', '$2a$12$9Pq5JvHh9Zm9z5v2Fq6w1O7Fj7f7f7f7f7f7f7f7f7f7f7f7f7f7f7f', 'João Silva', 'joao.silva@expressobrasil.com', '123.456.789-00', 'MOTORISTA', TRUE, CURRENT_DATE + 90)
ON CONFLICT (login) DO UPDATE SET
    nome = EXCLUDED.nome,
    email = EXCLUDED.email,
    perfil = EXCLUDED.perfil,
    ativo = EXCLUDED.ativo;

UPDATE usuarios SET motorista_id = 1 WHERE login = 'joao.silva';

SELECT setval(pg_get_serial_sequence('clientes', 'id'), GREATEST(MAX(id), 1), TRUE) FROM clientes;
SELECT setval(pg_get_serial_sequence('motoristas', 'id'), GREATEST(MAX(id), 1), TRUE) FROM motoristas;
SELECT setval(pg_get_serial_sequence('veiculos', 'id'), GREATEST(MAX(id), 1), TRUE) FROM veiculos;
