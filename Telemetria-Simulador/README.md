# Simulador de Telemetria

Simulador de telemetria de frota que gera dados realistas de veículos e envia para o Kafka e/ou para a API HTTP do telemetry-service.

## Funcionalidades

- Gera dados completos de telemetria baseados na entidade `Telemetria` do telemetry-service
- Suporta três modos de operação:
  - **kafka**: Envia apenas para o Kafka
  - **http**: Envia apenas para a API HTTP do telemetry-service
  - **ambos**: Envia para Kafka E para a API HTTP (padrão)
- Simula múltiplos veículos com diferentes planos (STARTER, PRO, ENTERPRISE)
- Gera dados de GPS, motor, combustível, comportamento, segurança, carga, pneus, DMS, ambiente, conectividade, tacógrafo e manutenção
- Simula movimento ao longo de uma rota (São Paulo -> Campinas)

## Instalação

1. Instale as dependências Python:
```bash
pip install -r requirements.txt
```

## Configuração

Edite as variáveis no início do arquivo `simulador_telemetria.py`:

### Configurações de Conexão
```python
# Kafka
KAFKA_BROKER = "localhost:9092"
TOPIC = "telemetria-raw"

# HTTP API (telemetry-service)
TELEMETRY_SERVICE_URL = "http://localhost:8080/api/v1/telemetria"

# Modo de envio: "kafka", "http", "ambos"
MODO_ENVIO = "kafka"
```

### Parâmetros de Simulação
```python
NUM_THREADS = 1                     # Número de veículos simulados
INTERVALO_BASE_MS = 2000            # Intervalo entre mensagens (ms)
VELOCIDADE_BASE = 80                # Velocidade base (km/h)
```

### Veículos Configurados
O simulador vem com 3 veículos pré-configurados:
- Veículo 1: STARTER
- Veículo 2: PRO
- Veículo 3: ENTERPRISE

## Uso

### Modo 1: Apenas Kafka
```python
MODO_ENVIO = "kafka"
```
Execute:
```bash
python simulador_telemetria.py
```

### Modo 2: Apenas HTTP API
```python
MODO_ENVIO = "http"
```
Execute:
```bash
python simulador_telemetria.py
```

### Modo 3: Ambos (padrão)
```python
MODO_ENVIO = "ambos"
```
Execute:
```bash
python simulador_telemetria.py
```

## Estrutura dos Dados

O simulador gera todos os campos da entidade Telemetria:

- **Identificação**: tenant_id, veiculo_id, veiculo_uuid, motorista_id, viagem_id, device_id, imei_dispositivo
- **GPS**: latitude, longitude, altitude, velocidade, direcao, hdop, satelites, precisao_gps, lat_snap, lng_snap, nome_via
- **Motor/OBD-II**: ignicao, rpm, carga_motor, torque_motor, temperatura_motor, pressao_oleo, tensao_bateria, odometro, horas_motor, aceleracao, inclinacao
- **Combustível**: nivel_combustivel, consumo_combustivel, consumo_acumulado, tempo_ocioso, tempo_motor_ligado
- **Comportamento**: frenagem_brusca, numero_frenagens, numero_aceleracoes_bruscas, excesso_velocidade, velocidade_limite_via, curva_brusca, pontuacao_motorista
- **Segurança**: colisao_detectada, geofence_violada, geofence_id, cinto_seguranca, porta_aberta, botao_panico, adulteracao_gps
- **Carga**: temperatura_carga, umidade_carga, peso_carga_kg, porta_bau_aberta, impacto_carga, g_force_impacto
- **Pneus**: pressao_pneus_json, alerta_pneu
- **DMS**: fadiga_detectada, distracao_detectada, uso_celular_detectado, cigarro_detectado, ausencia_cinto_dms, score_dms
- **Ambiente**: temperatura_externa, umidade_externa, chuva_detectada, condicao_pista
- **Conectividade**: sinal_gsm, sinal_gps, tecnologia_rede, firmware_versao, modo_offline, delay_sincronizacao_s
- **Tacógrafo**: tacografo_status, tacografo_velocidade, tacografo_distancia, horas_direcao_acumuladas
- **Manutenção**: manutencao_pendente, proxima_revisao, desgaste_freio
- **DTC/Payload**: dtc_codes, payload

## Métricas

O simulador exibe métricas em tempo real:
- Total de mensagens enviadas
- Mensagens por segundo
- Taxa média de envio
- Sucessos e erros
- Callbacks do Kafka

## Parada

Pressione `Ctrl+C` para encerrar a simulação de forma graciosa.

## Relatório Final

Ao encerrar, o simulador exibe um relatório com:
- Tempo total de execução
- Total de mensagens enviadas
- Sucessos e erros
- Callbacks do Kafka
- Média de mensagens por segundo

## Requisitos

- Python 3.7+
- Kafka (para modo kafka ou ambos)
- telemetry-service rodando (para modo http ou ambos)
- Bibliotecas: kafka-python, requests

## Troubleshooting

### Erro de conexão Kafka
Verifique se o Kafka está rodando:
```bash
docker ps  # Verifique se o container Kafka está rodando
```

### Erro de conexão HTTP
Verifique se o telemetry-service está rodando:
```bash
docker ps  # Verifique se o container telemetry-service está rodando
curl http://localhost:8080/api/v1/telemetria  # Teste a API
```
