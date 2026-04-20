<?php
require_once '../config.php';

$page_title = 'Central de Alertas';
$page_icon = '⚠️';

// Verificar permissão (apenas gestor)
if (($_SESSION['usuario']['perfil'] ?? '') !== 'gestor') {
    header('Location: dashboard.php');
    exit;
}

// Função para chamar a API de alertas
function chamarApiAlertas($endpoint, $params = []) {
    $url = API_URL . '/v1/alertas' . $endpoint;
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

// Função para chamar API PUT
function chamarApiPut($endpoint) {
    $url = API_URL . '/v1/alertas' . $endpoint;
    
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

// Processar ações POST
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $acao = $_POST['acao'] ?? '';
    $id = $_POST['id'] ?? '';
    
    if ($acao === 'marcar_lido' && $id) {
        chamarApiPut("/{$id}/ler");
    } elseif ($acao === 'resolver' && $id) {
        chamarApiPut("/{$id}/resolver");
    }
    
    $queryParams = $_GET;
    unset($queryParams['page']);
    
    $redirectUrl = 'alertas.php';
    if (!empty($queryParams)) {
        $redirectUrl .= '?' . http_build_query($queryParams);
    }
    
    header('Location: ' . $redirectUrl);
    exit;
}

// Buscar filtros
$filtro = $_GET['filtro'] ?? 'todos';
$veiculoId = $_GET['veiculo_id'] ?? '';
$motoristaId = $_GET['motorista_id'] ?? '';
$viagemId = $_GET['viagem_id'] ?? '';

// Carregar alertas conforme o filtro
$alertas = [];
$totalElements = 0;
$erroApi = false;

if ($filtro === 'ativos') {
    // Endpoint de ativos funciona!
    $resultado = chamarApiAlertas('/ativos');
    if ($resultado !== null) {
        $alertas = $resultado;
        $totalElements = count($alertas);
    } else {
        $erroApi = true;
    }
} else {
    // Para todos os outros, carregar todos e filtrar localmente
    $resultado = chamarApiAlertas('', ['page' => 0, 'size' => 100, 'sort' => 'dataHora,desc']);
    if ($resultado !== null) {
        $todosAlertas = $resultado['content'] ?? [];
        
        // Aplicar filtro local
        if ($filtro === 'veiculo' && !empty($veiculoId)) {
            foreach ($todosAlertas as $alerta) {
                if (($alerta['veiculoId'] ?? '') == $veiculoId) {
                    $alertas[] = $alerta;
                }
            }
        } elseif ($filtro === 'motorista' && !empty($motoristaId)) {
            foreach ($todosAlertas as $alerta) {
                if (($alerta['motoristaId'] ?? '') == $motoristaId) {
                    $alertas[] = $alerta;
                }
            }
        } elseif ($filtro === 'viagem' && !empty($viagemId)) {
            foreach ($todosAlertas as $alerta) {
                if (($alerta['viagemId'] ?? '') == $viagemId) {
                    $alertas[] = $alerta;
                }
            }
        } else {
            $alertas = $todosAlertas;
        }
        $totalElements = count($alertas);
    } else {
        $erroApi = true;
    }
}

// Dashboard de alertas
$dashboard = [
    'totalAtivos' => 0,
    'altaGravidade' => 0,
    'mediaGravidade' => 0,
    'baixaGravidade' => 0,
    'alertasPorTipo' => []
];

if (!$erroApi && !empty($alertas)) {
    foreach ($alertas as $alerta) {
        if (!($alerta['resolvido'] ?? false)) {
            $dashboard['totalAtivos']++;
            
            $severidade = $alerta['severidade'] ?? 'MEDIO';
            if ($severidade === 'ALTO' || $severidade === 'CRITICO') {
                $dashboard['altaGravidade']++;
            } elseif ($severidade === 'MEDIO') {
                $dashboard['mediaGravidade']++;
            } else {
                $dashboard['baixaGravidade']++;
            }
        }
        
        $tipo = $alerta['tipo'] ?? 'OUTRO';
        $dashboard['alertasPorTipo'][$tipo] = ($dashboard['alertasPorTipo'][$tipo] ?? 0) + 1;
    }
}

// Função para construir URL com filtros
function buildUrl($params = []) {
    $currentParams = $_GET;
    $mergedParams = array_merge($currentParams, $params);
    
    $url = 'alertas.php';
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
        .stat-card.critical { border-left: 4px solid #e74c3c; }
        .stat-card.high { border-left: 4px solid #e67e22; }
        .stat-card.medium { border-left: 4px solid #f1c40f; }
        .stat-card.low { border-left: 4px solid #27ae60; }
        .stat-value { font-size: 32px; font-weight: bold; color: #1a1a2e; }
        .stat-label { color: #666; font-size: 13px; margin-top: 5px; }
        
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
        .filter-select {
            padding: 10px 15px;
            border: 1px solid #ddd;
            border-radius: 8px;
            background: white;
        }
        
        .alerts-container {
            background: white;
            border-radius: 12px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
            overflow: hidden;
        }
        .alert-item {
            display: flex;
            align-items: flex-start;
            padding: 20px;
            border-bottom: 1px solid #eee;
            transition: background 0.2s;
        }
        .alert-item:hover { background: #f8f9fa; }
        .alert-item.nao-lido { background: #fef9e7; }
        .alert-item.nao-lido:hover { background: #fef3d9; }
        
        .alert-icon {
            width: 50px;
            height: 50px;
            border-radius: 12px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 24px;
            margin-right: 15px;
        }
        .alert-icon.danger { background: #fee; color: #c00; }
        .alert-icon.warning { background: #fef3e0; color: #e67e22; }
        .alert-icon.info { background: #e8f4f8; color: #2980b9; }
        .alert-icon.success { background: #d5f5e3; color: #1e8449; }
        
        .alert-content { flex: 1; }
        .alert-header {
            display: flex;
            align-items: center;
            gap: 10px;
            margin-bottom: 8px;
            flex-wrap: wrap;
        }
        .alert-type {
            font-weight: 600;
            font-size: 16px;
            color: #1a1a2e;
        }
        .alert-severidade {
            padding: 3px 10px;
            border-radius: 20px;
            font-size: 11px;
            font-weight: bold;
            text-transform: uppercase;
        }
        .alert-severidade.danger { background: #fee; color: #c00; }
        .alert-severidade.warning { background: #fef3e0; color: #e67e22; }
        .alert-severidade.info { background: #e8f4f8; color: #2980b9; }
        .alert-severidade.success { background: #d5f5e3; color: #1e8449; }
        
        .alert-message {
            color: #555;
            margin-bottom: 8px;
            line-height: 1.5;
        }
        .alert-meta {
            display: flex;
            gap: 20px;
            font-size: 13px;
            color: #888;
            flex-wrap: wrap;
        }
        .alert-meta span { display: flex; align-items: center; gap: 5px; }
        
        .alert-actions {
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
        .btn-icon.primary:hover { background: #2c5364; color: white; }
        
        .empty-state {
            text-align: center;
            padding: 60px;
            color: #999;
        }
        .empty-state-icon { font-size: 64px; margin-bottom: 20px; }
        
        .chart-container {
            background: white;
            padding: 20px;
            border-radius: 12px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
            height: 300px;
            margin-bottom: 25px;
        }
        
        .api-error {
            background: #fadbd8;
            color: #922b21;
            padding: 15px;
            border-radius: 8px;
            margin-bottom: 20px;
            border-left: 4px solid #e74c3c;
        }
        
        .veiculo-selector, .motorista-selector, .viagem-selector {
            display: flex;
            gap: 10px;
            align-items: center;
        }
        
        @media (max-width: 1024px) {
            .stats-grid { grid-template-columns: repeat(2, 1fr); }
        }
        @media (max-width: 768px) {
            .stats-grid { grid-template-columns: 1fr; }
            .alert-item { flex-wrap: wrap; }
            .alert-actions { margin-left: 65px; margin-top: 15px; }
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
                <span style="background: #e74c3c; color: white; padding: 5px 12px; border-radius: 20px; font-size: 14px;">
                    <?= $dashboard['totalAtivos'] ?> ativos
                </span>
            </div>
        </div>
        
        <!-- Stats Cards -->
        <div class="stats-grid">
            <div class="stat-card critical">
                <div class="stat-value"><?= $dashboard['altaGravidade'] ?></div>
                <div class="stat-label">Alta Gravidade</div>
            </div>
            <div class="stat-card high">
                <div class="stat-value"><?= $dashboard['mediaGravidade'] ?></div>
                <div class="stat-label">Média Gravidade</div>
            </div>
            <div class="stat-card medium">
                <div class="stat-value"><?= $dashboard['baixaGravidade'] ?></div>
                <div class="stat-label">Baixa Gravidade</div>
            </div>
            <div class="stat-card low">
                <div class="stat-value"><?= $totalElements ?></div>
                <div class="stat-label">Total de Alertas</div>
            </div>
        </div>
        
        <!-- Gráfico de Distribuição por Tipo -->
        <?php if (!empty($dashboard['alertasPorTipo'])): ?>
        <div class="chart-container">
            <h4 style="margin-bottom: 15px;">📊 Alertas por Tipo</h4>
            <canvas id="tipoChart"></canvas>
        </div>
        <?php endif; ?>
        
        <!-- Mensagem de erro da API -->
        <?php if ($erroApi): ?>
        <div class="api-error">
            ⚠️ Não foi possível carregar os alertas. Verifique se o serviço está disponível.
        </div>
        <?php endif; ?>
        
        <!-- Filtros -->
        <div class="filter-bar">
            <div class="filter-tabs">
                <a href="<?= buildUrl(['filtro' => 'todos']) ?>" class="filter-tab <?= $filtro === 'todos' ? 'active' : '' ?>">Todos</a>
                <a href="<?= buildUrl(['filtro' => 'ativos']) ?>" class="filter-tab <?= $filtro === 'ativos' ? 'active' : '' ?>">Ativos</a>
                <a href="<?= buildUrl(['filtro' => 'veiculo']) ?>" class="filter-tab <?= $filtro === 'veiculo' ? 'active' : '' ?>">Por Veículo</a>
                <a href="<?= buildUrl(['filtro' => 'motorista']) ?>" class="filter-tab <?= $filtro === 'motorista' ? 'active' : '' ?>">Por Motorista</a>
                <a href="<?= buildUrl(['filtro' => 'viagem']) ?>" class="filter-tab <?= $filtro === 'viagem' ? 'active' : '' ?>">Por Viagem</a>
            </div>
            
            <?php if ($filtro === 'veiculo'): ?>
            <form method="GET" class="veiculo-selector">
                <input type="hidden" name="filtro" value="veiculo">
                <input type="text" name="veiculo_id" placeholder="ID do veículo" class="filter-select" value="<?= htmlspecialchars($veiculoId) ?>">
                <button type="submit" class="btn btn-primary">🔍 Filtrar</button>
            </form>
            <?php elseif ($filtro === 'motorista'): ?>
            <form method="GET" class="motorista-selector">
                <input type="hidden" name="filtro" value="motorista">
                <input type="text" name="motorista_id" placeholder="ID do motorista" class="filter-select" value="<?= htmlspecialchars($motoristaId) ?>">
                <button type="submit" class="btn btn-primary">🔍 Filtrar</button>
            </form>
            <?php elseif ($filtro === 'viagem'): ?>
            <form method="GET" class="viagem-selector">
                <input type="hidden" name="filtro" value="viagem">
                <input type="text" name="viagem_id" placeholder="ID da viagem" class="filter-select" value="<?= htmlspecialchars($viagemId) ?>">
                <button type="submit" class="btn btn-primary">🔍 Filtrar</button>
            </form>
            <?php endif; ?>
        </div>
        
        <!-- Lista de Alertas -->
        <div class="alerts-container">
            <?php if (empty($alertas)): ?>
                <div class="empty-state">
                    <div class="empty-state-icon">✅</div>
                    <h3>Nenhum alerta encontrado</h3>
                    <p><?= $erroApi ? 'Erro na comunicação com o servidor.' : 'Todos os sistemas estão operando normalmente.' ?></p>
                </div>
            <?php else: ?>
                <?php foreach ($alertas as $alerta): ?>
                <?php 
                    $veiculoPlaca = $alerta['veiculoPlaca'] ?? '';
                    $motoristaNome = $alerta['motoristaNome'] ?? '';
                ?>
                <div class="alert-item <?= !($alerta['lido'] ?? false) ? 'nao-lido' : '' ?>">
                    <div class="alert-icon <?= getSeverityClass($alerta['severidade'] ?? 'MEDIO') ?>">
                        <?= getAlertaIcon($alerta['tipo'] ?? '') ?>
                    </div>
                    
                    <div class="alert-content">
                        <div class="alert-header">
                            <span class="alert-type"><?= traduzirTipoAlerta($alerta['tipo'] ?? '') ?></span>
                            <span class="alert-severidade <?= getSeverityClass($alerta['severidade'] ?? 'MEDIO') ?>">
                                <?= $alerta['severidade'] ?? 'MEDIO' ?>
                            </span>
                            <?php if (!($alerta['lido'] ?? true)): ?>
                                <span style="background: #f39c12; color: white; padding: 3px 8px; border-radius: 12px; font-size: 11px;">NOVO</span>
                            <?php endif; ?>
                            <?php if ($alerta['resolvido'] ?? false): ?>
                                <span style="background: #27ae60; color: white; padding: 3px 8px; border-radius: 12px; font-size: 11px;">RESOLVIDO</span>
                            <?php endif; ?>
                        </div>
                        
                        <div class="alert-message">
                            <?= htmlspecialchars($alerta['mensagem'] ?? 'Sem descrição') ?>
                        </div>
                        
                        <div class="alert-meta">
                            <?php if (!empty($veiculoPlaca)): ?>
                                <span>🚛 <?= htmlspecialchars($veiculoPlaca) ?></span>
                            <?php elseif (!empty($alerta['veiculoId'])): ?>
                                <span>🚛 Veículo #<?= $alerta['veiculoId'] ?></span>
                            <?php endif; ?>
                            
                            <?php if (!empty($motoristaNome)): ?>
                                <span>👤 <?= htmlspecialchars($motoristaNome) ?></span>
                            <?php elseif (!empty($alerta['motoristaId'])): ?>
                                <span>👤 Motorista #<?= $alerta['motoristaId'] ?></span>
                            <?php endif; ?>
                            
                            <?php if (!empty($alerta['viagemId'])): ?>
                                <span>🛣️ Viagem #<?= $alerta['viagemId'] ?></span>
                            <?php endif; ?>
                            
                            <span>🕐 <?= date('d/m/Y H:i', strtotime($alerta['dataHora'] ?? 'now')) ?></span>
                            
                            <?php if (isset($alerta['velocidadeKmh'])): ?>
                                <span>📊 <?= number_format($alerta['velocidadeKmh'], 1) ?> km/h</span>
                            <?php endif; ?>
                        </div>
                    </div>
                    
                    <div class="alert-actions">
                        <?php if (!($alerta['lido'] ?? true)): ?>
                            <form method="POST" style="display: inline;">
                                <input type="hidden" name="acao" value="marcar_lido">
                                <input type="hidden" name="id" value="<?= $alerta['id'] ?>">
                                <button type="submit" class="btn-icon primary" title="Marcar como lido">👁️</button>
                            </form>
                        <?php endif; ?>
                        
                        <?php if (!($alerta['resolvido'] ?? false)): ?>
                            <form method="POST" style="display: inline;">
                                <input type="hidden" name="acao" value="resolver">
                                <input type="hidden" name="id" value="<?= $alerta['id'] ?>">
                                <button type="submit" class="btn-icon success" title="Marcar como resolvido">✅</button>
                            </form>
                        <?php endif; ?>
                    </div>
                </div>
                <?php endforeach; ?>
            <?php endif; ?>
        </div>
    </div>
    
    <script>
        <?php if (!empty($dashboard['alertasPorTipo'])): ?>
        const tipoData = <?= json_encode($dashboard['alertasPorTipo']) ?>;
        const tipoLabels = Object.keys(tipoData).map(t => {
            const traducoes = {
                'EXCESSO_VELOCIDADE': 'Excesso Velocidade',
                'FRENAGEM_BRUSCA': 'Frenagem Brusca',
                'PARADA_PROLONGADA': 'Parada Prolongada',
                'SCORE_BAIXO': 'Score Baixo',
                'SCORE_CRITICO': 'Score Crítico',
                'NIVEL_COMBUSTIVEL_BAIXO': 'Combustível Baixo'
            };
            return traducoes[t] || t.replace(/_/g, ' ');
        });
        const tipoValues = Object.values(tipoData);
        
        const canvas = document.getElementById('tipoChart');
        if (canvas) {
            new Chart(canvas, {
                type: 'bar',
                data: {
                    labels: tipoLabels,
                    datasets: [{
                        label: 'Quantidade',
                        data: tipoValues,
                        backgroundColor: '#2c5364'
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    indexAxis: 'y',
                    plugins: {
                        legend: { display: false }
                    }
                }
            });
        }
        <?php endif; ?>
    </script>
</body>
</html>