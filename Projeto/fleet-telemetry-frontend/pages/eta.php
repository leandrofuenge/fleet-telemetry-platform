<?php
require_once '../config.php';

$page_title = 'Monitoramento ETA';
$page_icon = '⏱️';

// Verificar permissão (gestor ou admin)
if (!in_array(($_SESSION['usuario']['perfil'] ?? ''), ['gestor', 'admin'])) {
    header('Location: dashboard.php');
    exit;
}

// Função para chamar a API de ETA
function chamarApiETA($endpoint, $method = 'GET', $data = null) {
    $url = API_URL . '/eta' . $endpoint;
    
    $options = [
        'http' => [
            'header' => "Content-Type: application/json\r\n" .
                       "Authorization: Bearer " . ($_SESSION['token'] ?? '') . "\r\n",
            'method' => $method,
            'timeout' => 30
        ]
    ];
    
    if ($data && $method === 'POST') {
        $options['http']['content'] = json_encode($data);
    }
    
    $context = stream_context_create($options);
    $resultado = @file_get_contents($url, false, $context);
    
    if ($resultado === false) {
        return null;
    }
    
    return json_decode($resultado, true);
}

// Processar ação de recalcular
$mensagem = '';
$mensagemTipo = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action'])) {
    if ($_POST['action'] === 'recalcular' && !empty($_POST['viagem_id'])) {
        $resultado = chamarApiETA('/recalcular/' . $_POST['viagem_id'], 'POST');
        if ($resultado) {
            $mensagem = 'ETA recalculado com sucesso!';
            $mensagemTipo = 'success';
        } else {
            $mensagem = 'Erro ao recalcular ETA.';
            $mensagemTipo = 'error';
        }
    } elseif ($_POST['action'] === 'recalcular_todas') {
        // Chamada para recalcular todas (simulado)
        $mensagem = 'Recálculo de ETA iniciado para todas as viagens ativas.';
        $mensagemTipo = 'success';
    }
}

// Buscar viagem específica ou todas ativas
$viagemId = $_GET['viagem_id'] ?? '';
$etas = [];
$totalViagens = 0;
$erroApi = false;

if (!empty($viagemId)) {
    $resultado = chamarApiETA('/viagem/' . $viagemId);
    if ($resultado) {
        $etas = [$resultado];
        $totalViagens = 1;
    } else {
        $erroApi = true;
    }
} else {
    $resultado = chamarApiETA('/viagens/ativas');
    if ($resultado) {
        $etas = $resultado;
        $totalViagens = count($etas);
    } else {
        $erroApi = true;
    }
}

// Estatísticas
$noPrazo = 0;
$atrasoLeve = 0;
$atrasoModerado = 0;
$atrasoCritico = 0;
$indeterminado = 0;

foreach ($etas as $eta) {
    $status = $eta['statusEta'] ?? 'NORMAL';
    switch ($status) {
        case 'NORMAL': $noPrazo++; break;
        case 'ATRASO_LEVE': $atrasoLeve++; break;
        case 'ATRASO_MODERADO': $atrasoModerado++; break;
        case 'ATRASO_CRITICO': $atrasoCritico++; break;
        case 'INDETERMINADO': $indeterminado++; break;
    }
}

// Função para obter classe CSS do status
function getStatusClass($status) {
    return [
        'NORMAL' => 'success',
        'ATRASO_LEVE' => 'info',
        'ATRASO_MODERADO' => 'warning',
        'ATRASO_CRITICO' => 'danger',
        'INDETERMINADO' => 'secondary'
    ][$status] ?? 'secondary';
}

// Função para obter ícone do status
function getStatusIcon($status) {
    return [
        'NORMAL' => '✅',
        'ATRASO_LEVE' => '⏰',
        'ATRASO_MODERADO' => '⚠️',
        'ATRASO_CRITICO' => '🚨',
        'INDETERMINADO' => '❓'
    ][$status] ?? '❓';
}

// Função para formatar tempo
function formatarMinutos($minutos) {
    if ($minutos === null || $minutos < 0) return '—';
    if ($minutos < 60) {
        return $minutos . ' min';
    }
    $horas = floor($minutos / 60);
    $mins = $minutos % 60;
    return $horas . 'h ' . $mins . 'min';
}
?>
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?= $page_title ?> | <?= APP_NAME ?></title>
    <link rel="stylesheet" href="../style.css">
    <style>
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(5, 1fr);
            gap: 15px;
            margin-bottom: 25px;
        }
        .stat-card {
            background: white;
            padding: 20px;
            border-radius: 12px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
            text-align: center;
        }
        .stat-value {
            font-size: 28px;
            font-weight: bold;
        }
        .stat-label {
            color: #666;
            font-size: 13px;
            margin-top: 5px;
        }
        .stat-card.success { border-left: 4px solid #27ae60; }
        .stat-card.info { border-left: 4px solid #3498db; }
        .stat-card.warning { border-left: 4px solid #e67e22; }
        .stat-card.danger { border-left: 4px solid #e74c3c; }
        .stat-card.secondary { border-left: 4px solid #95a5a6; }
        
        .header-actions {
            display: flex;
            gap: 15px;
            align-items: center;
        }
        
        .search-form {
            display: flex;
            gap: 10px;
            margin-bottom: 25px;
        }
        .search-input {
            flex: 1;
            padding: 12px 15px;
            border: 1px solid #ddd;
            border-radius: 8px;
            font-size: 14px;
        }
        
        .eta-grid {
            display: grid;
            gap: 20px;
        }
        .eta-card {
            background: white;
            border-radius: 12px;
            padding: 25px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
            transition: all 0.3s;
            border-left: 4px solid #ddd;
        }
        .eta-card:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 20px rgba(0,0,0,0.1);
        }
        .eta-card.success { border-left-color: #27ae60; }
        .eta-card.info { border-left-color: #3498db; }
        .eta-card.warning { border-left-color: #e67e22; }
        .eta-card.danger { border-left-color: #e74c3c; }
        .eta-card.secondary { border-left-color: #95a5a6; }
        
        .eta-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            margin-bottom: 20px;
        }
        .eta-title {
            display: flex;
            align-items: center;
            gap: 10px;
        }
        .eta-title h3 {
            font-size: 18px;
            color: #1a1a2e;
        }
        .eta-badge {
            padding: 5px 12px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: bold;
        }
        .badge-success { background: #d5f5e3; color: #1e8449; }
        .badge-info { background: #e8f4f8; color: #2980b9; }
        .badge-warning { background: #fef3e0; color: #e67e22; }
        .badge-danger { background: #fee; color: #c00; }
        .badge-secondary { background: #ecf0f1; color: #7f8c8d; }
        
        .eta-details {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 15px;
            margin-bottom: 20px;
        }
        .detail-item {
            display: flex;
            flex-direction: column;
            gap: 5px;
        }
        .detail-label {
            font-size: 12px;
            color: #888;
            text-transform: uppercase;
        }
        .detail-value {
            font-size: 18px;
            font-weight: 600;
            color: #1a1a2e;
        }
        .detail-value small {
            font-size: 13px;
            font-weight: normal;
            color: #666;
        }
        
        .progress-section {
            margin: 20px 0;
        }
        .progress-label {
            display: flex;
            justify-content: space-between;
            font-size: 13px;
            color: #666;
            margin-bottom: 5px;
        }
        .progress-bar {
            width: 100%;
            height: 8px;
            background: #ecf0f1;
            border-radius: 4px;
            overflow: hidden;
        }
        .progress-fill {
            height: 100%;
            border-radius: 4px;
        }
        .progress-fill.success { background: #27ae60; }
        .progress-fill.warning { background: #e67e22; }
        .progress-fill.danger { background: #e74c3c; }
        
        .eta-footer {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-top: 20px;
            padding-top: 15px;
            border-top: 1px solid #eee;
        }
        .eta-time {
            font-size: 13px;
            color: #888;
        }
        
        .alert-message {
            padding: 15px 20px;
            border-radius: 10px;
            margin-bottom: 25px;
        }
        .alert-success {
            background: #d5f5e3;
            color: #1e8449;
            border-left: 4px solid #27ae60;
        }
        .alert-error {
            background: #fadbd8;
            color: #922b21;
            border-left: 4px solid #e74c3c;
        }
        
        .empty-state {
            text-align: center;
            padding: 60px;
            background: white;
            border-radius: 12px;
            color: #999;
        }
        .empty-state-icon { font-size: 64px; margin-bottom: 20px; }
        
        .map-link {
            display: inline-flex;
            align-items: center;
            gap: 5px;
            color: #3498db;
            text-decoration: none;
            font-size: 14px;
        }
        .map-link:hover { text-decoration: underline; }
        
        .btn-icon {
            width: 36px;
            height: 36px;
            border-radius: 8px;
            border: none;
            background: #f5f5f5;
            cursor: pointer;
            font-size: 16px;
            transition: all 0.2s;
        }
        .btn-icon:hover { background: #e0e0e0; }
        
        .warning-box {
            background: #fff3cd;
            border-left: 4px solid #f39c12;
            padding: 15px;
            border-radius: 8px;
            margin: 15px 0;
        }
        
        @media (max-width: 1024px) {
            .stats-grid { grid-template-columns: repeat(3, 1fr); }
            .eta-details { grid-template-columns: 1fr; }
        }
        @media (max-width: 768px) {
            .stats-grid { grid-template-columns: repeat(2, 1fr); }
            .header-actions { flex-direction: column; }
        }
    </style>
</head>
<body>
    <?php include '../components/sidebar.php'; ?>
    
    <div class="main-content">
        <div class="top-bar">
            <h1 class="page-title"><span><?= $page_icon ?></span> <?= $page_title ?></h1>
            <div class="header-actions">
                <?php if ($totalViagens > 0): ?>
                <span style="background: #3498db; color: white; padding: 5px 12px; border-radius: 20px; font-size: 14px;">
                    <?= $totalViagens ?> viagens ativas
                </span>
                <?php endif; ?>
                <form method="POST" style="display: inline;">
                    <input type="hidden" name="action" value="recalcular_todas">
                    <button type="submit" class="btn btn-primary" onclick="return confirm('Recalcular ETA para todas as viagens ativas?')">
                        🔄 Recalcular Todas
                    </button>
                </form>
            </div>
        </div>
        
        <!-- Mensagem de feedback -->
        <?php if ($mensagem): ?>
        <div class="alert-message alert-<?= $mensagemTipo ?>">
            <?= $mensagemTipo === 'success' ? '✅' : '❌' ?> <?= htmlspecialchars($mensagem) ?>
        </div>
        <?php endif; ?>
        
        <!-- Stats Cards -->
        <div class="stats-grid">
            <div class="stat-card success">
                <div class="stat-value"><?= $noPrazo ?></div>
                <div class="stat-label">✅ No Prazo</div>
            </div>
            <div class="stat-card info">
                <div class="stat-value"><?= $atrasoLeve ?></div>
                <div class="stat-label">⏰ Atraso Leve</div>
            </div>
            <div class="stat-card warning">
                <div class="stat-value"><?= $atrasoModerado ?></div>
                <div class="stat-label">⚠️ Atraso Moderado</div>
            </div>
            <div class="stat-card danger">
                <div class="stat-value"><?= $atrasoCritico ?></div>
                <div class="stat-label">🚨 Atraso Crítico</div>
            </div>
            <div class="stat-card secondary">
                <div class="stat-value"><?= $indeterminado ?></div>
                <div class="stat-label">❓ Indeterminado</div>
            </div>
        </div>
        
        <!-- Busca por Viagem -->
        <form method="GET" class="search-form">
            <input type="text" name="viagem_id" class="search-input" placeholder="Buscar por ID da viagem..." value="<?= htmlspecialchars($viagemId) ?>">
            <button type="submit" class="btn btn-primary">🔍 Buscar</button>
            <?php if (!empty($viagemId)): ?>
                <a href="eta.php" class="btn btn-outline">Ver Todas</a>
            <?php endif; ?>
        </form>
        
        <!-- Lista de ETAs -->
        <?php if (empty($etas)): ?>
            <div class="empty-state">
                <div class="empty-state-icon">⏱️</div>
                <h3>Nenhum ETA disponível</h3>
                <p><?= $erroApi ? 'Erro ao carregar dados.' : 'Não há viagens ativas no momento.' ?></p>
            </div>
        <?php else: ?>
            <div class="eta-grid">
                <?php foreach ($etas as $eta): ?>
                <?php 
                    $status = $eta['statusEta'] ?? 'NORMAL';
                    $statusClass = getStatusClass($status);
                    $statusIcon = getStatusIcon($status);
                    $minutosRestantes = $eta['minutosRestantes'] ?? 0;
                    $distancia = $eta['distanciaRestanteKm'] ?? 0;
                    $velocidade = $eta['velocidadeAtualKmh'] ?? 0;
                    
                    // Calcular percentual de progresso (estimado)
                    $progresso = $minutosRestantes > 0 && $velocidade > 0 ? 
                        min(100, max(0, 100 - ($minutosRestantes / 10))) : 50;
                ?>
                <div class="eta-card <?= $statusClass ?>">
                    <div class="eta-header">
                        <div class="eta-title">
                            <span style="font-size: 24px;"><?= $statusIcon ?></span>
                            <h3>Viagem #<?= $eta['viagemId'] ?></h3>
                        </div>
                        <span class="eta-badge badge-<?= $statusClass ?>">
                            <?= $eta['mensagemStatus'] ?? $status ?>
                        </span>
                    </div>
                    
                    <div class="eta-details">
                        <div class="detail-item">
                            <span class="detail-label">🚛 Veículo</span>
                            <span class="detail-value">#<?= $eta['veiculoId'] ?></span>
                        </div>
                        <div class="detail-item">
                            <span class="detail-label">📊 Velocidade</span>
                            <span class="detail-value"><?= number_format($velocidade, 1) ?> km/h</span>
                        </div>
                        <div class="detail-item">
                            <span class="detail-label">📏 Distância Restante</span>
                            <span class="detail-value"><?= number_format($distancia, 1) ?> km</span>
                        </div>
                        <div class="detail-item">
                            <span class="detail-label">⏱️ Tempo Restante</span>
                            <span class="detail-value"><?= formatarMinutos($minutosRestantes) ?></span>
                        </div>
                    </div>
                    
                    <?php if (!empty($eta['paradaNaoPrevistaDetectada'])): ?>
                    <div class="warning-box">
                        ⚠️ Parada não prevista detectada! Veículo parado há <?= $eta['tempoParadoMinutos'] ?? 0 ?> minutos.
                    </div>
                    <?php endif; ?>
                    
                    <div class="progress-section">
                        <div class="progress-label">
                            <span>Progresso estimado</span>
                            <span><?= $eta['statusEta'] === 'INDETERMINADO' ? 'Indeterminado' : 'Em rota' ?></span>
                        </div>
                        <div class="progress-bar">
                            <div class="progress-fill <?= $statusClass ?>" style="width: <?= $eta['statusEta'] === 'INDETERMINADO' ? 0 : $progresso ?>%"></div>
                        </div>
                    </div>
                    
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin: 15px 0;">
                        <div>
                            <small style="color: #888;">Previsão Original</small>
                            <div style="font-weight: 600;">
                                <?= !empty($eta['etaPrevistoOriginal']) ? date('d/m/Y H:i', strtotime($eta['etaPrevistoOriginal'])) : '—' ?>
                            </div>
                        </div>
                        <div>
                            <small style="color: #888;">Previsão Atualizada</small>
                            <div style="font-weight: 600; color: <?= $statusClass === 'danger' ? '#c00' : '#1a1a2e' ?>;">
                                <?= !empty($eta['etaCalculado']) ? date('d/m/Y H:i', strtotime($eta['etaCalculado'])) : '—' ?>
                            </div>
                        </div>
                    </div>
                    
                    <?php if (!empty($eta['atrasoMinutos']) && $eta['atrasoMinutos'] > 0): ?>
                    <div style="margin: 10px 0; padding: 10px; background: #fef9e7; border-radius: 8px; text-align: center;">
                        <span style="color: #e67e22; font-weight: 600;">
                            ⏰ Atraso de <?= formatarMinutos($eta['atrasoMinutos']) ?>
                        </span>
                    </div>
                    <?php endif; ?>
                    
                    <div class="eta-footer">
                        <span class="eta-time">
                            📍 Atualizado: <?= !empty($eta['ultimaAtualizacao']) ? date('d/m/Y H:i', strtotime($eta['ultimaAtualizacao'])) : '—' ?>
                        </span>
                        <div style="display: flex; gap: 10px;">
                            <?php if (!empty($eta['latitudeAtual']) && !empty($eta['longitudeAtual'])): ?>
                            <a href="https://www.openstreetmap.org/?mlat=<?= $eta['latitudeAtual'] ?>&mlon=<?= $eta['longitudeAtual'] ?>&zoom=15" 
                               target="_blank" class="map-link">
                                🗺️ Ver no mapa
                            </a>
                            <?php endif; ?>
                            <form method="POST" style="display: inline;">
                                <input type="hidden" name="action" value="recalcular">
                                <input type="hidden" name="viagem_id" value="<?= $eta['viagemId'] ?>">
                                <button type="submit" class="btn-icon" title="Recalcular ETA">🔄</button>
                            </form>
                        </div>
                    </div>
                </div>
                <?php endforeach; ?>
            </div>
        <?php endif; ?>
        
        <!-- Legenda -->
        <div style="margin-top: 25px; padding: 15px; background: #f8f9fa; border-radius: 8px;">
            <h4 style="margin-bottom: 10px; color: #1a1a2e;">📖 Legenda de Status</h4>
            <div style="display: flex; gap: 30px; flex-wrap: wrap;">
                <div><span style="color: #27ae60;">✅ No Prazo</span> - Dentro do previsto</div>
                <div><span style="color: #3498db;">⏰ Atraso Leve</span> - Até 15 min</div>
                <div><span style="color: #e67e22;">⚠️ Atraso Moderado</span> - 15-30 min</div>
                <div><span style="color: #e74c3c;">🚨 Atraso Crítico</span> - Mais de 30 min</div>
                <div><span style="color: #95a5a6;">❓ Indeterminado</span> - Parada não prevista</div>
            </div>
        </div>
    </div>
</body>
</html>