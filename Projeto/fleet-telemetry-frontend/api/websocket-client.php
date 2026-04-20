<?php
require_once '../config.php';

header('Content-Type: application/json');

// Simular cliente WebSocket para receber dados em tempo real
class WebSocketClient {
    private $wsUrl;
    private $connected = false;
    
    public function __construct($url) {
        $this->wsUrl = $url;
    }
    
    public function connect() {
        // Simular conexão WebSocket
        $this->connected = true;
        return true;
    }
    
    public function send($data) {
        if (!$this->connected) {
            return false;
        }
        
        // Simular envio de dados
        return true;
    }
    
    public function receive() {
        if (!$this->connected) {
            return null;
        }
        
        // Simular recebimento de dados de telemetria
        return [
            'type' => 'telemetria_update',
            'data' => [
                'veiculo_id' => rand(1, 10),
                'velocidade' => rand(0, 120),
                'lat' => -23.5505 + (rand(-100, 100) / 10000),
                'lng' => -46.6333 + (rand(-100, 100) / 10000),
                'timestamp' => date('Y-m-d H:i:s')
            ]
        ];
    }
    
    public function close() {
        $this->connected = false;
    }
}

$action = $_GET['action'] ?? '';

switch ($action) {
    case 'connect':
        $ws = new WebSocketClient(PUBLIC_WS_URL);
        
        if ($ws->connect()) {
            echo json_encode([
                'success' => true,
                'message' => 'Conectado ao WebSocket',
                'ws_url' => PUBLIC_WS_URL
            ]);
        } else {
            echo json_encode([
                'success' => false,
                'error' => 'Falha ao conectar ao WebSocket'
            ]);
        }
        break;
        
    case 'send_location':
        // Receber localização do frontend e enviar via WebSocket
        $data = json_decode(file_get_contents('php://input'), true);
        
        if (isset($data['lat']) && isset($data['lng'])) {
            $ws = new WebSocketClient(PUBLIC_WS_URL);
            $ws->connect();
            
            $message = [
                'type' => 'location_update',
                'user_id' => $_SESSION['usuario']['id'] ?? 0,
                'perfil' => $_SESSION['usuario']['perfil'] ?? '',
                'location' => [
                    'lat' => $data['lat'],
                    'lng' => $data['lng'],
                    'timestamp' => date('Y-m-d H:i:s')
                ]
            ];
            
            if ($ws->send($message)) {
                echo json_encode([
                    'success' => true,
                    'message' => 'Localização enviada com sucesso'
                ]);
            } else {
                echo json_encode([
                    'success' => false,
                    'error' => 'Falha ao enviar localização'
                ]);
            }
            
            $ws->close();
        } else {
            echo json_encode([
                'success' => false,
                'error' => 'Coordenadas inválidas'
            ]);
        }
        break;
        
    case 'get_realtime_data':
        // Simular recebimento de dados em tempo real
        $ws = new WebSocketClient(PUBLIC_WS_URL);
        
        if ($ws->connect()) {
            $data = $ws->receive();
            
            echo json_encode([
                'success' => true,
                'data' => $data
            ]);
            
            $ws->close();
        } else {
            echo json_encode([
                'success' => false,
                'error' => 'WebSocket não disponível'
            ]);
        }
        break;
        
    case 'send_alert':
        // Enviar alerta via WebSocket
        $data = json_decode(file_get_contents('php://input'), true);
        
        if (isset($data['tipo']) && isset($data['mensagem'])) {
            $ws = new WebSocketClient(PUBLIC_WS_URL);
            $ws->connect();
            
            $alert = [
                'type' => 'alert',
                'user_id' => $_SESSION['usuario']['id'] ?? 0,
                'alert' => [
                    'tipo' => $data['tipo'],
                    'mensagem' => $data['mensagem'],
                    'gravidade' => $data['gravidade'] ?? 'media',
                    'timestamp' => date('Y-m-d H:i:s')
                ]
            ];
            
            if ($ws->send($alert)) {
                echo json_encode([
                    'success' => true,
                    'message' => 'Alerta enviado com sucesso'
                ]);
            } else {
                echo json_encode([
                    'success' => false,
                    'error' => 'Falha ao enviar alerta'
                ]);
            }
            
            $ws->close();
        } else {
            echo json_encode([
                'success' => false,
                'error' => 'Dados do alerta inválidos'
            ]);
        }
        break;
        
    default:
        echo json_encode([
            'success' => false,
            'error' => 'Ação não encontrada',
            'available_actions' => [
                'connect' => 'Conectar ao WebSocket',
                'send_location' => 'Enviar atualização de localização',
                'get_realtime_data' => 'Receber dados em tempo real',
                'send_alert' => 'Enviar alerta'
            ]
        ]);
}
?>