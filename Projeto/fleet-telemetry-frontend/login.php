<?php
require_once 'config.php';

$error = '';
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $email = $_POST['email'] ?? '';
    $senha = $_POST['senha'] ?? '';
    
    // Simulação de autenticação
    if ($email === 'gestor@frota.com' && $senha === '123456') {
        $_SESSION['usuario'] = [
            'id' => 1,
            'nome' => 'Carlos Gestor',
            'email' => $email,
            'perfil' => PERFIL_GESTOR,
            'avatar' => 'CG'
        ];
        header('Location: pages/dashboard.php');
        exit;
    } elseif ($email === 'motorista@frota.com' && $senha === '123456') {
        $_SESSION['usuario'] = [
            'id' => 2,
            'nome' => 'João Motorista',
            'email' => $email,
            'perfil' => PERFIL_MOTORISTA,
            'veiculo_id' => 101,
            'avatar' => 'JM'
        ];
        header('Location: pages/motorista-dashboard.php');
        exit;
    } else {
        $error = 'Email ou senha inválidos';
    }
}
?>
<!DOCTYPE html>
<html lang="pt-br">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Login | <?= APP_NAME ?></title>
    <link rel="stylesheet" href="style.css">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: 'Segoe UI', sans-serif;
            background: linear-gradient(135deg, #0f2027 0%, #203a43 50%, #2c5364 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .login-container {
            width: 100%;
            max-width: 420px;
            padding: 20px;
        }
        .login-card {
            background: white;
            border-radius: 20px;
            padding: 40px;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
        }
        .logo {
            text-align: center;
            margin-bottom: 30px;
        }
        .logo-icon { font-size: 48px; margin-bottom: 10px; }
        .logo h1 { font-size: 24px; color: #1a1a2e; }
        .logo p { color: #666; font-size: 14px; }
        .form-group { margin-bottom: 20px; }
        .form-group label {
            display: block;
            margin-bottom: 8px;
            font-weight: 600;
            color: #333;
        }
        .input-wrapper { position: relative; }
        .input-icon {
            position: absolute;
            left: 15px;
            top: 50%;
            transform: translateY(-50%);
            font-size: 18px;
            color: #999;
        }
        .input-wrapper input {
            width: 100%;
            padding: 14px 15px 14px 50px;
            border: 2px solid #e0e0e0;
            border-radius: 10px;
            font-size: 15px;
            transition: all 0.3s;
        }
        .input-wrapper input:focus {
            outline: none;
            border-color: #2c5364;
            box-shadow: 0 0 0 4px rgba(44, 83, 100, 0.1);
        }
        .btn-login {
            width: 100%;
            padding: 15px;
            background: linear-gradient(135deg, #0f2027, #2c5364);
            color: white;
            border: none;
            border-radius: 10px;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s;
        }
        .btn-login:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 20px rgba(0,0,0,0.3);
        }
        .error-message {
            background: #fee;
            color: #c00;
            padding: 12px;
            border-radius: 8px;
            margin-bottom: 20px;
            border-left: 4px solid #c00;
        }
        .demo-credentials {
            margin-top: 20px;
            padding: 15px;
            background: #f5f5f5;
            border-radius: 10px;
            font-size: 13px;
        }
        .demo-title { font-weight: 600; margin-bottom: 10px; }
        .demo-item { display: flex; justify-content: space-between; padding: 3px 0; }
        .demo-item code {
            background: #e0e0e0;
            padding: 2px 8px;
            border-radius: 4px;
        }
    </style>
</head>
<body>
    <div class="login-container">
        <div class="login-card">
            <div class="logo">
                <div class="logo-icon">🚛</div>
                <h1><?= APP_NAME ?></h1>
                <p>Gestão Inteligente de Frotas</p>
            </div>
            
            <?php if ($error): ?>
                <div class="error-message">❌ <?= $error ?></div>
            <?php endif; ?>
            
            <form method="POST">
                <div class="form-group">
                    <label>Email</label>
                    <div class="input-wrapper">
                        <span class="input-icon">📧</span>
                        <input type="email" name="email" placeholder="seu@email.com" required>
                    </div>
                </div>
                <div class="form-group">
                    <label>Senha</label>
                    <div class="input-wrapper">
                        <span class="input-icon">🔒</span>
                        <input type="password" name="senha" placeholder="••••••••" required>
                    </div>
                </div>
                <button type="submit" class="btn-login">🚀 Entrar na Plataforma</button>
            </form>
            
            <div class="demo-credentials">
                <div class="demo-title">🔧 Credenciais de Teste</div>
                <div class="demo-item"><span>Gestor:</span> <code>gestor@frota.com / 123456</code></div>
                <div class="demo-item"><span>Motorista:</span> <code>motorista@frota.com / 123456</code></div>
            </div>
        </div>
    </div>
</body>
</html>