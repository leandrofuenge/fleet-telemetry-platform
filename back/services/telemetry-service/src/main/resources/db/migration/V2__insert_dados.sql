-- =============================================================================
-- INSERT DE DADOS PARA TELEMETRY SERVICE
-- =============================================================================

USE telemetria;

-- =============================================================================
-- 1. CLIENTES
-- =============================================================================
INSERT INTO clientes (nome_razao_social, cnpj, email, telefone, endereco, ativo) VALUES
('Transportadora Pantanal Ltda', '12.345.678/0001-90', 'contato@pantanal.com.br', '(65) 3456-7890', 'Av. Historiador Rubens de Mendonça, 1500 - Cuiabá/MT', TRUE),
('Agro Norte Logística S/A', '23.456.789/0001-01', 'financeiro@agronorte.com', '(65) 9876-5432', 'Rua Comandante Costa, 800 - Rondonópolis/MT', TRUE),
('Frota Centro-Oeste Ltda', '34.567.890/0001-12', 'operacoes@frotacentro.com', '(67) 3456-7890', 'Av. Afonso Pena, 2000 - Campo Grande/MS', TRUE),
('Distribuidora Sudeste', '45.678.901/0001-23', 'logistica@distribsudeste.com', '(11) 3232-4455', 'Av. Paulista, 1000 - São Paulo/SP', TRUE),
('Transportes Sul', '56.789.012/0001-34', 'sul@transportes.com', '(51) 3333-4444', 'Av. Ipiranga, 500 - Porto Alegre/RS', TRUE);

-- =============================================================================
-- 2. MOTORISTAS
-- =============================================================================
INSERT INTO motoristas (nome, cpf, cnh, categoria_cnh, email, telefone, ativo) VALUES
('Carlos Ferreira', '123.456.789-00', '12345678901', 'E', 'carlos.ferreira@email.com', '(65) 98765-4321', TRUE),
('Maria Aparecida', '234.567.890-11', '23456789012', 'D', 'maria.aparecida@email.com', '(65) 97654-3210', TRUE),
('José Roberto', '345.678.901-22', '34567890123', 'E', 'jose.roberto@email.com', '(66) 96543-2109', TRUE),
('Ana Carolina', '456.789.012-33', '45678901234', 'D', 'ana.carolina@email.com', '(67) 95432-1098', TRUE),
('Paulo Sérgio', '567.890.123-44', '56789012345', 'E', 'paulo.sergio@email.com', '(11) 94321-0987', TRUE),
('Fernanda Lima', '678.901.234-55', '67890123456', 'D', 'fernanda.lima@email.com', '(21) 93210-9876', TRUE),
('Roberto Alves', '789.012.345-66', '78901234567', 'E', 'roberto.alves@email.com', '(51) 92109-8765', TRUE);

-- =============================================================================
-- 3. VEÍCULOS
-- =============================================================================
INSERT INTO veiculos (placa, modelo, marca, capacidade_carga, ano_fabricacao, ativo, cliente_id, motorista_atual_id) VALUES
('MTX1A23', 'FH 540', 'Volvo', 28000, 2023, TRUE, 1, 1),
('MTX2B34', 'Actros 2651', 'Mercedes-Benz', 32000, 2022, TRUE, 1, 2),
('MTX3C45', 'Scania R500', 'Scania', 30000, 2023, TRUE, 2, 3),
('MTX4D56', 'XFX 105.460', 'DAF', 29000, 2021, TRUE, 2, 4),
('MSX5E67', 'Constellation', 'Volkswagen', 26000, 2023, TRUE, 3, 5),
('MSX6F78', 'S-Way', 'Iveco', 27000, 2022, TRUE, 3, 6),
('SPX7G89', 'FH 460', 'Volvo', 26000, 2021, TRUE, 4, 7),
('SPX8H90', 'Actros 2653', 'Mercedes-Benz', 34000, 2023, TRUE, 4, 1),
('RSX9I01', 'Scania R450', 'Scania', 28000, 2022, TRUE, 5, 2),
('RSX0J12', 'XFX 105.410', 'DAF', 25000, 2021, TRUE, 5, 3);

-- =============================================================================
-- 4. VEÍCULOS CACHE
-- =============================================================================
INSERT INTO veiculos_cache (id, uuid, tenant_id, placa, modelo, marca, tipo_veiculo, consumo_medio, capacidade_carga_kg, device_id, device_imei, pbt_kg, ativo) VALUES
(1,  UUID(), 1, 'MTX1A23', 'FH 540',         'Volvo',         'CAMINHAO_PESADO',    2.5, 28000, 'DEV001', '123456789012345', 58000, TRUE),
(2,  UUID(), 1, 'MTX2B34', 'Actros 2651',    'Mercedes-Benz', 'CAMINHAO_PESADO',    2.3, 32000, 'DEV002', '234567890123456', 62000, TRUE),
(3,  UUID(), 1, 'MTX3C45', 'Scania R500',    'Scania',        'CAMINHAO_PESADO',    2.4, 30000, 'DEV003', '345678901234567', 60000, TRUE),
(4,  UUID(), 1, 'MTX4D56', 'XFX 105.460',   'DAF',           'CAMINHAO_PESADO',    2.6, 29000, 'DEV004', '456789012345678', 59000, TRUE),
(5,  UUID(), 2, 'MSX5E67', 'Constellation', 'Volkswagen',    'CAMINHAO_SEMIPESADO', 3.0, 26000, 'DEV005', '567890123456789', 56000, TRUE),
(6,  UUID(), 2, 'MSX6F78', 'S-Way',         'Iveco',         'CAMINHAO_SEMIPESADO', 2.9, 27000, 'DEV006', '678901234567890', 57000, TRUE),
(7,  UUID(), 3, 'SPX7G89', 'FH 460',        'Volvo',         'CAMINHAO_PESADO',    2.4, 26000, 'DEV007', '789012345678901', 56000, TRUE),
(8,  UUID(), 3, 'SPX8H90', 'Actros 2653',   'Mercedes-Benz', 'CAMINHAO_PESADO',    2.2, 34000, 'DEV008', '890123456789012', 64000, TRUE),
(9,  UUID(), 4, 'RSX9I01', 'Scania R450',   'Scania',        'CAMINHAO_PESADO',    2.3, 28000, 'DEV009', '901234567890123', 58000, TRUE),
(10, UUID(), 4, 'RSX0J12', 'XFX 105.410',   'DAF',           'CAMINHAO_SEMIPESADO', 2.7, 25000, 'DEV010', '012345678901234', 55000, TRUE);

-- =============================================================================
-- 5. MOTORISTAS CACHE
-- =============================================================================
INSERT INTO motoristas_cache (id, uuid, tenant_id, nome, cpf, cnh, categoria_cnh, ativo) VALUES
(1, UUID(), 1, 'Carlos Ferreira', '123.456.789-00', '12345678901', 'E', TRUE),
(2, UUID(), 1, 'Maria Aparecida', '234.567.890-11', '23456789012', 'D', TRUE),
(3, UUID(), 1, 'José Roberto',    '345.678.901-22', '34567890123', 'E', TRUE),
(4, UUID(), 2, 'Ana Carolina',    '456.789.012-33', '45678901234', 'D', TRUE),
(5, UUID(), 2, 'Paulo Sérgio',    '567.890.123-44', '56789012345', 'E', TRUE),
(6, UUID(), 3, 'Fernanda Lima',   '678.901.234-55', '67890123456', 'D', TRUE),
(7, UUID(), 4, 'Roberto Alves',   '789.012.345-66', '78901234567', 'E', TRUE);

-- =============================================================================
-- 6. CARGAS
-- =============================================================================
INSERT INTO cargas (descricao, peso_kg, tipo, volume_m3, nfe_chave, cte_chave, cliente_id) VALUES
('Soja em grãos',          28000, 'GRANEL',     45, '35200612345678901234550000000012345678901', 'CTE-2026-0001', 1),
('Milho',                  30000, 'GRANEL',     48, '35200612345678901234550000000012345678902', 'CTE-2026-0002', 1),
('Fertilizantes',          25000, 'QUIMICO',    35, '35200612345678901234550000000012345678903', 'CTE-2026-0003', 2),
('Madeira',                20000, 'GERAL',      40, '35200612345678901234550000000012345678904', 'CTE-2026-0004', 2),
('Produtos frigorificados',18000, 'REFRIGERADA',30, '35200612345678901234550000000012345678905', 'CTE-2026-0005', 3),
('Máquinas agrícolas',     15000, 'FRAGIL',     50, '35200612345678901234550000000012345678906', 'CTE-2026-0006', 3),
('Eletrônicos',             8000, 'FRAGIL',     25, '35200612345678901234550000000012345678907', 'CTE-2026-0007', 4),
('Medicamentos',            5000, 'REFRIGERADA',15, '35200612345678901234550000000012345678908', 'CTE-2026-0008', 4),
('Bebidas',                12000, 'GERAL',      30, '35200612345678901234550000000012345678909', 'CTE-2026-0009', 5),
('Peças automotivas',      10000, 'GERAL',      25, '35200612345678901234550000000012345678910', 'CTE-2026-0010', 5);

-- =============================================================================
-- 7. ROTAS
-- =============================================================================
INSERT INTO rotas (nome, origem, latitude_origem, longitude_origem, destino, latitude_destino, longitude_destino,
                  distancia_prevista, tempo_previsto, tolerancia_desvio_m, threshold_alerta_m,
                  status, ativa, data_inicio, pontos_rota, veiculo_id, motorista_id) VALUES
('Cuiabá → Rondonópolis', 'Cuiabá/MT', -15.6010, -56.0974, 'Rondonópolis/MT', -16.4719, -54.6364,
 212, 180, 100.0, 50.0, 'FINALIZADA', TRUE, '2026-03-15 08:00:00',
 '{"points":[{"lat":-15.6010,"lng":-56.0974},{"lat":-15.7200,"lng":-55.8000},{"lat":-16.0000,"lng":-55.2000},{"lat":-16.4719,"lng":-54.6364}]}', 1, 1),

('Rondonópolis → Primavera do Leste', 'Rondonópolis/MT', -16.4719, -54.6364, 'Primavera do Leste/MT', -15.5286, -54.3460,
 180, 150, 100.0, 50.0, 'FINALIZADA', TRUE, '2026-03-16 09:00:00',
 '{"points":[{"lat":-16.4719,"lng":-54.6364},{"lat":-16.2000,"lng":-54.5000},{"lat":-15.8000,"lng":-54.4000},{"lat":-15.5286,"lng":-54.3460}]}', 2, 2),

('Cáceres → Tangará da Serra', 'Cáceres/MT', -16.0726, -57.6794, 'Tangará da Serra/MT', -14.6199, -57.4926,
 220, 190, 150.0, 75.0, 'EM_ANDAMENTO', TRUE, '2026-03-17 07:30:00',
 '{"points":[{"lat":-16.0726,"lng":-57.6794},{"lat":-15.5000,"lng":-57.6000},{"lat":-14.9000,"lng":-57.5000},{"lat":-14.6199,"lng":-57.4926}]}', 3, 3),

('Sinop → Sorriso', 'Sinop/MT', -11.8642, -55.5095, 'Sorriso/MT', -12.5472, -55.7219,
 85, 70, 100.0, 50.0, 'FINALIZADA', TRUE, '2026-03-16 13:00:00',
 '{"points":[{"lat":-11.8642,"lng":-55.5095},{"lat":-12.2000,"lng":-55.6000},{"lat":-12.5472,"lng":-55.7219}]}', 4, 4),

('Barra do Garças → Água Boa', 'Barra do Garças/MT', -15.8904, -52.2567, 'Água Boa/MT', -14.0524, -52.1557,
 210, 180, 100.0, 50.0, 'PLANEJADA', TRUE, NULL,
 '{"points":[{"lat":-15.8904,"lng":-52.2567},{"lat":-15.2000,"lng":-52.2000},{"lat":-14.6000,"lng":-52.1800},{"lat":-14.0524,"lng":-52.1557}]}', 5, 5),

('São Paulo → Rio de Janeiro', 'São Paulo/SP', -23.5505, -46.6333, 'Rio de Janeiro/RJ', -22.9068, -43.1729,
 430, 360, 100.0, 50.0, 'FINALIZADA', TRUE, '2026-03-14 06:00:00',
 '{"points":[{"lat":-23.5505,"lng":-46.6333},{"lat":-23.2000,"lng":-45.8000},{"lat":-22.8000,"lng":-44.5000},{"lat":-22.9068,"lng":-43.1729}]}', 6, 6),

('Belo Horizonte → Vitória', 'Belo Horizonte/MG', -19.9167, -43.9345, 'Vitória/ES', -20.2976, -40.2958,
 520, 450, 100.0, 50.0, 'EM_ANDAMENTO', TRUE, '2026-03-17 08:00:00',
 '{"points":[{"lat":-19.9167,"lng":-43.9345},{"lat":-20.0000,"lng":-42.5000},{"lat":-20.2000,"lng":-41.0000},{"lat":-20.2976,"lng":-40.2958}]}', 7, 7),

('Curitiba → Florianópolis', 'Curitiba/PR', -25.4297, -49.2719, 'Florianópolis/SC', -27.5954, -48.5480,
 300, 250, 100.0, 50.0, 'PLANEJADA', TRUE, NULL,
 '{"points":[{"lat":-25.4297,"lng":-49.2719},{"lat":-26.0000,"lng":-48.9000},{"lat":-26.5000,"lng":-48.7000},{"lat":-27.5954,"lng":-48.5480}]}', 8, 1),

('Porto Alegre → Caxias do Sul', 'Porto Alegre/RS', -30.0346, -51.2177, 'Caxias do Sul/RS', -29.1683, -51.1794,
 130, 100, 100.0, 50.0, 'FINALIZADA', TRUE, '2026-03-15 10:00:00',
 '{"points":[{"lat":-30.0346,"lng":-51.2177},{"lat":-29.5000,"lng":-51.2000},{"lat":-29.1683,"lng":-51.1794}]}', 9, 2),

('Campo Grande → Dourados', 'Campo Grande/MS', -20.4428, -54.6464, 'Dourados/MS', -22.2211, -54.8056,
 230, 200, 100.0, 50.0, 'EM_ANDAMENTO', TRUE, '2026-03-17 09:30:00',
 '{"points":[{"lat":-20.4428,"lng":-54.6464},{"lat":-21.0000,"lng":-54.7000},{"lat":-21.5000,"lng":-54.7500},{"lat":-22.2211,"lng":-54.8056}]}', 10, 3);

-- =============================================================================
-- 8. VIAGENS
-- =============================================================================
INSERT INTO viagens (status, observacoes, data_saida, data_chegada_prevista, data_chegada_real, data_inicio,
                    distancia_real_km, km_fora_rota, score_viagem, veiculo_id, motorista_id, carga_id, rota_id) VALUES
('FINALIZADA',   'Viagem dentro do MT concluída com sucesso',    '2026-03-15 08:00:00', '2026-03-15 14:00:00', '2026-03-15 13:45:00', '2026-03-15 08:15:00', 210, 2,  990, 1,  1, 1,  1),
('FINALIZADA',   'Transporte de soja MT concluído',              '2026-03-16 09:00:00', '2026-03-16 16:00:00', '2026-03-16 15:50:00', '2026-03-16 09:10:00', 175, 5,  980, 2,  2, 2,  2),
('EM_ANDAMENTO', 'Em andamento na BR-174 sentido Tangará',       '2026-03-17 07:30:00', '2026-03-17 15:30:00', NULL,                  '2026-03-17 07:45:00', 150, 0,  950, 3,  3, 3,  3),
('FINALIZADA',   'Rota curta Sinop-Sorriso concluída',           '2026-03-16 13:00:00', '2026-03-16 15:30:00', '2026-03-16 15:20:00', '2026-03-16 13:10:00', 83,  0, 1000, 4,  4, 4,  4),
('PLANEJADA',    'Aguardando carregamento em Barra do Garças',   NULL,                  '2026-03-18 10:00:00', NULL,                  NULL,                  0,   0, 1000, 5,  5, 5,  5),
('FINALIZADA',   'Viagem SP-RJ concluída dentro do prazo',       '2026-03-14 06:00:00', '2026-03-14 16:00:00', '2026-03-14 15:30:00', '2026-03-14 06:20:00', 425, 8,  970, 6,  6, 6,  6),
('EM_ANDAMENTO', 'Indo para Vitória via BR-262',                 '2026-03-17 08:00:00', '2026-03-17 20:00:00', NULL,                  '2026-03-17 08:15:00', 320, 15, 920, 7,  7, 7,  7),
('PLANEJADA',    'Aguardando liberação para rota litorânea',     NULL,                  '2026-03-19 09:00:00', NULL,                  NULL,                  0,   0, 1000, 8,  1, 8,  8),
('FINALIZADA',   'Rota RS concluída no prazo',                   '2026-03-15 10:00:00', '2026-03-15 14:00:00', '2026-03-15 13:40:00', '2026-03-15 10:10:00', 128, 2,  995, 9,  2, 9,  9),
('EM_ANDAMENTO', 'Rota MS em andamento normal',                  '2026-03-17 09:30:00', '2026-03-17 16:30:00', NULL,                  '2026-03-17 09:45:00', 180, 10, 940, 10, 3, 10, 10);

-- =============================================================================
-- 9. TELEMETRIA
-- =============================================================================
INSERT INTO telemetria (tenant_id, veiculo_id, veiculo_uuid, motorista_id, viagem_id, device_id, imei_dispositivo,
                       latitude, longitude, altitude, velocidade, direcao, hdop, satelites, precisao_gps,
                       ignicao, rpm, carga_motor, temperatura_motor, pressao_oleo, tensao_bateria,
                       odometro, nivel_combustivel, consumo_combustivel,
                       frenagem_brusca, excesso_velocidade, pontuacao_motorista,
                       sinal_gsm, sinal_gps, tecnologia_rede, firmware_versao,
                       data_hora, recebido_em) VALUES
-- Viagem 1: Cuiabá → Rondonópolis
(1, 1, (SELECT uuid FROM veiculos_cache WHERE id=1), 1, 1, 'DEV001', '123456789012345',
 -15.6010,-56.0974, 165,  0, 0,   1.2, 10, 4.0, FALSE,    0,  0, 75, 0, 24.5,  12450, 100, 0,   FALSE, FALSE, 1000, -65, -55, '4G', '3.25.4', '2026-03-15 08:00:00', '2026-03-15 08:00:05'),
(1, 1, (SELECT uuid FROM veiculos_cache WHERE id=1), 1, 1, 'DEV001', '123456789012345',
 -15.6500,-55.9500, 180, 75, 320, 0.8, 12, 3.0, TRUE,  1450, 65, 85, 3, 25.1,  12480,  95, 25,  FALSE, FALSE,  980, -63, -53, '4G', '3.25.4', '2026-03-15 08:30:00', '2026-03-15 08:30:05'),
(1, 1, (SELECT uuid FROM veiculos_cache WHERE id=1), 1, 1, 'DEV001', '123456789012345',
 -15.8000,-55.7000, 200, 82, 315, 0.7, 14, 2.5, TRUE,  1550, 70, 88, 3, 25.0,  12520,  90, 28,  FALSE, FALSE,  975, -64, -52, '4G', '3.25.4', '2026-03-15 09:00:00', '2026-03-15 09:00:05'),
(1, 1, (SELECT uuid FROM veiculos_cache WHERE id=1), 1, 1, 'DEV001', '123456789012345',
 -16.1000,-55.0000, 250, 78, 310, 0.9, 11, 3.2, TRUE,  1500, 68, 87, 3, 24.9,  12580,  85, 26,  FALSE, FALSE,  978, -65, -54, '4G', '3.25.4', '2026-03-15 09:30:00', '2026-03-15 09:30:05'),
(1, 1, (SELECT uuid FROM veiculos_cache WHERE id=1), 1, 1, 'DEV001', '123456789012345',
 -16.3500,-54.7000, 280, 85, 305, 0.8, 13, 2.8, TRUE,  1600, 75, 90, 3, 24.8,  12640,  80, 30,  TRUE,  TRUE,   920, -66, -55, '4G', '3.25.4', '2026-03-15 10:00:00', '2026-03-15 10:00:05'),
(1, 1, (SELECT uuid FROM veiculos_cache WHERE id=1), 1, 1, 'DEV001', '123456789012345',
 -16.4719,-54.6364, 300,  0, 0,   1.1,  9, 4.0, FALSE,    0,  0, 75, 0, 24.5,  12680,  78,  0,  FALSE, FALSE, 1000, -65, -55, '4G', '3.25.4', '2026-03-15 13:45:00', '2026-03-15 13:45:05'),

-- Viagem 3: Cáceres → Tangará (em andamento)
(1, 3, (SELECT uuid FROM veiculos_cache WHERE id=3), 3, 3, 'DEV003', '345678901234567',
 -16.0726,-57.6794, 140,  0, 0,   1.0, 11, 3.5, FALSE,    0,  0, 72, 0, 24.2,  23420, 100,  0,  FALSE, FALSE, 1000, -68, -58, '3G', '4.12.1', '2026-03-17 07:30:00', '2026-03-17 07:30:05'),
(1, 3, (SELECT uuid FROM veiculos_cache WHERE id=3), 3, 3, 'DEV003', '345678901234567',
 -15.5000,-57.6000, 160, 72, 320, 0.9, 13, 3.0, TRUE,  1420, 62, 83, 3, 24.8,  23460,  90, 24,  FALSE, FALSE,  985, -67, -57, '3G', '4.12.1', '2026-03-17 09:30:00', '2026-03-17 09:30:05'),
(1, 3, (SELECT uuid FROM veiculos_cache WHERE id=3), 3, 3, 'DEV003', '345678901234567',
 -14.9000,-57.5000, 175, 68, 315, 1.0, 10, 3.8, TRUE,  1380, 60, 82, 3, 24.6,  23500,  18, 22,  FALSE, FALSE,  990, -68, -58, '3G', '4.12.1', '2026-03-17 12:30:00', '2026-03-17 12:30:05'),

-- Viagem 6: SP → RJ
(3, 6, (SELECT uuid FROM veiculos_cache WHERE id=6), 6, 6, 'DEV006', '678901234567890',
 -23.5505,-46.6333, 760,   0,  0,  1.0,  9, 4.0, FALSE,    0,  0, 72, 0, 24.5,  45230, 100,  0,  FALSE, FALSE, 1000, -58, -48, '4G', '2.8.6', '2026-03-14 06:00:00', '2026-03-14 06:00:05'),
(3, 6, (SELECT uuid FROM veiculos_cache WHERE id=6), 6, 6, 'DEV006', '678901234567890',
 -23.2000,-45.8000, 750,  95, 85,  0.7, 15, 2.5, TRUE,  1650, 72, 88, 3, 25.2,  45280,  95, 32,  FALSE, FALSE,  980, -57, -47, '4G', '2.8.6', '2026-03-14 08:30:00', '2026-03-14 08:30:05'),
(3, 6, (SELECT uuid FROM veiculos_cache WHERE id=6), 6, 6, 'DEV006', '678901234567890',
 -22.8000,-44.5000, 720, 105, 80,  0.8, 14, 2.8, TRUE,  1800, 80, 92, 4, 25.0,  45350,  85, 38,  FALSE, TRUE,   930, -59, -49, '4G', '2.8.6', '2026-03-14 11:00:00', '2026-03-14 11:00:05'),
(3, 6, (SELECT uuid FROM veiculos_cache WHERE id=6), 6, 6, 'DEV006', '678901234567890',
 -22.6000,-43.8000, 700,  88, 75,  0.9, 12, 3.2, TRUE,  1550, 68, 86, 3, 24.8,  45400,  80, 29,  TRUE,  FALSE,  950, -60, -50, '4G', '2.8.6', '2026-03-14 13:30:00', '2026-03-14 13:30:05'),
(3, 6, (SELECT uuid FROM veiculos_cache WHERE id=6), 6, 6, 'DEV006', '678901234567890',
 -22.9068,-43.1729,   5,   0,  0,  1.1, 10, 3.8, FALSE,    0,  0, 72, 0, 24.5,  45450,  75,  0,  FALSE, FALSE, 1000, -58, -48, '4G', '2.8.6', '2026-03-14 15:30:00', '2026-03-14 15:30:05'),

-- Viagem 7: BH → Vitória
(3, 7, (SELECT uuid FROM veiculos_cache WHERE id=7), 7, 7, 'DEV007', '789012345678901',
 -19.9167,-43.9345, 850,   0,  0,  0.9, 11, 3.5, FALSE,    0,  0, 73, 0, 24.3,  67890, 100,  0,  FALSE, FALSE, 1000, -55, -45, '4G', '2.8.6', '2026-03-17 08:00:00', '2026-03-17 08:00:05'),
(3, 7, (SELECT uuid FROM veiculos_cache WHERE id=7), 7, 7, 'DEV007', '789012345678901',
 -20.0000,-42.5000, 600,  92, 90,  0.8, 14, 2.8, TRUE,  1700, 75, 89, 3, 25.0,  67940,  92, 31,  FALSE, FALSE,  982, -54, -44, '4G', '2.8.6', '2026-03-17 10:30:00', '2026-03-17 10:30:05'),
(3, 7, (SELECT uuid FROM veiculos_cache WHERE id=7), 7, 7, 'DEV007', '789012345678901',
 -20.2000,-41.0000, 450,  98, 95,  0.7, 15, 2.5, TRUE,  1750, 78, 91, 3, 24.9,  68020,  85, 34,  TRUE,  TRUE,   910, -55, -45, '4G', '2.8.6', '2026-03-17 13:00:00', '2026-03-17 13:00:05');

-- =============================================================================
-- 10. ALERTAS
-- =============================================================================
INSERT INTO alertas (uuid, tenant_id, veiculo_id, veiculo_uuid, motorista_id, viagem_id,
                    tipo, severidade, categoria, mensagem, latitude, longitude, velocidade_kmh,
                    odometro_km, data_hora, lido, resolvido, notificacao_enviada) VALUES
(UUID(), 1, 1, (SELECT uuid FROM veiculos_cache WHERE id=1), 1, 1,
 'FRENAGEM_BRUSCA', 'MEDIO', 'COMPORTAMENTO',
 'Frenagem brusca detectada próximo a Rondonópolis na BR-364',
 -16.3500, -54.7000, 85, 12640, '2026-03-15 10:00:00', FALSE, FALSE, TRUE),

(UUID(), 1, 1, (SELECT uuid FROM veiculos_cache WHERE id=1), 1, 1,
 'EXCESSO_VELOCIDADE', 'ALTO', 'VELOCIDADE',
 'Excesso de velocidade: 85 km/h em via de 70 km/h na BR-364',
 -16.3500, -54.7000, 85, 12640, '2026-03-15 10:00:00', FALSE, FALSE, TRUE),

(UUID(), 1, 3, (SELECT uuid FROM veiculos_cache WHERE id=3), 3, 3,
 'NIVEL_COMBUSTIVEL_BAIXO', 'MEDIO', 'COMBUSTIVEL',
 'Nível de combustível crítico: 18% - abastecer urgente',
 -14.9000, -57.5000, 68, 23500, '2026-03-17 12:30:00', FALSE, FALSE, TRUE),

(UUID(), 3, 6, (SELECT uuid FROM veiculos_cache WHERE id=6), 6, 6,
 'EXCESSO_VELOCIDADE', 'ALTO', 'VELOCIDADE',
 'Excesso de velocidade: 105 km/h na Rodovia Presidente Dutra',
 -22.8000, -44.5000, 105, 45350, '2026-03-14 11:00:00', TRUE, TRUE, TRUE),

(UUID(), 3, 6, (SELECT uuid FROM veiculos_cache WHERE id=6), 6, 6,
 'FRENAGEM_BRUSCA', 'BAIXO', 'COMPORTAMENTO',
 'Frenagem brusca detectada na serra das Araras',
 -22.6000, -43.8000, 88, 45400, '2026-03-14 13:30:00', TRUE, TRUE, TRUE),

(UUID(), 3, 7, (SELECT uuid FROM veiculos_cache WHERE id=7), 7, 7,
 'EXCESSO_VELOCIDADE', 'CRITICO', 'VELOCIDADE',
 'Excesso crítico de velocidade em curva perigosa: 98 km/h na BR-262',
 -20.2000, -41.0000, 98, 68020, '2026-03-17 13:00:00', FALSE, FALSE, TRUE),

(UUID(), 3, 7, (SELECT uuid FROM veiculos_cache WHERE id=7), 7, 7,
 'FRENAGEM_BRUSCA', 'ALTO', 'COMPORTAMENTO',
 'Frenagem brusca em trecho de serra na BR-262',
 -20.2000, -41.0000, 98, 68020, '2026-03-17 13:00:00', FALSE, FALSE, TRUE),

(UUID(), 2, 5, (SELECT uuid FROM veiculos_cache WHERE id=5), 5, 5,
 'INICIO_VIAGEM', 'BAIXO', 'VIAGEM',
 'Viagem iniciada em Barra do Garças com destino Água Boa',
 -15.8904, -52.2567, 0, 0, '2026-03-18 10:00:00', TRUE, TRUE, TRUE),

(UUID(), 4, 9, (SELECT uuid FROM veiculos_cache WHERE id=9), 2, 9,
 'CHEGADA_DESTINO', 'BAIXO', 'VIAGEM',
 'Veículo chegou ao destino em Caxias do Sul dentro do prazo',
 -29.1683, -51.1794, 0, 78900, '2026-03-15 13:40:00', TRUE, TRUE, TRUE),

(UUID(), 4, 10, (SELECT uuid FROM veiculos_cache WHERE id=10), 3, 10,
 'GPS_SEM_SINAL', 'ALTO', 'GPS',
 'Veículo sem sinal GPS por 18 minutos na BR-163',
 -21.5000, -54.7500, 75, 54280, '2026-03-17 12:00:00', FALSE, FALSE, TRUE);

-- =============================================================================
-- 11. DESVIOS DE ROTA
-- =============================================================================
INSERT INTO desvios_rota (tenant_id, rota_id, veiculo_id, veiculo_uuid, viagem_id,
                         latitude_desvio, longitude_desvio, velocidade_kmh, distancia_metros,
                         lat_ponto_mais_proximo, lng_ponto_mais_proximo, nome_via_desvio,
                         data_hora_desvio, data_hora_retorno, duracao_min, km_extras,
                         alerta_enviado, resolvido, motivo) VALUES
(1, 1, 1, (SELECT uuid FROM veiculos_cache WHERE id=1), 1,
 -15.7800,-55.6500, 65, 350, -15.7700,-55.6300,
 'Estrada vicinal MT-235',
 '2026-03-15 09:15:00', '2026-03-15 09:35:00', 20, 0.35, TRUE, TRUE,
 'Desvio por obras na BR-364'),

(3, 6, 6, (SELECT uuid FROM veiculos_cache WHERE id=6), 6,
 -22.7500,-44.4200, 92, 800, -22.7200,-44.4000,
 'Rodovia Presidente Dutra',
 '2026-03-14 12:15:00', '2026-03-14 12:40:00', 25, 0.8, TRUE, TRUE,
 'Desvio para abastecimento'),

(3, 7, 7, (SELECT uuid FROM veiculos_cache WHERE id=7), 7,
 -20.1500,-41.5000, 85, 1200, -20.1000,-41.4500,
 'BR-262 sentido Vitória',
 '2026-03-17 14:20:00', NULL, NULL, 0, TRUE, FALSE,
 'Desvio não autorizado - em investigação'),

(4, 10, 10, (SELECT uuid FROM veiculos_cache WHERE id=10), 10,
 -21.2000,-54.7200, 78, 520, -21.1800,-54.7100,
 'BR-163 sentido Dourados',
 '2026-03-17 11:30:00', '2026-03-17 11:55:00', 25, 1.2, TRUE, FALSE,
 'Desvio para refeição em posto');

-- =============================================================================
-- 12. GEOFENCES
-- =============================================================================
INSERT INTO geofences (uuid, tenant_id, nome, tipo, latitude_centro, longitude_centro, raio,
                      vertices, tipo_alerta, aplica_todos, veiculos_uuid, ativo) VALUES
(UUID(), 1, 'Centro de Cuiabá',           'CIRCULO',  -15.6010, -56.0974, 5000,  NULL, 'AMBOS',   TRUE,  NULL, TRUE),
(UUID(), 1, 'Terminal Rondonópolis',       'CIRCULO',  -16.4719, -54.6364, 3000,  NULL, 'ENTRADA', TRUE,  NULL, TRUE),
(UUID(), 1, 'Área de Preservação Pantanal','POLIGONO', -15.5000, -55.5000, NULL,
 '[{"lat":-15.3000,"lng":-55.3000},{"lat":-15.3000,"lng":-55.7000},{"lat":-15.7000,"lng":-55.7000},{"lat":-15.7000,"lng":-55.3000}]',
 'SAIDA', TRUE, NULL, TRUE),
(UUID(), 2, 'Terminal Sinop',              'CIRCULO',  -11.8642, -55.5095, 2000,  NULL, 'AMBOS',   TRUE,  NULL, TRUE),
(UUID(), 3, 'Centro de São Paulo',         'CIRCULO',  -23.5505, -46.6333, 8000,  NULL, 'AMBOS',   TRUE,  NULL, TRUE),
(UUID(), 3, 'Porto do Rio de Janeiro',     'CIRCULO',  -22.9068, -43.1729, 4000,  NULL, 'ENTRADA', FALSE, NULL, TRUE),
(UUID(), 4, 'Área Industrial Caxias',      'CIRCULO',  -29.1683, -51.1794, 6000,  NULL, 'AMBOS',   TRUE,  NULL, TRUE),
(UUID(), 4, 'Terminal Dourados',           'CIRCULO',  -22.2211, -54.8056, 2500,  NULL, 'ENTRADA', TRUE,  NULL, TRUE);

-- =============================================================================
-- 13. VEICULO_GEOFENCE
-- =============================================================================
INSERT INTO veiculo_geofence (veiculo_id, geofence_id, ativo) VALUES
(1, 1, TRUE), (1, 2, TRUE), (2, 2, TRUE), (3, 3, TRUE), (4, 4, TRUE),
(6, 5, TRUE), (6, 6, TRUE), (7, 5, TRUE), (9, 7, TRUE), (10, 8, TRUE);

-- =============================================================================
-- 14. GEOCODING CACHE
-- =============================================================================
INSERT INTO geocoding_cache (lat_arred, lng_arred, pais, estado, cidade, bairro, logradouro,
                           numero, cep, nome_local, tipo_local, is_urbano,
                           precisao_metros, fonte, consulta_em, expira_em) VALUES
(-15.6010,-56.0974,'Brasil','Mato Grosso',    'Cuiabá',          'Centro',    'Av. Historiador Rubens de Mendonça','1500','78008-000','Pantanal Shopping',      'commercial',TRUE, 10,'NOMINATIM',NOW(),DATE_ADD(NOW(),INTERVAL 30 DAY)),
(-16.4719,-54.6364,'Brasil','Mato Grosso',    'Rondonópolis',    'Centro',    'Av. Lions Internacional',           '800', '78700-000','Parque de Exposições',   'tourist',   TRUE, 10,'NOMINATIM',NOW(),DATE_ADD(NOW(),INTERVAL 30 DAY)),
(-11.8642,-55.5095,'Brasil','Mato Grosso',    'Sinop',           'Centro Sul','Av. das Figueiras',                 '500', '78550-000','Terminal Rodoviário',    'transport', TRUE, 10,'NOMINATIM',NOW(),DATE_ADD(NOW(),INTERVAL 30 DAY)),
(-15.8904,-52.2567,'Brasil','Mato Grosso',    'Barra do Garças', 'Centro',    'Av. Ministro João Alberto',         '100', '78600-000','Praça Central',          'tourist',   TRUE, 10,'NOMINATIM',NOW(),DATE_ADD(NOW(),INTERVAL 30 DAY)),
(-23.5505,-46.6333,'Brasil','São Paulo',       'São Paulo',       'Centro',    'Praça da Sé',                       'S/N','01001-000','Catedral da Sé',          'tourist',   TRUE, 10,'NOMINATIM',NOW(),DATE_ADD(NOW(),INTERVAL 30 DAY)),
(-22.9068,-43.1729,'Brasil','Rio de Janeiro',  'Rio de Janeiro',  'Centro',    'Av. Rio Branco',                    '1',  '20040-020','Teatro Municipal',        'tourist',   TRUE, 10,'NOMINATIM',NOW(),DATE_ADD(NOW(),INTERVAL 30 DAY)),
(-19.9167,-43.9345,'Brasil','Minas Gerais',    'Belo Horizonte',  'Savassi',   'Av. Getúlio Vargas',                '200','30112-010','Praça da Savassi',        'commercial',TRUE, 10,'NOMINATIM',NOW(),DATE_ADD(NOW(),INTERVAL 30 DAY)),
(-30.0346,-51.2177,'Brasil','Rio Grande do Sul','Porto Alegre',   'Centro',    'Rua da Praia',                      'S/N','90010-150','Mercado Público',         'commercial',TRUE, 10,'NOMINATIM',NOW(),DATE_ADD(NOW(),INTERVAL 30 DAY)),
(-29.1683,-51.1794,'Brasil','Rio Grande do Sul','Caxias do Sul',  'Centro',    'Rua Sinimbu',                       '100','95020-000','Terminal Rodoviário',     'transport', TRUE, 10,'NOMINATIM',NOW(),DATE_ADD(NOW(),INTERVAL 30 DAY)),
(-22.2211,-54.8056,'Brasil','Mato Grosso do Sul','Dourados',      'Centro',    'Av. Presidente Vargas',             '500','79800-000','Prefeitura de Dourados',  'government',TRUE, 10,'NOMINATIM',NOW(),DATE_ADD(NOW(),INTERVAL 30 DAY));

-- =============================================================================
-- 15. POSIÇÃO ATUAL
-- =============================================================================
INSERT INTO posicao_atual (veiculo_id, tenant_id, veiculo_uuid, latitude, longitude, velocidade,
                         direcao, ignicao, status_veiculo, motorista_id, viagem_id, odometro,
                         nivel_combustivel, bateria_v, ultima_telemetria, nome_local, alertas_ativos) VALUES
(1,  1, (SELECT uuid FROM veiculos_cache WHERE id=1),  -16.4719,-54.6364,  0,   0, FALSE,'PARADO',      1, 1, 12680, 78, 24.5, '2026-03-15 13:45:00','Rondonópolis/MT',        2),
(2,  1, (SELECT uuid FROM veiculos_cache WHERE id=2),  -15.5286,-54.3460,  0,   0, FALSE,'PARADO',      2, 2, 23500, 65, 24.2, '2026-03-16 15:50:00','Primavera do Leste/MT',  0),
(3,  1, (SELECT uuid FROM veiculos_cache WHERE id=3),  -14.9000,-57.5000, 68, 315, TRUE, 'EM_MOVIMENTO', 3, 3, 23500, 18, 25.0, '2026-03-17 12:30:00','Entre Cáceres e Tangará',1),
(4,  1, (SELECT uuid FROM veiculos_cache WHERE id=4),  -11.8642,-55.5095,  0,   0, FALSE,'PARADO',      4, 4, 18340, 95, 24.8, '2026-03-16 15:20:00','Sinop/MT',               0),
(5,  2, (SELECT uuid FROM veiculos_cache WHERE id=5),  -15.8904,-52.2567,  0,   0, FALSE,'PARADO',      5, 5,  8900, 98, 24.6, '2026-03-17 07:00:00','Barra do Garças/MT',     0),
(6,  3, (SELECT uuid FROM veiculos_cache WHERE id=6),  -22.9068,-43.1729,  0,   0, FALSE,'PARADO',      6, 6, 45450, 75, 24.8, '2026-03-14 15:30:00','Rio de Janeiro/RJ',      0),
(7,  3, (SELECT uuid FROM veiculos_cache WHERE id=7),  -20.2000,-41.0000, 98,  95, TRUE, 'EM_MOVIMENTO', 7, 7, 68020, 85, 24.3, '2026-03-17 13:00:00','BR-262, ES',             2),
(8,  3, (SELECT uuid FROM veiculos_cache WHERE id=8),  -25.4297,-49.2719,  0,   0, FALSE,'PARADO',      1, 8, 31200, 88, 24.5, '2026-03-17 08:00:00','Curitiba/PR',            0),
(9,  4, (SELECT uuid FROM veiculos_cache WHERE id=9),  -29.1683,-51.1794,  0,   0, FALSE,'PARADO',      2, 9, 78900, 72, 24.1, '2026-03-15 13:40:00','Caxias do Sul/RS',       0),
(10, 4, (SELECT uuid FROM veiculos_cache WHERE id=10), -22.2211,-54.8056,  0,   0, FALSE,'PARADO',      3,10, 54320, 90, 24.1, '2026-03-17 15:30:00','Dourados/MS',            0);

-- =============================================================================
-- 16. HISTÓRICO POSIÇÃO
-- =============================================================================
INSERT INTO historico_posicao (tenant_id, veiculo_id, data_hora, latitude, longitude, velocidade, ignicao) VALUES
(1, 1,'2026-03-15 08:00:00', -15.6010,-56.0974,  0, FALSE),
(1, 1,'2026-03-15 08:30:00', -15.6500,-55.9500, 75, TRUE),
(1, 1,'2026-03-15 09:00:00', -15.8000,-55.7000, 82, TRUE),
(1, 1,'2026-03-15 09:30:00', -16.1000,-55.0000, 78, TRUE),
(1, 1,'2026-03-15 10:00:00', -16.3500,-54.7000, 85, TRUE),
(1, 1,'2026-03-15 13:45:00', -16.4719,-54.6364,  0, FALSE),
(1, 3,'2026-03-17 07:30:00', -16.0726,-57.6794,  0, FALSE),
(1, 3,'2026-03-17 09:30:00', -15.5000,-57.6000, 72, TRUE),
(1, 3,'2026-03-17 12:30:00', -14.9000,-57.5000, 68, TRUE),
(3, 6,'2026-03-14 06:00:00', -23.5505,-46.6333,  0, FALSE),
(3, 6,'2026-03-14 08:30:00', -23.2000,-45.8000, 95, TRUE),
(3, 6,'2026-03-14 11:00:00', -22.8000,-44.5000,105, TRUE),
(3, 6,'2026-03-14 13:30:00', -22.6000,-43.8000, 88, TRUE),
(3, 6,'2026-03-14 15:30:00', -22.9068,-43.1729,  0, FALSE),
(3, 7,'2026-03-17 08:00:00', -19.9167,-43.9345,  0, FALSE),
(3, 7,'2026-03-17 10:30:00', -20.0000,-42.5000, 92, TRUE),
(3, 7,'2026-03-17 13:00:00', -20.2000,-41.0000, 98, TRUE),
(3, 7,'2026-03-17 14:30:00', -20.1500,-41.5000, 85, TRUE),
(4,10,'2026-03-17 09:30:00', -20.4428,-54.6464,  0, FALSE),
(4,10,'2026-03-17 11:30:00', -21.2000,-54.7200, 78, TRUE),
(4,10,'2026-03-17 13:30:00', -21.8000,-54.7700, 82, TRUE),
(4,10,'2026-03-17 15:30:00', -22.2211,-54.8056,  0, FALSE);

-- =============================================================================
-- 17. DISPOSITIVOS IOT
-- =============================================================================
INSERT INTO dispositivos_iot (device_id, imei, iccid, tenant_id, veiculo_id, fabricante, modelo_hw,
                            versao_firmware, versao_alvo, certificado_cn, certificado_expira,
                            status_cert, status, ultima_conexao, ultimo_heartbeat,
                            ip_ultima_conexao, tecnologia_rede, rssi, freq_envio_s,
                            buffer_horas, tem_satelite, instalado_em, instalado_por) VALUES
('DEV001','123456789012345','895500000000000001',1, 1,'Teltonika','FMB920', '3.25.4','3.26.0','CN=DEV001','2027-03-15','ATIVO','ATIVO',  NOW(),                       DATE_SUB(NOW(),INTERVAL 5 MINUTE),  '187.62.10.1',  '4G',-65,30,72,FALSE,'2023-01-15','João Técnico'),
('DEV002','234567890123456','895500000000000002',1, 2,'Teltonika','FMB920', '3.25.4','3.26.0','CN=DEV002','2027-03-15','ATIVO','ATIVO',  DATE_SUB(NOW(),INTERVAL 2 HOUR), DATE_SUB(NOW(),INTERVAL 2 HOUR), '187.62.10.2',  '4G',-72,30,72,FALSE,'2022-06-20','João Técnico'),
('DEV003','345678901234567','895500000000000003',1, 3,'CalAmp',  'LMU-4230','4.12.1','4.13.0','CN=DEV003','2027-06-10','ATIVO','ATIVO',  NOW(),                       DATE_SUB(NOW(),INTERVAL 3 MINUTE),  '201.12.45.30', '3G',-68,60,72,TRUE, '2023-03-10','Pedro Técnico'),
('DEV004','456789012345678','895500000000000004',1, 4,'CalAmp',  'LMU-4230','4.12.1','4.13.0','CN=DEV004','2027-06-10','ATIVO','INATIVO',DATE_SUB(NOW(),INTERVAL 2 DAY), DATE_SUB(NOW(),INTERVAL 2 DAY), '201.12.45.31', '3G',-90,60,72,FALSE,'2021-08-05','Pedro Técnico'),
('DEV005','567890123456789','895500000000000005',2, 5,'Queclink','GV320',   '2.8.6', '2.9.0', 'CN=DEV005','2026-12-01','EXPIRANDO','ATIVO',DATE_SUB(NOW(),INTERVAL 1 HOUR),DATE_SUB(NOW(),INTERVAL 1 HOUR),'189.45.22.10','4G',-62,30,72,FALSE,'2023-02-28','Ana Técnica'),
('DEV006','678901234567890','895500000000000006',2, 6,'Queclink','GV320',   '2.8.6', '2.9.0', 'CN=DEV006','2027-02-20','ATIVO','ATIVO',  NOW(),                       DATE_SUB(NOW(),INTERVAL 5 MINUTE),  '189.45.22.11', '4G',-58,30,72,FALSE,'2022-11-12','Ana Técnica'),
('DEV007','789012345678901','895500000000000007',3, 7,'Queclink','GV320',   '2.8.6', '2.9.0', 'CN=DEV007','2027-02-20','ATIVO','ATIVO',  NOW(),                       DATE_SUB(NOW(),INTERVAL 2 MINUTE),  '200.10.55.20', '4G',-55,30,72,FALSE,'2021-05-18','Carlos Técnico'),
('DEV008','890123456789012','895500000000000008',3, 8,'Teltonika','FMB920', '3.25.4','3.26.0','CN=DEV008','2027-03-15','ATIVO','ATIVO',  DATE_SUB(NOW(),INTERVAL 3 HOUR),DATE_SUB(NOW(),INTERVAL 3 HOUR),'200.10.55.21','4G',-71,30,72,FALSE,'2023-01-30','Carlos Técnico'),
('DEV009','901234567890123','895500000000000009',4, 9,'Teltonika','FMB920', '3.25.4','3.26.0','CN=DEV009','2027-03-15','ATIVO','ATIVO',  DATE_SUB(NOW(),INTERVAL 1 HOUR),DATE_SUB(NOW(),INTERVAL 1 HOUR),'177.85.63.10','4G',-69,30,72,FALSE,'2022-09-05','Roberto Técnico'),
('DEV010','012345678901234','895500000000000010',4,10,'Teltonika','FMB920', '3.25.4','3.26.0','CN=DEV010','2027-03-15','ATIVO','ATIVO',  DATE_SUB(NOW(),INTERVAL 30 MINUTE),DATE_SUB(NOW(),INTERVAL 30 MINUTE),'177.85.63.11','4G',-62,30,72,FALSE,'2021-11-22','Roberto Técnico');

-- =============================================================================
-- 18. HEARTBEAT LOG
-- =============================================================================
INSERT INTO heartbeat_log (device_id, tenant_id, tipo, ip, tecnologia, rssi, firmware, registrado_em) VALUES
('DEV001',1,'CONNECT',   '187.62.10.1', '4G',-65,'3.25.4',DATE_SUB(NOW(),INTERVAL 1 DAY)),
('DEV001',1,'HEARTBEAT', '187.62.10.1', '4G',-64,'3.25.4',DATE_SUB(NOW(),INTERVAL 12 HOUR)),
('DEV001',1,'HEARTBEAT', '187.62.10.1', '4G',-67,'3.25.4',DATE_SUB(NOW(),INTERVAL 1 HOUR)),
('DEV002',1,'CONNECT',   '187.62.10.2', '4G',-72,'3.25.4',DATE_SUB(NOW(),INTERVAL 3 DAY)),
('DEV002',1,'HEARTBEAT', '187.62.10.2', '4G',-70,'3.25.4',DATE_SUB(NOW(),INTERVAL 2 HOUR)),
('DEV003',1,'CONNECT',   '201.12.45.30','3G',-68,'4.12.1',DATE_SUB(NOW(),INTERVAL 5 DAY)),
('DEV003',1,'HEARTBEAT', '201.12.45.30','3G',-69,'4.12.1',DATE_SUB(NOW(),INTERVAL 3 MINUTE)),
('DEV006',2,'CONNECT',   '189.45.22.11','4G',-58,'2.8.6', DATE_SUB(NOW(),INTERVAL 2 HOUR)),
('DEV006',2,'HEARTBEAT', '189.45.22.11','4G',-59,'2.8.6', DATE_SUB(NOW(),INTERVAL 5 MINUTE)),
('DEV007',3,'CONNECT',   '200.10.55.20','4G',-55,'2.8.6', DATE_SUB(NOW(),INTERVAL 3 HOUR)),
('DEV007',3,'HEARTBEAT', '200.10.55.20','4G',-56,'2.8.6', DATE_SUB(NOW(),INTERVAL 2 MINUTE)),
('DEV009',4,'CONNECT',   '177.85.63.10','4G',-69,'3.25.4',DATE_SUB(NOW(),INTERVAL 1 DAY)),
('DEV009',4,'HEARTBEAT', '177.85.63.10','4G',-68,'3.25.4',DATE_SUB(NOW(),INTERVAL 1 HOUR)),
('DEV010',4,'CONNECT',   '177.85.63.11','4G',-62,'3.25.4',DATE_SUB(NOW(),INTERVAL 6 HOUR)),
('DEV010',4,'HEARTBEAT', '177.85.63.11','4G',-61,'3.25.4',DATE_SUB(NOW(),INTERVAL 30 MINUTE));

-- =============================================================================
-- 19. MANUTENÇÕES
-- =============================================================================
INSERT INTO manutencoes (data_manutencao, descricao, custo, tipo, oficina, km_realizacao,
                        proxima_manutencao_km, proxima_manutencao_data, observacoes,
                        veiculo_id, motorista_id) VALUES
('2026-02-10','Troca de óleo e filtros completa',          850.00, 'PREVENTIVA','Mecânica Pantanal Cuiabá',  12000,17000,'2026-05-10','Óleo sintético 15W40 Mobil',    1,1),
('2026-02-20','Alinhamento, balanceamento e pneus',       1200.00, 'PREVENTIVA','Pneus Center Rondonópolis', 23000,28000,'2026-05-20','4 pneus Michelin XTA',           2,2),
('2026-01-15','Reparo no sistema de freios dianteiros',   1800.00, 'CORRETIVA', 'Freios Express Cuiabá',     22500,32500,'2026-04-15','Pastilhas, discos e fluido',     3,3),
('2026-03-05','Revisão completa 30.000 km',               2500.00, 'PREVENTIVA','Concessionária DAF Cuiabá', 18000,28000,'2026-06-05','Troca completa de fluidos',      4,4),
('2026-02-28','Troca de pneus traseiros',                 3200.00, 'CORRETIVA', 'Pneus Center Sinop',         8500,18500,'2026-08-28','6 pneus Bridgestone R249',       5,5),
('2026-03-01','Manutenção no turbocompressor',            4200.00, 'CORRETIVA', 'Mecânica Diesel SP',        44800,54800,'2026-06-01','Troca do turbo e mangueiras',    6,6),
('2026-02-15','Revisão do sistema elétrico',               950.00, 'PREVENTIVA','Elétrica Veicular BH',      67500,77500,'2026-05-15','Troca de alternador e bateria',  7,7),
('2026-03-10','Troca de correia dentada e tensor',        1350.00, 'PREVENTIVA','Mecânica Curitiba',         31000,41000,'2026-06-10','Kit correia Gates completo',     8,1),
('2026-02-25','Revisão 80.000 km completa',               3100.00, 'PREVENTIVA','Concessionária Scania POA', 78500,88500,'2026-05-25','Revisão major service',          9,2),
('2026-03-08','Reparo na caixa de câmbio',                5800.00, 'CORRETIVA', 'Câmbio Especializado MS',   53800,63800,'2026-06-08','Troca de sincronizadores',      10,3);

-- =============================================================================
-- 20. USUÁRIOS (sem admin — apenas operadores, gestores e motoristas)
-- =============================================================================
INSERT INTO usuarios (login, senha, nome, email, cpf, ativo, perfil, motorista_id) VALUES
('operador.cuiaba',  '$2a$10$xK8vQ3mN9pL2rT7wY4uI6eZcBnJhFsDgRkOiWtPqUaVbCdEmXlYz', 'Operador Cuiabá',      'operador.cuiaba@telemetria.com',  '222.333.444-55', TRUE, 'OPERADOR', NULL),
('operador.rondon',  '$2a$10$xK8vQ3mN9pL2rT7wY4uI6eZcBnJhFsDgRkOiWtPqUaVbCdEmXlYz', 'Operador Rondonópolis','operador.rondon@telemetria.com',  '333.444.555-66', TRUE, 'OPERADOR', NULL),
('operador.sinop',   '$2a$10$xK8vQ3mN9pL2rT7wY4uI6eZcBnJhFsDgRkOiWtPqUaVbCdEmXlYz', 'Operador Sinop',       'operador.sinop@telemetria.com',   '444.555.666-77', TRUE, 'OPERADOR', NULL),
('gestor.logistica', '$2a$10$xK8vQ3mN9pL2rT7wY4uI6eZcBnJhFsDgRkOiWtPqUaVbCdEmXlYz', 'Gestor Logística MT',  'gestor.logistica@telemetria.com', '555.666.777-88', TRUE, 'GESTOR',   NULL),
('gestor.frota',     '$2a$10$xK8vQ3mN9pL2rT7wY4uI6eZcBnJhFsDgRkOiWtPqUaVbCdEmXlYz', 'Gestor de Frota',      'gestor.frota@telemetria.com',     '666.777.888-99', TRUE, 'GESTOR',   NULL),
('carlos.ferreira',  '$2a$10$xK8vQ3mN9pL2rT7wY4uI6eZcBnJhFsDgRkOiWtPqUaVbCdEmXlYz', 'Carlos Ferreira',      'carlos.f@telemetria.com',         '123.456.789-00', TRUE, 'MOTORISTA',1),
('maria.cida',       '$2a$10$xK8vQ3mN9pL2rT7wY4uI6eZcBnJhFsDgRkOiWtPqUaVbCdEmXlYz', 'Maria Aparecida',      'maria.a@telemetria.com',          '234.567.890-11', TRUE, 'MOTORISTA',2),
('jose.roberto',     '$2a$10$xK8vQ3mN9pL2rT7wY4uI6eZcBnJhFsDgRkOiWtPqUaVbCdEmXlYz', 'José Roberto',         'jose.r@telemetria.com',           '345.678.901-22', TRUE, 'MOTORISTA',3),
('ana.carolina',     '$2a$10$xK8vQ3mN9pL2rT7wY4uI6eZcBnJhFsDgRkOiWtPqUaVbCdEmXlYz', 'Ana Carolina',         'ana.c@telemetria.com',            '456.789.012-33', TRUE, 'MOTORISTA',4),
('paulo.sergio',     '$2a$10$xK8vQ3mN9pL2rT7wY4uI6eZcBnJhFsDgRkOiWtPqUaVbCdEmXlYz', 'Paulo Sérgio',         'paulo.s@telemetria.com',          '567.890.123-44', TRUE, 'MOTORISTA',5),
('fernanda.lima',    '$2a$10$xK8vQ3mN9pL2rT7wY4uI6eZcBnJhFsDgRkOiWtPqUaVbCdEmXlYz', 'Fernanda Lima',        'fernanda.l@telemetria.com',       '678.901.234-55', TRUE, 'MOTORISTA',6),
('roberto.alves',    '$2a$10$xK8vQ3mN9pL2rT7wY4uI6eZcBnJhFsDgRkOiWtPqUaVbCdEmXlYz', 'Roberto Alves',        'roberto.a@telemetria.com',        '789.012.345-66', TRUE, 'MOTORISTA',7);

-- =============================================================================
-- 21. RESUMO DIÁRIO VEÍCULO
-- =============================================================================
INSERT INTO resumo_diario_veiculo (tenant_id, veiculo_id, data, km_total, horas_uso, horas_ocioso,
                                  litros_consumidos, consumo_medio, velocidade_media, velocidade_maxima,
                                  frenagens_bruscas, aceleracoes_bruscas, excessos_velocidade, curvas_bruscas,
                                  total_alertas, alertas_criticos, total_viagens, alertas_fadiga,
                                  alertas_celular, score_dia, total_eventos) VALUES
(1,  1,'2026-03-15',210, 5.5, 0.5,  84, 2.5, 65, 85,  1, 1, 1, 0, 2, 0, 1, 0, 0,  950, 12),
(1,  2,'2026-03-16',175, 4.5, 0.3,  70, 2.5, 70, 82,  0, 0, 0, 0, 0, 0, 1, 0, 0, 1000,  8),
(1,  3,'2026-03-17',150, 3.5, 0.2,  60, 2.5, 72, 75,  0, 0, 0, 0, 1, 0, 1, 0, 0,  990,  6),
(1,  4,'2026-03-16', 83, 2.0, 0.1,  33, 2.5, 68, 78,  0, 0, 0, 0, 0, 0, 1, 0, 0, 1000,  4),
(3,  6,'2026-03-14',425, 9.0, 1.0, 170, 2.5, 80,105,  1, 1, 1, 0, 2, 0, 1, 0, 0,  920, 18),
(3,  7,'2026-03-17',320, 6.5, 0.5, 128, 2.5, 75, 98,  1, 1, 1, 0, 2, 1, 1, 0, 0,  910, 14),
(4,  9,'2026-03-15',128, 3.5, 0.2,  51, 2.5, 65, 75,  0, 0, 0, 0, 0, 0, 1, 0, 0, 1000,  6),
(4, 10,'2026-03-17',180, 4.0, 0.3,  72, 2.5, 70, 82,  0, 0, 0, 0, 1, 0, 1, 0, 0,  970,  8);

-- =============================================================================
-- 22. JORNADAS
-- =============================================================================
INSERT INTO jornadas (tenant_id, motorista_id, veiculo_id, viagem_id, data_inicio, data_fim,
                     horas_direcao, horas_disponivel, horas_repouso, horas_extras,
                     pausas_realizadas, km_rodados, limite_direcao_h, limite_extra_h,
                     alertas_enviados, alerta_limite_30min, status, origem_dado, irregular) VALUES
(1,1,1, 1,'2026-03-15 08:00:00','2026-03-15 14:00:00', 5.0,1.0,0.5,0.0, 2,210, 8.0,2.0, 0,FALSE,'FECHADA',  'TELEMETRIA',FALSE),
(1,2,2, 2,'2026-03-16 09:00:00','2026-03-16 16:00:00', 4.5,1.5,0.5,0.0, 1,175, 8.0,2.0, 0,FALSE,'FECHADA',  'TELEMETRIA',FALSE),
(1,3,3, 3,'2026-03-17 07:30:00',NULL,                  6.0,1.0,0.0,0.0, 0,150, 8.0,2.0, 0,FALSE,'ABERTA',   'TELEMETRIA',FALSE),
(1,4,4, 4,'2026-03-16 13:00:00','2026-03-16 15:30:00', 2.0,0.5,0.0,0.0, 1, 83, 8.0,2.0, 0,FALSE,'FECHADA',  'TELEMETRIA',FALSE),
(3,6,6, 6,'2026-03-14 06:00:00','2026-03-14 16:00:00', 8.5,1.5,0.5,0.5, 3,425, 8.0,2.0, 1,TRUE, 'FECHADA',  'TELEMETRIA',TRUE),
(3,7,7, 7,'2026-03-17 08:00:00',NULL,                  7.0,1.0,0.0,0.0, 1,320, 8.0,2.0, 1,TRUE, 'ABERTA',   'TELEMETRIA',FALSE),
(4,2,9, 9,'2026-03-15 10:00:00','2026-03-15 14:00:00', 3.5,0.5,0.0,0.0, 1,128, 8.0,2.0, 0,FALSE,'FECHADA',  'TELEMETRIA',FALSE),
(4,3,10,10,'2026-03-17 09:30:00',NULL,                 4.0,0.5,0.0,0.0, 1,180, 8.0,2.0, 0,FALSE,'ABERTA',   'TELEMETRIA',FALSE);

-- =============================================================================
-- FIM DO SCRIPT
-- =============================================================================