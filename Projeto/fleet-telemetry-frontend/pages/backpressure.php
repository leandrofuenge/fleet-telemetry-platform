<?php
require_once '../config.php';

$page_title = 'Backpressure Monitor';
$page_icon = '📊';

// Verificar permissão (apenas admin)
if (($_SESSION['usuario']['perfil'] ?? '') !== 'admin' && ($_SESSION['usuario']['perfil'] ?? '') !== 'gestor') {
    header('Location: dashboard.php');
    exit;
}

// Função para chamar a API de status
function getBackpressureStatus() {
    $url = API_URL . '/v1/admin/backpressure/status';
    
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

// Função para atualizar configurações
function updateBackpressureConfig($params) {
    $url = API_URL . '/v1/admin/backpressure/config';
    
    $queryParams = [];
    if (isset($params['lagThreshold'])) $queryParams['lagThreshold'] = $params['lagThreshold'];
    if (isset($params['cpuThreshold'])) $queryParams['cpuThreshold'] = $params['cpuThreshold'];
    if (isset($params['memoryThreshold'])) $queryParams['memoryThreshold'] = $params['memoryThreshold'];
    if (isset($params['pauseDuration'])) $queryParams['pauseDuration'] = $params['pauseDuration'];
    
    if (!empty($queryParams)) {
        $url .= '?' . http_build_query($queryParams);
    }
    
    $options = [
        'http' => [
            'header' => "Content-Type: application/json\r\n" .
                       "Authorization: Bearer " . ($_SESSION['token'] ?? '') . "\r\n",
            'method' => 'POST',
            'timeout' => 30
        ]
    ];
    
    $context = stream_context_create($options);
    $resultado = @file_get_contents($url, false, $context);
    
    return $resultado !== false;
}

// Processar atualização de configuração
$mensagem = '';
$mensagemTipo = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'update') {
    $params = [];
    if (!empty($_POST['lag_threshold'])) $params['lagThreshold'] = intval($_POST['lag_threshold']);
    if (!empty($_POST['cpu_threshold'])) $params['cpuThreshold'] = intval($_POST['cpu_threshold']);
    if (!empty($_POST['memory_threshold'])) $params['memoryThreshold'] = intval($_POST['memory_threshold']);
    if (!empty($_POST['pause_duration'])) $params['pauseDuration'] = intval($_POST['pause_duration']);
    
    if (updateBackpressureConfig($params)) {
        $mensagem = 'Configurações atualizadas com sucesso!';
        $mensagemTipo = 'success';
    } else {
        $mensagem = 'Erro ao atualizar configurações.';
        $mensagemTipo = 'error';
    }
}

// Obter status atual
$status = getBackpressureStatus();

// Valores padrão se API falhar
if (!$status) {
    $status = [
        'lag' => 0,
        'taxaProcessamento' => 0,
        'cpuUsage' => 0,
        'memoryUsage' => 0,
        'backpressureAtivo' => false,
        'mensagensRecebidas' => 0,
        'mensagensProcessadas' => 0,
        'tempoTotalProcessamento' => 0
    ];
}

// Calcular percentuais para barras de progresso
$cpuPercent = $status['cpuUsage'] ?? 0;
$memoryPercent = $status['memoryUsage'] ?? 0;
$lagPercent = min(100, ($status['lag'] / 10000) * 100);

// Determinar classes de alerta
$cpuClass = $cpuPercent > 80 ? 'danger' : ($cpuPercent > 60 ? 'warning' : 'success');
$memoryClass = $memoryPercent > 80 ? 'danger' : ($memoryPercent > 60 ? 'warning' : 'success');
$lagClass = $status['lag'] > 5000 ? 'danger' : ($status['lag'] > 2000 ? 'warning' : 'success');
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
        .status-badge {
            display: inline-block;
            padding: 5px 15px;
            border-radius: 20px;
            font-weight: bold;
            font-size: 14px;
        }
        .status-active {
            background: #fee;
            color: #c00;
            animation: pulse 2s infinite;
        }
        .status-inactive {
            background: #d5f5e3;
            color: #1e8449;
        }
        
        .metrics-grid {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 20px;
            margin-bottom: 25px;
        }
        .metric-card {
            background: white;
            padding: 25px;
            border-radius: 12px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
        }
        .metric-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 15px;
        }
        .metric-title {
            font-weight: 600;
            color: #1a1a2e;
            font-size: 16px;
        }
        .metric-value {
            font-size: 42px;
            font-weight: bold;
            margin-bottom: 10px;
        }
        .metric-value.danger { color: #e74c3c; }
        .metric-value.warning { color: #e67e22; }
        .metric-value.success { color: #27ae60; }
        
        .progress-bar {
            width: 100%;
            height: 8px;
            background: #ecf0f1;
            border-radius: 4px;
            overflow: hidden;
            margin: 15px 0;
        }
        .progress-fill {
            height: 100%;
            border-radius: 4px;
            transition: width 0.3s;
        }
        .progress-fill.danger { background: #e74c3c; }
        .progress-fill.warning { background: #e67e22; }
        .progress-fill.success { background: #27ae60; }
        
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
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
            color: #1a1a2e;
        }
        .stat-label {
            color: #666;
            font-size: 13px;
            margin-top: 5px;
        }
        
        .config-panel {
            background: white;
            padding: 25px;
            border-radius: 12px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
            margin-top: 25px;
        }
        .config-title {
            font-size: 18px;
            font-weight: 600;
            color: #1a1a2e;
            margin-bottom: 20px;
        }
        .config-form {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 20px;
        }
        .config-group {
            display: flex;
            flex-direction: column;
            gap: 8px;
        }
        .config-group label {
            font-weight: 600;
            color: #2c3e50;
            font-size: 14px;
        }
        .config-group input {
            padding: 12px 15px;
            border: 2px solid #e0e6ed;
            border-radius: 8px;
            font-size: 14px;
        }
        .config-group input:focus {
            outline: none;
            border-color: #2c5364;
        }
        .config-group small {
            color: #7f8c8d;
            font-size: 12px;
        }
        
        .alert-message {
            padding: 15px;
            border-radius: 8px;
            margin-bottom: 20px;
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
        
        .chart-container {
            background: white;
            padding: 20px;
            border-radius: 12px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
            height: 250px;
            margin-bottom: 25px;
        }
        
        .refresh-btn {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 10px 20px;
            background: #2c5364;
            color: white;
            border: none;
            border-radius: 8px;
            cursor: pointer;
            font-weight: 600;
            margin-left: 15px;
        }
        
        @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.7; }
        }
        
        @media (max-width: 1024px) {
            .metrics-grid { grid-template-columns: 1fr; }
            .config-form { grid-template-columns: repeat(2, 1fr); }
            .stats-grid { grid-template-columns: repeat(2, 1fr); }
        }
        @media (max-width: 768px) {
            .config-form { grid-template-columns: 1fr; }
            .stats-grid { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>
    <?php include '../components/sidebar.php'; ?>
    
    <div class="main-content">
        <div class="top-bar">
            <h1 class="page-title">
                <span><?= $page_icon ?></span> <?= $page_title ?>
                <button class="refresh-btn" onclick="location.reload()">
                    <span>🔄</span> Atualizar
                </button>
            </h1>
            <div>
                <span class="status-badge <?= $status['backpressureAtivo'] ? 'status-active' : 'status-inactive' ?>">
                    <?= $status['backpressureAtivo'] ? '⚠️ BACKPRESSURE ATIVO' : '✅ SISTEMA NORMAL' ?>
                </span>
            </div>
        </div>
        
        <!-- Mensagem de feedback -->
        <?php if ($mensagem): ?>
        <div class="alert-message alert-<?= $mensagemTipo ?>">
            <?= $mensagem ?>
        </div>
        <?php endif; ?>
        
        <!-- Métricas Principais -->
        <div class="metrics-grid">
            <div class="metric-card">
                <div class="metric-header">
                    <span class="metric-title">💻 CPU Usage</span>
                    <span><?= number_format($cpuPercent, 1) ?>%</span>
                </div>
                <div class="metric-value <?= $cpuClass ?>"><?= number_format($cpuPercent, 1) ?>%</div>
                <div class="progress-bar">
                    <div class="progress-fill <?= $cpuClass ?>" style="width: <?= $cpuPercent ?>%"></div>
                </div>
                <small style="color: #7f8c8d;">Threshold: 80%</small>
            </div>
            
            <div class="metric-card">
                <div class="metric-header">
                    <span class="metric-title">🧠 Memória</span>
                    <span><?= number_format($memoryPercent, 1) ?>%</span>
                </div>
                <div class="metric-value <?= $memoryClass ?>"><?= number_format($memoryPercent, 1) ?>%</div>
                <div class="progress-bar">
                    <div class="progress-fill <?= $memoryClass ?>" style="width: <?= $memoryPercent ?>%"></div>
                </div>
                <small style="color: #7f8c8d;">Threshold: 80%</small>
            </div>
            
            <div class="metric-card">
                <div class="metric-header">
                    <span class="metric-title">📨 Lag (Mensagens Pendentes)</span>
                    <span><?= number_format($status['lag']) ?></span>
                </div>
                <div class="metric-value <?= $lagClass ?>"><?= number_format($status['lag']) ?></div>
                <div class="progress-bar">
                    <div class="progress-fill <?= $lagClass ?>" style="width: <?= $lagPercent ?>%"></div>
                </div>
                <small style="color: #7f8c8d;">Threshold: 5.000</small>
            </div>
        </div>
        
        <!-- Estatísticas -->
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-value"><?= number_format($status['taxaProcessamento'], 2) ?></div>
                <div class="stat-label">Taxa de Processamento (msg/s)</div>
            </div>
            <div class="stat-card">
                <div class="stat-value"><?= number_format($status['mensagensRecebidas']) ?></div>
                <div class="stat-label">Mensagens Recebidas</div>
            </div>
            <div class="stat-card">
                <div class="stat-value"><?= number_format($status['mensagensProcessadas']) ?></div>
                <div class="stat-label">Mensagens Processadas</div>
            </div>
            <div class="stat-card">
                <div class="stat-value"><?= number_format($status['tempoTotalProcessamento'] / 1000, 2) ?>s</div>
                <div class="stat-label">Tempo Total de Processamento</div>
            </div>
        </div>
        
        <!-- Gráfico de Tendência -->
        <div class="chart-container">
            <h4 style="margin-bottom: 15px;">📈 Métricas em Tempo Real</h4>
            <canvas id="metricsChart"></canvas>
        </div>
        
        <!-- Painel de Configuração -->
        <div class="config-panel">
            <div class="config-title">⚙️ Configurações de Backpressure</div>
            <form method="POST" class="config-form">
                <input type="hidden" name="action" value="update">
                
                <div class="config-group">
                    <label>Lag Threshold</label>
                    <input type="number" name="lag_threshold" placeholder="5000" min="100" max="50000">
                    <small>Mensagens pendentes para ativar</small>
                </div>
                
                <div class="config-group">
                    <label>CPU Threshold (%)</label>
                    <input type="number" name="cpu_threshold" placeholder="80" min="10" max="100">
                    <small>Uso de CPU para ativar</small>
                </div>
                
                <div class="config-group">
                    <label>Memory Threshold (%)</label>
                    <input type="number" name="memory_threshold" placeholder="80" min="10" max="100">
                    <small>Uso de memória para ativar</small>
                </div>
                
                <div class="config-group">
                    <label>Pause Duration (ms)</label>
                    <input type="number" name="pause_duration" placeholder="100" min="10" max="5000">
                    <small>Tempo de pausa quando ativo</small>
                </div>
                
                <div style="grid-column: span 4; text-align: right; margin-top: 10px;">
                    <button type="submit" class="btn btn-primary">💾 Salvar Configurações</button>
                </div>
            </form>
        </div>
        
        <!-- Legenda -->
        <div style="margin-top: 20px; padding: 15px; background: #f8f9fa; border-radius: 8px;">
            <h4 style="margin-bottom: 10px; color: #1a1a2e;">📖 Legenda</h4>
            <div style="display: flex; gap: 30px; flex-wrap: wrap;">
                <div><span style="color: #27ae60;">🟢</span> Normal</div>
                <div><span style="color: #e67e22;">🟡</span> Atenção</div>
                <div><span style="color: #e74c3c;">🔴</span> Crítico</div>
                <div><span>⚠️ Backpressure Ativo</span> - Sistema está pausando consumo de mensagens</div>
            </div>
        </div>
    </div>
    
    <script>
        // Gráfico de métricas
        const ctx = document.getElementById('metricsChart').getContext('2d');
        
        // Valores atuais
        const cpu = <?= $cpuPercent ?>;
        const memory = <?= $memoryPercent ?>;
        const lag = <?= $status['lag'] ?? 0 ?>;
        const lagNormalized = Math.min(100, (lag / 10000) * 100);
        
        new Chart(ctx, {
            type: 'bar',
            data: {
                labels: ['CPU', 'Memória', 'Lag (normalizado)'],
                datasets: [{
                    label: 'Uso Atual (%)',
                    data: [cpu, memory, lagNormalized],
                    backgroundColor: [
                        cpu > 80 ? '#e74c3c' : (cpu > 60 ? '#e67e22' : '#27ae60'),
                        memory > 80 ? '#e74c3c' : (memory > 60 ? '#e67e22' : '#27ae60'),
                        lag > 5000 ? '#e74c3c' : (lag > 2000 ? '#e67e22' : '#27ae60')
                    ],
                    borderRadius: 8
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true,
                        max: 100,
                        ticks: {
                            callback: function(value) {
                                return value + '%';
                            }
                        }
                    }
                },
                plugins: {
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                if (context.dataIndex === 2) {
                                    return 'Lag: ' + lag + ' mensagens';
                                }
                                return context.parsed.y.toFixed(1) + '%';
                            }
                        }
                    }
                }
            }
        });
        
        // Auto-refresh a cada 30 segundos
        setTimeout(function() {
            location.reload();
        }, 30000);
    </script>
</body>
</html>