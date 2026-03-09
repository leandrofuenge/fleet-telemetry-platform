-- =========================================
-- DADOS INICIAIS - V2 (Sincronizado com V1)
-- =========================================

-- CLIENTES
INSERT INTO clientes (nome_razao_social, cnpj, created_at, updated_at, ativo) VALUES
('TransLog LTDA', '12.345.678/0001-99', NOW(), NOW(), TRUE),
('Carga Pesada Brasil', '98.765.432/0001-11', NOW(), NOW(), TRUE);

-- VEÍCULOS
INSERT INTO veiculos (placa, modelo, marca, ano_fabricacao, capacidade_carga, tipo_veiculo, consumo_medio, created_at, updated_at, ativo) VALUES
('ABC1D23', 'Volvo FH 540', 'Volvo', 2023, 25000, 'CAMINHAO', 2.5, NOW(), NOW(), TRUE),
('XYZ9K87', 'Scania R450', 'Scania', 2022, 22000, 'CAMINHAO', 2.8, NOW(), NOW(), TRUE);

-- MOTORISTAS
INSERT INTO motoristas (nome, cpf, cnh, categoria_cnh, data_validade_cnh, telefone, email, data_nascimento, created_at, updated_at, ativo) VALUES
('João da Silva', '123.456.789-00', '12345678901', 'E', '2026-12-31', '(11) 99999-9999', 'joao.silva@email.com', '1985-05-15', NOW(), NOW(), TRUE),
('Carlos Oliveira', '987.654.321-00', '10987654321', 'D', '2025-10-31', '(11) 98888-8888', 'carlos.oliveira@email.com', '1990-08-22', NOW(), NOW(), TRUE);

-- USUÁRIOS
INSERT INTO usuarios (login, senha, nome, email, ativo, perfil, ultimo_acesso, tentativas_login, bloqueado, created_at, updated_at) VALUES
('gestor1', '123456', 'Gestor Operacional', 'gestor@telemetria.com', TRUE, 'GERENTE', NOW(), 0, FALSE, NOW(), NOW()),
('motorista1', '123456', 'João da Silva', 'joao.silva@email.com', TRUE, 'MOTORISTA', NOW(), 0, FALSE, NOW(), NOW()),
('motorista2', '123456', 'Carlos Oliveira', 'carlos.oliveira@email.com', TRUE, 'MOTORISTA', NOW(), 0, FALSE, NOW(), NOW());

-- CARGAS (depende de clientes)
INSERT INTO cargas (descricao, peso, tipo_carga, valor, cliente_id, status, created_at, updated_at) VALUES
('Carga de Eletrônicos', 1200.50, 'Eletrônicos', 150000.00, 1, 'CARREGADA', NOW(), NOW()),
('Carga de Alimentos Refrigerados', 3500.00, 'Alimentos', 45000.00, 2, 'CARREGADA', NOW(), NOW());

-- ROTAS
INSERT INTO rotas (nome, origem, destino, latitude_origem, longitude_origem, latitude_destino, longitude_destino, 
                   distancia_prevista, tempo_previsto, ativa, veiculo_id, created_at, updated_at) VALUES
('Rota SP-RJ', 'São Paulo, SP', 'Rio de Janeiro, RJ', -23.5505, -46.6333, -22.9068, -43.1729, 430.5, 360, TRUE, 1, NOW(), NOW()),
('Rota SP-BH', 'São Paulo, SP', 'Belo Horizonte, MG', -23.5505, -46.6333, -19.9167, -43.9345, 586.0, 480, TRUE, 2, NOW(), NOW());

-- MANUTENÇÕES (depende de veiculos)
INSERT INTO manutencoes (veiculo_id, data_manutencao, descricao, custo, tipo, status, observacoes, created_at, updated_at) VALUES
(1, '2025-01-10', 'Troca de óleo e filtros', 1500.00, 'PREVENTIVA', 'CONCLUIDA', 'Troca de óleo e filtros de ar e combustível', NOW(), NOW()),
(2, '2025-01-15', 'Substituição de pastilhas de freio', 2200.00, 'CORRETIVA', 'CONCLUIDA', 'Pastilhas dianteiras e traseiras', NOW(), NOW());

-- VIAGENS (depende de veiculos, motoristas, cargas, rotas)
INSERT INTO viagens (veiculo_id, motorista_id, carga_id, rota_id, data_saida, data_chegada_prevista, status, observacoes, created_at, updated_at) VALUES
(1, 1, 1, 1, NOW(), DATE_ADD(NOW(), INTERVAL 2 DAY), 'EM_ANDAMENTO', 'Carga de eletrônicos com seguro', NOW(), NOW()),
(2, 2, 2, 2, NOW(), DATE_ADD(NOW(), INTERVAL 3 DAY), 'EM_ANDAMENTO', 'Carga refrigerada - temperatura controlada', NOW(), NOW());

-- GEOFENCES
INSERT INTO geofences (nome, tipo, latitude_centro, longitude_centro, raio, vertices, ativo, created_at) VALUES
('Zona Sul SP', 'CIRCULO', -23.6500, -46.7000, 5000, NULL, TRUE, NOW()),
('Zona Norte SP', 'CIRCULO', -23.4800, -46.6300, 5000, NULL, TRUE, NOW()),
('Centro RJ', 'RETANGULO', -22.9068, -43.1729, NULL, '{"norte": -22.8900, "sul": -22.9200, "leste": -43.1600, "oeste": -43.1900}', TRUE, NOW());

-- VEICULO_GEOFENCE (relacionamento)
INSERT INTO veiculo_geofence (veiculo_id, geofence_id, ativo, created_at) VALUES
(1, 1, TRUE, NOW()),
(1, 2, TRUE, NOW()),
(2, 3, TRUE, NOW());

-- TELEMETRIA (dados simulados - depende de veiculos)
INSERT INTO telemetria (
    veiculo_id, viagem_id, latitude, longitude, velocidade, odometro, altitude, 
    precisao_gps, satelites, direcao, aceleracao, inclinacao, ignicao, 
    nivel_combustivel, temperatura_motor, pressao_oleo, consumo_combustivel, 
    tensao_bateria, carga_motor, torque_motor, rpm, frenagem_brusca, 
    numero_frenagens, numero_aceleracoes_bruscas, pontuacao_motorista, 
    tempo_motor_ligado, tempo_ocioso, colisao_detectada, excesso_velocidade, 
    geofence_violada, cinto_seguranca, porta_aberta, temperatura_externa, 
    umidade_externa, chuva_detectada, sinal_gsm, sinal_gps, firmware_versao, 
    imei_dispositivo, manutencao_pendente, proxima_revisao, horas_motor, 
    desgaste_freio, data_hora, created_at
) VALUES
(
    1, 1, -23.5505, -46.6333, 80.5, 150000, 760, 1.2, 12, 180, 0.5, 2.0, TRUE,
    65.0, 90.0, 4.5, 2.8, 24.5, 70.0, 1800.0, 1500, FALSE,
    3, 1, 95, 7200, 600, FALSE, FALSE, FALSE, TRUE, FALSE,
    28.0, 60.0, FALSE, 85.0, 90.0, 'v1.0.0', '359881234567890',
    FALSE, DATE_ADD(NOW(), INTERVAL 30 DAY), 4500, 20.0, NOW(), NOW()
),
(
    2, 2, -23.5200, -46.6000, 65.2, 180000, 750, 1.5, 10, 90, 0.3, 1.5, TRUE,
    70.0, 88.0, 4.2, 3.1, 25.0, 65.0, 1700.0, 1300, FALSE,
    2, 0, 98, 5000, 300, FALSE, FALSE, FALSE, TRUE, FALSE,
    29.0, 65.0, FALSE, 82.0, 88.0, 'v1.0.0', '359881234567891',
    FALSE, DATE_ADD(NOW(), INTERVAL 45 DAY), 5200, 15.0, DATE_SUB(NOW(), INTERVAL 30 MINUTE), NOW()
);

-- ALERTAS
INSERT INTO alertas (veiculo_id, motorista_id, tipo, gravidade, mensagem, latitude, longitude, 
                     velocidade, lido, resolvido, data_hora, created_at) VALUES
(1, 1, 'MANUTENCAO', 'MEDIA', 'Manutenção preventiva programada para 30 dias', -23.5505, -46.6333, 80.5, FALSE, FALSE, NOW(), NOW()),
(2, 2, 'COMBUSTIVEL', 'BAIXA', 'Nível de combustível abaixo de 30%', -23.5200, -46.6000, 65.2, FALSE, FALSE, DATE_SUB(NOW(), INTERVAL 15 MINUTE), NOW());