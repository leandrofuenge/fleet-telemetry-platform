<?php
require_once '../config.php';

$page_title = 'Motoristas';
$page_icon = '👤';

// Verificar permissão (gestor ou admin)
if (!in_array(($_SESSION['usuario']['perfil'] ?? ''), ['gestor', 'admin'])) {
    header('Location: dashboard.php');
    exit;
}

// Função para chamar a API de motoristas
function chamarApiMotoristas($endpoint, $method = 'GET', $data = null) {
    $url = API_URL . '/v1/motoristas' . $endpoint;
    
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
            'cpf' => $_POST['cpf'] ?? '',
            'cnh' => $_POST['cnh'] ?? '',
            'categoriaCnh' => $_POST['categoria_cnh'] ?? ''
        ];
        
        $id = $_POST['id'] ?? '';
        
        if ($id) {
            $resultado = chamarApiMotoristas('/' . $id, 'PUT', $dados);
        } else {
            $resultado = chamarApiMotoristas('', 'POST', $dados);
        }
        
        if ($resultado) {
            $mensagem = $id ? 'Motorista atualizado com sucesso!' : 'Motorista criado com sucesso!';
            $mensagemTipo = 'success';
        } else {
            $mensagem = 'Erro ao salvar motorista.';
            $mensagemTipo = 'error';
        }
    } elseif ($action === 'excluir') {
        $id = $_POST['id'] ?? '';
        if ($id) {
            $resultado = chamarApiMotoristas('/' . $id, 'DELETE');
            if ($resultado !== false) {
                $mensagem = 'Motorista excluído com sucesso!';
                $mensagemTipo = 'success';
            } else {
                $mensagem = 'Erro ao excluir motorista.';
                $mensagemTipo = 'error';
            }
        }
    }
}

// Carregar lista de motoristas
$motoristas = [];
$resultado = chamarApiMotoristas('');
if ($resultado) {
    $motoristas = $resultado;
}

// Carregar motorista para edição
$motoristaEdicao = null;
if (isset($_GET['editar'])) {
    $motoristaEdicao = chamarApiMotoristas('/' . $_GET['editar']);
}

// Estatísticas
$totalMotoristas = count($motoristas);
$motoristasAtivos = count(array_filter($motoristas, fn($m) => $m['ativo'] ?? true));
$scoreMedio = $totalMotoristas > 0 ? 
    array_sum(array_column($motoristas, 'score')) / $totalMotoristas : 0;

// Função para formatar CPF
function formatarCpf($cpf) {
    $cpf = preg_replace('/\D/', '', $cpf);
    if (strlen($cpf) != 11) return $cpf;
    return substr($cpf, 0, 3) . '.' . substr($cpf, 3, 3) . '.' . substr($cpf, 6, 3) . '-' . substr($cpf, 9, 2);
}

// Função para obter classe do score
function getScoreClass($score) {
    if ($score >= 800) return 'success';
    if ($score >= 600) return 'info';
    if ($score >= 400) return 'warning';
    return 'danger';
}

// Função para obter texto do score
function getScoreText($score) {
    if ($score >= 800) return 'Excelente';
    if ($score >= 600) return 'Bom';
    if ($score >= 400) return 'Regular';
    return 'Crítico';
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
        
        .search-bar {
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
        
        .motoristas-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
            gap: 20px;
        }
        .motorista-card {
            background: white;
            border-radius: 12px;
            padding: 25px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.05);
            transition: all 0.3s;
            position: relative;
        }
        .motorista-card:hover {
            transform: translateY(-3px);
            box-shadow: 0 5px 20px rgba(0,0,0,0.1);
        }
        .motorista-card.inativo {
            opacity: 0.6;
            background: #f8f9fa;
        }
        
        .motorista-header {
            display: flex;
            align-items: center;
            gap: 15px;
            margin-bottom: 20px;
        }
        .motorista-avatar {
            width: 60px;
            height: 60px;
            border-radius: 50%;
            background: linear-gradient(135deg, #2c5364, #0f2027);
            color: white;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 24px;
            font-weight: bold;
        }
        .motorista-info h3 {
            font-size: 18px;
            color: #1a1a2e;
            margin-bottom: 5px;
        }
        .motorista-info .cpf {
            font-size: 13px;
            color: #666;
        }
        
        .motorista-details {
            margin: 15px 0;
        }
        .detail-row {
            display: flex;
            justify-content: space-between;
            padding: 8px 0;
            border-bottom: 1px solid #eee;
        }
        .detail-row:last-child {
            border-bottom: none;
        }
        .detail-label {
            color: #888;
            font-size: 13px;
        }
        .detail-value {
            font-weight: 600;
            color: #1a1a2e;
        }
        
        .score-section {
            margin: 15px 0;
            padding: 15px;
            background: #f8f9fa;
            border-radius: 8px;
        }
        .score-header {
            display: flex;
            justify-content: space-between;
            margin-bottom: 10px;
        }
        .score-value {
            font-size: 24px;
            font-weight: bold;
        }
        .score-value.success { color: #27ae60; }
        .score-value.info { color: #3498db; }
        .score-value.warning { color: #e67e22; }
        .score-value.danger { color: #e74c3c; }
        
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
        .progress-fill.info { background: #3498db; }
        .progress-fill.warning { background: #e67e22; }
        .progress-fill.danger { background: #e74c3c; }
        
        .documentos-section {
            margin-top: 15px;
            font-size: 12px;
        }
        .documento-item {
            display: flex;
            align-items: center;
            gap: 8px;
            padding: 5px 0;
        }
        .documento-status {
            width: 8px;
            height: 8px;
            border-radius: 50%;
        }
        .documento-status.success { background: #27ae60; }
        .documento-status.warning { background: #e67e22; }
        .documento-status.danger { background: #e74c3c; }
        
        .motorista-actions {
            display: flex;
            gap: 10px;
            margin-top: 20px;
            padding-top: 15px;
            border-top: 1px solid #eee;
        }
        
        .status-badge {
            position: absolute;
            top: 20px;
            right: 20px;
            padding: 4px 10px;
            border-radius: 20px;
            font-size: 11px;
            font-weight: bold;
        }
        .badge-ativo { background: #d5f5e3; color: #1e8449; }
        .badge-inativo { background: #fadbd8; color: #922b21; }
        
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
        .modal-header h2 {
            font-size: 20px;
            color: #1a1a2e;
        }
        .modal-close {
            background: none;
            border: none;
            font-size: 24px;
            cursor: pointer;
            color: #999;
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
        
        @media (max-width: 768px) {
            .stats-grid { grid-template-columns: 1fr; }
            .motoristas-grid { grid-template-columns: 1fr; }
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
                <span style="background: #3498db; color: white; padding: 5px 12px; border-radius: 20px; font-size: 14px;">
                    <?= $totalMotoristas ?> cadastrados
                </span>
                <button class="btn btn-primary" onclick="abrirModal()">
                    ➕ Novo Motorista
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
                <div class="stat-value"><?= $totalMotoristas ?></div>
                <div class="stat-label">Total de Motoristas</div>
            </div>
            <div class="stat-card">
                <div class="stat-value"><?= $motoristasAtivos ?></div>
                <div class="stat-label">Motoristas Ativos</div>
            </div>
            <div class="stat-card">
                <div class="stat-value"><?= number_format($scoreMedio, 0) ?></div>
                <div class="stat-label">Score Médio</div>
            </div>
        </div>
        
        <!-- Barra de Busca -->
        <div class="search-bar">
            <input type="text" class="search-input" placeholder="Buscar por nome, CPF ou CNH..." id="searchInput" onkeyup="filtrarMotoristas()">
            <select class="filter-select" id="statusFilter" onchange="filtrarMotoristas()">
                <option value="todos">Todos</option>
                <option value="ativo">Ativos</option>
                <option value="inativo">Inativos</option>
            </select>
        </div>
        
        <!-- Lista de Motoristas -->
        <?php if (empty($motoristas)): ?>
            <div class="empty-state">
                <div class="empty-state-icon">👤</div>
                <h3>Nenhum motorista cadastrado</h3>
                <p>Clique em "Novo Motorista" para começar.</p>
            </div>
        <?php else: ?>
            <div class="motoristas-grid" id="motoristasGrid">
                <?php foreach ($motoristas as $m): ?>
                <?php 
                    $ativo = $m['ativo'] ?? true;
                    $score = $m['score'] ?? 1000;
                    $scoreClass = getScoreClass($score);
                    $scoreText = getScoreText($score);
                    $iniciais = strtoupper(substr($m['nome'] ?? 'M', 0, 2));
                ?>
                <div class="motorista-card <?= !$ativo ? 'inativo' : '' ?>" 
                     data-nome="<?= strtolower($m['nome'] ?? '') ?>"
                     data-cpf="<?= $m['cpf'] ?? '' ?>"
                     data-cnh="<?= $m['cnh'] ?? '' ?>"
                     data-ativo="<?= $ativo ? 'ativo' : 'inativo' ?>">
                    
                    <span class="status-badge <?= $ativo ? 'badge-ativo' : 'badge-inativo' ?>">
                        <?= $ativo ? 'ATIVO' : 'INATIVO' ?>
                    </span>
                    
                    <div class="motorista-header">
                        <div class="motorista-avatar"><?= $iniciais ?></div>
                        <div class="motorista-info">
                            <h3><?= htmlspecialchars($m['nome'] ?? '—') ?></h3>
                            <span class="cpf">CPF: <?= formatarCpf($m['cpf'] ?? '') ?></span>
                        </div>
                    </div>
                    
                    <div class="motorista-details">
                        <div class="detail-row">
                            <span class="detail-label">CNH</span>
                            <span class="detail-value"><?= htmlspecialchars($m['cnh'] ?? '—') ?></span>
                        </div>
                        <div class="detail-row">
                            <span class="detail-label">Categoria</span>
                            <span class="detail-value"><?= htmlspecialchars($m['categoriaCnh'] ?? '—') ?></span>
                        </div>
                        <?php if (!empty($m['telefone'])): ?>
                        <div class="detail-row">
                            <span class="detail-label">Telefone</span>
                            <span class="detail-value"><?= htmlspecialchars($m['telefone']) ?></span>
                        </div>
                        <?php endif; ?>
                        <?php if (!empty($m['email'])): ?>
                        <div class="detail-row">
                            <span class="detail-label">Email</span>
                            <span class="detail-value"><?= htmlspecialchars($m['email']) ?></span>
                        </div>
                        <?php endif; ?>
                    </div>
                    
                    <div class="score-section">
                        <div class="score-header">
                            <span>Score de Comportamento</span>
                            <span class="score-value <?= $scoreClass ?>"><?= $score ?></span>
                        </div>
                        <div class="progress-bar">
                            <div class="progress-fill <?= $scoreClass ?>" style="width: <?= $score / 10 ?>%"></div>
                        </div>
                        <div style="text-align: right; margin-top: 5px; font-size: 12px; color: #666;">
                            <?= $scoreText ?>
                        </div>
                    </div>
                    
                    <div class="documentos-section">
                        <?php 
                        $cnhVencida = isset($m['dataVencimentoCnh']) && strtotime($m['dataVencimentoCnh']) < time();
                        $asoVencido = isset($m['dataVencimentoAso']) && strtotime($m['dataVencimentoAso']) < time();
                        ?>
                        <div class="documento-item">
                            <span class="documento-status <?= $cnhVencida ? 'danger' : 'success' ?>"></span>
                            <span>CNH <?= $cnhVencida ? '(Vencida)' : '(Válida)' ?></span>
                        </div>
                        <div class="documento-item">
                            <span class="documento-status <?= $asoVencido ? 'danger' : 'success' ?>"></span>
                            <span>ASO <?= $asoVencido ? '(Vencido)' : '(Válido)' ?></span>
                        </div>
                        <?php if (isset($m['moppValido'])): ?>
                        <div class="documento-item">
                            <span class="documento-status <?= $m['moppValido'] ? 'success' : 'warning' ?>"></span>
                            <span>MOPP <?= $m['moppValido'] ? '(Válido)' : '(Não possui)' ?></span>
                        </div>
                        <?php endif; ?>
                    </div>
                    
                    <div class="motorista-actions">
                        <button class="btn btn-outline btn-sm" onclick="editarMotorista(<?= $m['id'] ?>)">
                            ✏️ Editar
                        </button>
                        <button class="btn btn-outline btn-sm" onclick="verDetalhes(<?= $m['id'] ?>)">
                            📋 Detalhes
                        </button>
                        <form method="POST" style="display: inline;" onsubmit="return confirm('Excluir este motorista?')">
                            <input type="hidden" name="action" value="excluir">
                            <input type="hidden" name="id" value="<?= $m['id'] ?>">
                            <button type="submit" class="btn btn-outline btn-sm" style="color: #e74c3c;">🗑️</button>
                        </form>
                    </div>
                </div>
                <?php endforeach; ?>
            </div>
        <?php endif; ?>
    </div>
    
    <!-- Modal Novo/Editar Motorista -->
    <div class="modal" id="motoristaModal">
        <div class="modal-content">
            <div class="modal-header">
                <h2 id="modalTitle">Novo Motorista</h2>
                <button class="modal-close" onclick="fecharModal()">&times;</button>
            </div>
            <form method="POST" id="motoristaForm">
                <input type="hidden" name="action" value="salvar">
                <input type="hidden" name="id" id="motoristaId">
                
                <div class="form-group">
                    <label>Nome Completo *</label>
                    <input type="text" name="nome" id="nome" required placeholder="Ex: João da Silva">
                </div>
                
                <div class="form-group">
                    <label>CPF *</label>
                    <input type="text" name="cpf" id="cpf" required placeholder="000.000.000-00" maxlength="14">
                </div>
                
                <div class="form-group">
                    <label>CNH *</label>
                    <input type="text" name="cnh" id="cnh" required placeholder="Número da CNH">
                </div>
                
                <div class="form-group">
                    <label>Categoria da CNH *</label>
                    <select name="categoria_cnh" id="categoria_cnh" required>
                        <option value="">Selecione...</option>
                        <option value="A">A - Moto</option>
                        <option value="B">B - Carro</option>
                        <option value="C">C - Caminhão</option>
                        <option value="D">D - Ônibus</option>
                        <option value="E">E - Carreta</option>
                        <option value="AB">AB - Moto e Carro</option>
                        <option value="AC">AC - Moto e Caminhão</option>
                        <option value="AD">AD - Moto e Ônibus</option>
                        <option value="AE">AE - Moto e Carreta</option>
                    </select>
                </div>
                
                <div class="form-group">
                    <label>Telefone</label>
                    <input type="tel" name="telefone" id="telefone" placeholder="(00) 00000-0000">
                </div>
                
                <div class="form-group">
                    <label>Email</label>
                    <input type="email" name="email" id="email" placeholder="email@exemplo.com">
                </div>
                
                <div class="form-actions" style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 20px;">
                    <button type="button" class="btn btn-outline" onclick="fecharModal()">Cancelar</button>
                    <button type="submit" class="btn btn-primary">Salvar</button>
                </div>
            </form>
        </div>
    </div>
    
    <script>
        function abrirModal() {
            document.getElementById('modalTitle').textContent = 'Novo Motorista';
            document.getElementById('motoristaId').value = '';
            document.getElementById('motoristaForm').reset();
            document.getElementById('motoristaModal').classList.add('active');
        }
        
        function fecharModal() {
            document.getElementById('motoristaModal').classList.remove('active');
        }
        
        function editarMotorista(id) {
            fetch(`<?= API_URL ?>/v1/motoristas/${id}`, {
                headers: { 'Authorization': 'Bearer <?= $_SESSION['token'] ?? '' ?>' }
            })
            .then(r => r.json())
            .then(m => {
                document.getElementById('modalTitle').textContent = 'Editar Motorista';
                document.getElementById('motoristaId').value = m.id;
                document.getElementById('nome').value = m.nome || '';
                document.getElementById('cpf').value = m.cpf || '';
                document.getElementById('cnh').value = m.cnh || '';
                document.getElementById('categoria_cnh').value = m.categoriaCnh || '';
                document.getElementById('telefone').value = m.telefone || '';
                document.getElementById('email').value = m.email || '';
                document.getElementById('motoristaModal').classList.add('active');
            });
        }
        
        function verDetalhes(id) {
            window.location.href = `motorista-detalhes.php?id=${id}`;
        }
        
        function filtrarMotoristas() {
            const searchTerm = document.getElementById('searchInput').value.toLowerCase();
            const statusFilter = document.getElementById('statusFilter').value;
            
            document.querySelectorAll('.motorista-card').forEach(card => {
                const nome = card.dataset.nome || '';
                const cpf = card.dataset.cpf || '';
                const cnh = card.dataset.cnh || '';
                const ativo = card.dataset.ativo;
                
                let show = true;
                
                if (searchTerm && !nome.includes(searchTerm) && !cpf.includes(searchTerm) && !cnh.includes(searchTerm)) {
                    show = false;
                }
                
                if (statusFilter !== 'todos' && ativo !== statusFilter) {
                    show = false;
                }
                
                card.style.display = show ? 'block' : 'none';
            });
        }
        
        // Máscara para CPF
        document.getElementById('cpf').addEventListener('input', function(e) {
            let value = e.target.value.replace(/\D/g, '');
            if (value.length > 11) value = value.slice(0, 11);
            
            if (value.length > 9) {
                value = value.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
            } else if (value.length > 6) {
                value = value.replace(/(\d{3})(\d{3})(\d{3})/, '$1.$2.$3');
            } else if (value.length > 3) {
                value = value.replace(/(\d{3})(\d{3})/, '$1.$2');
            }
            
            e.target.value = value;
        });
        
        // Fechar modal ao clicar fora
        document.getElementById('motoristaModal').addEventListener('click', function(e) {
            if (e.target === this) {
                fecharModal();
            }
        });
    </script>
</body>
</html>