<?php
require_once 'config.php';

// Se já estiver logado, redireciona
if (isset($_SESSION['usuario']) && isset($_SESSION['token'])) {
    $redirect = $_SESSION['usuario']['perfil'] === PERFIL_GESTOR ? 'pages/dashboard.php' : 'pages/motorista-dashboard.php';
    header('Location: ' . $redirect);
    exit;
}

$error = '';
$success = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $login = $_POST['email'] ?? '';
    $senha = $_POST['senha'] ?? '';
    $lembrar = isset($_POST['lembrar']);
    
    if (empty($login) || empty($senha)) {
        $error = 'Por favor, preencha todos os campos.';
    } else {
        // Chamar API Java para autenticação
        $loginUrl = API_URL . '/auth/login';
        
        $dados = [
            'login' => $login,
            'senha' => $senha
        ];
        
        $options = [
            'http' => [
                'header'  => "Content-Type: application/json\r\n",
                'method'  => 'POST',
                'content' => json_encode($dados),
                'timeout' => 30.0,
                'ignore_errors' => true
            ]
        ];
        
        $context = stream_context_create($options);
        $resultado = @file_get_contents($loginUrl, false, $context);
        
        $statusCode = 500;
        if (isset($http_response_header)) {
            foreach ($http_response_header as $header) {
                if (preg_match('#HTTP/\d+\.\d+ (\d+)#', $header, $matches)) {
                    $statusCode = intval($matches[1]);
                }
            }
        }
        
        if ($resultado === false) {
            $error = 'Serviço de autenticação indisponível. Tente novamente mais tarde.';
            error_log("[Login] Erro ao conectar à API: " . $loginUrl);
        } else {
            $resposta = json_decode($resultado, true);
            
            if ($statusCode === 200 && isset($resposta['accessToken'])) {
                $token = $resposta['accessToken'];
                $refreshToken = $resposta['refreshToken'] ?? '';
                
                $tokenParts = explode('.', $token);
                if (count($tokenParts) >= 2) {
                    $payload = json_decode(base64_decode(strtr($tokenParts[1], '-_', '+/')), true);
                    
                    // CORREÇÃO: Normalizar perfil para minúsculo
                    $perfil = PERFIL_GESTOR; // padrão 'gestor'
                    $roles = $payload['roles'] ?? $payload['authorities'] ?? [];
                    
                    // Mapear roles para perfis (case-insensitive)
                    foreach ($roles as $role) {
                        $roleUpper = strtoupper($role);
                        if (strpos($roleUpper, 'MOTORISTA') !== false) {
                            $perfil = PERFIL_MOTORISTA;
                            break;
                        } elseif (strpos($roleUpper, 'ADMIN') !== false) {
                            $perfil = PERFIL_ADMIN;
                            break;
                        } elseif (strpos($roleUpper, 'GESTOR') !== false) {
                            $perfil = PERFIL_GESTOR;
                            break;
                        }
                    }
                    
                    // CORREÇÃO: Garantir que o perfil está em minúsculo
                    $perfil = strtolower($perfil);
                    
                    $_SESSION['usuario'] = [
                        'id' => $payload['userId'] ?? $payload['sub'] ?? 0,
                        'nome' => $payload['nome'] ?? explode('@', $login)[0],
                        'email' => $login,
                        'login' => $payload['sub'] ?? $login,
                        'perfil' => $perfil,
                        'avatar' => getUserInitials($payload['nome'] ?? $login),
                        'roles' => $roles
                    ];
                    
                    $_SESSION['token'] = $token;
                    $_SESSION['refresh_token'] = $refreshToken;
                    $_SESSION['login_time'] = time();
                    
                    if ($lembrar) {
                        setcookie('remember_token', $refreshToken, time() + 30*24*3600, '/');
                    }
                    
                    // CORREÇÃO: Redirecionar baseado no perfil normalizado
                    if ($perfil === PERFIL_ADMIN || $perfil === PERFIL_GESTOR) {
                        $redirect = 'pages/dashboard.php';
                    } elseif ($perfil === PERFIL_MOTORISTA) {
                        $redirect = 'pages/motorista-dashboard.php';
                    } else {
                        $redirect = 'pages/dashboard.php';
                    }
                    
                    $redirectUrl = $_SESSION['redirect_url'] ?? null;
                    unset($_SESSION['redirect_url']);
                    
                    header('Location: ' . ($redirectUrl ?: $redirect));
                    exit;
                } else {
                    $error = 'Erro ao processar token de autenticação.';
                }
            } else {
                $errorMessages = [
                    401 => 'Email ou senha inválidos.',
                    403 => 'Conta bloqueada ou desativada.',
                    423 => 'Conta bloqueada por excesso de tentativas.'
                ];
                $error = $errorMessages[$statusCode] ?? ($resposta['message'] ?? 'Erro na autenticação.');
            }
        }
    }
}

// Verificar cookie de "lembrar"
if (!isset($_SESSION['usuario']) && isset($_COOKIE['remember_token'])) {
    $refreshToken = $_COOKIE['remember_token'];
    // TODO: implementar refresh token
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
        .password-toggle {
            position: absolute;
            right: 15px;
            top: 50%;
            transform: translateY(-50%);
            cursor: pointer;
            font-size: 18px;
            color: #999;
        }
        .form-options {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 25px;
        }
        .remember-me {
            display: flex;
            align-items: center;
            gap: 8px;
            cursor: pointer;
            font-size: 14px;
            color: #333;
        }
        .remember-me input {
            width: 18px;
            height: 18px;
            cursor: pointer;
            accent-color: #2c5364;
        }
        .forgot-password {
            color: #2c5364;
            text-decoration: none;
            font-size: 14px;
            font-weight: 500;
        }
        .forgot-password:hover {
            text-decoration: underline;
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
        .success-message {
            background: #d5f5e3;
            color: #1e8449;
            padding: 12px;
            border-radius: 8px;
            margin-bottom: 20px;
            border-left: 4px solid #27ae60;
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
        .footer {
            text-align: center;
            margin-top: 20px;
            color: rgba(255,255,255,0.8);
            font-size: 13px;
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
                <div class="error-message">❌ <?= htmlspecialchars($error) ?></div>
            <?php endif; ?>
            
            <?php if ($success): ?>
                <div class="success-message">✅ <?= htmlspecialchars($success) ?></div>
            <?php endif; ?>
            
            <form method="POST">
                <div class="form-group">
                    <label>Email / Login</label>
                    <div class="input-wrapper">
                        <span class="input-icon">📧</span>
                        <input type="text" name="email" placeholder="seu@email.com" value="<?= htmlspecialchars($_POST['email'] ?? '') ?>" required>
                    </div>
                </div>
                <div class="form-group">
                    <label>Senha</label>
                    <div class="input-wrapper">
                        <span class="input-icon">🔒</span>
                        <input type="password" name="senha" id="senha" placeholder="••••••••" required>
                        <span class="password-toggle" onclick="togglePassword()">👁️</span>
                    </div>
                </div>
                
                <div class="form-options">
                    <label class="remember-me">
                        <input type="checkbox" name="lembrar" <?= isset($_POST['lembrar']) ? 'checked' : '' ?>>
                        <span>Lembrar-me</span>
                    </label>
                    <a href="recuperar-senha.php" class="forgot-password">Esqueceu a senha?</a>
                </div>
                
                <button type="submit" class="btn-login">🚀 Entrar na Plataforma</button>
            </form>
            
            <?php if (DEBUG_MODE): ?>
            <div class="demo-credentials">
                <div class="demo-title">🔧 Credenciais de Teste (Debug Mode)</div>
                <div class="demo-item"><span>Gestor:</span> <code>gestor@frota.com / 123456</code></div>
                <div class="demo-item"><span>Motorista:</span> <code>motorista@frota.com / 123456</code></div>
                <div class="demo-item"><span>Admin:</span> <code>admin@telemetria.com / admin123</code></div>
            </div>
            <?php endif; ?>
        </div>
        
        <div class="footer">
            &copy; <?= date('Y') ?> <?= APP_NAME ?> v<?= APP_VERSION ?>
        </div>
    </div>
    
    <script>
        function togglePassword() {
            const input = document.getElementById('senha');
            const toggle = document.querySelector('.password-toggle');
            
            if (input.type === 'password') {
                input.type = 'text';
                toggle.textContent = '🙈';
            } else {
                input.type = 'password';
                toggle.textContent = '👁️';
            }
        }
    </script>
</body>
</html>