<?php
require_once '../config.php';

// Verificar se usuário está logado
if (!isset($_SESSION['usuario']) || !isset($_SESSION['token'])) {
    header('Location: ../login.php');
    exit;
}

// Verificar se é motorista
if ($_SESSION['usuario']['perfil'] !== PERFIL_MOTORISTA) {
    header('Location: dashboard.php');
    exit;
}

$pageTitle = 'Dashboard Motorista';
?>

<?php include '../components/header.php'; ?>

<div style="display: flex; min-height: 100vh;">
    <?php include '../components/sidebar.php'; ?>
    
    <main style="flex: 1; padding: 2rem; background: #f5f5f5;">
        <div style="max-width: 1200px; margin: 0 auto;">
            <h2 style="color: #2c5364; margin-bottom: 2rem;">
                <i class="fas fa-tachometer-alt"></i> Dashboard Motorista
            </h2>
            
            <!-- Cards de Informações -->
            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 1.5rem; margin-bottom: 2rem;">
                <div style="background: white; padding: 1.5rem; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                    <div style="display: flex; align-items: center; gap: 1rem;">
                        <div style="background: #4CAF50; color: white; width: 50px; height: 50px; border-radius: 50%; display: flex; align-items: center; justify-content: center;">
                            <i class="fas fa-route"></i>
                        </div>
                        <div>
                            <h3 style="margin: 0; color: #333;">Rota Atual</h3>
                            <p style="margin: 0; color: #666; font-size: 0.9rem;">Centro - Entregas</p>
                        </div>
                    </div>
                </div>
                
                <div style="background: white; padding: 1.5rem; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                    <div style="display: flex; align-items: center; gap: 1rem;">
                        <div style="background: #2196F3; color: white; width: 50px; height: 50px; border-radius: 50%; display: flex; align-items: center; justify-content: center;">
                            <i class="fas fa-clock"></i>
                        </div>
                        <div>
                            <h3 style="margin: 0; color: #333;">Tempo em Rota</h3>
                            <p style="margin: 0; color: #666; font-size: 0.9rem;">2h 35min</p>
                        </div>
                    </div>
                </div>
                
                <div style="background: white; padding: 1.5rem; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                    <div style="display: flex; align-items: center; gap: 1rem;">
                        <div style="background: #FF9800; color: white; width: 50px; height: 50px; border-radius: 50%; display: flex; align-items: center; justify-content: center;">
                            <i class="fas fa-box"></i>
                        </div>
                        <div>
                            <h3 style="margin: 0; color: #333;">Entregas Hoje</h3>
                            <p style="margin: 0; color: #666; font-size: 0.9rem;">8 de 12</p>
                        </div>
                    </div>
                </div>
            </div>
            
            <!-- Mapa e Status -->
            <div style="display: grid; grid-template-columns: 2fr 1fr; gap: 1.5rem; margin-bottom: 2rem;">
                <div style="background: white; padding: 1.5rem; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                    <h3 style="color: #2c5364; margin-bottom: 1rem;">
                        <i class="fas fa-map"></i> Minha Rota
                    </h3>
                    <div style="height: 400px; background: #e0e0e0; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: #666;">
                        <div style="text-align: center;">
                            <i class="fas fa-map-marked-alt" style="font-size: 3rem; margin-bottom: 1rem;"></i>
                            <p>Mapa em desenvolvimento</p>
                            <small>Integração com sistema de GPS</small>
                        </div>
                    </div>
                </div>
                
                <div style="background: white; padding: 1.5rem; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                    <h3 style="color: #2c5364; margin-bottom: 1rem;">
                        <i class="fas fa-info-circle"></i> Status da Viagem
                    </h3>
                    <div style="space-y: 1rem;">
                        <div style="padding: 1rem; background: #f0f8ff; border-radius: 8px; margin-bottom: 1rem;">
                            <div style="display: flex; justify-content: space-between; align-items: center;">
                                <span style="color: #333;">Status</span>
                                <span style="background: #4CAF50; color: white; padding: 0.25rem 0.75rem; border-radius: 15px; font-size: 0.8rem;">Em Andamento</span>
                            </div>
                        </div>
                        
                        <div style="padding: 1rem; background: #f9f9f9; border-radius: 8px; margin-bottom: 1rem;">
                            <div style="display: flex; justify-content: space-between; align-items: center;">
                                <span style="color: #333;">Próxima Entrega</span>
                                <span style="color: #666;">Rua A, 123</span>
                            </div>
                        </div>
                        
                        <div style="padding: 1rem; background: #f9f9f9; border-radius: 8px; margin-bottom: 1rem;">
                            <div style="display: flex; justify-content: space-between; align-items: center;">
                                <span style="color: #333;">ETA</span>
                                <span style="color: #666;">15 min</span>
                            </div>
                        </div>
                        
                        <button style="width: 100%; background: #2c5364; color: white; border: none; padding: 0.75rem; border-radius: 8px; cursor: pointer;">
                            <i class="fas fa-phone"></i> Contatar Central
                        </button>
                    </div>
                </div>
            </div>
            
            <!-- Ações Rápidas -->
            <div style="background: white; padding: 1.5rem; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                <h3 style="color: #2c5364; margin-bottom: 1rem;">
                    <i class="fas fa-bolt"></i> Ações Rápidas
                </h3>
                <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem;">
                    <button style="background: #4CAF50; color: white; border: none; padding: 1rem; border-radius: 8px; cursor: pointer;">
                        <i class="fas fa-check"></i> Entrega Realizada
                    </button>
                    <button style="background: #FF9800; color: white; border: none; padding: 1rem; border-radius: 8px; cursor: pointer;">
                        <i class="fas fa-exclamation-triangle"></i> Reportar Problema
                    </button>
                    <button style="background: #2196F3; color: white; border: none; padding: 1rem; border-radius: 8px; cursor: pointer;">
                        <i class="fas fa-camera"></i> Foto da Entrega
                    </button>
                    <button style="background: #9C27B0; color: white; border: none; padding: 1rem; border-radius: 8px; cursor: pointer;">
                        <i class="fas fa-comment"></i> Enviar Mensagem
                    </button>
                </div>
            </div>
        </div>
    </main>
</div>

<?php include '../components/footer.php'; ?>