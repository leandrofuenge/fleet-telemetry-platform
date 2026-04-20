<?php
session_start();

// ============================================================================
// CONFIGURAÇÕES DA PLATAFORMA
// ============================================================================
define('APP_NAME', 'FleetTelemetry');
define('APP_VERSION', '1.0.0');
define('APP_DESCRIPTION', 'Plataforma de Telemetria e Gestão de Frotas');

// ============================================================================
// CONFIGURAÇÕES DE API
// ============================================================================
define('API_URL', getenv('API_URL') ?: 'http://localhost:8081/api');
define('WS_URL', getenv('WS_URL') ?: 'ws://localhost:8081/ws');

// ============================================================================
// CONFIGURAÇÕES DE SESSÃO
// ============================================================================
if (!isset($_SESSION['usuario']) && basename($_SERVER['PHP_SELF']) != 'login.php') {
    header('Location: login.php');
    exit;
}

// Perfis de usuário
define('PERFIL_GESTOR', 'gestor');
define('PERFIL_MOTORISTA', 'motorista');
define('PERFIL_CLIENTE', 'cliente');

// ============================================================================
// FUNÇÕES UTILITÁRIAS
// ============================================================================
function isActive($page) {
    $current = basename($_SERVER['PHP_SELF'], '.php');
    return $current == $page ? 'active' : '';
}

function getUserInitials($name) {
    $words = explode(' ', trim($name));
    $initials = '';
    foreach ($words as $word) {
        if (!empty($word)) $initials .= strtoupper(substr($word, 0, 1));
    }
    return substr($initials, 0, 2);
}

function formatDateTime($date) {
    return date('d/m/Y H:i', strtotime($date));
}

function formatDuration($minutes) {
    $hours = floor($minutes / 60);
    $mins = $minutes % 60;
    return sprintf('%02d:%02d', $hours, $mins);
}

function getStatusBadge($status) {
    $badges = [
        'ativo' => ['label' => 'Ativo', 'class' => 'success'],
        'em_rota' => ['label' => 'Em Rota', 'class' => 'info'],
        'parado' => ['label' => 'Parado', 'class' => 'warning'],
        'entregue' => ['label' => 'Entregue', 'class' => 'success'],
        'atrasado' => ['label' => 'Atrasado', 'class' => 'danger'],
        'desviado' => ['label' => 'Desviado', 'class' => 'danger'],
    ];
    return $badges[$status] ?? ['label' => $status, 'class' => 'default'];
}

function getSeverityClass($severity) {
    return [
        'CRITICA' => 'danger',
        'ALTA' => 'warning',
        'MEDIA' => 'info',
        'BAIXA' => 'success'
    ][$severity] ?? 'default';
}
?>