<?php
require_once '../config.php';

$page_title = 'Desvios de Rota';
$page_icon = '🔄';

// Verificar permissão (gestor ou admin)
if (!in_array(($_SESSION['usuario']['perfil'] ?? ''), ['gestor', 'admin'])) {
    header('Location: dashboard.php');
    exit;
}

// Função para chamar a API de desvios
function chamarApiDesvios($endpoint, $params = []) {
    $url = API_URL . '/v1/desvios' . $endpoint;
    if (!empty($params)) {
        $url .= '?' . http_build_query($params);
    }
    
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

// Função para resolver desvio
function resolverDesvio($id) {
    $url = API_URL . '/v1/desvios/' . $id . '/resolver';
    
    $options = [
        'http' => [
            'header' => "Content-Type: application/json\r\n" .
                       "Authorization: Bearer " . ($_SESSION['token'] ?? '') . "\r\n",
            'method' => 'PUT',
            'timeout' => 30
        ]
    ];
    
    $context = stream_context_create($options);
    $resultado = @file_get_contents($url, false, $context);
    
    return $resultado !== false;
}

// Processar ação de resolver
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'resolver') {
    $id = $_POST['id'] ?? '';
    if ($id && resolverDesvio($id)) {
        $mensagem = 'Desvio resolvido com sucesso!';
        $mensagemTipo = 'success';
    } else {
        $mensagem = 'Erro ao resolver desvio.';
        $mensagemTipo = 'error';
    }
}

// Buscar filtros
$filtro = $_GET['filtro'] ?? 'todos';
$rotaId = $_GET['rota_id'] ?? '';
$veiculoId = $_GET['veiculo_id'] ?? '';

// Carregar desvios conforme filtro
$desvios = [];
$totalDesvios = 0;
$erroApi = false;
$mensagemErro = '';

if ($filtro === 'ativos') {
    $resultado = chamarApiDesvios('/ativos');
    if ($resultado !== null) {
        $desvios = $resultado;
        $totalDesvios = count($desvios);
    } else {
        $erroApi = true;
        $mensagemErro = 'Não foi possível carregar os desvios ativos.';
    }
} elseif ($filtro === 'rota' && !empty($rotaId)) {
    $resultado = chamarApiDesvios("/rota/{$rotaId}");
    if ($resultado !== null) {
        $desvios = $resultado;
        $totalDesvios = count($desvios);
    } else {
        $erroApi = true;
        $mensagemErro = "Nenhum desvio encontrado para a rota #{$rotaId}.";
    }
} elseif ($filtro === 'veiculo' && !empty($veiculoId)) {
    $resultado = chamarApiDesvios("/veiculo/{$veiculoId}");
    if ($resultado !== null) {
        $desvios = $resultado;
        $totalDesvios = count($desvios);
    } else {
        $erroApi = true;
        $mensagemErro = "Nenhum desvio encontrado para o veículo #{$veiculoId}.";
    }
} else {
    // Se o filtro requer ID mas não foi fornecido, tentar carregar ativos
    if (in_array($filtro, ['rota', 'veiculo'])) {
        $resultado = chamarApiDesvios('/ativos');
        if ($resultado !== null) {
            $desvios = $resultado;
            $totalDesvios = count($desvios);
        }
    }
}

// Estatísticas
$desviosAtivos = 0;
$desviosResolvidos = 0;
$distanciaTotal = 0;

foreach ($desvios as $desvio) {
    if ($desvio['resolvido'] ?? false) {
        $desviosResolvidos++;
    } else {
        $desviosAtivos++;
    }
    $distanciaTotal += $desvio['distanciaMetros'] ?? 0;
}

// Função para formatar distância
function formatarDistancia($metros) {
    if ($metros < 1000) {
        return round($metros) . ' m';
    }
    return round($metros / 1000, 2) . ' km';
}

// Função para formatar duração
function formatarDuracao($minutos) {
    if (!$minutos) return '—';
    if ($minutos < 60) {
        return $minutos . ' min';
    }
    $horas = floor($minutos / 60);
    $mins = $minutos % 60;
    return $horas . 'h ' . $mins . 'min';
}

// Função para obter classe de severidade baseada na distância
function getSeveridadeDesvio($distanciaMetros) {
    if ($distanciaMetros > 500) return 'danger';
    if ($distanciaMetros > 200) return 'warning';
    return 'info';
}

// Função para construir URL com filtros
function buildUrl($params = []) {
    $currentParams = $_GET;
    $mergedParams = array_merge($currentParams, $params);
    
    $url = 'desvios.php';
    if (!empty($mergedParams)) {
        $url .= '?' . http_build_query($mergedParams);
    }
    return $url;
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
            font-size: 32px;
            font-weight: bold;
            color: #1a1a2e;
        }
        .stat-label {
            color: #666;
            font-size: 13px;
            margin-top: 5px;
        }
        .stat-card.warning { border-left: 4px solid #e67e22; }
        .stat-card.success { border-left: 4px solid #27ae60; }
        .stat-card.info { border-left: 4px solid #3498db; }
        
        .filter-bar {
            display: flex;
            gap: 15px;
            margin-bottom: 25px;
            flex-wrap: wrap;
            align-items: center;
        }
        .filter-tabs {
            display: flex;
            gap: 5px;
            background: white;
            padding: 5px;
            border-radius: 10px;
            box-shadow: 0 2px 5px rgba(0,0,0,0.05);
        }
        .filter-tab {
            padding: 10px 20px;
            border: none;
            background: transparent;
            border-radius: 8px;
            cursor: pointer;
            font-weight: 500;
            color: #666;
            transition: all 0.2s;
            text-decoration: none;
        }
        .filter-tab:hover { background: #f0f0f0; }
        .filter-tab.active {
            background: #2c5364;
            color: white;
        }
        
        .desvios-container {
            background: white;
            border-radius: 12px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
            overflow: hidden;
        }
        .desvio-item {
            display: flex;
            align-items: flex-start;
            padding: 20px;
            border-bottom: 1px solid #eee;
            transition: background 0.2s;
        }
        .desvio-item:hover { background: #f8f9fa; }
        .desvio-item.resolvido { background: #f0f9f4; }
        .desvio-item.resolvido:hover { background: #e8f5ec; }
        
        .desvio-icon {
            width: 50px;
            height: 50px;
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 24px;
            margin-right: 15px;
        }
        .desvio-icon.danger { background: #fee; color: #c00; }
        .desvio-icon.warning { background: #fef3e0; color: #e67e22; }
        .desvio-icon.info { background: #e8f4f8; color: #2980b9; }
        
        .desvio-content { flex: 1; }
        .desvio-header {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 8px;
            flex-wrap: wrap;
        }
        .desvio-title {
            font-weight: 600;
            font-size: 16px;
            color: #1a1a2e;
        }
        .desvio-badge {
            padding: 3px 10px;
            border-radius: 20px;
            font-size: 11px;
            font-weight: bold;
        }
        .badge-danger { background: #fee; color: #c00; }
        .badge-warning { background: #fef3e0; color: #e67e22; }
        .badge-info { background: #e8f4f8; color: #2980b9; }
        .badge-success { background: #d5f5e3; color: #1e8449; }
        
        .desvio-details {
            display: flex;
            gap: 20px;
            margin: 10px 0;
            flex-wrap: wrap;
        }
        .detail-item {
            display: flex;
            align-items: center;
            gap: 5px;
            font-size: 14px;
            color: #555;
        }
        
        .desvio-meta {
            display: flex;
            gap: 20px;
            font-size: 13px;
            color: #888;
            flex-wrap: wrap;
        }
        
        .desvio-actions {
            display: flex;
            gap: 10px;
            margin-left: 15px;
        }
        .btn-icon {
            width: 40px;
            height: 40px;
            border-radius: 8px;
            border: none;
            background: #f5f5f5;
            cursor: pointer;
            font-size: 18px;
            transition: all 0.2s;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .btn-icon:hover { background: #e0e0e0; }
        .btn-icon.success:hover { background: #27ae60; color: white; }
        
        .empty-state {
            text-align: center;
            padding: 60px;
            color: #999;
        }
        .empty-state-icon { font-size: 64px; margin-bottom: 20px; }
        
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
        
        .chart-container {
            background: white;
            padding: 20px;
            border-radius: 12px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
            height: 250px;
            margin-bottom: 25px;
        }
        
        .filter-form {
            display: flex;
            gap: 10px;
            align-items: center;
        }
        .filter-input {
            padding: 10px 15px;
            border: 1px solid #ddd;
            border-radius: 8px;
            width: 200px;
        }
        
        @media (max-width: 1024px) {
            .stats-grid { grid-template-columns: repeat(2, 1fr); }
        }
        @media (max-width: 768px) {
            .stats-grid { grid-template-columns: 1fr; }
            .desvio-item { flex-wrap: wrap; }
            .desvio-actions { margin-left: 65px; margin-top: 15px; }
            .filter-bar { flex-direction: column; align-items: stretch; }
        }
    </style>
</head>
<body>
    <?php include '../components/sidebar.php'; ?>
    
    <div class="main-content">
        <div class="top-bar">
            <h1 class="page-title"><span><?= $page_icon ?></span> <?= $page_title ?></h1>
            <div>
                <?php if ($desviosAtivos > 0): ?>
                <span style="background: #e67e22; color: white; padding: 5px 12px; border-radius: 20px; font-size: 14px;">
                    <?= $desviosAtivos ?> ativos
                </span>
                <?php endif; ?>
            </div>
        </div>
        
        <!-- Mensagem de feedback -->
        <?php if (isset($mensagem)): ?>
        <div class="alert-message alert-<?= $mensagemTipo ?>">
            <?= $mensagemTipo === 'success' ? '✅' : '❌' ?> <?= htmlspecialchars($mensagem) ?>
        </div>
        <?php endif; ?>
        
        <!-- Stats Cards -->
        <div class="stats-grid">
            <div class="stat-card warning">
                <div class="stat-value"><?= $desviosAtivos ?></div>
                <div class="stat-label">Desvios Ativos</div>
            </div>
            <div class="stat-card success">
                <div class="stat-value"><?= $desviosResolvidos ?></div>
                <div class="stat-label">Desvios Resolvidos</div>
            </div>
            <div class="stat-card info">
                <div class="stat-value"><?= $totalDesvios ?></div>
                <div class="stat-label">Total de Desvios</div>
            </div>
            <div class="stat-card">
                <div class="stat-value"><?= formatarDistancia($distanciaTotal) ?></div>
                <div class="stat-label">Distância Total Desviada</div>
            </div>
        </div>
        
        <!-- Gráfico de Distribuição -->
        <?php if ($totalDesvios > 0): ?>
        <div class="chart-container">
            <h4 style="margin-bottom: 15px;">📊 Distribuição de Desvios</h4>
            <canvas id="desviosChart"></canvas>
        </div>
        <?php endif; ?>
        
        <!-- Filtros -->
        <div class="filter-bar">
            <div class="filter-tabs">
                <a href="<?= buildUrl(['filtro' => 'todos']) ?>" class="filter-tab <?= $filtro === 'todos' ? 'active' : '' ?>">Todos</a>
                <a href="<?= buildUrl(['filtro' => 'ativos']) ?>" class="filter-tab <?= $filtro === 'ativos' ? 'active' : '' ?>">Ativos</a>
                <a href="<?= buildUrl(['filtro' => 'rota']) ?>" class="filter-tab <?= $filtro === 'rota' ? 'active' : '' ?>">Por Rota</a>
                <a href="<?= buildUrl(['filtro' => 'veiculo']) ?>" class="filter-tab <?= $filtro === 'veiculo' ? 'active' : '' ?>">Por Veículo</a>
            </div>
            
            <?php if ($filtro === 'rota'): ?>
            <form method="GET" class="filter-form">
                <input type="hidden" name="filtro" value="rota">
                <input type="text" name="rota_id" placeholder="ID da Rota" class="filter-input" value="<?= htmlspecialchars($rotaId) ?>">
                <button type="submit" class="btn btn-primary">🔍 Filtrar</button>
            </form>
            <?php elseif ($filtro === 'veiculo'): ?>
            <form method="GET" class="filter-form">
                <input type="hidden" name="filtro" value="veiculo">
                <input type="text" name="veiculo_id" placeholder="ID do Veículo" class="filter-input" value="<?= htmlspecialchars($veiculoId) ?>">
                <button type="submit" class="btn btn-primary">🔍 Filtrar</button>
            </form>
            <?php endif; ?>
        </div>
        
        <!-- Mensagem de erro da API -->
        <?php if ($erroApi && !empty($mensagemErro)): ?>
        <div class="alert-message alert-error">
            ⚠️ <?= htmlspecialchars($mensagemErro) ?>
        </div>
        <?php endif; ?>
        
        <!-- Lista de Desvios -->
        <div class="desvios-container">
            <?php if (empty($desvios)): ?>
                <div class="empty-state">
                    <div class="empty-state-icon">✅</div>
                    <h3>Nenhum desvio encontrado</h3>
                    <p>Todas as rotas estão sendo seguidas corretamente.</p>
                </div>
            <?php else: ?>
                <?php foreach ($desvios as $desvio): ?>
                <?php 
                    $resolvido = $desvio['resolvido'] ?? false;
                    $distancia = $desvio['distanciaMetros'] ?? 0;
                    $severidade = getSeveridadeDesvio($distancia);
                ?>
                <div class="desvio-item <?= $resolvido ? 'resolvido' : '' ?>">
                    <div class="desvio-icon <?= $severidade ?>">
                        <?= $resolvido ? '✅' : '🔄' ?>
                    </div>
                    
                    <div class="desvio-content">
                        <div class="desvio-header">
                            <span class="desvio-title">
                                Rota #<?= $desvio['rotaId'] ?? 'N/A' ?>
                            </span>
                            <span class="desvio-badge badge-<?= $severidade ?>">
                                <?= formatarDistancia($distancia) ?> de desvio
                            </span>
                            <?php if (!$resolvido): ?>
                                <span class="desvio-badge badge-warning">ATIVO</span>
                            <?php else: ?>
                                <span class="desvio-badge badge-success">RESOLVIDO</span>
                            <?php endif; ?>
                        </div>
                        
                        <div class="desvio-details">
                            <div class="detail-item">
                                <span>🚛</span>
                                <span>Veículo #<?= $desvio['veiculoId'] ?? 'N/A' ?></span>
                            </div>
                            <?php if (!empty($desvio['viagemId'])): ?>
                            <div class="detail-item">
                                <span>🛣️</span>
                                <span>Viagem #<?= $desvio['viagemId'] ?></span>
                            </div>
                            <?php endif; ?>
                            <?php if (!empty($desvio['velocidadeKmh'])): ?>
                            <div class="detail-item">
                                <span>📊</span>
                                <span><?= number_format($desvio['velocidadeKmh'], 1) ?> km/h</span>
                            </div>
                            <?php endif; ?>
                            <?php if (!empty($desvio['kmExtras'])): ?>
                            <div class="detail-item">
                                <span>📏</span>
                                <span>+<?= number_format($desvio['kmExtras'], 2) ?> km extras</span>
                            </div>
                            <?php endif; ?>
                        </div>
                        
                        <?php if (!empty($desvio['nomeViaDesvio'])): ?>
                        <div style="margin: 8px 0; color: #666; font-size: 14px;">
                            📍 <?= htmlspecialchars($desvio['nomeViaDesvio']) ?>
                        </div>
                        <?php endif; ?>
                        
                        <div class="desvio-meta">
                            <span>🕐 Início: <?= date('d/m/Y H:i', strtotime($desvio['dataHoraDesvio'] ?? 'now')) ?></span>
                            <?php if (!empty($desvio['dataHoraRetorno'])): ?>
                                <span>✅ Retorno: <?= date('d/m/Y H:i', strtotime($desvio['dataHoraRetorno'])) ?></span>
                            <?php endif; ?>
                            <?php if (!empty($desvio['duracaoMin'])): ?>
                                <span>⏱️ Duração: <?= formatarDuracao($desvio['duracaoMin']) ?></span>
                            <?php endif; ?>
                            <?php if (!empty($desvio['motivo'])): ?>
                                <span>📝 Motivo: <?= htmlspecialchars($desvio['motivo']) ?></span>
                            <?php endif; ?>
                        </div>
                    </div>
                    
                    <div class="desvio-actions">
                        <?php if (!$resolvido): ?>
                            <form method="POST" style="display: inline;" onsubmit="return confirm('Marcar este desvio como resolvido?')">
                                <input type="hidden" name="action" value="resolver">
                                <input type="hidden" name="id" value="<?= $desvio['id'] ?>">
                                <button type="submit" class="btn-icon success" title="Marcar como resolvido">✅</button>
                            </form>
                        <?php endif; ?>
                        <button class="btn-icon" onclick="verNoMapa(<?= $desvio['latitudeDesvio'] ?? 0 ?>, <?= $desvio['longitudeDesvio'] ?? 0 ?>)" title="Ver no mapa">
                            🗺️
                        </button>
                    </div>
                </div>
                <?php endforeach; ?>
            <?php endif; ?>
        </div>
    </div>
    
    <script>
        function verNoMapa(lat, lng) {
            if (lat && lng) {
                window.open(`https://www.openstreetmap.org/?mlat=${lat}&mlon=${lng}&zoom=15`, '_blank');
            } else {
                alert('Coordenadas não disponíveis para este desvio.');
            }
        }
        
        <?php if ($totalDesvios > 0): ?>
        // Gráfico de distribuição
        const ctx = document.getElementById('desviosChart').getContext('2d');
        new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: ['Ativos', 'Resolvidos'],
                datasets: [{
                    data: [<?= $desviosAtivos ?>, <?= $desviosResolvidos ?>],
                    backgroundColor: ['#e67e22', '#27ae60'],
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom'
                    }
                }
            }
        });
        <?php endif; ?>
    </script>
</body>
</html>