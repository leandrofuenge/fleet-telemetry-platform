<?php
session_start();

// ============================================================================
// CONFIGURAÇÕES DA PLATAFORMA
// ============================================================================
define('APP_NAME', 'FleetTelemetry');
define('APP_VERSION', '1.0.0');

// ============================================================================
// CONFIGURAÇÕES DE API - SOMENTE LOCALHOST
// ============================================================================
define('API_URL', 'http://localhost:9050/api/v1');
define('ROUTING_API_URL', 'http://localhost:8082/api');
define('PUBLIC_API_URL', 'http://localhost:9050/api/v1');
define('PUBLIC_WS_URL', 'ws://localhost:9050/ws');

// ============================================================================
// CONFIGURAÇÕES DO BANCO DE DADOS
// ============================================================================
define('DB_HOST', 'localhost');
define('DB_PORT', 3306);
define('DB_NAME', 'telemetria');
define('DB_USER', 'root');
define('DB_PASS', 'root123');

// ============================================================================
// CONFIGURAÇÕES DE DEBUG
// ============================================================================
define('DEBUG_MODE', true);

if (DEBUG_MODE) {
    error_reporting(E_ALL);
    ini_set('display_errors', 1);
}

// ============================================================================
// FUNÇÃO DE AUTENTICAÇÃO
// ============================================================================
function fazerLogin($email, $senha) {
    $url = API_URL . '/auth/login';
    
    $data = json_encode([
        'email' => $email,
        'senha' => $senha
    ]);
    
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $data);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Content-Type: application/json',
        'Content-Length: ' . strlen($data)
    ]);
    curl_setopt($ch, CURLOPT_TIMEOUT, 30);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 5);
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    $curlError = curl_error($ch);
    
    curl_close($ch);
    
    error_log("[Login] URL: $url");
    error_log("[Login] HTTP Code: $httpCode");
    if ($curlError) {
        error_log("[Login] CURL Error: $curlError");
    }
    
    if ($httpCode == 200 && $response) {
        return json_decode($response, true);
    }
    
    return null;
}

// ============================================================================
// FUNÇÃO PARA TESTAR CONEXÃO COM A API
// ============================================================================
function testarApiConnection() {
    $url = API_URL . '/actuator/health';
    
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 5);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 3);
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    
    curl_close($ch);
    
    return $httpCode == 200;
}

// ============================================================================
// CONFIGURAÇÕES DE TIMEZONE
// ============================================================================
date_default_timezone_set('America/Sao_Paulo');

// ============================================================================
// PERFIS DE USUÁRIO
// ============================================================================
define('PERFIL_GESTOR', 'gestor');
define('PERFIL_MOTORISTA', 'motorista');
define('PERFIL_CLIENTE', 'cliente');
define('PERFIL_ADMIN', 'admin');

// ============================================================================
// FUNÇÕES UTILITÁRIAS
// ============================================================================
function getUserInitials($name) {
    $words = explode(' ', trim($name));
    $initials = '';
    foreach ($words as $word) {
        if (!empty($word)) $initials .= strtoupper(substr($word, 0, 1));
    }
    return substr($initials, 0, 2);
}

// ============================================================================
// TESTAR CONEXÃO NA INICIALIZAÇÃO
// ============================================================================
if (DEBUG_MODE) {
    $apiOk = testarApiConnection();
    error_log("[FleetTelemetry] API Connection: " . ($apiOk ? "OK" : "FAILED - " . API_URL));
}
?>