<?php
require_once '../config.php';
$page_title = 'Dashboard';
$page_icon = '📊';

// Dados simulados
$stats = [
    'veiculos_ativos' => 12,
    'veiculos_total' => 15,
    'entregas_hoje' => 87,
    'entregas_concluidas' => 64,
    'motoristas_ativos' => 10,
    'alertas_ativos' => 3,
    'eficiencia_media' => 92.5
];

$veiculos_em_rota = [
    ['id' => 'V001', 'placa' => 'ABC-1234', 'motorista' => 'João Silva', 'status' => 'em_rota', 'progresso' => 65, 'eta' => '14:30'],
    ['id' => 'V002', 'placa' => 'DEF-5678', 'motorista' => 'Maria Santos', 'status' => 'em_rota', 'progresso' => 40, 'eta' => '16:15'],
    ['id' => 'V003', 'placa' => 'GHI-9012', 'motorista' => 'Pedro Costa', 'status' => 'parado', 'progresso' => 80, 'eta' => '13:45'],
    ['id' => 'V004', 'placa' => 'JKL-3456', 'motorista' => 'Ana Oliveira', 'status' => 'em_rota', 'progresso' => 25, 'eta' => '17:00'],
];

$alertas_recentes = [
    ['id' => 1, 'tipo' => 'DESVIO_ROTA', 'veiculo' => 'ABC-1234', 'motorista' => 'João Silva', 'gravidade' => 'MEDIA', 'timestamp' => '2026-04-20 09:15'],
    ['id' => 2, 'tipo' => 'PARADA_LONGA', 'veiculo' => 'GHI-9012', 'motorista' => 'Pedro Costa', 'gravidade' => 'BAIXA', 'timestamp' => '2026-04-20 08:45'],
    ['id' => 3, 'tipo' => 'EXCESSO_VELOCIDADE', 'veiculo' => 'DEF-5678', 'motorista' => 'Maria Santos', 'gravidade' => 'ALTA', 'timestamp' => '2026-04-20 08:30'],
];
?>
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?= $page_title ?> | <?= APP_NAME ?></title>
    <link rel="stylesheet" href="../style.css">
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 20px;
            margin-bottom: 25px;
        }
        .stat-card {
            background: white;
            padding: 20px;
            border-radius: 12px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
        }
        .stat-value { font-size: 32px; font-weight: bold; color: #1a1a2e; }
        .stat-label { color: #666; font-size: 14px; }
        .stat-trend { font-size: 13px; margin-top: 8px; }
        .stat-trend.up { color: #27ae60; }
        .dashboard-row {
            display: grid;
            grid-template-columns: 1.5fr 1fr;
            gap: 25px;
            margin-bottom: 25px;
        }
        .card {
            background: white;
            border-radius: 12px;
            padding: 20px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
        }
        .card-title {
            font-size: 18px;
            font-weight: 600;
            margin-bottom: 15px;
            display: flex;
            justify-content: space-between;
        }
        .vehicle-list { max-height: 350px; overflow-y: auto; }
        .vehicle-item {
            display: flex;
            align-items: center;
            padding: 12px 0;
            border-bottom: 1px solid #eee;
        }
        .vehicle-item:last-child { border-bottom: none; }
        .vehicle-info { flex: 1; }
        .vehicle-plate { font-weight: 600; }
        .vehicle-driver { font-size: 13px; color: #666; }
        .progress-bar {
            width: 100px;
            height: 6px;
            background: #e0e0e0;
            border-radius: 3px;
            margin: 5px 0;
            overflow: hidden;
        }
        .progress-fill {
            height: 100%;
            background: #2c5364;
            border-radius: 3px;
        }
        .alert-item {
            display: flex;
            padding: 12px 0;
            border-bottom: 1px solid #eee;
        }
        .alert-icon {
            width: 40px;
            height: 40px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            margin-right: 12px;
        }
        .alert-icon.CRITICA { background: #fee; color: #c00; }
        .alert-icon.ALTA { background: #fef3e0; color: #e67e22; }
        .alert-icon.MEDIA { background: #fff3cd; color: #856404; }
        .alert-icon.BAIXA { background: #d5f5e3; color: #1e8449; }
        .alert-content { flex: 1; }
        .alert-title { font-weight: 600; margin-bottom: 3px; }
        .alert-meta { font-size: 12px; color: #666; }
        .map-preview {
            height: 200px;
            background: #e8f4f8;
            border-radius: 8px;
            display: flex;
            align-items: center;
            justify-content: center;
            color: #666;
        }
        .quick-actions {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 15px;
            margin-top: 20px;
        }
        .quick-btn {
            background: #f5f5f5;
            padding: 15px;
            border-radius: 8px;
            text-align: center;
            text-decoration: none;
            color: #333;
            font-weight: 500;
            transition: all 0.2s;
        }
        .quick-btn:hover {
            background: #2c5364;
            color: white;
        }
        .status-badge {
            padding: 4px 8px;
            border-radius: 12px;
            font-size: 11px;
            font-weight: 600;
        }
        .status-em_rota { background: #d1ecf1; color: #0c5460; }
        .status-parado { background: #fef9e7; color: #7d6608; }
        .live-badge {
            background: #e74c3c;
            color: white;
            padding: 2px 8px;
            border-radius: 12px;
            font-size: 11px;
            animation: pulse 2s infinite;
        }
        @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.5; }
        }
        @media (max-width: 1024px) {
            .stats-grid { grid-template-columns: repeat(2, 1fr); }
            .dashboard-row { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>
    <?php include '../components/sidebar.php'; ?>
    
    <div class="main-content">
        <div class="top-bar">
            <h1 class="page-title"><span><?= $page_icon ?></span> <?= $page_title ?></h1>
            <div class="header-actions">
                <span class="live-badge">🟢 AO VIVO</span>
            </div>
        </div>
        
        <!-- Stats Cards -->
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-value"><?= $stats['veiculos_ativos'] ?>/<?= $stats['veiculos_total'] ?></div>
                <div class="stat-label">Veículos Ativos</div>
                <div class="stat-trend up">↑ 2 desde ontem</div>
            </div>
            <div class="stat-card">
                <div class="stat-value"><?= $stats['entregas_concluidas'] ?>/<?= $stats['entregas_hoje'] ?></div>
                <div class="stat-label">Entregas Hoje</div>
                <div class="stat-trend"><?= round(($stats['entregas_concluidas']/$stats['entregas_hoje'])*100) ?>% concluído</div>
            </div>
            <div class="stat-card">
                <div class="stat-value"><?= $stats['motoristas_ativos'] ?></div>
                <div class="stat-label">Motoristas Ativos</div>
                <div class="stat-trend up">↑ 1 em treinamento</div>
            </div>
            <div class="stat-card">
                <div class="stat-value"><?= $stats['eficiencia_media'] ?>%</div>
                <div class="stat-label">Eficiência Média</div>
                <div class="stat-trend up">↑ 2.5% vs ontem</div>
            </div>
        </div>
        
        <div class="dashboard-row">
            <!-- Veículos em Rota -->
            <div class="card">
                <div class="card-title">
                    <span>🚚 Veículos em Rota</span>
                    <a href="monitoramento.php" style="color: #2c5364; text-decoration: none;">Ver todos →</a>
                </div>
                <div class="vehicle-list">
                    <?php foreach ($veiculos_em_rota as $v): ?>
                    <div class="vehicle-item">
                        <div style="margin-right: 15px;">
                            <span style="font-size: 24px;">🚛</span>
                        </div>
                        <div class="vehicle-info">
                            <div class="vehicle-plate"><?= $v['placa'] ?></div>
                            <div class="vehicle-driver"><?= $v['motorista'] ?></div>
                            <div class="progress-bar">
                                <div class="progress-fill" style="width: <?= $v['progresso'] ?>%"></div>
                            </div>
                        </div>
                        <div style="text-align: right;">
                            <span class="status-badge status-<?= $v['status'] ?>"><?= ucfirst($v['status']) ?></span>
                            <div style="font-size: 12px; margin-top: 5px;">ETA: <?= $v['eta'] ?></div>
                        </div>
                    </div>
                    <?php endforeach; ?>
                </div>
            </div>
            
            <!-- Alertas Recentes -->
            <div class="card">
                <div class="card-title">
                    <span>⚠️ Alertas Recentes</span>
                    <a href="alertas.php" style="color: #2c5364; text-decoration: none;">Ver todos →</a>
                </div>
                <div style="max-height: 350px; overflow-y: auto;">
                    <?php foreach ($alertas_recentes as $alerta): ?>
                    <div class="alert-item">
                        <div class="alert-icon <?= $alerta['gravidade'] ?>">
                            <?= $alerta['tipo'] === 'DESVIO_ROTA' ? '🔄' : ($alerta['tipo'] === 'PARADA_LONGA' ? '⏸️' : '⚡') ?>
                        </div>
                        <div class="alert-content">
                            <div class="alert-title"><?= str_replace('_', ' ', $alerta['tipo']) ?></div>
                            <div class="alert-meta"><?= $alerta['veiculo'] ?> • <?= $alerta['motorista'] ?></div>
                        </div>
                        <div style="font-size: 12px; color: #666;">
                            <?= date('H:i', strtotime($alerta['timestamp'])) ?>
                        </div>
                    </div>
                    <?php endforeach; ?>
                </div>
            </div>
        </div>
        
        <!-- Mapa Preview e Ações Rápidas -->
        <div class="dashboard-row">
            <div class="card">
                <div class="card-title">🗺️ Visão Geral do Mapa</div>
                <div class="map-preview">
                    <div style="text-align: center;">
                        <span style="font-size: 48px;">🗺️</span>
                        <p>Mapa em tempo real<br><a href="monitoramento.php" style="color: #2c5364;">Abrir mapa completo →</a></p>
                    </div>
                </div>
            </div>
            <div class="card">
                <div class="card-title">⚡ Ações Rápidas</div>
                <div class="quick-actions">
                    <a href="rotas.php?nova" class="quick-btn">📍 Nova Rota</a>
                    <a href="veiculos.php?novo" class="quick-btn">🚚 Adicionar Veículo</a>
                    <a href="motoristas.php?novo" class="quick-btn">👤 Cadastrar Motorista</a>
                    <a href="relatorios.php" class="quick-btn">📊 Gerar Relatório</a>
                </div>
            </div>
        </div>
    </div>
</body>
</html>