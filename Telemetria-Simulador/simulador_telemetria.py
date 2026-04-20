import json
import random
import time
import math
import sys
import threading
from kafka import KafkaProducer

# =========================
# CONFIGURAÇÕES
# =========================
KAFKA_BROKER = "localhost:9092"
TOPIC = "telemetria-raw"

# Parâmetros de carga extremamente baixa
NUM_THREADS = 1
INTERVALO_BASE_MS = 2000            # 2 segundos entre mensagens
ENVIAR_BATCH_INICIAL = False
BATCH_SIZE = 0
DURACAO_TESTE_SEGUNDOS = 0
RAMP_UP_MS = 1000

ATIVAR_BURST = False
FLUSH_A_CADA = 0

# Probabilidades zero para evitar eventos críticos/altos
INJETAR_CRITICO_PROB = 0.0
INJETAR_ALTO_PROB = 0.0
PROB_VELOCIDADE_IMPOS = 0.0
PROB_SALTO_POSICAO = 0.0
PROB_HDOP_ALTO = 0.0
PROB_SATELITES_BAIXOS = 0.0

# GSM – sinal normal sempre
PROB_RSSI_NORMAL = 1.0
PROB_RSSI_REDUZIDO = 0.0
PROB_RSSI_BAIXO = 0.0
RSSI_NORMAL_MIN = -85
RSSI_NORMAL_MAX = -50
TEMPO_MUDANCA_SINAL = 60

PROB_PRESERVAR_DADOS = 0.0

# Veículos existentes no banco (IDs, UUIDs, planos) – atualizados com os UUIDs reais dos dados
VEICULOS = [
    {
        "id": 1,
        "uuid": "e7469e2f-3096-11f1-993c-5e38365aa120",
        "plano": "STARTER",
        "device_id": "DEV-001",
        "imei": "123456789012345",
        "tenant_id": 1,
        "motorista_id": None,
        "viagem_id": None
    },
    {
        "id": 2,
        "uuid": "e7489952-3096-11f1-993c-5e38365aa120",
        "plano": "PRO",
        "device_id": "DEV-002",
        "imei": "234567890123456",
        "tenant_id": 1,
        "motorista_id": None,
        "viagem_id": None
    },
    {
        "id": 3,
        "uuid": "e74a6b07-3096-11f1-993c-5e38365aa120",
        "plano": "ENTERPRISE",
        "device_id": "DEV-003",
        "imei": "345678901234567",
        "tenant_id": 2,
        "motorista_id": None,
        "viagem_id": None
    },
    {
        "id": 4,
        "uuid": "e74c512a-3096-11f1-993c-5e38365aa120",
        "plano": "STARTER",
        "device_id": "DEV-004",
        "imei": "456789012345678",
        "tenant_id": 1,
        "motorista_id": None,
        "viagem_id": None
    },
    {
        "id": 5,
        "uuid": "e74e7f8d-3096-11f1-993c-5e38365aa120",
        "plano": "PRO",
        "device_id": "DEV-005",
        "imei": "567890123456789",
        "tenant_id": 2,
        "motorista_id": None,
        "viagem_id": None
    }
]

# Rota (São Paulo -> Campinas)
ROTA_PONTOS = [
    (-23.5505, -46.6333),
    (-23.5200, -46.6000),
    (-23.4900, -46.5500),
    (-23.4600, -46.5000),
    (-23.4200, -46.4500),
    (-23.3800, -46.4000),
    (-23.3400, -46.3500),
    (-23.3000, -46.3000),
    (-23.2600, -46.2500),
    (-23.2200, -46.2000),
    (-23.1800, -46.1500),
    (-23.1400, -46.1000),
    (-23.1000, -46.0500),
    (-23.0600, -46.0000),
    (-22.9500, -45.9500),
]
VELOCIDADE_BASE = 80

# Estado global
lock = threading.Lock()
total_tentadas = 0
total_sucesso = 0
total_erros = 0
total_callbacks_ok = 0
total_callbacks_erro = 0
encerrar = False
teste_inicio = time.time()

# =========================
# FUNÇÕES AUXILIARES
# =========================
def now_ms():
    return int(time.time() * 1000)

def distancia_total_rota():
    total = 0
    for i in range(len(ROTA_PONTOS)-1):
        dlat = math.radians(ROTA_PONTOS[i+1][0] - ROTA_PONTOS[i][0])
        dlon = math.radians(ROTA_PONTOS[i+1][1] - ROTA_PONTOS[i][1])
        a = math.sin(dlat/2)**2 + math.cos(math.radians(ROTA_PONTOS[i][0])) * \
            math.cos(math.radians(ROTA_PONTOS[i+1][0])) * math.sin(dlon/2)**2
        c = 2 * math.atan2(math.sqrt(a), math.sqrt(1-a))
        total += 6371 * c
    return total

DISTANCIA_TOTAL = distancia_total_rota()

def interpolar_posicao(progresso):
    if progresso <= 0:
        return ROTA_PONTOS[0]
    if progresso >= 1:
        return ROTA_PONTOS[-1]
    total_segmentos = len(ROTA_PONTOS)-1
    idx_float = progresso * total_segmentos
    idx = int(idx_float)
    t = idx_float - idx
    if idx >= total_segmentos:
        return ROTA_PONTOS[-1]
    lat1, lon1 = ROTA_PONTOS[idx]
    lat2, lon2 = ROTA_PONTOS[idx+1]
    lat = lat1 + (lat2 - lat1) * t
    lon = lon1 + (lon2 - lon1) * t
    return lat, lon

def gerar_rssi():
    return random.randint(RSSI_NORMAL_MIN, RSSI_NORMAL_MAX)

def criar_producer():
    try:
        print("[DEBUG] Criando producer Kafka...")
        producer = KafkaProducer(
            bootstrap_servers=KAFKA_BROKER,
            value_serializer=lambda v: json.dumps(v).encode("utf-8"),
            acks=1,
            linger_ms=20,
            batch_size=131072,
            buffer_memory=67108864,
            compression_type="gzip",
            retries=3,
            max_in_flight_requests_per_connection=5,
            request_timeout_ms=10000,
            max_block_ms=20000
        )
        print("[DEBUG] Producer Kafka criado com sucesso")
        return producer
    except Exception as e:
        print(f"[ERRO] Falha ao criar producer: {e}")
        sys.exit(1)

def on_send_success(record_metadata):
    global total_callbacks_ok
    with lock:
        total_callbacks_ok += 1
    print(f"[DEBUG] Callback success: offset={record_metadata.offset}, partition={record_metadata.partition}")

def on_send_error(excp):
    global total_callbacks_erro
    with lock:
        total_callbacks_erro += 1
    print(f"[CALLBACK ERRO] {excp}")

def gerar_pressao_pneus():
    return [
        {"pos": "D1", "psi": round(random.uniform(90, 110), 2)},
        {"pos": "D2", "psi": round(random.uniform(90, 110), 2)},
        {"pos": "T1", "psi": round(random.uniform(90, 110), 2)},
        {"pos": "T2", "psi": round(random.uniform(90, 110), 2)},
    ]

def montar_mensagem(veiculo, lat, lon, velocidade, odometro, rssi_atual):
    """Gera uma mensagem com TODOS os campos da tabela telemetria."""
    print(f"\n[DEBUG] Montando mensagem para veiculo_id={veiculo['id']}")
    print(f"[DEBUG] Dados do veiculo: id={veiculo['id']}, uuid={veiculo['uuid']}, plano={veiculo['plano']}, device_id={veiculo['device_id']}, imei={veiculo.get('imei')}, tenant_id={veiculo['tenant_id']}, motorista_id={veiculo.get('motorista_id')}, viagem_id={veiculo.get('viagem_id')}")
    print(f"[DEBUG] Dados GPS: lat={lat}, lon={lon}, velocidade={velocidade}, odometro={odometro}, rssi={rssi_atual}")
    msg = {
        # Identificação
        "tenant_id": veiculo["tenant_id"],
        "veiculo_id": veiculo["id"],
        "veiculo_uuid": veiculo["uuid"],
        "motorista_id": veiculo.get("motorista_id"),
        "viagem_id": veiculo.get("viagem_id"),
        "device_id": veiculo["device_id"],
        "imei_dispositivo": veiculo.get("imei"),
        "impreciso": False,
        "preservar_dados": random.random() < PROB_PRESERVAR_DADOS,

        # GPS
        "latitude": round(lat, 6),
        "longitude": round(lon, 6),
        "altitude": round(random.uniform(700, 1200), 1),
        "velocidade": round(velocidade, 1),
        "direcao": random.randint(0, 359),
        "hdop": round(random.uniform(0.8, 2.5), 2),
        "satelites": random.randint(4, 12),
        "precisao_gps": round(random.uniform(3, 10), 1),
        "lat_snap": round(lat, 6),
        "lng_snap": round(lon, 6),
        "nome_via": "Rodovia Simulada",

        # Motor / OBD-II
        "ignicao": True,
        "rpm": random.randint(1200, 2500),
        "carga_motor": round(random.uniform(20, 95), 1),
        "torque_motor": round(random.uniform(200, 600), 1),
        "temperatura_motor": round(random.uniform(85, 105), 1),
        "pressao_oleo": round(random.uniform(30, 70), 1),
        "tensao_bateria": round(random.uniform(11.5, 14.5), 1),
        "odometro": round(odometro, 3),
        "horas_motor": round(random.uniform(1000, 50000), 1),
        "aceleracao": round(random.uniform(-2, 2), 2),
        "inclinacao": round(random.uniform(-5, 5), 2),

        # Combustível
        "nivel_combustivel": round(random.uniform(20, 95), 1),
        "consumo_combustivel": round(random.uniform(25, 45), 1),
        "consumo_acumulado": round(random.uniform(1000, 20000), 1),
        "tempo_ocioso": random.randint(0, 300),
        "tempo_motor_ligado": random.randint(0, 3600),

        # Comportamento
        "frenagem_brusca": False,
        "numero_frenagens": random.randint(0, 5),
        "numero_aceleracoes_bruscas": random.randint(0, 5),
        "excesso_velocidade": False,
        "velocidade_limite_via": 110.0,
        "curva_brusca": False,
        "pontuacao_motorista": random.randint(800, 1000),

        # Segurança
        "colisao_detectada": False,
        "geofence_violada": False,
        "geofence_id": None,
        "cinto_seguranca": True,
        "porta_aberta": False,
        "botao_panico": False,
        "adulteracao_gps": False,

        # Carga
        "temperatura_carga": round(random.uniform(-5, 25), 1),
        "umidade_carga": round(random.uniform(40, 80), 1),
        "peso_carga_kg": round(random.uniform(0, 20000), 1),
        "porta_bau_aberta": False,
        "impacto_carga": False,
        "g_force_impacto": 0.0,

        # Pneus
        "pressao_pneus_json": gerar_pressao_pneus(),
        "alerta_pneu": False,

        # DMS (câmera)
        "fadiga_detectada": False,
        "distracao_detectada": False,
        "uso_celular_detectado": False,
        "cigarro_detectado": False,
        "ausencia_cinto_dms": False,
        "score_dms": random.randint(0, 100),

        # Ambiente
        "temperatura_externa": round(random.uniform(15, 35), 1),
        "umidade_externa": round(random.uniform(40, 90), 1),
        "chuva_detectada": random.random() < 0.1,
        "condicao_pista": random.choice(["SECA", "MOLHADA", "LAMACENTA"]),

        # Conectividade
        "sinal_gsm": rssi_atual,
        "sinal_gps": round(random.uniform(50, 100), 1),
        "tecnologia_rede": random.choice(["4G", "5G", "3G"]),
        "firmware_versao": "v2.1.0",
        "modo_offline": False,
        "delay_sincronizacao_s": 0,

        # Tacógrafo
        "tacografo_status": "NORMAL",
        "tacografo_velocidade": round(velocidade, 1),
        "tacografo_distancia": round(odometro, 3),
        "horas_direcao_acumuladas": round(random.uniform(0, 8), 1),

        # Manutenção
        "manutencao_pendente": False,
        "proxima_revisao": None,
        "desgaste_freio": round(random.uniform(10, 90), 1),

        # DTC / Payload
        "dtc_codes": [],
        "payload": {},

        # Timestamp
        "timestamp": now_ms(),

        # Prioridade (sempre NORMAL)
        "priority": "NORMAL",

        # Plano (para RF06)
        "plano": veiculo["plano"]
    }
    print(f"[DEBUG] Mensagem montada com {len(msg)} campos")
    print(f"[DEBUG] Campos: {list(msg.keys())}")
    return msg

def enviar_msg(producer, msg):
    global total_tentadas, total_sucesso, total_erros
    print(f"\n[DEBUG] enviar_msg() - Iniciando envio")
    print(f"[DEBUG] veiculo_id={msg.get('veiculo_id')}, tenant_id={msg.get('tenant_id')}")
    print(f"[DEBUG] Producer exists: {producer is not None}")
    try:
        future = producer.send(TOPIC, key=str(msg["veiculo_id"]).encode("utf-8"), value=msg)
        print(f"[DEBUG] Mensagem enviada para o Kafka (future criado)")
        future.add_callback(on_send_success)
        future.add_errback(on_send_error)
        with lock:
            total_tentadas += 1
            total_sucesso += 1
        print(f"[DEBUG] Contador: tentadas={total_tentadas}, sucesso={total_sucesso}")
    except Exception as e:
        with lock:
            total_tentadas += 1
            total_erros += 1
        print(f"[ERRO ENVIO] {e}")
        import traceback
        traceback.print_exc()

def monitorar_metricas():
    ultimo_total = 0
    while not encerrar:
        time.sleep(1)
        with lock:
            atual = total_tentadas
            sucesso = total_sucesso
            erros = total_erros
            cb_ok = total_callbacks_ok
            cb_erro = total_callbacks_erro
        delta = atual - ultimo_total
        ultimo_total = atual
        tempo = time.time() - teste_inicio
        media = atual / tempo if tempo > 0 else 0
        print(f"[METRICAS] total_enviadas={atual} | msg/s={delta} | "
              f"media={media:.2f}/s | sucesso_local={sucesso} | erros_local={erros} | "
              f"callback_ok={cb_ok} | callback_erro={cb_erro}")

def simular_thread(veiculo, producer):
    veiculo_id = veiculo["id"]
    device_id = veiculo["device_id"]
    plano = veiculo["plano"]
    
    progresso = 0.0
    odometro = 10000.0 + veiculo_id * 1000
    rssi_atual = gerar_rssi()
    mensagens_thread = 0

    print(f"[Veículo {veiculo_id}] Iniciado | uuid={veiculo['uuid']} | plano={plano} | device={device_id}")
    print(f"[DEBUG] Config veiculo {veiculo_id}: {json.dumps(veiculo, indent=2)}")

    while not encerrar:
        lat, lon = interpolar_posicao(progresso)
        velocidade = VELOCIDADE_BASE + random.uniform(-15, 15)
        delta_km = velocidade * (INTERVALO_BASE_MS / 1000.0) / 3600.0
        odometro += delta_km
        
        print(f"[DEBUG] Veiculo {veiculo_id} - Ciclo {mensagens_thread}: lat={lat:.6f}, lon={lon:.6f}, vel={velocidade:.1f}, odo={odometro:.3f}")

        msg = montar_mensagem(veiculo, lat, lon, velocidade, odometro, rssi_atual)
        enviar_msg(producer, msg)
        mensagens_thread += 1

        delta_progresso = delta_km / DISTANCIA_TOTAL
        progresso += delta_progresso
        if progresso > 1:
            progresso = 0

        time.sleep(INTERVALO_BASE_MS / 1000.0)

    producer.flush()
    print(f"[Veículo {veiculo_id}] Finalizado | mensagens enviadas={mensagens_thread}")

def main():
    global encerrar, teste_inicio

    print("=== SIMULADOR DE TELEMETRIA (TODOS OS CAMPOS) ===")
    print(f"Kafka Broker: {KAFKA_BROKER}")
    print(f"Topic: {TOPIC}")
    print(f"Veículos: {[v['id'] for v in VEICULOS]}")
    print(f"Intervalo: {INTERVALO_BASE_MS}ms")
    print("Pressione Ctrl+C para encerrar.\n")

    producer = criar_producer()
    teste_inicio = time.time()

    monitor = threading.Thread(target=monitorar_metricas, daemon=True)
    monitor.start()

    threads = []
    for veiculo in VEICULOS:
        t = threading.Thread(target=simular_thread, args=(veiculo, producer), daemon=True)
        t.start()
        threads.append(t)
        time.sleep(RAMP_UP_MS / 1000.0)

    try:
        while not encerrar:
            time.sleep(1)
    except KeyboardInterrupt:
        print("\n🛑 Encerrando simulação...")
    finally:
        encerrar = True
        for t in threads:
            t.join(timeout=5)
        producer.flush()
        producer.close()

    tempo_total = time.time() - teste_inicio
    media = total_tentadas / tempo_total if tempo_total > 0 else 0

    print("\n=== RELATÓRIO FINAL ===")
    print(f"Tempo total: {tempo_total:.2f}s")
    print(f"Mensagens enviadas: {total_tentadas}")
    print(f"Sucessos locais: {total_sucesso}")
    print(f"Erros locais: {total_erros}")
    print(f"Callbacks OK: {total_callbacks_ok}")
    print(f"Callbacks erro: {total_callbacks_erro}")
    print(f"Média msg/s: {media:.2f}")
    print("=======================")

if __name__ == "__main__":
    main()