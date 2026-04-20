<?php
require_once 'config.php';

// Limpar sessão
session_destroy();

// Limpar cookie de remember
if (isset($_COOKIE['remember_token'])) {
    setcookie('remember_token', '', time() - 3600, '/');
}

// Redirecionar para login
header('Location: login.php');
exit;
?>