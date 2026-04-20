<?php
require_once '../config.php';
$page_title = 'Dashboard';
$page_icon = '📊';

// Verificar se usuário está logado
if (!isset($_SESSION['usuario'])) {
    header('Location: login.php');
    exit;
}

// Função para chamar a API de dashboard
function getDashboardStats() {
    $url = API_URL . '/dashboard/stats';
    
    $options = [
        'http' => [
            'header' => "Content-Type: application/json\r\n" .
                       "Authorization: Bearer " . ($_SESSION['token'] ?? '') . "\r\n",
            'method' => 'GET',
            'timeout' => 30
        ]
    ];
    
    $context = stream_context_create($options);
    $resultado = @file_get_contents($url, false, $context);
    
    if ($resultado === false) {
        return null;
    }
    
    return json_decode($resultado, true);
}

// Função para buscar veículos em rota
function getVeiculosEmRota() {
    $url = API_URL . '/veiculos/em-rota';
    
    $options = [
        'http' => [
            'header' => "Content-Type: application/json\r\n" .
                       "Authorization: Bearer " . ($_SESSION['token'] ?? '') . "\r\n",
            'method' => 'GET',
            'timeout' => 30
        ]
    ];
    
    $context = stream_context_create($options);
    $resultado = @file_get_contents($url, false, $context);
    
    if ($resultado === false) {
        return [];
    }
    
    return json_decode($resultado, true);
}

// Função para buscar alertas recentes
function getAlertasRecentes() {
    $url = API_URL . '/alertas/recentes?limit=5';
    
    $options = [
        'http' => [
            'header' => "Content-Type: application/json\r\n" .
                       "Authorization: Bearer " . ($_SESSION['token'] ?? '') . "\r\n",
            'method' => 'GET',
            'timeout' => 30
        ]
    ];
    
    $context = stream_context_create($options);
    $resultado = @file_get_contents($url, false, $context);
    
    if ($resultado === false) {
        return [];
    }
    
    return json_decode($resultado, true);
}

// Buscar dados da API
$stats = getDashboardStats();
$veiculos_em_rota = getVeiculosEmRota();
$alertas_recentes = getAlertasRecentes();

// Verificar se API está disponível
$api_disponivel = ($stats !== null);
$erro_api = !$api_disponivel;

// Valores padrão apenas se API falhar (fallback vazio)
if (!$api_disponivel) {
    $stats = [
        'veiculos_ativos' => 0,
        'veiculos_total' => 0,
        'entregas_hoje' => 0,
        'entregas_concluidas' => 0,
        'motoristas_ativos' => 0,
        'alertas_ativos' => 0,
        'eficiencia_media' => 0
    ];
}

// Verificar se as funções já existem no config.php antes de declarar
if (!function_exists('getAlertaIcon')) {
    function getAlertaIcon($tipo) {
        $icones = [
            'DESVIO_ROTA' => '🔄',
            'PARADA_LONGA' => '⏸️',
            'EXCESSO_VELOCIDADE' => '⚡',
            'FRENAGEM_BRUSCA' => '🛑',
            'COLISAO' => '💥',
            'MANUTENCAO' => '🔧',
            'GEOFENCE' => '🚧'
        ];
        return $icones[$tipo] ?? '⚠️';
    }
}

if (!function_exists('traduzirTipoAlerta')) {
    function traduzirTipoAlerta($tipo) {
        $traducoes = [
            'DESVIO_ROTA' => 'Desvio de Rota',
            'PARADA_LONGA' => 'Parada Longa',
            'EXCESSO_VELOCIDADE' => 'Excesso de Velocidade',
            'FRENAGEM_BRUSCA' => 'Frenagem Brusca',
            'COLISAO' => 'Colisão Detectada',
            'MANUTENCAO' => 'Manutenção Pendente',
            'GEOFENCE' => 'Violação de Geofence'
        ];
        return $traducoes[$tipo] ?? str_replace('_', ' ', $tipo);
    }
}
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
        .stat-trend.down { color: #e74c3c; }
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
        .alert-icon.CRITICO { background: #fee; color: #c00; }
        .alert-icon.ALTO { background: #fef3e0; color: #e67e22; }
        .alert-icon.MEDIO { background: #fff3cd; color: #856404; }
        .alert-icon.BAIXO { background: #d5f5e3; color: #1e8449; }
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
        .status-offline { background: #f8d7da; color: #721c24; }
        .live-badge {
            background: #e74c3c;
            color: white;
            padding: 2px 8px;
            border-radius: 12px;
            font-size: 11px;
            animation: pulse 2s infinite;
        }
        .api-error {
            background: #fadbd8;
            color: #922b21;
            padding: 15px;
            border-radius: 8px;
            margin-bottom: 20px;
            border-left: 4px solid #e74c3c;
        }
        .empty-state {
            text-align: center;
            padding: 40px;
            color: #999;
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
        
        <!-- Mensagem de erro da API -->
        <?php if ($erro_api): ?>
        <div class="api-error">
            ⚠️ Não foi possível conectar à API. Verifique se o serviço está disponível em <?= API_URL ?>
        </div>
        <?php endif; ?>
        
        <!-- Stats Cards -->
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-value"><?= $stats['veiculos_ativos'] ?>/<?= $stats['veiculos_total'] ?></div>
                <div class="stat-label">Veículos Ativos</div>
            </div>
            <div class="stat-card">
                <div class="stat-value"><?= $stats['entregas_concluidas'] ?>/<?= $stats['entregas_hoje'] ?></div>
                <div class="stat-label">Entregas Hoje</div>
                <?php if ($stats['entregas_hoje'] > 0): ?>
                <div class="stat-trend"><?= round(($stats['entregas_concluidas']/$stats['entregas_hoje'])*100) ?>% concluído</div>
                <?php endif; ?>
            </div>
            <div class="stat-card">
                <div class="stat-value"><?= $stats['motoristas_ativos'] ?></div>
                <div class="stat-label">Motoristas Ativos</div>
            </div>
            <div class="stat-card">
                <div class="stat-value"><?= number_format($stats['eficiencia_media'], 1) ?>%</div>
                <div class="stat-label">Eficiência Média</div>
            </div>
        </div>
        
        <div class="dashboard-row">
            <!-- Veículos em Rota -->
            <div class="card">
                <div class="card-title">
                    <span>🚚 Veículos em Rota</span>
                    <a href="monitoramento.php" style="color: #2c5364; text-decoration: none;">Ver todos →</a>
                </div>
                <?php if (empty($veiculos_em_rota)): ?>
                <div class="empty-state">
                    <span style="font-size: 48px;">🚛</span>
                    <p>Nenhum veículo em rota no momento</p>
                </div>
                <?php else: ?>
                <div class="vehicle-list">
                    <?php foreach ($veiculos_em_rota as $v): ?>
                    <div class="vehicle-item">
                        <div style="margin-right: 15px;">
                            <span style="font-size: 24px;">🚛</span>
                        </div>
                        <div class="vehicle-info">
                            <div class="vehicle-plate"><?= htmlspecialchars($v['placa'] ?? $v['id'] ?? 'N/A') ?></div>
                            <div class="vehicle-driver"><?= htmlspecialchars($v['motorista'] ?? 'Sem motorista') ?></div>
                            <?php if (isset($v['progresso'])): ?>
                            <div class="progress-bar">
                                <div class="progress-fill" style="width: <?= $v['progresso'] ?>%"></div>
                            </div>
                            <?php endif; ?>
                        </div>
                        <div style="text-align: right;">
                            <span class="status-badge status-<?= $v['status'] ?? 'offline' ?>">
                                <?= ucfirst($v['status'] ?? 'Offline') ?>
                            </span>
                            <?php if (isset($v['eta'])): ?>
                            <div style="font-size: 12px; margin-top: 5px;">ETA: <?= $v['eta'] ?></div>
                            <?php endif; ?>
                        </div>
                    </div>
                    <?php endforeach; ?>
                </div>
                <?php endif; ?>
            </div>
            
            <!-- Alertas Recentes -->
            <div class="card">
                <div class="card-title">
                    <span>⚠️ Alertas Recentes</span>
                    <a href="alertas.php" style="color: #2c5364; text-decoration: none;">Ver todos →</a>
                </div>
                <?php if (empty($alertas_recentes)): ?>
                <div class="empty-state">
                    <span style="font-size: 48px;">✅</span>
                    <p>Nenhum alerta recente</p>
                </div>
                <?php else: ?>
                <div style="max-height: 350px; overflow-y: auto;">
                    <?php foreach ($alertas_recentes as $alerta): ?>
                    <div class="alert-item">
                        <div class="alert-icon <?= $alerta['severidade'] ?? 'MEDIO' ?>">
                            <?= getAlertaIcon($alerta['tipo'] ?? '') ?>
                        </div>
                        <div class="alert-content">
                            <div class="alert-title"><?= traduzirTipoAlerta($alerta['tipo'] ?? '') ?></div>
                            <div class="alert-meta">
                                <?= htmlspecialchars($alerta['veiculo'] ?? 'Veículo #' . ($alerta['veiculoId'] ?? '?')) ?>
                                <?php if (isset($alerta['motorista'])): ?>
                                • <?= htmlspecialchars($alerta['motorista']) ?>
                                <?php endif; ?>
                            </div>
                        </div>
                        <div style="font-size: 12px; color: #666;">
                            <?= isset($alerta['timestamp']) ? date('H:i', strtotime($alerta['timestamp'])) : '--:--' ?>
                        </div>
                    </div>
                    <?php endforeach; ?>
                </div>
                <?php endif; ?>
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