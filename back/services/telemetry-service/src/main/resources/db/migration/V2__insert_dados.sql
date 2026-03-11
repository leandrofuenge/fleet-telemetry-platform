USE telemetry_db;

-- =============================================================================
-- INSERT DE DADOS FICTÍCIOS PARA TELEMETRY SERVICE
-- =============================================================================

-- =============================================================================
-- 1. CLIENTES
-- =============================================================================
INSERT INTO clientes (nome_razao_social, cnpj, email, telefone, endereco, ativo)
VALUES 
('Transportadora Rápido Ltda', '12.345.678/0001-90', 'contato@rapido.com.br', '(11) 3456-7890', 'Av. Brasil, 1000 - São Paulo/SP', TRUE),
('Logística Express S/A', '23.456.789/0001-01', 'financeiro@logexpress.com', '(21) 9876-5432', 'Rua da Matriz, 500 - Rio de Janeiro/RJ', TRUE),
('Frota Nacional Ltda', '34.567.890/0001-12', 'operacoes@frotanacional.com', '(31) 3456-7890', 'Av. Amazonas, 2000 - Belo Horizonte/MG', TRUE),
('Distribuidora Central', '45.678.901/0001-23', 'logistica@central.com', '(41) 3232-4455', 'Rua XV de Novembro, 300 - Curitiba/PR', TRUE),
('Agro Transportes', '56.789.012/0001-34', 'agro@transportes.com', '(51) 3333-4444', 'Rodovia BR-116, km 200 - Porto Alegre/RS', TRUE);

-- =============================================================================
-- 2. MOTORISTAS (principais)
-- =============================================================================
INSERT INTO motoristas (nome, cpf, cnh, categoria_cnh, email, telefone, ativo)
VALUES 
('João da Silva', '123.456.789-00', '12345678901', 'E', 'joao.silva@email.com', '(11) 98765-4321', TRUE),
('Maria Santos', '234.567.890-11', '23456789012', 'D', 'maria.santos@email.com', '(21) 97654-3210', TRUE),
('Pedro Oliveira', '345.678.901-22', '34567890123', 'E', 'pedro.oliveira@email.com', '(31) 96543-2109', TRUE),
('Ana Costa', '456.789.012-33', '45678901234', 'D', 'ana.costa@email.com', '(41) 95432-1098', TRUE),
('Carlos Souza', '567.890.123-44', '56789012345', 'E', 'carlos.souza@email.com', '(51) 94321-0987', TRUE),
('Fernanda Lima', '678.901.234-55', '67890123456', 'D', 'fernanda.lima@email.com', '(11) 93210-9876', TRUE),
('Roberto Alves', '789.012.345-66', '78901234567', 'E', 'roberto.alves@email.com', '(21) 92109-8765', TRUE);

-- =============================================================================
-- 3. VEÍCULOS (principais)
-- =============================================================================
INSERT INTO veiculos (placa, modelo, marca, capacidade_carga, ano_fabricacao, ativo, cliente_id, motorista_atual_id)
VALUES 
('ABC1D23', 'FH 540', 'Volvo', 28000, 2023, TRUE, 1, 1),
('DEF2E34', 'Actros 2651', 'Mercedes-Benz', 32000, 2022, TRUE, 1, 2),
('GHI3F45', 'Scania R500', 'Scania', 30000, 2023, TRUE, 2, 3),
('JKL4G56', 'XFX 105.460', 'DAF', 29000, 2021, TRUE, 2, 4),
('MNO5H67', 'Constellation', 'Volkswagen', 26000, 2023, TRUE, 3, 5),
('PQR6I78', 'S-Way', 'Iveco', 27000, 2022, TRUE, 3, 6),
('STU7J89', 'FH 460', 'Volvo', 26000, 2021, TRUE, 4, 7),
('VWX8K90', 'Actros 2653', 'Mercedes-Benz', 34000, 2023, TRUE, 4, 1),
('YZA9L01', 'Scania R450', 'Scania', 28000, 2022, TRUE, 5, 2),
('BCD0M12', 'XFX 105.410', 'DAF', 25000, 2021, TRUE, 5, 3);

-- =============================================================================
-- 4. VEÍCULOS CACHE (cópia dos principais)
-- =============================================================================
INSERT INTO veiculos_cache (id, uuid, tenant_id, placa, modelo, marca, tipo_veiculo, consumo_medio, capacidade_carga_kg, device_id, device_imei, pbt_kg, ativo)
VALUES 
(1, UUID(), 1, 'ABC1D23', 'FH 540', 'Volvo', 'CAMINHAO_PESADO', 2.5, 28000, 'DEV001', '123456789012345', 58000, TRUE),
(2, UUID(), 1, 'DEF2E34', 'Actros 2651', 'Mercedes-Benz', 'CAMINHAO_PESADO', 2.3, 32000, 'DEV002', '234567890123456', 62000, TRUE),
(3, UUID(), 2, 'GHI3F45', 'Scania R500', 'Scania', 'CAMINHAO_PESADO', 2.4, 30000, 'DEV003', '345678901234567', 60000, TRUE),
(4, UUID(), 2, 'JKL4G56', 'XFX 105.460', 'DAF', 'CAMINHAO_PESADO', 2.6, 29000, 'DEV004', '456789012345678', 59000, TRUE),
(5, UUID(), 3, 'MNO5H67', 'Constellation', 'Volkswagen', 'CAMINHAO_SEMIPESADO', 3.0, 26000, 'DEV005', '567890123456789', 56000, TRUE);

-- =============================================================================
-- 5. MOTORISTAS CACHE (cópia dos principais)
-- =============================================================================
INSERT INTO motoristas_cache (id, uuid, tenant_id, nome, cpf, cnh, categoria_cnh, ativo)
VALUES 
(1, UUID(), 1, 'João da Silva', '123.456.789-00', '12345678901', 'E', TRUE),
(2, UUID(), 1, 'Maria Santos', '234.567.890-11', '23456789012', 'D', TRUE),
(3, UUID(), 2, 'Pedro Oliveira', '345.678.901-22', '34567890123', 'E', TRUE),
(4, UUID(), 2, 'Ana Costa', '456.789.012-33', '45678901234', 'D', TRUE),
(5, UUID(), 3, 'Carlos Souza', '567.890.123-44', '56789012345', 'E', TRUE);

-- =============================================================================
-- 6. CARGAS
-- =============================================================================
INSERT INTO cargas (descricao, peso_kg, tipo, volume_m3, nfe_chave, cte_chave, cliente_id)
VALUES 
('Eletrônicos diversos', 5000, 'GERAL', 25, '35200612345678901234550000000012345678901', 'CTE-2025-0001', 1),
('Alimentos não perecíveis', 8000, 'GERAL', 40, '35200612345678901234550000000012345678902', 'CTE-2025-0002', 1),
('Produtos farmacêuticos', 2000, 'REFRIGERADA', 15, '35200612345678901234550000000012345678903', 'CTE-2025-0003', 2),
('Móveis planejados', 6000, 'FRAGIL', 35, '35200612345678901234550000000012345678904', 'CTE-2025-0004', 2),
('Materiais de construção', 12000, 'GERAL', 50, '35200612345678901234550000000012345678905', 'CTE-2025-0005', 3),
('Produtos químicos', 4000, 'PERIGOSA', 20, '35200612345678901234550000000012345678906', 'CTE-2025-0006', 3),
('Carnes congeladas', 7000, 'CONGELADA', 30, '35200612345678901234550000000012345678907', 'CTE-2025-0007', 4),
('Bebidas', 9000, 'GERAL', 45, '35200612345678901234550000000012345678908', 'CTE-2025-0008', 4),
('Peças automotivas', 5500, 'GERAL', 28, '35200612345678901234550000000012345678909', 'CTE-2025-0009', 5),
('Produtos têxteis', 3500, 'FRAGIL', 22, '35200612345678901234550000000012345678910', 'CTE-2025-0010', 5);

-- =============================================================================
-- 7. ROTAS
-- =============================================================================
INSERT INTO rotas (nome, origem, latitude_origem, longitude_origem, destino, latitude_destino, longitude_destino, 
                  distancia_prevista, tempo_previsto, status, ativa, veiculo_id, motorista_id)
VALUES 
('SP → RJ', 'São Paulo/SP', -23.5505, -46.6333, 'Rio de Janeiro/RJ', -22.9068, -43.1729, 430, 360, 'FINALIZADA', TRUE, 1, 1),
('RJ → BH', 'Rio de Janeiro/RJ', -22.9068, -43.1729, 'Belo Horizonte/MG', -19.9167, -43.9345, 440, 390, 'EM_ANDAMENTO', TRUE, 2, 2),
('BH → SP', 'Belo Horizonte/MG', -19.9167, -43.9345, 'São Paulo/SP', -23.5505, -46.6333, 590, 540, 'PLANEJADA', TRUE, 3, 3),
('SP → Curitiba', 'São Paulo/SP', -23.5505, -46.6333, 'Curitiba/PR', -25.4297, -49.2719, 410, 360, 'PLANEJADA', TRUE, 4, 4),
('Curitiba → POA', 'Curitiba/PR', -25.4297, -49.2719, 'Porto Alegre/RS', -30.0346, -51.2177, 710, 630, 'PLANEJADA', TRUE, 5, 5),
('POA → SP', 'Porto Alegre/RS', -30.0346, -51.2177, 'São Paulo/SP', -23.5505, -46.6333, 1120, 960, 'PLANEJADA', TRUE, 6, 6),
('SP → BH', 'São Paulo/SP', -23.5505, -46.6333, 'Belo Horizonte/MG', -19.9167, -43.9345, 590, 510, 'FINALIZADA', TRUE, 7, 7),
('BH → Salvador', 'Belo Horizonte/MG', -19.9167, -43.9345, 'Salvador/BA', -12.9777, -38.5016, 1370, 1170, 'PLANEJADA', TRUE, 8, 1),
('Salvador → Recife', 'Salvador/BA', -12.9777, -38.5016, 'Recife/PE', -8.0476, -34.8770, 840, 720, 'PLANEJADA', TRUE, 9, 2),
('Recife → Fortaleza', 'Recife/PE', -8.0476, -34.8770, 'Fortaleza/CE', -3.7172, -38.5433, 800, 690, 'PLANEJADA', TRUE, 10, 3);

-- =============================================================================
-- 8. VIAGENS
-- =============================================================================
INSERT INTO viagens (status, observacoes, data_saida, data_chegada_prevista, data_chegada_real, data_inicio,
                    distancia_real_km, km_fora_rota, score_viagem, veiculo_id, motorista_id, carga_id, rota_id)
VALUES 
('FINALIZADA', 'Viagem concluída com sucesso', '2026-03-01 08:00:00', '2026-03-01 18:00:00', '2026-03-01 17:45:00', '2026-03-01 08:15:00', 428, 5, 985, 1, 1, 1, 1),
('EM_ANDAMENTO', 'Em andamento, sem problemas', '2026-03-10 09:00:00', '2026-03-10 19:00:00', NULL, '2026-03-10 09:10:00', 312, 12, 920, 2, 2, 2, 2),
('PLANEJADA', 'Aguardando liberação', NULL, '2026-03-15 08:00:00', NULL, NULL, 0, 0, 1000, 3, 3, 3, 3),
('PLANEJADA', 'Documentação pendente', NULL, '2026-03-16 09:00:00', NULL, NULL, 0, 0, 1000, 4, 4, 4, 4),
('FINALIZADA', 'Viagem com pequenos atrasos', '2026-03-05 07:30:00', '2026-03-05 18:30:00', '2026-03-05 19:15:00', '2026-03-05 07:45:00', 405, 8, 950, 5, 5, 5, 5),
('FINALIZADA', 'Concluída dentro do previsto', '2026-03-08 10:00:00', '2026-03-09 08:00:00', '2026-03-09 07:50:00', '2026-03-08 10:15:00', 705, 3, 990, 6, 6, 6, 6),
('CANCELADA', 'Problemas mecânicos', NULL, '2026-03-12 10:00:00', NULL, NULL, 0, 0, 800, 7, 7, 7, 7),
('PLANEJADA', 'Aguardando carregamento', NULL, '2026-03-18 07:00:00', NULL, NULL, 0, 0, 1000, 8, 1, 8, 8),
('PLANEJADA', 'Documentação ok', NULL, '2026-03-20 08:30:00', NULL, NULL, 0, 0, 1000, 9, 2, 9, 9),
('PLANEJADA', 'Aguardando confirmação', NULL, '2026-03-22 09:00:00', NULL, NULL, 0, 0, 1000, 10, 3, 10, 10);

-- =============================================================================
-- 9. TELEMETRIA (amostra de 10 registros)
-- =============================================================================
INSERT INTO telemetria (tenant_id, veiculo_id, veiculo_uuid, motorista_id, viagem_id, device_id, imei_dispositivo,
                       latitude, longitude, altitude, velocidade, direcao, hdop, satelites, precisao_gps,
                       ignicao, rpm, odometro, nivel_combustivel, data_hora)
VALUES 
(1, 1, (SELECT uuid FROM veiculos_cache WHERE id = 1), 1, 1, 'DEV001', '123456789012345',
 -23.5505, -46.6333, 760, 85.5, 90, 0.8, 12, 3.0,
 TRUE, 1500, 1250.5, 75, '2026-03-01 08:30:00'),

(1, 1, (SELECT uuid FROM veiculos_cache WHERE id = 1), 1, 1, 'DEV001', '123456789012345',
 -23.2000, -46.3000, 750, 92.3, 95, 0.7, 14, 2.5,
 TRUE, 1600, 1275.8, 70, '2026-03-01 09:45:00'),

(1, 1, (SELECT uuid FROM veiculos_cache WHERE id = 1), 1, 1, 'DEV001', '123456789012345',
 -22.9000, -45.8000, 720, 88.7, 85, 0.9, 11, 3.2,
 TRUE, 1450, 1300.2, 65, '2026-03-01 11:00:00'),

(1, 2, (SELECT uuid FROM veiculos_cache WHERE id = 2), 2, 2, 'DEV002', '234567890123456',
 -22.9068, -43.1729, 5, 0, 0, 1.2, 8, 5.0,
 FALSE, 0, 22450.3, 45, '2026-03-10 12:30:00'),

(1, 2, (SELECT uuid FROM veiculos_cache WHERE id = 2), 2, 2, 'DEV002', '234567890123456',
 -22.8000, -43.3000, 10, 65.4, 270, 0.8, 13, 2.8,
 TRUE, 1400, 22475.6, 42, '2026-03-10 13:45:00'),

(1, 2, (SELECT uuid FROM veiculos_cache WHERE id = 2), 2, 2, 'DEV002', '234567890123456',
 -22.5000, -43.5000, 15, 72.1, 280, 0.7, 15, 2.4,
 TRUE, 1550, 22510.9, 40, '2026-03-10 15:00:00'),

(2, 3, (SELECT uuid FROM veiculos_cache WHERE id = 3), 3, 3, 'DEV003', '345678901234567',
 -19.9167, -43.9345, 850, 0, 0, 0.5, 16, 1.5,
 FALSE, 0, 18900.0, 80, '2026-03-05 08:00:00'),

(2, 4, (SELECT uuid FROM veiculos_cache WHERE id = 4), 4, 4, 'DEV004', '456789012345678',
 -23.5505, -46.6333, 760, 0, 0, 0.6, 14, 2.0,
 FALSE, 0, 15780.5, 90, '2026-03-08 10:30:00'),

(3, 5, (SELECT uuid FROM veiculos_cache WHERE id = 5), 5, 5, 'DEV005', '567890123456789',
 -25.4297, -49.2719, 920, 55.2, 180, 0.9, 10, 3.5,
 TRUE, 1300, 30250.0, 60, '2026-03-09 14:20:00'),

(1, 1, (SELECT uuid FROM veiculos_cache WHERE id = 1), 1, 1, 'DEV001', '123456789012345',
 -22.9068, -43.1729, 5, 0, 0, 1.0, 9, 4.0,
 FALSE, 0, 1325.7, 62, '2026-03-01 18:00:00');

-- =============================================================================
-- 10. ALERTAS
-- =============================================================================
INSERT INTO alertas (uuid, tenant_id, veiculo_id, veiculo_uuid, motorista_id, viagem_id,
                    tipo, severidade, mensagem, latitude, longitude, velocidade_kmh, 
                    odometro_km, data_hora, lido, resolvido)
VALUES 
(UUID(), 1, 1, (SELECT uuid FROM veiculos_cache WHERE id = 1), 1, 1,
 'EXCESSO_VELOCIDADE', 'MEDIO', 'Excesso de velocidade detectado: 98 km/h em via de 80 km/h',
 -23.2000, -46.3000, 98.5, 1275.8, '2026-03-01 09:45:00', FALSE, FALSE),

(UUID(), 1, 2, (SELECT uuid FROM veiculos_cache WHERE id = 2), 2, 2,
 'FRENAGEM_BRUSCA', 'BAIXO', 'Frenagem brusca detectada',
 -22.8000, -43.3000, 65.4, 22475.6, '2026-03-10 13:45:00', FALSE, FALSE),

(UUID(), 1, 1, (SELECT uuid FROM veiculos_cache WHERE id = 1), 1, 1,
 'CHEGADA_DESTINO', 'BAIXO', 'Veículo chegou ao destino',
 -22.9068, -43.1729, 0, 1325.7, '2026-03-01 18:00:00', TRUE, TRUE),

(UUID(), 2, 3, (SELECT uuid FROM veiculos_cache WHERE id = 3), 3, 3,
 'NIVEL_COMBUSTIVEL_BAIXO', 'MEDIO', 'Nível de combustível abaixo de 15%',
 -19.9167, -43.9345, 0, 18900.0, '2026-03-05 08:00:00', FALSE, FALSE),

(UUID(), 2, 4, (SELECT uuid FROM veiculos_cache WHERE id = 4), 4, 4,
 'INICIO_VIAGEM', 'BAIXO', 'Viagem iniciada',
 -23.5505, -46.6333, 0, 15780.5, '2026-03-08 10:30:00', TRUE, TRUE),

(UUID(), 3, 5, (SELECT uuid FROM veiculos_cache WHERE id = 5), 5, 5,
 'TEMPO_DIRECAO', 'ALTO', 'Motorista dirigindo por mais de 4 horas sem pausa',
 -25.4297, -49.2719, 55.2, 30250.0, '2026-03-09 14:20:00', FALSE, FALSE);

-- =============================================================================
-- 11. DESVIOS DE ROTA
-- =============================================================================
INSERT INTO desvios_rota (tenant_id, rota_id, veiculo_id, veiculo_uuid, viagem_id,
                         latitude_desvio, longitude_desvio, velocidade_kmh, distancia_metros,
                         lat_ponto_mais_proximo, lng_ponto_mais_proximo, nome_via_desvio,
                         data_hora_desvio, data_hora_retorno, duracao_min, km_extras,
                         alerta_enviado, resolvido)
VALUES 
(1, 1, 1, (SELECT uuid FROM veiculos_cache WHERE id = 1), 1,
 -23.2500, -46.3500, 75.2, 150,
 -23.2450, -46.3450, 'Rodovia Presidente Dutra',
 '2026-03-01 10:15:00', '2026-03-01 10:25:00', 10, 0.8, TRUE, TRUE),

(1, 2, 2, (SELECT uuid FROM veiculos_cache WHERE id = 2), 2,
 -22.7500, -43.4000, 68.5, 220,
 -22.7350, -43.3800, 'Rodovia BR-040',
 '2026-03-10 14:30:00', '2026-03-10 14:55:00', 25, 2.5, TRUE, FALSE),

(2, 5, 5, (SELECT uuid FROM veiculos_cache WHERE id = 5), 5,
 -25.5000, -49.2000, 52.0, 180,
 -25.4850, -49.1850, 'BR-376',
 '2026-03-09 15:10:00', NULL, NULL, 0, TRUE, FALSE);

-- =============================================================================
-- 12. GEOFENCES
-- =============================================================================
INSERT INTO geofences (uuid, tenant_id, nome, tipo, latitude_centro, longitude_centro, raio,
                      tipo_alerta, aplica_todos, ativo)
VALUES 
(UUID(), 1, 'Centro de São Paulo', 'CIRCULO', -23.5505, -46.6333, 5000, 'AMBOS', TRUE, TRUE),
(UUID(), 1, 'Zona Sul SP', 'CIRCULO', -23.6500, -46.7000, 8000, 'ENTRADA', TRUE, TRUE),
(UUID(), 2, 'Porto do Rio', 'CIRCULO', -22.8950, -43.1800, 3000, 'AMBOS', TRUE, TRUE),
(UUID(), 2, 'Aeroporto de Confins', 'CIRCULO', -19.6246, -43.9719, 5000, 'SAIDA', FALSE, TRUE),
(UUID(), 3, 'Área Industrial', 'POLIGONO', -25.4500, -49.2500, NULL, 'AMBOS', TRUE, TRUE);

-- =============================================================================
-- 13. VEICULO_GEOFENCE (vínculos específicos)
-- =============================================================================
INSERT INTO veiculo_geofence (veiculo_id, geofence_id, ativo)
VALUES 
(1, 1, TRUE),
(1, 2, TRUE),
(2, 3, TRUE),
(3, 4, TRUE),
(4, 5, TRUE),
(5, 5, TRUE);

-- =============================================================================
-- 14. GEOCODING CACHE
-- =============================================================================
INSERT INTO geocoding_cache (lat_arred, lng_arred, pais, estado, cidade, bairro, logradouro,
                           nome_local, tipo_local, is_urbano, precisao_metros, fonte,
                           consulta_em, expira_em)
VALUES 
( -23.5505, -46.6333, 'Brasil', 'São Paulo', 'São Paulo', 'Centro', 'Praça da Sé', 
 'Catedral da Sé', 'tourist', TRUE, 10, 'NOMINATIM', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY)),

( -22.9068, -43.1729, 'Brasil', 'Rio de Janeiro', 'Rio de Janeiro', 'Centro', 'Av. Rio Branco',
 'Teatro Municipal', 'tourist', TRUE, 10, 'NOMINATIM', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY)),

( -19.9167, -43.9345, 'Brasil', 'Minas Gerais', 'Belo Horizonte', 'Savassi', 'Av. Getúlio Vargas',
 'Praça da Savassi', 'commercial', TRUE, 10, 'NOMINATIM', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY)),

( -25.4297, -49.2719, 'Brasil', 'Paraná', 'Curitiba', 'Centro', 'Rua XV de Novembro',
 'Boca Maldita', 'tourist', TRUE, 10, 'NOMINATIM', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY)),

( -30.0346, -51.2177, 'Brasil', 'Rio Grande do Sul', 'Porto Alegre', 'Centro', 'Rua da Praia',
 'Mercado Público', 'commercial', TRUE, 10, 'NOMINATIM', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY));

-- =============================================================================
-- 15. POSIÇÃO ATUAL
-- =============================================================================
INSERT INTO posicao_atual (veiculo_id, tenant_id, veiculo_uuid, latitude, longitude, velocidade, 
                         direcao, ignicao, status_veiculo, motorista_id, viagem_id, odometro,
                         nivel_combustivel, bateria_v, ultima_telemetria, nome_local, alertas_ativos)
VALUES 
(1, 1, (SELECT uuid FROM veiculos_cache WHERE id = 1), -22.9068, -43.1729, 0, 0, FALSE, 'PARADO', 1, 1, 1325.7, 62, 24.5, '2026-03-01 18:00:00', 'Rio de Janeiro/RJ', 0),
(2, 1, (SELECT uuid FROM veiculos_cache WHERE id = 2), -22.5000, -43.5000, 72.1, 280, TRUE, 'EM_MOVIMENTO', 2, 2, 22510.9, 40, 25.2, '2026-03-10 15:00:00', 'Entre Rio e BH', 1),
(3, 2, (SELECT uuid FROM veiculos_cache WHERE id = 3), -19.9167, -43.9345, 0, 0, FALSE, 'PARADO', 3, 3, 18900.0, 14, 24.0, '2026-03-05 08:00:00', 'Belo Horizonte/MG', 1),
(4, 2, (SELECT uuid FROM veiculos_cache WHERE id = 4), -23.5505, -46.6333, 0, 0, FALSE, 'PARADO', 4, 4, 15780.5, 90, 24.8, '2026-03-08 10:30:00', 'São Paulo/SP', 0),
(5, 3, (SELECT uuid FROM veiculos_cache WHERE id = 5), -25.4297, -49.2719, 0, 0, FALSE, 'PARADO', 5, 5, 30250.0, 60, 24.3, '2026-03-09 14:20:00', 'Curitiba/PR', 1);

-- =============================================================================
-- 16. HISTÓRICO POSIÇÃO (últimos pontos)
-- =============================================================================
INSERT INTO historico_posicao (tenant_id, veiculo_id, data_hora, latitude, longitude, velocidade, ignicao)
VALUES 
(1, 1, '2026-03-01 08:30:00', -23.5505, -46.6333, 85.5, TRUE),
(1, 1, '2026-03-01 09:45:00', -23.2000, -46.3000, 92.3, TRUE),
(1, 1, '2026-03-01 11:00:00', -22.9000, -45.8000, 88.7, TRUE),
(1, 1, '2026-03-01 12:30:00', -22.6000, -44.5000, 79.2, TRUE),
(1, 1, '2026-03-01 14:00:00', -22.9068, -43.1729, 0, FALSE),
(1, 2, '2026-03-10 12:30:00', -22.9068, -43.1729, 0, FALSE),
(1, 2, '2026-03-10 13:45:00', -22.8000, -43.3000, 65.4, TRUE),
(1, 2, '2026-03-10 15:00:00', -22.5000, -43.5000, 72.1, TRUE),
(2, 3, '2026-03-05 08:00:00', -19.9167, -43.9345, 0, FALSE),
(3, 5, '2026-03-09 14:20:00', -25.4297, -49.2719, 55.2, TRUE);

-- =============================================================================
-- 17. DISPOSITIVOS IOT
-- =============================================================================
INSERT INTO dispositivos_iot (device_id, imei, iccid, tenant_id, veiculo_id, fabricante, modelo_hw,
                            versao_firmware, versao_alvo, status, ultima_conexao, ultimo_heartbeat,
                            tecnologia_rede, rssi, freq_envio_s, tem_satelite)
VALUES 
('DEV001', '123456789012345', '895500000000000001', 1, 1, 'Teltonika', 'FMB920', '3.25.4', '3.26.0', 'ATIVO', NOW(), DATE_SUB(NOW(), INTERVAL 5 MINUTE), '4G', -65, 30, FALSE),
('DEV002', '234567890123456', '895500000000000002', 1, 2, 'Teltonika', 'FMB920', '3.25.4', '3.26.0', 'ATIVO', NOW(), DATE_SUB(NOW(), INTERVAL 2 MINUTE), '4G', -72, 30, FALSE),
('DEV003', '345678901234567', '895500000000000003', 2, 3, 'CalAmp', 'LMU-4230', '4.12.1', '4.13.0', 'ATIVO', DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR), '3G', -85, 60, TRUE),
('DEV004', '456789012345678', '895500000000000004', 2, 4, 'CalAmp', 'LMU-4230', '4.12.1', '4.13.0', 'INATIVO', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), '3G', -90, 60, FALSE),
('DEV005', '567890123456789', '895500000000000005', 3, 5, 'Queclink', 'GV320', '2.8.6', '2.9.0', 'ATIVO', NOW(), DATE_SUB(NOW(), INTERVAL 1 MINUTE), '4G', -58, 30, FALSE),
('DEV006', '678901234567890', '895500000000000006', 3, 6, 'Queclink', 'GV320', '2.8.6', '2.9.0', 'MANUTENCAO', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), '4G', NULL, 30, FALSE);

-- =============================================================================
-- 18. HEARTBEAT LOG
-- =============================================================================
INSERT INTO heartbeat_log (device_id, tenant_id, tipo, ip, tecnologia, rssi, firmware, registrado_em)
VALUES 
('DEV001', 1, 'CONNECT', '192.168.1.100', '4G', -65, '3.25.4', DATE_SUB(NOW(), INTERVAL 1 DAY)),
('DEV001', 1, 'HEARTBEAT', '192.168.1.100', '4G', -64, '3.25.4', DATE_SUB(NOW(), INTERVAL 12 HOUR)),
('DEV001', 1, 'HEARTBEAT', '192.168.1.100', '4G', -67, '3.25.4', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
('DEV002', 1, 'CONNECT', '192.168.1.101', '4G', -72, '3.25.4', DATE_SUB(NOW(), INTERVAL 3 DAY)),
('DEV002', 1, 'HEARTBEAT', '192.168.1.101', '4G', -70, '3.25.4', DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
('DEV003', 2, 'CONNECT', '192.168.2.150', '3G', -85, '4.12.1', DATE_SUB(NOW(), INTERVAL 5 DAY)),
('DEV003', 2, 'HEARTBEAT', '192.168.2.150', '3G', -86, '4.12.1', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
('DEV005', 3, 'CONNECT', '192.168.3.200', '4G', -58, '2.8.6', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
('DEV005', 3, 'HEARTBEAT', '192.168.3.200', '4G', -59, '2.8.6', DATE_SUB(NOW(), INTERVAL 15 MINUTE));

-- =============================================================================
-- 19. JORNADAS
-- =============================================================================
INSERT INTO jornadas (tenant_id, motorista_id, veiculo_id, viagem_id, data_inicio, data_fim,
                     horas_direcao, horas_disponivel, horas_repouso, horas_extras,
                     pausas_realizadas, km_rodados, alertas_enviados, alerta_limite_30min,
                     status, origem_dado, irregular)
VALUES 
(1, 1, 1, 1, '2026-03-01 08:00:00', '2026-03-01 18:00:00', 9.5, 1.0, 1.5, 1.5, 2, 428, 1, FALSE, 'FECHADA', 'TELEMETRIA', FALSE),
(1, 2, 2, 2, '2026-03-10 09:00:00', NULL, 6.0, 1.0, 2.0, 0, 1, 312, 0, FALSE, 'ABERTA', 'TELEMETRIA', FALSE),
(2, 3, 3, 3, '2026-03-05 08:00:00', '2026-03-05 08:30:00', 0.5, 0, 0, 0, 0, 0, 1, FALSE, 'FECHADA', 'TELEMETRIA', FALSE),
(3, 5, 5, 5, '2026-03-09 10:00:00', '2026-03-09 20:00:00', 8.5, 1.5, 2.0, 0.5, 1, 405, 1, TRUE, 'FECHADA', 'TELEMETRIA', TRUE),
(1, 6, 6, 6, '2026-03-08 10:15:00', '2026-03-09 08:00:00', 12.5, 2.0, 6.0, 4.5, 3, 705, 2, TRUE, 'FECHADA', 'TELEMETRIA', TRUE);

-- =============================================================================
-- 20. MANUTENÇÕES
-- =============================================================================
INSERT INTO manutencoes (data_manutencao, descricao, custo, tipo, oficina, km_realizacao,
                        proxima_manutencao_km, proxima_manutencao_data, observacoes,
                        veiculo_id, motorista_id)
VALUES 
('2026-02-15', 'Troca de óleo e filtros', 850.00, 'PREVENTIVA', 'Auto Mecânica Centro', 15000, 20000, '2026-05-15', 'Óleo sintético 15W40', 1, 1),
('2026-02-20', 'Alinhamento e balanceamento', 320.00, 'PREVENTIVA', 'Pneus Center', 22450, 27450, '2026-05-20', 'Pneus dianteiros', 2, 2),
('2026-01-10', 'Reparo no sistema de freios', 1250.00, 'CORRETIVA', 'Freios Express', 18000, 28000, '2026-04-10', 'Pastilhas e discos', 3, 3),
('2026-03-05', 'Revisão completa 20000km', 2100.00, 'PREVENTIVA', 'Concessionária', 20000, 30000, '2026-06-05', 'Inclui troca de fluidos', 4, 4),
('2026-02-28', 'Troca de pneus', 4800.00, 'CORRETIVA', 'Pneus Center', 29500, 39500, '2026-08-28', 'Pneus novos Michelin', 5, 5),
('2026-03-01', 'Manutenção do motor', 3500.00, 'CORRETIVA', 'Mecânica Diesel', 30100, 40100, '2026-06-01', 'Reparo no turbo', 6, 6);

-- =============================================================================
-- 21. USUÁRIOS
-- =============================================================================
-- Senha: admin123 (codificada em BCrypt - exemplo)
INSERT INTO usuarios (login, senha, nome, email, cpf, ativo, perfil, motorista_id)
VALUES 
('admin', '$2a$10$X7VYx8Yx8Yx8Yx8Yx8Yx8u', 'Administrador Sistema', 'admin@telemetria.com', '111.222.333-44', TRUE, 'ADMIN', NULL),
('operador1', '$2a$10$X7VYx8Yx8Yx8Yx8Yx8Yx8u', 'João Operador', 'joao.operador@telemetria.com', '222.333.444-55', TRUE, 'OPERADOR', NULL),
('operador2', '$2a$10$X7VYx8Yx8Yx8Yx8Yx8Yx8u', 'Maria Operadora', 'maria.operadora@telemetria.com', '333.444.555-66', TRUE, 'OPERADOR', NULL),
('gestor1', '$2a$10$X7VYx8Yx8Yx8Yx8Yx8Yx8u', 'Carlos Gestor', 'carlos.gestor@telemetria.com', '444.555.666-77', TRUE, 'GESTOR', NULL),
('motorista.joao', '$2a$10$X7VYx8Yx8Yx8Yx8Yx8Yx8u', 'João da Silva', 'joao.silva@telemetria.com', '123.456.789-00', TRUE, 'MOTORISTA', 1),
('motorista.maria', '$2a$10$X7VYx8Yx8Yx8Yx8Yx8Yx8u', 'Maria Santos', 'maria.santos@telemetria.com', '234.567.890-11', TRUE, 'MOTORISTA', 2),
('motorista.pedro', '$2a$10$X7VYx8Yx8Yx8Yx8Yx8Yx8u', 'Pedro Oliveira', 'pedro.oliveira@telemetria.com', '345.678.901-22', TRUE, 'MOTORISTA', 3),
('motorista.ana', '$2a$10$X7VYx8Yx8Yx8Yx8Yx8Yx8u', 'Ana Costa', 'ana.costa@telemetria.com', '456.789.012-33', TRUE, 'MOTORISTA', 4),
('motorista.carlos', '$2a$10$X7VYx8Yx8Yx8Yx8Yx8Yx8u', 'Carlos Souza', 'carlos.souza@telemetria.com', '567.890.123-44', TRUE, 'MOTORISTA', 5);

-- =============================================================================
-- 22. RESUMO DIÁRIO VEÍCULO
-- =============================================================================
INSERT INTO resumo_diario_veiculo (tenant_id, veiculo_id, data, km_total, horas_uso, horas_ocioso,
                                  litros_consumidos, consumo_medio, velocidade_media, velocidade_maxima,
                                  frenagens_bruscas, aceleracoes_bruscas, excessos_velocidade,
                                  total_alertas, total_viagens, score_dia, total_eventos)
VALUES 
(1, 1, '2026-03-01', 428, 9.5, 1.2, 171.2, 2.5, 75.8, 98, 3, 2, 1, 2, 1, 950, 350),
(1, 2, '2026-03-10', 312, 6.5, 0.8, 124.8, 2.5, 72.5, 92, 2, 3, 0, 1, 1, 960, 280),
(2, 3, '2026-03-05', 0, 0.5, 0.5, 0, 0, 0, 0, 0, 0, 0, 1, 0, 850, 5),
(2, 4, '2026-03-08', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1000, 0),
(3, 5, '2026-03-09', 405, 8.5, 1.5, 162, 2.5, 70.2, 88, 2, 1, 0, 1, 1, 930, 320);

-- =============================================================================
-- FIM DO SCRIPT
-- =============================================================================