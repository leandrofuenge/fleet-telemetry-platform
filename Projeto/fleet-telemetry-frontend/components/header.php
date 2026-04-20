<?php
require_once 'config.php';
?>
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><?= $pageTitle ?? 'Dashboard' ?> | <?= APP_NAME ?></title>
    <link rel="stylesheet" href="../style.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
    <style>
        .header {
            background: linear-gradient(135deg, #0f2027, #203a43, #2c5364);
            color: white;
            padding: 1rem 2rem;
            display: flex;
            justify-content: space-between;
            align-items: center;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        .header-left {
            display: flex;
            align-items: center;
            gap: 1rem;
        }
        .header-left h1 {
            font-size: 1.5rem;
            margin: 0;
        }
        .header-left .logo {
            font-size: 2rem;
        }
        .header-right {
            display: flex;
            align-items: center;
            gap: 1.5rem;
        }
        .user-info {
            display: flex;
            align-items: center;
            gap: 0.75rem;
            background: rgba(255,255,255,0.1);
            padding: 0.5rem 1rem;
            border-radius: 25px;
        }
        .user-avatar {
            width: 35px;
            height: 35px;
            border-radius: 50%;
            background: #4CAF50;
            display: flex;
            align-items: center;
            justify-content: center;
            font-weight: bold;
        }
        .user-details {
            display: flex;
            flex-direction: column;
        }
        .user-name {
            font-weight: 600;
            font-size: 0.9rem;
        }
        .user-role {
            font-size: 0.8rem;
            opacity: 0.8;
        }
        .logout-btn {
            background: rgba(255,255,255,0.2);
            border: none;
            color: white;
            padding: 0.5rem 1rem;
            border-radius: 20px;
            cursor: pointer;
            transition: all 0.3s;
        }
        .logout-btn:hover {
            background: rgba(255,255,255,0.3);
        }
    </style>
</head>
<body>
    <header class="header">
        <div class="header-left">
            <div class="logo">??</div>
            <h1><?= APP_NAME ?></h1>
        </div>
        <div class="header-right">
            <?php if (isset($_SESSION['usuario'])): ?>
                <div class="user-info">
                    <div class="user-avatar"><?= $_SESSION['usuario']['avatar'] ?></div>
                    <div class="user-details">
                        <span class="user-name"><?= $_SESSION['usuario']['nome'] ?></span>
                        <span class="user-role"><?= $_SESSION['usuario']['perfil'] ?></span>
                    </div>
                </div>
                <a href="../logout.php" class="logout-btn">
                    <i class="fas fa-sign-out-alt"></i> Sair
                </a>
            <?php endif; ?>
        </div>
    </header>