<?php
session_start();

define('APP_NAME', 'FleetTelemetry');
define('APP_VERSION', '1.0.0');
define('DEBUG_MODE', getenv('PHP_DISPLAY_ERRORS') === 'On' || false);

define('API_URL', getenv('API_URL') ?: 'http://localhost:9050/api/v1');
define('PUBLIC_API_URL', getenv('PUBLIC_API_URL') ?: 'http://localhost:9050/api/v1');
define('PUBLIC_WS_URL', getenv('PUBLIC_WS_URL') ?: 'ws://localhost:9050/ws');

define('PERFIL_MOTORISTA', 'MOTORISTA');
define('PERFIL_OPERADOR', 'OPERADOR');
define('PERFIL_GESTOR', 'GESTOR');
define('PERFIL_ADMIN', 'ADMIN');
define('PERFIL_SUPER_ADMIN', 'SUPER_ADMIN');

date_default_timezone_set('America/Sao_Paulo');

function getUserInitials($name) {
    $words = explode(' ', trim($name));
    $initials = '';
    foreach ($words as $word) {
        if (!empty($word)) $initials .= strtoupper(substr($word, 0, 1));
    }
    return substr($initials, 0, 2);
}

function fazerLogin($login, $senha) {
    $url = API_URL . '/auth/login';  // ✅ Java endpoint
    
    $data = json_encode([
        'login' => $login,  // ✅ Java AuthRequest.getLogin()
        'senha' => $senha
    ]);
    
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $data);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, [
        'Content-Type: application/json'
    ]);
    curl_setopt($ch, CURLOPT_TIMEOUT, 30);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    
    curl_close($ch);
    
    if (DEBUG_MODE) {
        error_log("[Login] $url → $httpCode");
    }
    
    return ($httpCode == 200) ? json_decode($response, true) : null;
}

function testarApiConnection() {
    $ch = curl_init(API_URL . '/actuator/health');
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_TIMEOUT, 5);
    $response = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    return $httpCode == 200;
}
?>