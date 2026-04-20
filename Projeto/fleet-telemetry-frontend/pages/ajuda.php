<?php
require_once '../config.php';

// Verificar se usuário está logado
if (!isset($_SESSION['usuario']) || !isset($_SESSION['token'])) {
    header('Location: ../login.php');
    exit;
}

$pageTitle = 'Ajuda';
?>

<?php include '../components/header.php'; ?>

<div style="display: flex; min-height: 100vh;">
    <?php include '../components/sidebar.php'; ?>
    
    <main style="flex: 1; padding: 2rem; background: #f5f5f5;">
        <div style="max-width: 1000px; margin: 0 auto;">
            <h2 style="color: #2c5364; margin-bottom: 2rem;">
                <i class="fas fa-question-circle"></i> Central de Ajuda
            </h2>
            
            <!-- Busca -->
            <div style="background: white; padding: 1.5rem; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); margin-bottom: 2rem;">
                <input type="text" placeholder="Buscar ajuda..." style="width: 100%; padding: 1rem; border: 1px solid #ddd; border-radius: 8px; font-size: 1rem;">
            </div>
            
            <!-- Categorias de Ajuda -->
            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 1.5rem; margin-bottom: 2rem;">
                <div style="background: white; padding: 1.5rem; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                    <div style="display: flex; align-items: center; gap: 1rem; margin-bottom: 1rem;">
                        <div style="background: #4CAF50; color: white; width: 40px; height: 40px; border-radius: 50%; display: flex; align-items: center; justify-content: center;">
                            <i class="fas fa-route"></i>
                        </div>
                        <h3 style="margin: 0; color: #2c5364;">Rotas e Entregas</h3>
                    </div>
                    <ul style="list-style: none; padding: 0;">
                        <li style="padding: 0.5rem 0; border-bottom: 1px solid #f0f0f0;">
                            <a href="#" style="color: #333; text-decoration: none;">Como iniciar uma rota</a>
                        </li>
                        <li style="padding: 0.5rem 0; border-bottom: 1px solid #f0f0f0;">
                            <a href="#" style="color: #333; text-decoration: none;">Registrar entrega</a>
                        </li>
                        <li style="padding: 0.5rem 0;">
                            <a href="#" style="color: #333; text-decoration: none;">Problemas na rota</a>
                        </li>
                    </ul>
                </div>
                
                <div style="background: white; padding: 1.5rem; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                    <div style="display: flex; align-items: center; gap: 1rem; margin-bottom: 1rem;">
                        <div style="background: #2196F3; color: white; width: 40px; height: 40px; border-radius: 50%; display: flex; align-items: center; justify-content: center;">
                            <i class="fas fa-truck"></i>
                        </div>
                        <h3 style="margin: 0; color: #2c5364;">Veículos</h3>
                    </div>
                    <ul style="list-style: none; padding: 0;">
                        <li style="padding: 0.5rem 0; border-bottom: 1px solid #f0f0f0;">
                            <a href="#" style="color: #333; text-decoration: none;">Status do veículo</a>
                        </li>
                        <li style="padding: 0.5rem 0; border-bottom: 1px solid #f0f0f0;">
                            <a href="#" style="color: #333; text-decoration: none;">Manutenção</a>
                        </li>
                        <li style="padding: 0.5rem 0;">
                            <a href="#" style="color: #333; text-decoration: none;">Documentação</a>
                        </li>
                    </ul>
                </div>
                
                <div style="background: white; padding: 1.5rem; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                    <div style="display: flex; align-items: center; gap: 1rem; margin-bottom: 1rem;">
                        <div style="background: #FF9800; color: white; width: 40px; height: 40px; border-radius: 50%; display: flex; align-items: center; justify-content: center;">
                            <i class="fas fa-exclamation-triangle"></i>
                        </div>
                        <h3 style="margin: 0; color: #2c5364;">Alertas</h3>
                    </div>
                    <ul style="list-style: none; padding: 0;">
                        <li style="padding: 0.5rem 0; border-bottom: 1px solid #f0f0f0;">
                            <a href="#" style="color: #333; text-decoration: none;">Tipos de alertas</a>
                        </li>
                        <li style="padding: 0.5rem 0; border-bottom: 1px solid #f0f0f0;">
                            <a href="#" style="color: #333; text-decoration: none;">Ação recomendada</a>
                        </li>
                        <li style="padding: 0.5rem 0;">
                            <a href="#" style="color: #333; text-decoration: none;">Configurar notificações</a>
                        </li>
                    </ul>
                </div>
                
                <div style="background: white; padding: 1.5rem; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                    <div style="display: flex; align-items: center; gap: 1rem; margin-bottom: 1rem;">
                        <div style="background: #9C27B0; color: white; width: 40px; height: 40px; border-radius: 50%; display: flex; align-items: center; justify-content: center;">
                            <i class="fas fa-cog"></i>
                        </div>
                        <h3 style="margin: 0; color: #2c5364;">Configurações</h3>
                    </div>
                    <ul style="list-style: none; padding: 0;">
                        <li style="padding: 0.5rem 0; border-bottom: 1px solid #f0f0f0;">
                            <a href="#" style="color: #333; text-decoration: none;">Perfil do usuário</a>
                        </li>
                        <li style="padding: 0.5rem 0; border-bottom: 1px solid #f0f0f0;">
                            <a href="#" style="color: #333; text-decoration: none;">Preferências</a>
                        </li>
                        <li style="padding: 0.5rem 0;">
                            <a href="#" style="color: #333; text-decoration: none;">Segurança</a>
                        </li>
                    </ul>
                </div>
            </div>
            
            <!-- FAQs -->
            <div style="background: white; padding: 2rem; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1);">
                <h3 style="color: #2c5364; margin-bottom: 1.5rem;">
                    <i class="fas fa-question"></i> Perguntas Frequentes
                </h3>
                
                <div style="space-y: 1rem;">
                    <div style="border-bottom: 1px solid #f0f0f0; padding-bottom: 1rem;">
                        <h4 style="color: #333; margin-bottom: 0.5rem;">Como faço para registrar uma entrega?</h4>
                        <p style="color: #666; line-height: 1.6;">
                            Para registrar uma entrega, acesse o dashboard motorista, clique em "Entrega Realizada" 
                            no menu de ações rápidas. Você pode adicionar fotos e observações sobre a entrega.
                        </p>
                    </div>
                    
                    <div style="border-bottom: 1px solid #f0f0f0; padding-bottom: 1rem;">
                        <h4 style="color: #333; margin-bottom: 0.5rem;">O que fazer se o veículo apresentar problemas?</h4>
                        <p style="color: #666; line-height: 1.6;">
                            Use o botão "Reportar Problema" no dashboard. Descreva o problema em detalhes e, 
                            se necessário, contate a central de suporte imediatamente.
                        </p>
                    </div>
                    
                    <div style="border-bottom: 1px solid #f0f0f0; padding-bottom: 1rem;">
                        <h4 style="color: #333; margin-bottom: 0.5rem;">Como alterar meus dados pessoais?</h4>
                        <p style="color: #666; line-height: 1.6;">
                            Acesse o menu "Configurações" > "Perfil do Usuário". Lá você pode atualizar 
                            suas informações de contato e preferências.
                        </p>
                    </div>
                    
                    <div style="padding-bottom: 1rem;">
                        <h4 style="color: #333; margin-bottom: 0.5rem;">Qual o significado dos alertas?</h4>
                        <p style="color: #666; line-height: 1.6;">
                            Os alertas indicam situações que requerem atenção: velocidade excessiva, 
                            baixo combustível, necessidade de manutenção, entre outros.
                        </p>
                    </div>
                </div>
            </div>
            
            <!-- Contato -->
            <div style="background: #2c5364; color: white; padding: 2rem; border-radius: 10px; margin-top: 2rem; text-align: center;">
                <h3 style="margin-bottom: 1rem;">Precisa de mais ajuda?</h3>
                <p style="margin-bottom: 1.5rem;">Nossa equipe de suporte está disponível 24/7</p>
                <div style="display: flex; justify-content: center; gap: 2rem; flex-wrap: wrap;">
                    <div>
                        <i class="fas fa-phone"></i> (11) 4000-0000
                    </div>
                    <div>
                        <i class="fas fa-envelope"></i> suporte@fleettelemetry.com
                    </div>
                    <div>
                        <i class="fas fa-comments"></i> Chat Online
                    </div>
                </div>
            </div>
        </div>
    </main>
</div>

<?php include '../components/footer.php'; ?>