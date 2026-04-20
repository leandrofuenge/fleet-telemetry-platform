<?php
require_once '../config.php';

$page_title = 'Cache Admin';
$page_icon = '🗃️';

// Verificar permissão (apenas admin)
if (($_SESSION['usuario']['perfil'] ?? '') !== 'admin') {
    header('Location: dashboard.php');
    exit;
}

// Função para obter status dos caches da API
function getCacheStatus() {
    $url = API_URL . '/v1/admin/cache/status';
    
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

// Função para executar cache warming
function executarCacheWarming() {
    $url = API_URL . '/v1/admin/cache/warm';
    
    $options = [
        'http' => [
            'header' => "Content-Type: application/json\r\n" .
                       "Authorization: Bearer " . ($_SESSION['token'] ?? '') . "\r\n",
            'method' => 'POST',
            'timeout' => 60
        ]
    ];
    
    $context = stream_context_create($options);
    $resultado = @file_get_contents($url, false, $context);
    
    if ($resultado === false) {
        return null;
    }
    
    return json_decode($resultado, true);
}

// Processar ação
$resultado = null;
$mensagem = '';
$mensagemTipo = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'warm') {
    $inicio = microtime(true);
    $resultado = executarCacheWarming();
    $tempoExecucao = round((microtime(true) - $inicio) * 1000);
    
    if ($resultado && ($resultado['status'] ?? '') === 'success') {
        $mensagem = 'Cache warming executado com sucesso em ' . ($resultado['timeMs'] ?? $tempoExecucao) . 'ms!';
        $mensagemTipo = 'success';
    } else {
        $mensagem = $resultado['message'] ?? 'Erro ao executar cache warming.';
        $mensagemTipo = 'error';
    }
}

// Obter status dos caches da API
$cacheStatus = getCacheStatus();
$caches = $cacheStatus['caches'] ?? [];
$totalCaches = count($caches);
$redisInfo = $cacheStatus['redis'] ?? null;
$ultimaExecucao = $cacheStatus['ultimaExecucao'] ?? null;
?>
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?= $page_title ?> | <?= APP_NAME ?></title>
    <link rel="stylesheet" href="../style.css">
    <style>
        .cache-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 25px;
        }
        
        .warm-btn {
            padding: 15px 30px;
            background: linear-gradient(135deg, #e67e22, #d35400);
            color: white;
            border: none;
            border-radius: 10px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s;
            display: inline-flex;
            align-items: center;
            gap: 10px;
        }
        
        .warm-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 20px rgba(230, 126, 34, 0.3);
        }
        
        .warm-btn:disabled {
            opacity: 0.6;
            cursor: not-allowed;
            transform: none;
        }
        
        .warm-btn .spinner {
            display: none;
            width: 20px;
            height: 20px;
            border: 2px solid white;
            border-top-color: transparent;
            border-radius: 50%;
            animation: spin 1s linear infinite;
        }
        
        .warm-btn.loading .spinner {
            display: inline-block;
        }
        
        .warm-btn.loading .btn-text {
            display: none;
        }
        
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 20px;
            margin-bottom: 30px;
        }
        
        .stat-card {
            background: white;
            padding: 25px;
            border-radius: 12px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
            text-align: center;
        }
        
        .stat-icon {
            font-size: 36px;
            margin-bottom: 10px;
        }
        
        .stat-value {
            font-size: 32px;
            font-weight: bold;
            color: #1a1a2e;
        }
        
        .stat-label {
            color: #666;
            font-size: 14px;
            margin-top: 5px;
        }
        
        .cache-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
            gap: 20px;
            margin-top: 25px;
        }
        
        .cache-card {
            background: white;
            border-radius: 12px;
            padding: 25px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
            transition: all 0.3s;
            border: 2px solid transparent;
        }
        
        .cache-card:hover {
            transform: translateY(-3px);
            box-shadow: 0 5px 20px rgba(0,0,0,0.1);
            border-color: #e67e22;
        }
        
        .cache-icon {
            font-size: 48px;
            margin-bottom: 15px;
        }
        
        .cache-name {
            font-size: 18px;
            font-weight: 600;
            color: #1a1a2e;
            margin-bottom: 8px;
        }
        
        .cache-desc {
            color: #666;
            font-size: 14px;
            margin-bottom: 15px;
            line-height: 1.5;
        }
        
        .cache-ttl {
            display: inline-block;
            padding: 5px 12px;
            background: #f0f0f0;
            border-radius: 20px;
            font-size: 12px;
            color: #666;
        }
        
        .cache-ttl i {
            margin-right: 5px;
        }
        
        .cache-size {
            display: inline-block;
            padding: 5px 12px;
            background: #e8f4f8;
            border-radius: 20px;
            font-size: 12px;
            color: #2980b9;
            margin-left: 8px;
        }
        
        .alert-message {
            padding: 15px 20px;
            border-radius: 10px;
            margin-bottom: 25px;
            display: flex;
            align-items: center;
            gap: 12px;
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
        
        .alert-info {
            background: #e8f4f8;
            color: #2980b9;
            border-left: 4px solid #3498db;
        }
        
        .alert-warning {
            background: #fef3e0;
            color: #e67e22;
            border-left: 4px solid #f39c12;
        }
        
        .result-panel {
            background: white;
            border-radius: 12px;
            padding: 25px;
            margin-top: 25px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
        }
        
        .result-title {
            font-size: 16px;
            font-weight: 600;
            color: #1a1a2e;
            margin-bottom: 15px;
        }
        
        .result-content {
            background: #f8f9fa;
            padding: 20px;
            border-radius: 8px;
            font-family: 'Courier New', monospace;
            font-size: 14px;
            overflow-x: auto;
        }
        
        .result-content pre {
            margin: 0;
            white-space: pre-wrap;
            word-wrap: break-word;
        }
        
        .info-box {
            background: #f0f7ff;
            border-left: 4px solid #3498db;
            padding: 20px;
            border-radius: 8px;
            margin-bottom: 25px;
        }
        
        .info-box h4 {
            color: #1a1a2e;
            margin-bottom: 10px;
        }
        
        .info-box p {
            color: #555;
            line-height: 1.6;
        }
        
        .empty-state {
            text-align: center;
            padding: 60px;
            background: white;
            border-radius: 12px;
            margin-top: 25px;
        }
        
        .empty-state-icon {
            font-size: 64px;
            margin-bottom: 20px;
        }
        
        @keyframes spin {
            to { transform: rotate(360deg); }
        }
        
        @media (max-width: 768px) {
            .stats-grid {
                grid-template-columns: 1fr;
            }
            .cache-header {
                flex-direction: column;
                gap: 15px;
            }
            .warm-btn {
                width: 100%;
                justify-content: center;
            }
        }
    </style>
</head>
<body>
    <?php include '../components/sidebar.php'; ?>
    
    <div class="main-content">
        <div class="top-bar">
            <h1 class="page-title"><span><?= $page_icon ?></span> <?= $page_title ?></h1>
        </div>
        
        <!-- Mensagem de feedback -->
        <?php if ($mensagem): ?>
        <div class="alert-message alert-<?= $mensagemTipo ?>">
            <span><?= $mensagemTipo === 'success' ? '✅' : '❌' ?></span>
            <span><?= htmlspecialchars($mensagem) ?></span>
        </div>
        <?php endif; ?>
        
        <!-- Mensagem se API não está disponível -->
        <?php if ($cacheStatus === null && empty($caches)): ?>
        <div class="alert-message alert-warning">
            <span>⚠️</span>
            <span>Não foi possível conectar à API de cache. Verifique se o serviço está disponível.</span>
        </div>
        <?php endif; ?>
        
        <!-- Info Box -->
        <div class="info-box">
            <h4>📖 Sobre o Cache Warming</h4>
            <p>
                O Cache Warming pré-carrega dados frequentemente acessados no Redis, melhorando significativamente 
                o tempo de resposta da aplicação. Recomenda-se executar este processo após reinicializações do sistema 
                ou em momentos de baixa demanda.
            </p>
        </div>
        
        <!-- Header com botão de ação -->
        <div class="cache-header">
            <div>
                <h2 style="color: #1a1a2e;">🗃️ Caches Gerenciados</h2>
                <p style="color: #666; margin-top: 5px;">
                    <?= $totalCaches ?> caches configurados
                </p>
            </div>
            
            <form method="POST" id="warmForm">
                <input type="hidden" name="action" value="warm">
                <button type="submit" class="warm-btn" id="warmBtn" onclick="return confirmarWarming()">
                    <span class="spinner"></span>
                    <span class="btn-text">🔥 Executar Cache Warming</span>
                </button>
            </form>
        </div>
        
        <!-- Estatísticas -->
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-icon">🗃️</div>
                <div class="stat-value"><?= $totalCaches ?></div>
                <div class="stat-label">Caches Ativos</div>
            </div>
            <div class="stat-card">
                <div class="stat-icon">⚡</div>
                <div class="stat-value"><?= htmlspecialchars($redisInfo['type'] ?? 'Redis') ?></div>
                <div class="stat-label">Tipo de Cache</div>
            </div>
            <div class="stat-card">
                <div class="stat-icon">🔄</div>
                <div class="stat-value"><?= $redisInfo['avgResponseTime'] ?? '--' ?>ms</div>
                <div class="stat-label">Tempo Médio</div>
            </div>
        </div>
        
        <!-- Grid de Caches (dados reais da API) -->
        <?php if (!empty($caches)): ?>
        <div class="cache-grid">
            <?php foreach ($caches as $cache): ?>
            <div class="cache-card">
                <div class="cache-icon"><?= htmlspecialchars($cache['icone'] ?? '🗃️') ?></div>
                <div class="cache-name"><?= htmlspecialchars($cache['nome'] ?? 'Cache') ?></div>
                <div class="cache-desc"><?= htmlspecialchars($cache['descricao'] ?? 'Sem descrição') ?></div>
                <div>
                    <span class="cache-ttl">
                        <i>⏱️</i> TTL: <?= htmlspecialchars($cache['ttl'] ?? 'N/A') ?>
                    </span>
                    <?php if (isset($cache['size'])): ?>
                    <span class="cache-size">
                        📦 <?= number_format($cache['size']) ?> itens
                    </span>
                    <?php endif; ?>
                </div>
            </div>
            <?php endforeach; ?>
        </div>
        <?php elseif ($cacheStatus !== null): ?>
        <div class="empty-state">
            <div class="empty-state-icon">🗃️</div>
            <h3>Nenhum cache encontrado</h3>
            <p>Não há caches configurados no momento.</p>
        </div>
        <?php endif; ?>
        
        <!-- Resultado da última execução -->
        <?php if ($resultado): ?>
        <div class="result-panel">
            <div class="result-title">📋 Resultado da Última Execução</div>
            <div class="result-content">
                <pre><?= json_encode($resultado, JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE) ?></pre>
            </div>
        </div>
        <?php endif; ?>
        
        <!-- Painel de Monitoramento (dados reais da API) -->
        <?php if ($redisInfo): ?>
        <div class="result-panel">
            <div class="result-title">📊 Informações do Sistema</div>
            <div style="display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-top: 15px;">
                <div>
                    <strong style="color: #666; font-size: 13px;">Redis Host</strong>
                    <p style="font-size: 16px; margin-top: 5px;"><?= htmlspecialchars($redisInfo['host'] ?? 'redis:6379') ?></p>
                </div>
                <div>
                    <strong style="color: #666; font-size: 13px;">Timeout</strong>
                    <p style="font-size: 16px; margin-top: 5px;"><?= htmlspecialchars($redisInfo['timeout'] ?? '60') ?> segundos</p>
                </div>
                <div>
                    <strong style="color: #666; font-size: 13px;">Última Execução</strong>
                    <p style="font-size: 16px; margin-top: 5px;">
                        <?= $ultimaExecucao ? date('d/m/Y H:i:s', strtotime($ultimaExecucao)) : 'Nunca' ?>
                    </p>
                </div>
                <div>
                    <strong style="color: #666; font-size: 13px;">Status</strong>
                    <p style="font-size: 16px; margin-top: 5px; color: <?= $redisInfo['connected'] ? '#27ae60' : '#e74c3c' ?>;">
                        <?= $redisInfo['connected'] ? '✅ Online' : '❌ Offline' ?>
                    </p>
                </div>
            </div>
            
            <?php if (isset($redisInfo['memory_usage'])): ?>
            <div style="margin-top: 20px;">
                <strong style="color: #666; font-size: 13px;">Uso de Memória Redis</strong>
                <div class="progress-bar" style="margin-top: 8px;">
                    <div class="progress-fill" style="width: <?= min(100, $redisInfo['memory_usage']) ?>%; background: #3498db;"></div>
                </div>
                <small><?= number_format($redisInfo['memory_usage'], 1) ?>% usado</small>
            </div>
            <?php endif; ?>
        </div>
        <?php endif; ?>
        
        <!-- Agendamento Automático -->
        <div class="alert-info" style="margin-top: 25px;">
            <strong>⏰ Agendamento Automático:</strong> O cache warming é executado automaticamente a cada 6 horas 
            (00:00, 06:00, 12:00, 18:00) para manter os dados sempre atualizados.
        </div>
    </div>
    
    <script>
        function confirmarWarming() {
            const confirmado = confirm(
                '🔥 Executar Cache Warming?\n\n' +
                'Este processo irá:\n' +
                '• Pré-carregar todos os caches no Redis\n' +
                '• Pode levar alguns segundos\n' +
                '• Melhorar o desempenho do sistema\n\n' +
                'Deseja continuar?'
            );
            
            if (confirmado) {
                const btn = document.getElementById('warmBtn');
                btn.classList.add('loading');
                btn.disabled = true;
            }
            
            return confirmado;
        }
        
        <?php if ($mensagemTipo === 'error'): ?>
        console.error('Erro no cache warming:', <?= json_encode($mensagem) ?>);
        <?php endif; ?>
    </script>
</body>
</html>