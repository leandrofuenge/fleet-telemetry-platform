<?php
$perfil = $_SESSION['usuario']['perfil'] ?? PERFIL_GESTOR;
$current_page = basename($_SERVER['PHP_SELF'], '.php');
?>
<div class="sidebar">
    <div class="logo">
        <h2><span>🚛</span><span><?= APP_NAME ?></span></h2>
        <p>Gestão de Frotas</p>
    </div>
    
    <div class="user-profile">
        <div class="user-avatar"><?= getUserInitials($_SESSION['usuario']['nome']) ?></div>
        <div class="user-info">
            <h4><?= htmlspecialchars($_SESSION['usuario']['nome']) ?></h4>
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
                <?php $alertasCount = 3; if ($alertasCount > 0): ?>
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
            <a href="configuracoes.php" class="nav-item">
                <span class="nav-icon">⚙️</span>
                <span class="nav-label">Configurações</span>
            </a>
            <a href="../logout.php" class="nav-item">
                <span class="nav-icon">🚪</span>
                <span class="nav-label">Sair</span>
            </a>
        </div>
    </div>
</div>