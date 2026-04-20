<!DOCTYPE html>
<html lang="pt-BR">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
    <title>Sistema de Telemetria - Gestão de Frotas</title>
    
    <!-- Mapbox GL JS -->
    <link href="https://api.mapbox.com/mapbox-gl-js/v3.5.0/mapbox-gl.css" rel="stylesheet">
    <script src="https://api.mapbox.com/mapbox-gl-js/v3.5.0/mapbox-gl.js"></script>
    
    <!-- Chart.js -->
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.0/dist/chart.umd.min.js"></script>
    
    <!-- Font Awesome -->
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css">
    
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
        }

        .dashboard-container {
            display: flex;
            min-height: 100vh;
        }

        .sidebar {
            width: 280px;
            background: rgba(255, 255, 255, 0.95);
            backdrop-filter: blur(10px);
            box-shadow: 2px 0 20px rgba(0,0,0,0.1);
            padding: 20px;
            overflow-y: auto;
            transition: transform 0.3s ease;
            z-index: 100;
        }

        .sidebar-header {
            text-align: center;
            padding-bottom: 20px;
            border-bottom: 2px solid #e0e0e0;
            margin-bottom: 20px;
        }

        .sidebar-header h2 {
            color: #333;
            font-size: 1.5rem;
        }

        .sidebar-header .logo-icon {
            font-size: 2.5rem;
            color: #667eea;
            margin-bottom: 10px;
        }

        .veiculo-list {
            list-style: none;
        }

        .veiculo-item {
            background: #f8f9fa;
            border-radius: 12px;
            padding: 15px;
            margin-bottom: 12px;
            cursor: pointer;
            transition: all 0.3s ease;
            border: 2px solid transparent;
        }

        .veiculo-item:hover {
            transform: translateX(5px);
            background: #e9ecef;
        }

        .veiculo-item.active {
            border-color: #667eea;
            background: linear-gradient(135deg, #667eea20 0%, #764ba220 100%);
        }

        .veiculo-placa {
            font-weight: bold;
            font-size: 1.1rem;
            color: #333;
            margin-bottom: 8px;
        }

        .veiculo-info {
            font-size: 0.85rem;
            color: #666;
            margin: 5px 0;
        }

        .veiculo-status {
            display: inline-block;
            padding: 3px 8px;
            border-radius: 20px;
            font-size: 0.75rem;
            font-weight: bold;
        }

        .status-online { background: #d4edda; color: #155724; }
        .status-offline { background: #f8d7da; color: #721c24; }
        .status-moving { background: #cce5ff; color: #004085; }

        .main-content {
            flex: 1;
            display: flex;
            flex-direction: column;
            overflow: hidden;
        }

        .top-bar {
            background: white;
            padding: 15px 25px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }

        .page-title {
            font-size: 1.5rem;
            font-weight: 600;
            color: #333;
        }

        .stats-badge {
            display: flex;
            gap: 20px;
        }

        .stat-item {
            text-align: center;
        }

        .stat-value {
            font-size: 1.5rem;
            font-weight: bold;
            color: #667eea;
        }

        .stat-label {
            font-size: 0.75rem;
            color: #666;
        }

        .map-container {
            height: 400px;
            position: relative;
            margin: 20px;
            border-radius: 12px;
            overflow: hidden;
            box-shadow: 0 4px 15px rgba(0,0,0,0.1);
        }

        #map {
            height: 100%;
            width: 100%;
        }

        .cards-section {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 20px;
            padding: 0 20px 20px 20px;
        }

        .card {
            background: white;
            border-radius: 12px;
            padding: 20px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }

        .card-title {
            font-size: 1.1rem;
            font-weight: 600;
            color: #333;
            margin-bottom: 15px;
            display: flex;
            align-items: center;
            gap: 8px;
        }

        .card-title i {
            color: #667eea;
        }

        .telemetria-grid {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 15px;
        }

        .telemetria-item {
            text-align: center;
        }

        .telemetria-label {
            font-size: 0.75rem;
            color: #666;
            margin-bottom: 5px;
        }

        .telemetria-value {
            font-size: 1.5rem;
            font-weight: bold;
            color: #333;
        }

        .telemetria-unit {
            font-size: 0.75rem;
            color: #666;
        }

        .chart-container {
            height: 250px;
            position: relative;
        }

        .form-group {
            margin-bottom: 15px;
        }

        .form-group label {
            display: block;
            margin-bottom: 5px;
            font-weight: 500;
            color: #333;
        }

        .form-control {
            width: 100%;
            padding: 10px;
            border: 1px solid #ddd;
            border-radius: 8px;
            font-size: 0.9rem;
        }

        .btn {
            padding: 10px 20px;
            border: none;
            border-radius: 8px;
            cursor: pointer;
            font-weight: 500;
            transition: all 0.3s ease;
        }

        .btn-primary {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
        }

        .btn-primary:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 12px rgba(102,126,234,0.4);
        }

        .alerta-item {
            padding: 10px;
            margin-bottom: 10px;
            border-radius: 8px;
            background: #fff3cd;
            border-left: 4px solid #ffc107;
            font-size: 0.85rem;
        }

        .alerta-critical {
            background: #f8d7da;
            border-left-color: #dc3545;
        }

        .api-error {
            background: #fadbd8;
            color: #922b21;
            padding: 15px;
            margin: 20px;
            border-radius: 8px;
            border-left: 4px solid #e74c3c;
        }

        @media (max-width: 768px) {
            .sidebar {
                position: fixed;
                left: -280px;
                height: 100%;
                transition: left 0.3s ease;
            }
            .sidebar.open { left: 0; }
            .cards-section { grid-template-columns: 1fr; }
            .telemetria-grid { grid-template-columns: 1fr; }
        }

        .loading {
            display: inline-block;
            width: 20px;
            height: 20px;
            border: 3px solid #f3f3f3;
            border-top: 3px solid #667eea;
            border-radius: 50%;
            animation: spin 1s linear infinite;
        }

        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }

        .toast-container {
            position: fixed;
            bottom: 20px;
            right: 20px;
            z-index: 1000;
        }

        .toast {
            background: white;
            border-radius: 8px;
            padding: 12px 20px;
            margin-top: 10px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            animation: slideIn 0.3s ease;
        }

        .toast-success { border-left: 4px solid #28a745; }
        .toast-error { border-left: 4px solid #dc3545; }

        @keyframes slideIn {
            from { transform: translateX(100%); opacity: 0; }
            to { transform: translateX(0); opacity: 1; }
        }
    </style>
</head>
<body>
    <div class="dashboard-container">
        <div class="sidebar" id="sidebar">
            <div class="sidebar-header">
                <div class="logo-icon"><i class="fas fa-tachometer-alt"></i></div>
                <h2>Telemetria Pro</h2>
                <p style="font-size: 0.8rem; color: #666;">Gestão Inteligente de Frotas</p>
            </div>
            
            <div style="margin-bottom: 20px;">
                <button class="btn btn-primary" style="width: 100%;" onclick="toggleSidebar()">
                    <i class="fas fa-chevron-left"></i> Fechar Menu
                </button>
            </div>
            
            <h3 style="margin-bottom: 15px; font-size: 1rem;">
                <i class="fas fa-truck"></i> Veículos
            </h3>
            
            <div id="veiculo-search" style="margin-bottom: 15px;">
                <input type="text" class="form-control" placeholder="Buscar veículo..." id="searchVeiculo">
            </div>
            
            <ul class="veiculo-list" id="veiculoList">
                <li style="text-align: center; padding: 20px;"><div class="loading"></div> Carregando...</li>
            </ul>
        </div>
        
        <div class="main-content">
            <div class="top-bar">
                <button class="btn" style="background: transparent;" onclick="toggleSidebar()">
                    <i class="fas fa-bars fa-lg"></i>
                </button>
                
                <div class="page-title" id="selectedVeiculoTitle">Selecione um veículo</div>
                
                <div class="stats-badge">
                    <div class="stat-item">
                        <div class="stat-value" id="totalVeiculos">0</div>
                        <div class="stat-label">Veículos</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-value" id="onlineVeiculos">0</div>
                        <div class="stat-label">Online</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-value" id="alertasCount">0</div>
                        <div class="stat-label">Alertas</div>
                    </div>
                </div>
            </div>
            
            <div id="apiError" class="api-error" style="display: none;">
                ⚠️ Não foi possível conectar à API. Verifique se o serviço está disponível.
            </div>
            
            <div class="map-container">
                <div id="map"></div>
            </div>
            
            <div class="cards-section">
                <div class="card">
                    <div class="card-title"><i class="fas fa-gps"></i> Telemetria em Tempo Real</div>
                    <div class="telemetria-grid">
                        <div class="telemetria-item">
                            <div class="telemetria-label">Velocidade</div>
                            <div class="telemetria-value" id="velocidadeAtual">--</div>
                            <div class="telemetria-unit">km/h</div>
                        </div>
                        <div class="telemetria-item">
                            <div class="telemetria-label">Posição</div>
                            <div class="telemetria-value" id="posicaoAtual">--</div>
                            <div class="telemetria-unit">lat/lng</div>
                        </div>
                        <div class="telemetria-item">
                            <div class="telemetria-label">Última Atualização</div>
                            <div class="telemetria-value" id="ultimaAtualizacao">--</div>
                        </div>
                        <div class="telemetria-item">
                            <div class="telemetria-label">Status</div>
                            <div class="telemetria-value" id="statusVeiculo">--</div>
                        </div>
                    </div>
                </div>
                
                <div class="card">
                    <div class="card-title"><i class="fas fa-chart-line"></i> Histórico de Velocidade</div>
                    <div class="chart-container"><canvas id="velocidadeChart"></canvas></div>
                </div>
                
                <div class="card">
                    <div class="card-title"><i class="fas fa-paper-plane"></i> Enviar Telemetria</div>
                    <form id="telemetriaForm">
                        <input type="hidden" id="veiculoId" name="veiculoId">
                        <div class="form-group">
                            <label>Latitude</label>
                            <input type="number" step="any" class="form-control" id="latitude" required>
                        </div>
                        <div class="form-group">
                            <label>Longitude</label>
                            <input type="number" step="any" class="form-control" id="longitude" required>
                        </div>
                        <div class="form-group">
                            <label>Velocidade (km/h)</label>
                            <input type="number" step="any" class="form-control" id="velocidade" required>
                        </div>
                        <div class="form-group">
                            <label>Nível de Combustível (%)</label>
                            <input type="number" step="any" class="form-control" id="nivelCombustivel">
                        </div>
                        <button type="submit" class="btn btn-primary" style="width: 100%;">
                            <i class="fas fa-send"></i> Enviar Telemetria
                        </button>
                    </form>
                </div>
                
                <div class="card">
                    <div class="card-title"><i class="fas fa-bell"></i> Últimos Alertas</div>
                    <div id="alertasList" style="max-height: 300px; overflow-y: auto;">
                        <p style="color: #666; text-align: center;">Nenhum alerta recente</p>
                    </div>
                </div>
            </div>
            
            <div class="card" style="margin: 0 20px 20px 20px;">
                <div class="card-title">
                    <i class="fas fa-history"></i> Histórico de Telemetria
                    <button class="btn" style="margin-left: auto; padding: 5px 10px;" onclick="carregarHistorico()">
                        <i class="fas fa-sync-alt"></i> Atualizar
                    </button>
                </div>
                <div style="overflow-x: auto;">
                    <table style="width: 100%; border-collapse: collapse;">
                        <thead>
                            <tr style="background: #f8f9fa; border-bottom: 2px solid #dee2e6;">
                                <th style="padding: 12px; text-align: left;">Data/Hora</th>
                                <th style="padding: 12px; text-align: left;">Latitude</th>
                                <th style="padding: 12px; text-align: left;">Longitude</th>
                                <th style="padding: 12px; text-align: left;">Velocidade</th>
                                <th style="padding: 12px; text-align: left;">Combustível</th>
                                <th style="padding: 12px; text-align: left;">Ações</th>
                            </tr>
                        </thead>
                        <tbody id="historicoTable">
                            <tr><td colspan="6" style="text-align: center; padding: 20px;">Selecione um veículo para ver o histórico</td></tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
    
    <div class="toast-container" id="toastContainer"></div>
    
    <script>
        // Configurações da API
        const API_BASE_URL = 'http://localhost:9050/api/v1';
        const MAPBOX_TOKEN = 'pk.eyJ1IjoiZXhlbXBsbyIsImEiOiJjbXRlc3RlMDAwMDAwMDAwMDAwMDAwMDAifQ.xxxxxxxxxxx';
        const DEFAULT_CENTER_LAT = -23.5505;
        const DEFAULT_CENTER_LNG = -46.6333;
        const DEFAULT_ZOOM = 12;
        
        let map, markers = {}, currentVeiculo = null, velocidadeChart = null;
        let veiculosData = [], alertas = [];
        let apiDisponivel = true;
        
        document.addEventListener('DOMContentLoaded', function() {
            initMap();
            carregarVeiculos();
            startAutoRefresh();
        });
        
        function initMap() {
            mapboxgl.accessToken = MAPBOX_TOKEN;
            map = new mapboxgl.Map({
                container: 'map',
                style: 'mapbox://styles/mapbox/dark-v11',
                center: [DEFAULT_CENTER_LNG, DEFAULT_CENTER_LAT],
                zoom: DEFAULT_ZOOM
            });
            map.addControl(new mapboxgl.NavigationControl());
            map.addControl(new mapboxgl.FullscreenControl());
        }
        
        async function carregarVeiculos() {
            try {
                const response = await fetch(`${API_BASE_URL}/veiculos`);
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                const data = await response.json();
                
                veiculosData = data;
                renderVeiculoList(data);
                
                document.getElementById('totalVeiculos').textContent = data.length;
                const online = data.filter(v => v.status === 'ONLINE' || v.status === 'EM_MOVIMENTO').length;
                document.getElementById('onlineVeiculos').textContent = online;
                
                if (data.length > 0 && !currentVeiculo) selecionarVeiculo(data[0].id);
                
                document.getElementById('apiError').style.display = 'none';
                apiDisponivel = true;
            } catch (error) {
                console.error('Erro ao carregar veículos:', error);
                document.getElementById('apiError').style.display = 'block';
                apiDisponivel = false;
            }
        }
        
        function renderVeiculoList(veiculos) {
            const container = document.getElementById('veiculoList');
            const searchTerm = document.getElementById('searchVeiculo').value.toLowerCase();
            
            const filtered = veiculos.filter(v => 
                v.placa?.toLowerCase().includes(searchTerm) || 
                v.modelo?.toLowerCase().includes(searchTerm)
            );
            
            if (filtered.length === 0) {
                container.innerHTML = '<li style="text-align: center; padding: 20px; color: #666;">Nenhum veículo encontrado</li>';
                return;
            }
            
            container.innerHTML = filtered.map(veiculo => `
                <li class="veiculo-item ${currentVeiculo === veiculo.id ? 'active' : ''}" onclick="selecionarVeiculo(${veiculo.id})">
                    <div class="veiculo-placa"><i class="fas fa-truck"></i> ${veiculo.placa}</div>
                    <div class="veiculo-info"><i class="fas fa-car"></i> ${veiculo.modelo || 'Não informado'}</div>
                    <div class="veiculo-info"><i class="fas fa-map-marker-alt"></i> ${veiculo.ultimaPosicao || '--'}</div>
                    <div><span class="veiculo-status ${getStatusClass(veiculo.status)}">${getStatusText(veiculo.status)}</span></div>
                </li>
            `).join('');
        }
        
        function getStatusClass(status) {
            if (status === 'ONLINE') return 'status-online';
            if (status === 'EM_MOVIMENTO') return 'status-moving';
            return 'status-offline';
        }
        
        function getStatusText(status) {
            if (status === 'ONLINE') return '🟢 Online';
            if (status === 'EM_MOVIMENTO') return '🟡 Em Movimento';
            return '🔴 Offline';
        }
        
        async function selecionarVeiculo(veiculoId) {
            currentVeiculo = veiculoId;
            document.getElementById('veiculoId').value = veiculoId;
            
            const veiculo = veiculosData.find(v => v.id === veiculoId);
            document.getElementById('selectedVeiculoTitle').innerHTML = `<i class="fas fa-truck"></i> ${veiculo?.placa || 'Veículo'} - ${veiculo?.modelo || ''}`;
            
            renderVeiculoList(veiculosData);
            await carregarUltimaTelemetria(veiculoId);
            await carregarHistorico();
            await carregarAlertas(veiculoId);
        }
        
        async function carregarUltimaTelemetria(veiculoId) {
            if (!apiDisponivel) return;
            
            try {
                const response = await fetch(`${API_BASE_URL}/telemetria/veiculo/${veiculoId}/ultima`);
                if (response.status === 404) {
                    resetDisplayTelemetria();
                    return;
                }
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                
                const data = await response.json();
                atualizarDisplayTelemetria(data);
                atualizarMarkerMapa(data);
                await carregarHistoricoVelocidade(veiculoId);
            } catch (error) {
                console.error('Erro ao carregar última telemetria:', error);
            }
        }
        
        function resetDisplayTelemetria() {
            document.getElementById('velocidadeAtual').textContent = '--';
            document.getElementById('posicaoAtual').textContent = '--';
            document.getElementById('ultimaAtualizacao').textContent = '--';
            document.getElementById('statusVeiculo').innerHTML = '<span style="color: #666;">--</span>';
        }
        
        function atualizarDisplayTelemetria(data) {
            document.getElementById('velocidadeAtual').textContent = data.velocidade?.toFixed(1) || '--';
            document.getElementById('posicaoAtual').textContent = data.latitude && data.longitude ? `${data.latitude.toFixed(6)}, ${data.longitude.toFixed(6)}` : '--';
            document.getElementById('ultimaAtualizacao').textContent = data.dataHora ? new Date(data.dataHora).toLocaleString() : '--';
            document.getElementById('statusVeiculo').innerHTML = data.velocidade > 0 ? '<span style="color: #28a745;">🟢 Em Movimento</span>' : '<span style="color: #ffc107;">🟡 Parado</span>';
        }
        
        function atualizarMarkerMapa(data) {
            if (!data.latitude || !data.longitude) return;
            if (markers[currentVeiculo]) markers[currentVeiculo].remove();
            
            const el = document.createElement('div');
            el.innerHTML = `<div style="width:40px;height:40px;background:linear-gradient(135deg,#667eea,#764ba2);border-radius:50%;display:flex;align-items:center;justify-content:center;color:white;font-size:20px;border:3px solid white;box-shadow:0 2px 10px rgba(0,0,0,0.3);"><i class="fas fa-truck"></i></div>`;
            
            markers[currentVeiculo] = new mapboxgl.Marker(el.firstChild)
                .setLngLat([data.longitude, data.latitude])
                .setPopup(new mapboxgl.Popup().setHTML(`<strong>Veículo ${data.veiculoId}</strong><br>Velocidade: ${data.velocidade?.toFixed(1) || '--'} km/h<br>Data: ${new Date(data.dataHora).toLocaleString()}`))
                .addTo(map);
        }
        
        async function carregarHistorico() {
            if (!currentVeiculo || !apiDisponivel) return;
            
            try {
                const response = await fetch(`${API_BASE_URL}/telemetria/veiculo/${currentVeiculo}?limit=50`);
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                const data = await response.json();
                
                const tbody = document.getElementById('historicoTable');
                if (data.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; padding: 20px;">Nenhum dado de telemetria encontrado</td></tr>';
                    return;
                }
                
                tbody.innerHTML = data.map(item => `
                    <tr style="border-bottom: 1px solid #dee2e6;">
                        <td style="padding: 12px;">${new Date(item.dataHora).toLocaleString()}</td>
                        <td style="padding: 12px;">${item.latitude?.toFixed(6) || '--'}</td>
                        <td style="padding: 12px;">${item.longitude?.toFixed(6) || '--'}</td>
                        <td style="padding: 12px;">${item.velocidade?.toFixed(1) || '--'} km/h</td>
                        <td style="padding: 12px;">${item.nivelCombustivel || '--'}%</td>
                        <td style="padding: 12px;"><button class="btn" style="padding: 5px 10px;" onclick="verNoMapa(${item.latitude}, ${item.longitude})"><i class="fas fa-map-marker-alt"></i></button></td>
                    </tr>
                `).join('');
                
                atualizarGraficoVelocidade(data);
            } catch (error) {
                console.error('Erro ao carregar histórico:', error);
            }
        }
        
        async function carregarHistoricoVelocidade(veiculoId) {
            if (!apiDisponivel) return;
            try {
                const response = await fetch(`${API_BASE_URL}/telemetria/veiculo/${veiculoId}?limit=30`);
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                const data = await response.json();
                atualizarGraficoVelocidade(data);
            } catch (error) {
                console.error('Erro ao carregar histórico de velocidade:', error);
            }
        }
        
        function atualizarGraficoVelocidade(data) {
            const ctx = document.getElementById('velocidadeChart').getContext('2d');
            const labels = data.map(item => new Date(item.dataHora).toLocaleTimeString());
            const velocidades = data.map(item => item.velocidade || 0);
            
            if (velocidadeChart) velocidadeChart.destroy();
            
            velocidadeChart = new Chart(ctx, {
                type: 'line',
                data: {
                    labels: labels.reverse(),
                    datasets: [{
                        label: 'Velocidade (km/h)',
                        data: velocidades.reverse(),
                        borderColor: '#667eea',
                        backgroundColor: 'rgba(102, 126, 234, 0.1)',
                        tension: 0.4,
                        fill: true
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { position: 'top' } },
                    scales: { y: { beginAtZero: true, title: { display: true, text: 'Velocidade (km/h)' } } }
                }
            });
        }
        
        async function carregarAlertas(veiculoId) {
            if (!apiDisponivel) return;
            try {
                const response = await fetch(`${API_BASE_URL}/alertas/veiculo/${veiculoId}?limit=10`);
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                const data = await response.json();
                alertas = data;
                renderAlertas(data);
                document.getElementById('alertasCount').textContent = data.length;
            } catch (error) {
                console.error('Erro ao carregar alertas:', error);
            }
        }
        
        function renderAlertas(alertas) {
            const container = document.getElementById('alertasList');
            if (alertas.length === 0) {
                container.innerHTML = '<p style="color: #666; text-align: center;">Nenhum alerta recente</p>';
                return;
            }
            container.innerHTML = alertas.map(alerta => `
                <div class="alerta-item ${alerta.severidade === 'CRITICO' ? 'alerta-critical' : ''}">
                    <strong><i class="fas fa-exclamation-triangle"></i> ${alerta.tipo}</strong><br>
                    ${alerta.mensagem}<br>
                    <small>${new Date(alerta.dataHora).toLocaleString()}</small>
                </div>
            `).join('');
        }
        
        document.getElementById('telemetriaForm').addEventListener('submit', async function(e) {
            e.preventDefault();
            if (!currentVeiculo) { showToast('Selecione um veículo primeiro', 'error'); return; }
            if (!apiDisponivel) { showToast('API indisponível. Tente novamente mais tarde.', 'error'); return; }
            
            const data = {
                veiculo_id: currentVeiculo,
                latitude: parseFloat(document.getElementById('latitude').value),
                longitude: parseFloat(document.getElementById('longitude').value),
                velocidade: parseFloat(document.getElementById('velocidade').value),
                nivel_combustivel: document.getElementById('nivelCombustivel').value ? parseFloat(document.getElementById('nivelCombustivel').value) : null,
                data_hora: new Date().toISOString()
            };
            
            try {
                const response = await fetch(`${API_BASE_URL}/telemetria`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(data)
                });
                
                if (response.ok) {
                    showToast('Telemetria enviada com sucesso!', 'success');
                    this.reset();
                    await carregarUltimaTelemetria(currentVeiculo);
                    await carregarHistorico();
                } else {
                    const error = await response.text();
                    showToast(`Erro: ${error}`, 'error');
                }
            } catch (error) {
                console.error('Erro ao enviar telemetria:', error);
                showToast('Erro ao enviar telemetria', 'error');
            }
        });
        
        function verNoMapa(lat, lng) {
            if (lat && lng) {
                map.flyTo({ center: [lng, lat], zoom: 16, duration: 1000 });
                const el = document.createElement('div');
                el.innerHTML = '📍';
                el.style.fontSize = '30px';
                new mapboxgl.Marker(el.firstChild).setLngLat([lng, lat]).addTo(map);
                showToast('Localização centralizada no mapa', 'success');
            }
        }
        
        function showToast(message, type = 'info') {
            const container = document.getElementById('toastContainer');
            const toast = document.createElement('div');
            toast.className = `toast toast-${type}`;
            toast.innerHTML = `<i class="fas ${type === 'success' ? 'fa-check-circle' : type === 'error' ? 'fa-exclamation-circle' : 'fa-info-circle'}"></i> ${message}`;
            container.appendChild(toast);
            setTimeout(() => toast.remove(), 3000);
        }
        
        function toggleSidebar() { document.getElementById('sidebar').classList.toggle('open'); }
        
        function startAutoRefresh() {
            setInterval(() => {
                if (currentVeiculo && apiDisponivel) {
                    carregarUltimaTelemetria(currentVeiculo);
                    carregarVeiculos();
                }
            }, 10000);
        }
        
        document.getElementById('searchVeiculo').addEventListener('input', () => renderVeiculoList(veiculosData));
    </script>
</body>
</html>