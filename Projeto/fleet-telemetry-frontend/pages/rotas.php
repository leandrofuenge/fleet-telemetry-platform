<?php
require_once '../config.php';

$page_title = 'Rotas';
$page_icon = '📍';

// Verificar permissão (gestor, admin ou operador)
if (!in_array(($_SESSION['usuario']['perfil'] ?? ''), ['gestor', 'admin', 'operador'])) {
    header('Location: dashboard.php');
    exit;
}

// Função para chamar a API de rotas
function chamarApiRotas($endpoint, $method = 'GET', $data = null) {
    $url = API_URL . '/v1/rotas' . $endpoint;
    
    $options = [
        'http' => [
            'header' => "Content-Type: application/json\r\n" .
                       "Authorization: Bearer " . ($_SESSION['token'] ?? '') . "\r\n",
            'method' => $method,
            'timeout' => 30
        ]
    ];
    
    if ($data && in_array($method, ['POST', 'PUT'])) {
        $options['http']['content'] = json_encode($data);
    }
    
    $context = stream_context_create($options);
    $resultado = @file_get_contents($url, false, $context);
    
    if ($resultado === false) {
        return null;
    }
    
    return json_decode($resultado, true);
}

// Processar ações
$mensagem = '';
$mensagemTipo = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $action = $_POST['action'] ?? '';
    
    if ($action === 'salvar') {
        $dados = [
            'nome' => $_POST['nome'] ?? '',
            'origem' => $_POST['origem'] ?? '',
            'destino' => $_POST['destino'] ?? '',
            'latitudeOrigem' => floatval($_POST['latitude_origem'] ?? 0),
            'longitudeOrigem' => floatval($_POST['longitude_origem'] ?? 0),
            'latitudeDestino' => floatval($_POST['latitude_destino'] ?? 0),
            'longitudeDestino' => floatval($_POST['longitude_destino'] ?? 0),
            'distanciaPrevista' => floatval($_POST['distancia_prevista'] ?? 0),
            'tempoPrevisto' => intval($_POST['tempo_previsto'] ?? 0),
            'status' => $_POST['status'] ?? 'PLANEJADA',
            'ativa' => isset($_POST['ativa']) ? true : false
        ];
        
        $id = $_POST['id'] ?? '';
        
        if ($id) {
            $resultado = chamarApiRotas('/' . $id, 'PUT', $dados);
        } else {
            $resultado = chamarApiRotas('', 'POST', $dados);
        }
        
        if ($resultado) {
            $mensagem = $id ? 'Rota atualizada com sucesso!' : 'Rota criada com sucesso!';
            $mensagemTipo = 'success';
        } else {
            $mensagem = 'Erro ao salvar rota. Verifique se o nome já existe.';
            $mensagemTipo = 'error';
        }
    } elseif ($action === 'excluir') {
        $id = $_POST['id'] ?? '';
        if ($id) {
            $resultado = chamarApiRotas('/' . $id, 'DELETE');
            if ($resultado !== false) {
                $mensagem = 'Rota excluída com sucesso!';
                $mensagemTipo = 'success';
            } else {
                $mensagem = 'Erro ao excluir rota.';
                $mensagemTipo = 'error';
            }
        }
    }
}

// Carregar lista de rotas
$rotas = [];
$filtroStatus = $_GET['status'] ?? 'todos';

if ($filtroStatus === 'ativas') {
    $resultado = chamarApiRotas('/ativas');
} elseif ($filtroStatus !== 'todos') {
    $resultado = chamarApiRotas('/status/' . $filtroStatus);
} else {
    $resultado = chamarApiRotas('');
}

if ($resultado) {
    $rotas = $resultado;
}

// Carregar rota para edição
$rotaEdicao = null;
if (isset($_GET['editar'])) {
    $rotaEdicao = chamarApiRotas('/' . $_GET['editar']);
}

// Estatísticas
$totalRotas = count($rotas);
$rotasAtivas = count(array_filter($rotas, fn($r) => $r['ativa'] ?? false));
$distanciaTotal = array_sum(array_column($rotas, 'distanciaPrevista'));

// Função para obter classe do status
function getStatusClass($status) {
    return [
        'PLANEJADA' => 'info',
        'EM_ANDAMENTO' => 'warning',
        'CONCLUIDA' => 'success',
        'CANCELADA' => 'danger'
    ][$status] ?? 'secondary';
}

// Função para formatar distância
function formatarDistancia($km) {
    if ($km === null) return '—';
    if ($km < 1) return round($km * 1000) . ' m';
    return number_format($km, 1) . ' km';
}

// Função para formatar tempo
function formatarTempo($minutos) {
    if (!$minutos) return '—';
    if ($minutos < 60) return $minutos . ' min';
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
            grid-template-columns: repeat(3, 1fr);
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
        
        .header-actions {
            display: flex;
            gap: 15px;
            align-items: center;
        }
        
        .filter-bar {
            display: flex;
            gap: 10px;
            margin-bottom: 25px;
        }
        .filter-tabs {
            display: flex;
            gap: 5px;
            background: white;
            padding: 5px;
            border-radius: 10px;
        }
        .filter-tab {
            padding: 8px 16px;
            border: none;
            background: transparent;
            border-radius: 8px;
            cursor: pointer;
            font-weight: 500;
            color: #666;
            text-decoration: none;
        }
        .filter-tab.active {
            background: #2c5364;
            color: white;
        }
        
        .rotas-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
            gap: 20px;
        }
        .rota-card {
            background: white;
            border-radius: 12px;
            padding: 25px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
            transition: all 0.3s;
            position: relative;
        }
        .rota-card:hover {
            transform: translateY(-3px);
            box-shadow: 0 5px 20px rgba(0,0,0,0.1);
        }
        .rota-card.inativa {
            opacity: 0.7;
            background: #f8f9fa;
        }
        
        .rota-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            margin-bottom: 15px;
        }
        .rota-nome {
            font-size: 18px;
            font-weight: 600;
            color: #1a1a2e;
        }
        .rota-status {
            padding: 4px 10px;
            border-radius: 20px;
            font-size: 11px;
            font-weight: bold;
        }
        .status-info { background: #e8f4f8; color: #2980b9; }
        .status-warning { background: #fef9e7; color: #d4ac0d; }
        .status-success { background: #d5f5e3; color: #1e8449; }
        .status-danger { background: #fadbd8; color: #922b21; }
        
        .rota-path {
            display: flex;
            align-items: center;
            gap: 10px;
            margin: 20px 0;
            padding: 15px;
            background: #f8f9fa;
            border-radius: 8px;
        }
        .path-origem, .path-destino {
            flex: 1;
            text-align: center;
        }
        .path-label {
            font-size: 11px;
            color: #888;
            text-transform: uppercase;
        }
        .path-value {
            font-weight: 600;
            color: #1a1a2e;
        }
        .path-arrow {
            font-size: 24px;
            color: #2c5364;
        }
        
        .rota-details {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 15px;
            margin: 15px 0;
        }
        .detail-item {
            display: flex;
            flex-direction: column;
            gap: 3px;
        }
        .detail-label {
            font-size: 11px;
            color: #888;
        }
        .detail-value {
            font-weight: 600;
            color: #1a1a2e;
        }
        
        .rota-actions {
            display: flex;
            gap: 10px;
            margin-top: 20px;
            padding-top: 15px;
            border-top: 1px solid #eee;
        }
        
        .badge-ativa {
            position: absolute;
            top: 20px;
            right: 20px;
            background: #27ae60;
            color: white;
            padding: 3px 10px;
            border-radius: 20px;
            font-size: 11px;
            font-weight: bold;
        }
        
        .modal {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0,0,0,0.5);
            z-index: 1000;
            align-items: center;
            justify-content: center;
        }
        .modal.active { display: flex; }
        .modal-content {
            background: white;
            border-radius: 12px;
            padding: 30px;
            width: 90%;
            max-width: 600px;
            max-height: 90vh;
            overflow-y: auto;
        }
        .modal-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 20px;
        }
        .modal-close {
            background: none;
            border: none;
            font-size: 24px;
            cursor: pointer;
        }
        
        .form-row {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 15px;
        }
        .form-group {
            margin-bottom: 20px;
        }
        .form-group label {
            display: block;
            margin-bottom: 8px;
            font-weight: 600;
            color: #1a1a2e;
            font-size: 14px;
        }
        .form-group input,
        .form-group select {
            width: 100%;
            padding: 12px 15px;
            border: 2px solid #e0e6ed;
            border-radius: 8px;
            font-size: 14px;
        }
        .form-group input:focus,
        .form-group select:focus {
            outline: none;
            border-color: #2c5364;
        }
        .form-group small {
            display: block;
            margin-top: 5px;
            color: #888;
            font-size: 12px;
        }
        
        .checkbox-group {
            display: flex;
            align-items: center;
            gap: 10px;
            margin: 20px 0;
        }
        .checkbox-group input {
            width: 20px;
            height: 20px;
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
        
        .warning-note {
            background: #fef9e7;
            border-left: 4px solid #f39c12;
            padding: 15px;
            border-radius: 8px;
            margin: 15px 0;
            font-size: 13px;
        }
        
        @media (max-width: 768px) {
            .stats-grid { grid-template-columns: 1fr; }
            .rotas-grid { grid-template-columns: 1fr; }
            .header-actions { flex-direction: column; }
            .form-row { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>
    <?php include '../components/sidebar.php'; ?>
    
    <div class="main-content">
        <div class="top-bar">
            <h1 class="page-title"><span><?= $page_icon ?></span> <?= $page_title ?></h1>
            <div class="header-actions">
                <span style="background: #3498db; color: white; padding: 5px 12px; border-radius: 20px; font-size: 14px;">
                    <?= $totalRotas ?> rotas
                </span>
                <button class="btn btn-primary" onclick="abrirModal()">
                    ➕ Nova Rota
                </button>
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
            <div class="stat-card">
                <div class="stat-value"><?= $totalRotas ?></div>
                <div class="stat-label">Total de Rotas</div>
            </div>
            <div class="stat-card">
                <div class="stat-value"><?= $rotasAtivas ?></div>
                <div class="stat-label">Rotas Ativas</div>
            </div>
            <div class="stat-card">
                <div class="stat-value"><?= formatarDistancia($distanciaTotal) ?></div>
                <div class="stat-label">Distância Total</div>
            </div>
        </div>
        
        <!-- Filtros -->
        <div class="filter-bar">
            <div class="filter-tabs">
                <a href="?status=todos" class="filter-tab <?= $filtroStatus === 'todos' ? 'active' : '' ?>">Todas</a>
                <a href="?status=ativas" class="filter-tab <?= $filtroStatus === 'ativas' ? 'active' : '' ?>">Ativas</a>
                <a href="?status=PLANEJADA" class="filter-tab <?= $filtroStatus === 'PLANEJADA' ? 'active' : '' ?>">Planejadas</a>
                <a href="?status=EM_ANDAMENTO" class="filter-tab <?= $filtroStatus === 'EM_ANDAMENTO' ? 'active' : '' ?>">Em Andamento</a>
                <a href="?status=CONCLUIDA" class="filter-tab <?= $filtroStatus === 'CONCLUIDA' ? 'active' : '' ?>">Concluídas</a>
            </div>
        </div>
        
        <!-- Lista de Rotas -->
        <?php if (empty($rotas)): ?>
            <div class="empty-state">
                <div class="empty-state-icon">📍</div>
                <h3>Nenhuma rota encontrada</h3>
                <p>Clique em "Nova Rota" para começar.</p>
            </div>
        <?php else: ?>
            <div class="rotas-grid">
                <?php foreach ($rotas as $rota): ?>
                <?php 
                    $ativa = $rota['ativa'] ?? false;
                    $status = $rota['status'] ?? 'PLANEJADA';
                    $statusClass = getStatusClass($status);
                ?>
                <div class="rota-card <?= !$ativa ? 'inativa' : '' ?>">
                    <?php if ($ativa): ?>
                        <span class="badge-ativa">✅ ATIVA</span>
                    <?php endif; ?>
                    
                    <div class="rota-header">
                        <span class="rota-nome"><?= htmlspecialchars($rota['nome'] ?? '—') ?></span>
                        <span class="rota-status status-<?= $statusClass ?>"><?= $status ?></span>
                    </div>
                    
                    <div class="rota-path">
                        <div class="path-origem">
                            <div class="path-label">Origem</div>
                            <div class="path-value"><?= htmlspecialchars($rota['origem'] ?? '—') ?></div>
                        </div>
                        <div class="path-arrow">→</div>
                        <div class="path-destino">
                            <div class="path-label">Destino</div>
                            <div class="path-value"><?= htmlspecialchars($rota['destino'] ?? '—') ?></div>
                        </div>
                    </div>
                    
                    <div class="rota-details">
                        <div class="detail-item">
                            <span class="detail-label">📏 Distância Prevista</span>
                            <span class="detail-value"><?= formatarDistancia($rota['distanciaPrevista'] ?? 0) ?></span>
                        </div>
                        <div class="detail-item">
                            <span class="detail-label">⏱️ Tempo Previsto</span>
                            <span class="detail-value"><?= formatarTempo($rota['tempoPrevisto'] ?? 0) ?></span>
                        </div>
                        <?php if (!empty($rota['veiculoId'])): ?>
                        <div class="detail-item">
                            <span class="detail-label">🚛 Veículo</span>
                            <span class="detail-value">#<?= $rota['veiculoId'] ?></span>
                        </div>
                        <?php endif; ?>
                        <?php if (!empty($rota['motoristaId'])): ?>
                        <div class="detail-item">
                            <span class="detail-label">👤 Motorista</span>
                            <span class="detail-value">#<?= $rota['motoristaId'] ?></span>
                        </div>
                        <?php endif; ?>
                    </div>
                    
                    <div class="rota-actions">
                        <button class="btn btn-outline btn-sm" onclick="editarRota(<?= $rota['id'] ?>)">
                            ✏️ Editar
                        </button>
                        <button class="btn btn-outline btn-sm" onclick="verNoMapa(<?= $rota['id'] ?>)">
                            🗺️ Ver no Mapa
                        </button>
                        <form method="POST" style="display: inline;" onsubmit="return confirm('Excluir esta rota?')">
                            <input type="hidden" name="action" value="excluir">
                            <input type="hidden" name="id" value="<?= $rota['id'] ?>">
                            <button type="submit" class="btn btn-outline btn-sm" style="color: #e74c3c;">🗑️</button>
                        </form>
                    </div>
                </div>
                <?php endforeach; ?>
            </div>
        <?php endif; ?>
    </div>
    
    <!-- Modal Nova/Editar Rota -->
    <div class="modal" id="rotaModal">
        <div class="modal-content">
            <div class="modal-header">
                <h2 id="modalTitle">Nova Rota</h2>
                <button class="modal-close" onclick="fecharModal()">&times;</button>
            </div>
            <form method="POST" id="rotaForm">
                <input type="hidden" name="action" value="salvar">
                <input type="hidden" name="id" id="rotaId">
                
                <div class="form-group">
                    <label>Nome da Rota *</label>
                    <input type="text" name="nome" id="nome" required placeholder="Ex: Rota São Paulo - Campinas">
                </div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label>Origem *</label>
                        <input type="text" name="origem" id="origem" required placeholder="Cidade de origem">
                    </div>
                    <div class="form-group">
                        <label>Destino *</label>
                        <input type="text" name="destino" id="destino" required placeholder="Cidade de destino">
                    </div>
                </div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label>Latitude Origem</label>
                        <input type="number" step="0.000001" name="latitude_origem" id="latitude_origem" placeholder="-23.5505">
                    </div>
                    <div class="form-group">
                        <label>Longitude Origem</label>
                        <input type="number" step="0.000001" name="longitude_origem" id="longitude_origem" placeholder="-46.6333">
                    </div>
                </div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label>Latitude Destino</label>
                        <input type="number" step="0.000001" name="latitude_destino" id="latitude_destino" placeholder="-22.9068">
                    </div>
                    <div class="form-group">
                        <label>Longitude Destino</label>
                        <input type="number" step="0.000001" name="longitude_destino" id="longitude_destino" placeholder="-47.0619">
                    </div>
                </div>
                
                <div class="form-row">
                    <div class="form-group">
                        <label>Distância Prevista (km)</label>
                        <input type="number" step="0.1" name="distancia_prevista" id="distancia_prevista" placeholder="0.0">
                        <small>Calculada automaticamente pelo OSRM</small>
                    </div>
                    <div class="form-group">
                        <label>Tempo Previsto (min)</label>
                        <input type="number" name="tempo_previsto" id="tempo_previsto" placeholder="0">
                        <small>Calculado automaticamente pelo OSRM</small>
                    </div>
                </div>
                
                <div class="form-group">
                    <label>Status</label>
                    <select name="status" id="status">
                        <option value="PLANEJADA">Planejada</option>
                        <option value="EM_ANDAMENTO">Em Andamento</option>
                        <option value="CONCLUIDA">Concluída</option>
                        <option value="CANCELADA">Cancelada</option>
                    </select>
                </div>
                
                <div class="checkbox-group">
                    <input type="checkbox" name="ativa" id="ativa" value="1">
                    <label for="ativa">Rota Ativa</label>
                </div>
                
                <div class="warning-note">
                    <strong>⚠️ Atenção (RN-ROT-001):</strong> Para ativar uma rota, é necessário que o cálculo OSRM 
                    tenha sido realizado previamente. Certifique-se de que a distância prevista e o tempo previsto 
                    estejam preenchidos.
                </div>
                
                <div class="form-actions" style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px;">
                    <button type="button" class="btn btn-outline" onclick="fecharModal()">Cancelar</button>
                    <button type="submit" class="btn btn-primary">Salvar Rota</button>
                </div>
            </form>
        </div>
    </div>
    
    <script>
        function abrirModal() {
            document.getElementById('modalTitle').textContent = 'Nova Rota';
            document.getElementById('rotaId').value = '';
            document.getElementById('rotaForm').reset();
            document.getElementById('rotaModal').classList.add('active');
        }
        
        function fecharModal() {
            document.getElementById('rotaModal').classList.remove('active');
        }
        
        function editarRota(id) {
            fetch(`<?= API_URL ?>/v1/rotas/${id}`, {
                headers: { 'Authorization': 'Bearer <?= $_SESSION['token'] ?? '' ?>' }
            })
            .then(r => r.json())
            .then(r => {
                document.getElementById('modalTitle').textContent = 'Editar Rota';
                document.getElementById('rotaId').value = r.id;
                document.getElementById('nome').value = r.nome || '';
                document.getElementById('origem').value = r.origem || '';
                document.getElementById('destino').value = r.destino || '';
                document.getElementById('latitude_origem').value = r.latitudeOrigem || '';
                document.getElementById('longitude_origem').value = r.longitudeOrigem || '';
                document.getElementById('latitude_destino').value = r.latitudeDestino || '';
                document.getElementById('longitude_destino').value = r.longitudeDestino || '';
                document.getElementById('distancia_prevista').value = r.distanciaPrevista || '';
                document.getElementById('tempo_previsto').value = r.tempoPrevisto || '';
                document.getElementById('status').value = r.status || 'PLANEJADA';
                document.getElementById('ativa').checked = r.ativa || false;
                document.getElementById('rotaModal').classList.add('active');
            });
        }
        
        function verNoMapa(id) {
            fetch(`<?= API_URL ?>/v1/rotas/${id}`, {
                headers: { 'Authorization': 'Bearer <?= $_SESSION['token'] ?? '' ?>' }
            })
            .then(r => r.json())
            .then(r => {
                if (r.latitudeOrigem && r.longitudeOrigem && r.latitudeDestino && r.longitudeDestino) {
                    const url = `https://www.openstreetmap.org/directions?engine=osrm_car&route=${r.latitudeOrigem}%2C${r.longitudeOrigem}%3B${r.latitudeDestino}%2C${r.longitudeDestino}`;
                    window.open(url, '_blank');
                } else {
                    alert('Coordenadas não disponíveis para esta rota.');
                }
            });
        }
        
        // Fechar modal ao clicar fora
        document.getElementById('rotaModal').addEventListener('click', function(e) {
            if (e.target === this) {
                fecharModal();
            }
        });
    </script>
</body>
</html>