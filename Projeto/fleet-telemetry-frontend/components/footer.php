<?php
require_once 'config.php';
?>
<footer style="background: #2c5364; color: white; padding: 1rem; text-align: center; margin-top: auto;">
    <div style="max-width: 1200px; margin: 0 auto;">
        <p>&copy; <?= date('Y') ?> <?= APP_NAME ?> v<?= APP_VERSION ?></p>
        <p style="font-size: 0.9rem; opacity: 0.8;">
            Gestão Inteligente de Frotas | 
            <a href="pages/ajuda.php" style="color: white;">Ajuda</a> | 
            <a href="#" style="color: white;">Suporte</a>
        </p>
    </div>
</footer>

<script src="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/js/all.min.js"></script>
<script>
// Auto-refresh para páginas de monitoramento
function autoRefresh() {
    const refreshRate = 30000; // 30 segundos
    setInterval(() => {
        if (document.visibilityState === 'visible') {
            location.reload();
        }
    }, refreshRate);
}

// Verificar sessão periodicamente
function checkSession() {
    setInterval(() => {
        fetch('../api/telemetria.php?action=check_session')
        .then(response => response.json())
        .then(data => {
            if (!data.active) {
                window.location.href = '../login.php';
            }
        })
        .catch(() => {
            // Ignorar erros de rede
        });
    }, 60000); // 1 minuto
}

// Inicializar se estiver em página de dashboard
if (window.location.pathname.includes('dashboard')) {
    autoRefresh();
    checkSession();
}
</script>
</body>
</html>