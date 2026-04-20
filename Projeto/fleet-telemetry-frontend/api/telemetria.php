<?php
require_once '../config.php';

header('Content-Type: application/json');

// Verificar sessão para ações que requerem autenticação
function checkAuth() {
    if (!isset($_SESSION['usuario']) || !isset($_SESSION['token'])) {
        http_response_code(401);
        echo json_encode(['error' => 'Não autorizado']);
        exit;
    }
}

$action = $_GET['action'] ?? '';

switch ($action) {
    case 'check_session':
        // Verificar se sessão está ativa
        echo json_encode([
            'active' => isset($_SESSION['usuario']) && isset($_SESSION['token']),
            'user' => $_SESSION['usuario']['nome'] ?? null,
            'perfil' => $_SESSION['usuario']['perfil'] ?? null
        ]);
        break;
        
    case 'get_user_info':
        checkAuth();
        echo json_encode([
            'success' => true,
            'user' => $_SESSION['usuario']
        ]);
        break;
        
    case 'get_telemetria_data':
        checkAuth();
        
        // Simular dados de telemetria
        echo json_encode([
            'success' => true,
            'data' => [
                'veiculos' => [
                    [
                        'id' => 1,
                        'placa' => 'ABC-1234',
                        'modelo' => 'Ford Transit',
                        'status' => 'em_rota',
                        'localizacao' => ['lat' => -23.5505, 'lng' => -46.6333],
                        'motorista' => 'João Silva',
                        'velocidade' => 45,
                        'combustivel' => 75
                    ],
                    [
                        'id' => 2,
                        'placa' => 'DEF-5678',
                        'modelo' => 'Volkswagen Delivery',
                        'status' => 'parado',
                        'localizacao' => ['lat' => -23.5605, 'lng' => -46.6433],
                        'motorista' => 'Maria Santos',
                        'velocidade' => 0,
                        'combustivel' => 60
                    ]
                ],
                'alertas' => [
                    [
                        'id' => 1,
                        'tipo' => 'velocidade',
                        'mensagem' => 'Veículo ABC-1234 acima do limite',
                        'gravidade' => 'media',
                        'timestamp' => date('Y-m-d H:i:s')
                    ]
                ]
            ]
        ]);
        break;
        
    case 'update_location':
        checkAuth();
        
        // Receber atualização de localização
        $data = json_decode(file_get_contents('php://input'), true);
        
        if (isset($data['lat']) && isset($data['lng'])) {
            // Aqui você salvaria no banco de dados
            echo json_encode([
                'success' => true,
                'message' => 'Localização atualizada com sucesso'
            ]);
        } else {
            http_response_code(400);
            echo json_encode([
                'success' => false,
                'error' => 'Coordenadas inválidas'
            ]);
        }
        break;
        
    case 'get_alertas':
        checkAuth();
        
        // Simular alertas
        echo json_encode([
            'success' => true,
            'alertas' => [
                [
                    'id' => 1,
                    'tipo' => 'velocidade',
                    'mensagem' => 'Veículo ABC-1234 acima do limite (85 km/h)',
                    'gravidade' => 'alta',
                    'timestamp' => date('Y-m-d H:i:s', strtotime('-10 minutes'))
                ],
                [
                    'id' => 2,
                    'tipo' => 'combustivel',
                    'mensagem' => 'Veículo DEF-5678 com combustível baixo (20%)',
                    'gravidade' => 'media',
                    'timestamp' => date('Y-m-d H:i:s', strtotime('-30 minutes'))
                ],
                [
                    'id' => 3,
                    'tipo' => 'manutencao',
                    'mensagem' => 'Veículo GHI-9012 necessita revisão',
                    'gravidade' => 'baixa',
                    'timestamp' => date('Y-m-d H:i:s', strtotime('-2 hours'))
                ]
            ]
        ]);
        break;
        
    default:
        http_response_code(404);
        echo json_encode([
            'success' => false,
            'error' => 'Ação não encontrada'
        ]);
}
?>