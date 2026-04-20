<?php
require_once 'config.php';

// Se já estiver logado, redireciona para dashboard apropriado
if (isset($_SESSION['usuario']) && isset($_SESSION['token'])) {
    $perfil = $_SESSION['usuario']['perfil'];
    
    if ($perfil === PERFIL_MOTORISTA) {
        header('Location: pages/motorista-dashboard.php');
    } else {
        header('Location: pages/dashboard.php');
    }
    exit;
}

// Se não estiver logado, redireciona para login
header('Location: login.php');
exit;
?>