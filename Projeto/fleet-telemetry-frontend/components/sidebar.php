<?php
// =====================================================================
// components/sidebar.php - Menu Lateral do Sistema
// =====================================================================

// Garantir que a sessão está iniciada
if (session_status() === PHP_SESSION_NONE) {
    session_start();
}

// Constantes de perfil (devem estar definidas no config.php)
if (!defined('PERFIL_GESTOR')) define('PERFIL_GESTOR', 'gestor');
if (!defined('PERFIL_MOTORISTA')) define('PERFIL_MOTORISTA', 'motorista');
if (!defined('APP_NAME')) define('APP_NAME', 'Telemetria Pro');

// Verificar se usuário está logado
if (!isset($_SESSION['usuario'])) {
    // Redirecionar para login se não estiver logado
    header('Location: /login.php');
    exit;
}

$perfil = $_SESSION['usuario']['perfil'] ?? PERFIL_GESTOR;
$current_page = basename($_SERVER['PHP_SELF'], '.php');

// Verificar se a função isActive NÃO existe antes de declarar
if (!function_exists('isActive')) {
    function isActive($pageName) {
        global $current_page;
        return $current_page === $pageName ? 'active' : '';
    }
}

// Função para obter iniciais do usuário
if (!function_exists('getUserInitials')) {
    function getUserInitials($nome) {
        if (empty($nome)) return 'U';
        $parts = explode(' ', trim($nome));
        if (count($parts) >= 2) {
            return strtoupper(substr($parts[0], 0, 1) . substr($parts[1], 0, 1));
        }
        return strtoupper(substr($nome, 0, 2));
    }
}

// Contagem de alertas (pode vir da API ou banco)
$alertasCount = 0;
try {
    // Tentar buscar contagem de alertas da API
    if (function_exists('apiRequest')) {
        $alertas = apiRequest('/alertas?limit=1');
        if (isset($alertas['total'])) {
            $alertasCount = $alertas['total'];
        }
    }
} catch (Exception $e) {
    $alertasCount = 3; // Fallback
}
?>

<style>
.sidebar {
    width: 280px;
    background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
    color: #ffffff;
    display: flex;
    flex-direction: column;
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 1000;
    overflow-y: auto;
    transition: transform 0.3s ease;
}

.sidebar::-webkit-scrollbar {
    width: 5px;
}

.sidebar::-webkit-scrollbar-track {
    background: rgba(255,255,255,0.1);
}

.sidebar::-webkit-scrollbar-thumb {
    background: #667eea;
    border-radius: 5px;
}

/* Logo */
.logo {
    padding: 25px 20px;
    text-align: center;
    border-bottom: 1px solid rgba(255,255,255,0.1);
}

.logo h2 {
    font-size: 1.4rem;
    margin-bottom: 5px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
}

.logo h2 span:first-child {
    font-size: 1.8rem;
}

.logo p {
    font-size: 0.75rem;
    opacity: 0.7;
    margin: 0;
}

/* Perfil do Usuário */
.user-profile {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 20px;
    background: rgba(255,255,255,0.05);
    margin: 15px;
    border-radius: 12px;
}

.user-avatar {
    width: 45px;
    height: 45px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: bold;
    font-size: 1.1rem;
}

.user-info h4 {
    font-size: 0.9rem;
    margin-bottom: 3px;
}

.user-info small {
    font-size: 0.7rem;
    opacity: 0.7;
}

/* Menu de Navegação */
.nav-menu {
    flex: 1;
    padding: 0 15px 20px 15px;
}

.nav-section {
    margin-bottom: 20px;
}

.nav-section-title {
    font-size: 0.7rem;
    text-transform: uppercase;
    letter-spacing: 1px;
    color: rgba(255,255,255,0.5);
    padding: 10px 12px 5px 12px;
}

.nav-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 12px;
    margin: 3px 0;
    border-radius: 10px;
    color: rgba(255,255,255,0.8);
    text-decoration: none;
    transition: all 0.3s ease;
    position: relative;
}

.nav-item:hover {
    background: rgba(255,255,255,0.1);
    color: white;
}

.nav-item.active {
    background: linear-gradient(135deg, #667eea, #764ba2);
    color: white;
    box-shadow: 0 4px 12px rgba(102,126,234,0.3);
}

.nav-icon {
    font-size: 1.2rem;
    width: 24px;
    text-align: center;
}

.nav-label {
    flex: 1;
    font-size: 0.85rem;
}

.nav-badge {
    font-size: 0.7rem;
    padding: 2px 6px;
    border-radius: 20px;
    background: rgba(255,255,255,0.2);
}

.nav-badge.live {
    background: #ef4444;
    animation: pulse 1.5s infinite;
}

.nav-badge.danger {
    background: #ef4444;
}

@keyframes pulse {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.5; }
}

/* Responsivo */
@media (max-width: 768px) {
    .sidebar {
        transform: translateX(-100%);
    }
    
    .sidebar.open {
        transform: translateX(0);
    }
}

/* Overlay para mobile */
.sidebar-overlay {
    display: none;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0,0,0,0.5);
    z-index: 999;
}

.sidebar-overlay.active {
    display: block;
}

@media (max-width: 768px) {
    .sidebar-overlay.active {
        display: block;
    }
}
</style>

<div class="sidebar" id="mainSidebar">
    <div class="logo">
        <h2>
            <span>🚛</span>
            <span><?= APP_NAME ?></span>
        </h2>
        <p>Gestão de Frotas</p>
    </div>
    
    <div class="user-profile">
        <div class="user-avatar"><?= getUserInitials($_SESSION['usuario']['nome'] ?? 'Usuário') ?></div>
        <div class="user-info">
            <h4><?= htmlspecialchars($_SESSION['usuario']['nome'] ?? 'Usuário') ?></h4>
            <small><?= $perfil === PERFIL_GESTOR ? 'Gestor' : 'Motorista' ?></small>
        </div>
    </div>
    
    <div class="nav-menu">
        <?php if ($perfil === PERFIL_GESTOR): ?>
        <!-- Menu do Gestor -->
        <div class="nav-section">
            <div class="nav-section-title">Principal</div>
            <a href="dashboard.php" class="nav-item <?= isActive('dashboard') ?>">
                <span class="nav-icon">📊</span>
                <span class="nav-label">Dashboard</span>
            </a>
            <a href="monitoramento.php" class="nav-item <?= isActive('monitoramento') ?>">
                <span class="nav-icon">🗺️</span>
                <span class="nav-label">Monitoramento</span>
                <span class="nav-badge live">LIVE</span>
            </a>
        </div>
        
        <div class="nav-section">
            <div class="nav-section-title">Gestão de Frotas</div>
            <a href="veiculos.php" class="nav-item <?= isActive('veiculos') ?>">
                <span class="nav-icon">🚚</span>
                <span class="nav-label">Veículos</span>
            </a>
            <a href="motoristas.php" class="nav-item <?= isActive('motoristas') ?>">
                <span class="nav-icon">👤</span>
                <span class="nav-label">Motoristas</span>
            </a>
            <a href="rotas.php" class="nav-item <?= isActive('rotas') ?>">
                <span class="nav-icon">📍</span>
                <span class="nav-label">Rotas</span>
            </a>
            <a href="entregas.php" class="nav-item <?= isActive('entregas') ?>">
                <span class="nav-icon">📦</span>
                <span class="nav-label">Entregas</span>
            </a>
        </div>
        
        <div class="nav-section">
            <div class="nav-section-title">Monitoramento</div>
            <a href="alertas.php" class="nav-item <?= isActive('alertas') ?>">
                <span class="nav-icon">⚠️</span>
                <span class="nav-label">Alertas</span>
                <?php if ($alertasCount > 0): ?>
                <span class="nav-badge danger"><?= $alertasCount ?></span>
                <?php endif; ?>
            </a>
            <a href="desvios.php" class="nav-item <?= isActive('desvios') ?>">
                <span class="nav-icon">🔄</span>
                <span class="nav-label">Desvios</span>
            </a>
            <a href="geofences.php" class="nav-item <?= isActive('geofences') ?>">
                <span class="nav-icon">🚧</span>
                <span class="nav-label">Geofences</span>
            </a>
        </div>
        
        <div class="nav-section">
            <div class="nav-section-title">Análise</div>
            <a href="scores.php" class="nav-item <?= isActive('scores') ?>">
                <span class="nav-icon">⭐</span>
                <span class="nav-label">Score Motoristas</span>
            </a>
            <a href="manutencao.php" class="nav-item <?= isActive('manutencao') ?>">
                <span class="nav-icon">🔧</span>
                <span class="nav-label">Manutenção</span>
            </a>
            <a href="relatorios.php" class="nav-item <?= isActive('relatorios') ?>">
                <span class="nav-icon">📄</span>
                <span class="nav-label">Relatórios</span>
            </a>
            <a href="cache-admin.php" class="nav-item <?= isActive('cache-admin') ?>">
                <span class="nav-icon">💾</span>
                <span class="nav-label">Cache Admin</span>
            </a>
            <a href="backpressure.php" class="nav-item <?= isActive('backpressure') ?>">
                <span class="nav-icon">📊</span>
                <span class="nav-label">Backpressure</span>
            </a>
        </div>
        
        <?php else: ?>
        <!-- Menu do Motorista -->
        <div class="nav-section">
            <div class="nav-section-title">Principal</div>
            <a href="motorista-dashboard.php" class="nav-item <?= isActive('motorista-dashboard') ?>">
                <span class="nav-icon">📊</span>
                <span class="nav-label">Minha Jornada</span>
            </a>
            <a href="minha-rota.php" class="nav-item">
                <span class="nav-icon">🗺️</span>
                <span class="nav-label">Rota Atual</span>
            </a>
            <a href="minhas-entregas.php" class="nav-item">
                <span class="nav-icon">📦</span>
                <span class="nav-label">Entregas</span>
            </a>
        </div>
        
        <div class="nav-section">
            <div class="nav-section-title">Desempenho</div>
            <a href="meu-score.php" class="nav-item">
                <span class="nav-icon">⭐</span>
                <span class="nav-label">Meu Score</span>
            </a>
            <a href="meu-historico.php" class="nav-item">
                <span class="nav-icon">📋</span>
                <span class="nav-label">Histórico</span>
            </a>
        </div>
        <?php endif; ?>
        
        <div class="nav-section">
            <div class="nav-section-title">Sistema</div>
            <a href="rastreio-publico.php" class="nav-item <?= isActive('rastreio-publico') ?>">
                <span class="nav-icon">🌍</span>
                <span class="nav-label">Rastreio Público</span>
            </a>
            <a href="eta.php" class="nav-item <?= isActive('eta') ?>">
                <span class="nav-icon">⏱️</span>
                <span class="nav-label">ETA</span>
            </a>
            <a href="telemetria.php" class="nav-item <?= isActive('telemetria') ?>">
                <span class="nav-icon">📡</span>
                <span class="nav-label">Telemetria</span>
            </a>
            <a href="configuracoes.php" class="nav-item <?= isActive('configuracoes') ?>">
                <span class="nav-icon">⚙️</span>
                <span class="nav-label">Configurações</span>
            </a>
            <a href="ajuda.php" class="nav-item <?= isActive('ajuda') ?>">
                <span class="nav-icon">❓</span>
                <span class="nav-label">Ajuda</span>
            </a>
            <a href="../logout.php" class="nav-item">
                <span class="nav-icon">🚪</span>
                <span class="nav-label">Sair</span>
            </a>
        </div>
    </div>
</div>

<!-- Overlay para mobile -->
<div class="sidebar-overlay" id="sidebarOverlay"></div>

<script>
// Funções para controle do sidebar em mobile
function toggleSidebar() {
    const sidebar = document.getElementById('mainSidebar');
    const overlay = document.getElementById('sidebarOverlay');
    
    sidebar.classList.toggle('open');
    overlay.classList.toggle('active');
}

function closeSidebar() {
    const sidebar = document.getElementById('mainSidebar');
    const overlay = document.getElementById('sidebarOverlay');
    
    sidebar.classList.remove('open');
    overlay.classList.remove('active');
}

// Event listeners
document.addEventListener('DOMContentLoaded', function() {
    const overlay = document.getElementById('sidebarOverlay');
    if (overlay) {
        overlay.addEventListener('click', closeSidebar);
    }
});
</script>