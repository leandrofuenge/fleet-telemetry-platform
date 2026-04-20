<?php
require_once '../config.php';

$page_title = 'Pontos de Entrega';
$page_icon = '📦';

// Verificar permissão (gestor, admin ou operador)
if (!in_array(($_SESSION['usuario']['perfil'] ?? ''), ['gestor', 'admin', 'operador'])) {
    header('Location: dashboard.php');
    exit;
}

// Função para chamar a API de pontos de entrega
function chamarApiPontos($endpoint, $method = 'GET', $data = null) {
    $url = API_URL . '/v1/pontos-entrega' . $endpoint;
    
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
    
    if ($action === 'atualizar_status') {
        $dados = [
            'pontoEntregaId' => intval($_POST['ponto_id']),
            'novoStatus' => $_POST['novo_status'],
            'assinaturaPath' => $_POST['assinatura_path'] ?? null,
            'fotoEntregaPath' => $_POST['foto_path'] ?? null,
            'ocorrencia' => $_POST['ocorrencia'] ?? null
        ];
        
        $resultado = chamarApiPontos('/atualizar-status', 'POST', $dados);
        
        if ($resultado) {
            $mensagem = 'Status atualizado com sucesso!';
            $mensagemTipo = 'success';
        } else {
            $mensagem = 'Erro ao atualizar status.';
            $mensagemTipo = 'error';
        }
    }
}

// Buscar viagem específica
$viagemId = $_GET['viagem_id'] ?? '';
$pontos = [];
$estatisticas = null;
$compliance = null;
$proximoPendente = null;
$concluida = false;
$erroApi = false;

if (!empty($viagemId)) {
    // Carregar pontos da viagem
    $resultado = chamarApiPontos('/viagem/' . $viagemId);
    if ($resultado) {
        $pontos = $resultado;
    } else {
        $erroApi = true;
    }
    
    // Carregar estatísticas
    $estatisticas = chamarApiPontos('/viagem/' . $viagemId . '/estatisticas');
    
    // Carregar compliance
    $compliance = chamarApiPontos('/viagem/' . $viagemId . '/compliance');
    
    // Carregar próximo pendente
    $proximoPendente = chamarApiPontos('/viagem/' . $viagemId . '/proximo-pendente');
    
    // Verificar se viagem está concluída
    $concluida = chamarApiPontos('/viagem/' . $viagemId . '/concluida') ?? false;
}

// Função para obter classe do status
function getStatusClass($status) {
    return [
        'PENDENTE' => 'warning',
        'CHEGOU' => 'info',
        'ENTREGUE' => 'success',
        'FALHOU' => 'danger',
        'PULADO' => 'secondary'
    ][$status] ?? 'secondary';
}

// Função para obter ícone do status
function getStatusIcon($status) {
    return [
        'PENDENTE' => '⏳',
        'CHEGOU' => '📍',
        'ENTREGUE' => '✅',
        'FALHOU' => '❌',
        'PULADO' => '⏭️'
    ][$status] ?? '❓';
}

// Função para obter ícone do tipo
function getTipoIcon($tipo) {
    return [
        'ENTREGA' => '📦',
        'COLETA' => '📤',
        'PARADA' => '⏸️',
        'PERNOITE' => '🌙',
        'ABASTECIMENTO' => '⛽'
    ][$tipo] ?? '📍';
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
        .search-section {
            background: white;
            padding: 25px;
            border-radius: 12px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
            margin-bottom: 25px;
        }
        .search-form {
            display: flex;
            gap: 15px;
            align-items: flex-end;
        }
        .search-input {
            flex: 1;
            padding: 14px 15px;
            border: 2px solid #e0e6ed;
            border-radius: 8px;
            font-size: 16px;
        }
        .search-input:focus {
            outline: none;
            border-color: #2c5364;
        }
        
        .stats-mini {
            display: grid;
            grid-template-columns: repeat(5, 1fr);
            gap: 15px;
            margin-bottom: 25px;
        }
        .stat-mini {
            background: white;
            padding: 15px;
            border-radius: 10px;
            text-align: center;
            box-shadow: 0 2px 5px rgba(0,0,0,0.05);
        }
        .stat-mini-value {
            font-size: 24px;
            font-weight: bold;
        }
        .stat-mini-label {
            font-size: 12px;
            color: #666;
            margin-top: 5px;
        }
        
        .progress-container {
            margin: 20px 0;
        }
        .progress-header {
            display: flex;
            justify-content: space-between;
            margin-bottom: 10px;
        }
        .progress-bar {
            width: 100%;
            height: 12px;
            background: #ecf0f1;
            border-radius: 6px;
            overflow: hidden;
        }
        .progress-fill {
            height: 100%;
            background: linear-gradient(90deg, #27ae60, #2ecc71);
            border-radius: 6px;
            transition: width 0.3s;
        }
        
        .pontos-timeline {
            background: white;
            border-radius: 12px;
            padding: 30px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
        }
        .timeline-title {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 25px;
        }
        
        .ponto-item {
            display: flex;
            gap: 20px;
            padding: 20px 0;
            border-bottom: 1px solid #eee;
            position: relative;
        }
        .ponto-item:last-child {
            border-bottom: none;
        }
        .ponto-marker {
            display: flex;
            flex-direction: column;
            align-items: center;
        }
        .ponto-icon {
            width: 50px;
            height: 50px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 24px;
            z-index: 2;
        }
        .ponto-icon.success { background: #d5f5e3; color: #1e8449; }
        .ponto-icon.warning { background: #fef9e7; color: #d4ac0d; }
        .ponto-icon.info { background: #e8f4f8; color: #2980b9; }
        .ponto-icon.danger { background: #fadbd8; color: #922b21; }
        .ponto-icon.secondary { background: #ecf0f1; color: #7f8c8d; }
        
        .ponto-line {
            width: 2px;
            flex: 1;
            background: #ddd;
            margin: 5px 0;
        }
        .ponto-line.completed {
            background: #27ae60;
        }
        
        .ponto-content {
            flex: 1;
        }
        .ponto-header {
            display: flex;
            align-items: center;
            gap: 15px;
            margin-bottom: 10px;
            flex-wrap: wrap;
        }
        .ponto-ordem {
            font-weight: 600;
            color: #1a1a2e;
            font-size: 16px;
        }
        .ponto-tipo {
            padding: 3px 10px;
            border-radius: 20px;
            font-size: 11px;
            font-weight: bold;
            background: #ecf0f1;
            color: #7f8c8d;
        }
        .ponto-status {
            padding: 3px 10px;
            border-radius: 20px;
            font-size: 11px;
            font-weight: bold;
        }
        
        .ponto-detalhes {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 15px;
            margin-top: 15px;
        }
        .detalhe-item {
            display: flex;
            flex-direction: column;
            gap: 3px;
        }
        .detalhe-label {
            font-size: 11px;
            color: #888;
            text-transform: uppercase;
        }
        .detalhe-valor {
            font-weight: 500;
            color: #1a1a2e;
        }
        
        .ponto-actions {
            display: flex;
            gap: 10px;
            margin-left: 20px;
        }
        
        .compliance-alert {
            background: #fef9e7;
            border-left: 4px solid #f39c12;
            padding: 20px;
            border-radius: 8px;
            margin: 25px 0;
        }
        .compliance-alert.critical {
            background: #fadbd8;
            border-left-color: #e74c3c;
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
            max-width: 500px;
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
        
        .form-group {
            margin-bottom: 20px;
        }
        .form-group label {
            display: block;
            margin-bottom: 8px;
            font-weight: 600;
        }
        .form-group input,
        .form-group select,
        .form-group textarea {
            width: 100%;
            padding: 12px 15px;
            border: 2px solid #e0e6ed;
            border-radius: 8px;
            font-size: 14px;
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
            color: #999;
        }
        
        .badge-concluida {
            background: #27ae60;
            color: white;
            padding: 5px 15px;
            border-radius: 20px;
            font-size: 14px;
        }
        .badge-andamento {
            background: #3498db;
            color: white;
            padding: 5px 15px;
            border-radius: 20px;
            font-size: 14px;
        }
        
        @media (max-width: 768px) {
            .stats-mini { grid-template-columns: repeat(2, 1fr); }
            .ponto-item { flex-wrap: wrap; }
            .ponto-actions { margin-left: 70px; margin-top: 15px; }
            .search-form { flex-direction: column; }
            .ponto-detalhes { grid-template-columns: 1fr; }
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
            <?= $mensagemTipo === 'success' ? '✅' : '❌' ?> <?= htmlspecialchars($mensagem) ?>
        </div>
        <?php endif; ?>
        
        <!-- Busca por Viagem -->
        <div class="search-section">
            <form method="GET" class="search-form">
                <input type="text" name="viagem_id" class="search-input" 
                       placeholder="Digite o ID da viagem..." 
                       value="<?= htmlspecialchars($viagemId) ?>" required>
                <button type="submit" class="btn btn-primary">🔍 Buscar Pontos de Entrega</button>
            </form>
        </div>
        
        <?php if (!empty($viagemId) && !empty($pontos)): ?>
            
            <!-- Status da Viagem -->
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                <h2>Viagem #<?= $viagemId ?></h2>
                <span class="<?= $concluida ? 'badge-concluida' : 'badge-andamento' ?>">
                    <?= $concluida ? '✅ CONCLUÍDA' : '🔄 EM ANDAMENTO' ?>
                </span>
            </div>
            
            <!-- Estatísticas -->
            <?php if ($estatisticas): ?>
            <div class="stats-mini">
                <div class="stat-mini">
                    <div class="stat-mini-value"><?= $estatisticas['total'] ?? 0 ?></div>
                    <div class="stat-mini-label">Total de Pontos</div>
                </div>
                <div class="stat-mini">
                    <div class="stat-mini-value" style="color: #27ae60;"><?= $estatisticas['entregues'] ?? 0 ?></div>
                    <div class="stat-mini-label">Entregues</div>
                </div>
                <div class="stat-mini">
                    <div class="stat-mini-value" style="color: #e74c3c;"><?= $estatisticas['falhos'] ?? 0 ?></div>
                    <div class="stat-mini-label">Falhos</div>
                </div>
                <div class="stat-mini">
                    <div class="stat-mini-value" style="color: #f39c12;"><?= $estatisticas['semPod'] ?? 0 ?></div>
                    <div class="stat-mini-label">Sem POD</div>
                </div>
                <div class="stat-mini">
                    <div class="stat-mini-value"><?= number_format($estatisticas['taxaSucesso'] ?? 0, 1) ?>%</div>
                    <div class="stat-mini-label">Taxa de Sucesso</div>
                </div>
            </div>
            
            <!-- Barra de Progresso -->
            <div class="progress-container">
                <div class="progress-header">
                    <span>Progresso da Viagem</span>
                    <span>
                        <?= $estatisticas['entregues'] ?? 0 ?> / <?= $estatisticas['total'] ?? 0 ?> entregues
                    </span>
                </div>
                <div class="progress-bar">
                    <?php 
                    $percentual = ($estatisticas['total'] ?? 0) > 0 ? 
                        (($estatisticas['entregues'] ?? 0) / $estatisticas['total']) * 100 : 0;
                    ?>
                    <div class="progress-fill" style="width: <?= $percentual ?>%"></div>
                </div>
            </div>
            <?php endif; ?>
            
            <!-- Alerta de Compliance -->
            <?php if ($compliance && !($compliance['complianceOk'] ?? true)): ?>
            <div class="compliance-alert <?= ($compliance['quantidadeSemPod'] ?? 0) > 2 ? 'critical' : '' ?>">
                <strong>⚠️ Alerta de Compliance</strong><br>
                Existem <?= $compliance['quantidadeSemPod'] ?> entrega(s) sem Proof of Delivery (assinatura ou foto).
                Isso pode gerar não-conformidade em auditorias.
            </div>
            <?php endif; ?>
            
            <!-- Próximo Ponto Pendente -->
            <?php if ($proximoPendente): ?>
            <div style="background: #e8f4f8; padding: 15px; border-radius: 8px; margin-bottom: 25px;">
                <strong>📍 Próximo Ponto:</strong> 
                #<?= $proximoPendente['ordem'] ?> - 
                <?= htmlspecialchars($proximoPendente['nomeDestinatario'] ?? 'Destino') ?>
                (<?= htmlspecialchars($proximoPendente['endereco'] ?? '—') ?>)
            </div>
            <?php endif; ?>
            
            <!-- Timeline de Pontos -->
            <div class="pontos-timeline">
                <div class="timeline-title">
                    <h3>📋 Pontos de Entrega</h3>
                </div>
                
                <?php foreach ($pontos as $index => $ponto): ?>
                <?php 
                    $status = $ponto['status'] ?? 'PENDENTE';
                    $statusClass = getStatusClass($status);
                    $statusIcon = getStatusIcon($status);
                    $tipoIcon = getTipoIcon($ponto['tipo'] ?? 'ENTREGA');
                    $isLast = $index === count($pontos) - 1;
                ?>
                <div class="ponto-item">
                    <div class="ponto-marker">
                        <div class="ponto-icon <?= $statusClass ?>">
                            <?= $tipoIcon ?>
                        </div>
                        <?php if (!$isLast): ?>
                        <div class="ponto-line <?= $status === 'ENTREGUE' ? 'completed' : '' ?>"></div>
                        <?php endif; ?>
                    </div>
                    
                    <div class="ponto-content">
                        <div class="ponto-header">
                            <span class="ponto-ordem">Ponto #<?= $ponto['ordem'] ?></span>
                            <span class="ponto-tipo"><?= $ponto['tipo'] ?></span>
                            <span class="ponto-status" style="background: <?= $statusClass === 'success' ? '#d5f5e3' : ($statusClass === 'warning' ? '#fef9e7' : '#ecf0f1') ?>; color: <?= $statusClass === 'success' ? '#1e8449' : ($statusClass === 'warning' ? '#d4ac0d' : '#7f8c8d') ?>;">
                                <?= $statusIcon ?> <?= $status ?>
                            </span>
                        </div>
                        
                        <div style="margin: 10px 0;">
                            <strong><?= htmlspecialchars($ponto['nomeDestinatario'] ?? '—') ?></strong><br>
                            <span style="color: #666; font-size: 14px;">
                                <?= htmlspecialchars($ponto['endereco'] ?? 'Endereço não informado') ?>
                            </span>
                        </div>
                        
                        <?php if ($status === 'ENTREGUE' || $status === 'FALHOU' || $status === 'CHEGOU'): ?>
                        <div class="ponto-detalhes">
                            <?php if ($ponto['dataChegada']): ?>
                            <div class="detalhe-item">
                                <span class="detalhe-label">📍 Chegada</span>
                                <span class="detalhe-valor"><?= date('d/m/Y H:i', strtotime($ponto['dataChegada'])) ?></span>
                            </div>
                            <?php endif; ?>
                            
                            <?php if ($ponto['dataEntrega']): ?>
                            <div class="detalhe-item">
                                <span class="detalhe-label">✅ Entrega</span>
                                <span class="detalhe-valor"><?= date('d/m/Y H:i', strtotime($ponto['dataEntrega'])) ?></span>
                            </div>
                            <?php endif; ?>
                            
                            <?php if ($ponto['tempoPermanenciaMin']): ?>
                            <div class="detalhe-item">
                                <span class="detalhe-label">⏱️ Permanência</span>
                                <span class="detalhe-valor"><?= $ponto['tempoPermanenciaMin'] ?> min</span>
                            </div>
                            <?php endif; ?>
                            
                            <?php if ($ponto['assinaturaPath']): ?>
                            <div class="detalhe-item">
                                <span class="detalhe-label">✍️ Assinatura</span>
                                <span class="detalhe-valor">✅ Registrada</span>
                            </div>
                            <?php endif; ?>
                            
                            <?php if ($ponto['fotoEntregaPath']): ?>
                            <div class="detalhe-item">
                                <span class="detalhe-label">📸 Foto</span>
                                <span class="detalhe-valor">✅ Registrada</span>
                            </div>
                            <?php endif; ?>
                            
                            <?php if ($ponto['ocorrencia']): ?>
                            <div class="detalhe-item" style="grid-column: span 2;">
                                <span class="detalhe-label">📝 Ocorrência</span>
                                <span class="detalhe-valor"><?= htmlspecialchars($ponto['ocorrencia']) ?></span>
                            </div>
                            <?php endif; ?>
                        </div>
                        <?php endif; ?>
                    </div>
                    
                    <div class="ponto-actions">
                        <?php if ($status === 'PENDENTE'): ?>
                        <button class="btn btn-outline btn-sm" onclick="abrirModalChegada(<?= $ponto['id'] ?>)">
                            📍 Chegou
                        </button>
                        <?php elseif ($status === 'CHEGOU'): ?>
                        <button class="btn btn-success btn-sm" onclick="abrirModalEntrega(<?= $ponto['id'] ?>)">
                            ✅ Entregar
                        </button>
                        <button class="btn btn-outline btn-sm" onclick="abrirModalFalha(<?= $ponto['id'] ?>)">
                            ❌ Falhou
                        </button>
                        <?php endif; ?>
                        
                        <?php if ($ponto['latitude'] && $ponto['longitude']): ?>
                        <a href="https://www.openstreetmap.org/?mlat=<?= $ponto['latitude'] ?>&mlon=<?= $ponto['longitude'] ?>&zoom=17" 
                           target="_blank" class="btn btn-outline btn-sm">🗺️ Mapa</a>
                        <?php endif; ?>
                    </div>
                </div>
                <?php endforeach; ?>
            </div>
            
        <?php elseif (!empty($viagemId)): ?>
            <div class="empty-state">
                <div class="empty-state-icon">📦</div>
                <h3>Nenhum ponto encontrado</h3>
                <p>Viagem #<?= $viagemId ?> não possui pontos de entrega cadastrados.</p>
            </div>
        <?php else: ?>
            <div class="empty-state">
                <div class="empty-state-icon">🔍</div>
                <h3>Informe o ID da viagem</h3>
                <p>Digite o número da viagem para visualizar seus pontos de entrega.</p>
            </div>
        <?php endif; ?>
    </div>
    
    <!-- Modal de Ação -->
    <div class="modal" id="acaoModal">
        <div class="modal-content">
            <div class="modal-header">
                <h2 id="modalTitle">Atualizar Status</h2>
                <button class="modal-close" onclick="fecharModal()">&times;</button>
            </div>
            <form method="POST" id="acaoForm">
                <input type="hidden" name="action" value="atualizar_status">
                <input type="hidden" name="ponto_id" id="modalPontoId">
                <input type="hidden" name="novo_status" id="modalNovoStatus">
                
                <div id="modalFields"></div>
                
                <div class="form-actions" style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px;">
                    <button type="button" class="btn btn-outline" onclick="fecharModal()">Cancelar</button>
                    <button type="submit" class="btn btn-primary">Confirmar</button>
                </div>
            </form>
        </div>
    </div>
    
    <script>
        function abrirModalChegada(pontoId) {
            document.getElementById('modalTitle').textContent = 'Registrar Chegada';
            document.getElementById('modalPontoId').value = pontoId;
            document.getElementById('modalNovoStatus').value = 'CHEGOU';
            document.getElementById('modalFields').innerHTML = `
                <p style="color: #666; margin-bottom: 20px;">Confirme que o veículo chegou ao ponto de entrega.</p>
            `;
            document.getElementById('acaoModal').classList.add('active');
        }
        
        function abrirModalEntrega(pontoId) {
            document.getElementById('modalTitle').textContent = 'Registrar Entrega';
            document.getElementById('modalPontoId').value = pontoId;
            document.getElementById('modalNovoStatus').value = 'ENTREGUE';
            document.getElementById('modalFields').innerHTML = `
                <div class="form-group">
                    <label>Assinatura (Path no MinIO)</label>
                    <input type="text" name="assinatura_path" placeholder="caminho/para/assinatura.png">
                    <small style="color: #888;">OU</small>
                </div>
                <div class="form-group">
                    <label>Foto da Entrega (Path no MinIO)</label>
                    <input type="text" name="foto_path" placeholder="caminho/para/foto.jpg">
                </div>
                <p style="color: #e67e22; font-size: 13px; margin-top: 10px;">
                    ⚠️ Pelo menos um dos campos acima é obrigatório (assinatura OU foto).
                </p>
            `;
            document.getElementById('acaoModal').classList.add('active');
        }
        
        function abrirModalFalha(pontoId) {
            document.getElementById('modalTitle').textContent = 'Registrar Falha';
            document.getElementById('modalPontoId').value = pontoId;
            document.getElementById('modalNovoStatus').value = 'FALHOU';
            document.getElementById('modalFields').innerHTML = `
                <div class="form-group">
                    <label>Motivo da Falha *</label>
                    <textarea name="ocorrencia" rows="4" required 
                              placeholder="Descreva o motivo da falha na entrega... (mínimo 10 caracteres)"></textarea>
                    <small style="color: #888;">Campo obrigatório para registrar falha.</small>
                </div>
            `;
            document.getElementById('acaoModal').classList.add('active');
        }
        
        function fecharModal() {
            document.getElementById('acaoModal').classList.remove('active');
        }
        
        // Fechar modal ao clicar fora
        document.getElementById('acaoModal').addEventListener('click', function(e) {
            if (e.target === this) {
                fecharModal();
            }
        });
    </script>
</body>
</html>