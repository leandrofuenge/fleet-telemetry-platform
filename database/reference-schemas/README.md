# 🚛 Frota Telemetria — Microserviços: Bancos + Entidades Java

**Sistema de Telemetria para Frotas v3.0 — Stack: Java 17 · Spring Boot 3.x · MySQL · Kafka**

---

## 📁 Estrutura dos Arquivos

```
frota-telemetria/
├── sql/
│   ├── telemetry_service_db.sql      ← Banco do telemetry-service (COMPLETO)
│   ├── routing_service_db.sql        ← Banco do routing-service (COMPLETO)
│   ├── 01_auth_service.sql           ← Banco do auth-service
│   ├── 02_vehicle_service.sql        ← Banco do vehicle-service
│   ├── 03_route_service.sql          ← Banco auxiliar do route-service
│   └── 04_alert_maintenance_billing_ota.sql
│
├── java/
│   ├── shared-enums.java             ← Enums compartilhados entre serviços
│   ├── telemetry-service/
│   │   └── TelemetryEntities.java    ← Todas as entidades JPA do telemetry-service
│   └── routing-service/
│       └── RoutingEntities.java      ← Todas as entidades JPA do routing-service
```

---

## 🗄️ Bancos de Dados dos Microserviços

| Microserviço      | Banco              | Arquivo SQL                           |
|-------------------|--------------------|---------------------------------------|
| telemetry-service | `telemetry_db`     | `telemetry_service_db.sql`            |
| routing-service   | `routing_db`       | `routing_service_db.sql`              |
| auth-service      | `auth_db`          | `01_auth_service.sql`                 |
| vehicle-service   | `vehicle_db`       | `02_vehicle_service.sql`              |
| alert-service     | `alert_db`         | `04_alert_maintenance_billing_ota.sql`|
| maintenance-service| `maintenance_db`  | `04_alert_maintenance_billing_ota.sql`|
| billing-service   | `billing_db`       | `04_alert_maintenance_billing_ota.sql`|
| ota-service       | `ota_db`           | `04_alert_maintenance_billing_ota.sql`|

---

## 🏗️ telemetry_db — Tabelas

| Tabela                    | Descrição                                                        |
|---------------------------|------------------------------------------------------------------|
| `veiculos_cache`          | Cópia local dos veículos (sincronizada via Kafka)               |
| `motoristas_cache`        | Cópia local dos motoristas                                       |
| `telemetria`              | ⭐ Tabela principal: todos os eventos IoT (GPS, OBD-II, DMS...)  |
| `alertas`                 | Alertas gerados por eventos de telemetria                        |
| `geofences`               | Zonas geográficas configuráveis por tenant                       |
| `veiculo_geofence`        | N:N veículos ↔ geofences                                        |
| `desvios_rota`            | Desvios detectados durante viagens                               |
| `geocoding_cache`         | Cache de reverse geocoding (30 dias de TTL)                      |
| `resumo_diario_veiculo`   | Agregação diária para dashboard (gerada às 01h)                 |
| `posicao_atual`           | Estado em tempo real de cada veículo                             |
| `historico_posicao`       | Trajetória simplificada últimas 24h                              |
| `dispositivos_iot`        | Rastreadores GPS com lifecycle completo (JITP → revogação)      |
| `heartbeat_log`           | Log de conectividade dos devices                                 |
| `jornadas`                | Controle de jornada Lei 12.619/2012                              |

---

## 🗺️ routing_db — Tabelas

| Tabela                      | Descrição                                                      |
|-----------------------------|----------------------------------------------------------------|
| `veiculos_cache`            | Cópia local dos veículos                                       |
| `motoristas_cache`          | Cópia local dos motoristas                                     |
| `rotas`                     | ⭐ Rota planejada com dados OSRM completos                      |
| `viagens`                   | ⭐ Execução real da rota em tempo real                          |
| `desvios_rota`              | Desvios persistidos com aprovação do gestor                    |
| `pontos_entrega`            | Paradas com proof-of-delivery (assinatura, foto)               |
| `osrm_route_cache`          | Cache de rotas calculadas pelo OSRM (TTL 7 dias)              |
| `geocoding_cache`           | Cache de geocoding                                             |
| `historico_trajeto_viagem`  | Trajeto real simplificado por segmento                         |
| `relatorio_viagem`          | Relatório pós-viagem consolidado                               |

---

## ☕ Entidades Java — telemetry-service

```
VeiculoCache          → veiculos_cache
MotoristaCache        → motoristas_cache
Telemetria            → telemetria           (entidade principal, 60+ campos)
Alerta                → alertas
Geofence              → geofences
DesvioRota            → desvios_rota
GeocodingCache        → geocoding_cache
PosicaoAtual          → posicao_atual
ResumoDiarioVeiculo   → resumo_diario_veiculo
DispositivoIot        → dispositivos_iot
Jornada               → jornadas
```

## ☕ Entidades Java — routing-service

```
VeiculoCache              → veiculos_cache
MotoristaCache            → motoristas_cache
Rota                      → rotas              (entidade central)
Viagem                    → viagens
DesvioRota                → desvios_rota
PontoEntrega              → pontos_entrega
OsrmRouteCache            → osrm_route_cache
GeocodingCache            → geocoding_cache
HistoricoTrajetoViagem    → historico_trajeto_viagem
RelatorioViagem           → relatorio_viagem
```

---

## 🔧 Dependências Maven (pom.xml)

```xml
<!-- Spring Boot 3.x -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- MySQL Driver -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- Hibernate types para JSON -->
<dependency>
    <groupId>io.hypersistence</groupId>
    <artifactId>hypersistence-utils-hibernate-63</artifactId>
    <version>3.7.0</version>
</dependency>
```

---

## ⚙️ application.yml (telemetry-service)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/telemetry_db?useUnicode=true&characterEncoding=utf8&serverTimezone=America/Sao_Paulo
    username: ${DB_USER:root}
    password: ${DB_PASS:secret}
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 20000
  jpa:
    hibernate:
      ddl-auto: validate          # Flyway gerencia o schema
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true
        default_batch_fetch_size: 50

  kafka:
    bootstrap-servers: ${KAFKA_SERVERS:localhost:9092}
    consumer:
      group-id: telemetry-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
```

---

## 📡 Kafka Topics (comunicação entre serviços)

| Topic                    | Producer              | Consumer(s)                          |
|--------------------------|-----------------------|--------------------------------------|
| `telemetry.raw`          | MQTT Gateway          | telemetry-service                    |
| `telemetry.processed`    | telemetry-service     | alert-service, ml-service            |
| `route.events`           | routing-service       | telemetry-service, alert-service     |
| `vehicle.events`         | vehicle-service       | telemetry-service, routing-service   |
| `driver.events`          | driver-service        | telemetry-service, routing-service   |
| `alerts.generated`       | alert-service         | notification-service                 |
| `deviation.detected`     | telemetry-service     | routing-service, alert-service       |

---

## 🔄 Padrão de Sincronização dos Caches

Os caches locais (`veiculos_cache`, `motoristas_cache`) são atualizados via **Kafka Event Sourcing**:

```java
// Exemplo: consumer no telemetry-service
@KafkaListener(topics = "vehicle.events", groupId = "telemetry-service")
public void onVehicleEvent(VehicleEvent event) {
    VeiculoCache cache = veiculoCacheRepository.findById(event.getId())
        .orElse(new VeiculoCache());
    // mapear campos e salvar
    veiculoCacheRepository.save(cache);
}
```

---

## 🛡️ Multi-tenancy

Todas as tabelas possuem `tenant_id`. O filtro é aplicado automaticamente via:

```java
@Component
public class TenantFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest req, ...) {
        String tenantId = jwtService.extractTenantId(req);
        TenantContext.set(tenantId);
        try {
            chain.doFilter(req, resp);
        } finally {
            TenantContext.clear(); // CRÍTICO: sempre limpar
        }
    }
}
```

---

🚛 Sistema De Monitoramento De Frotas
Java Spring Boot Kafka Redis Docker Status

📌 Overview
Sistema de monitoramento em tempo real para frotas de caminhões e carretas com foco em:

📡 Telemetria em tempo real\
📍 Rastreamento GPS\
🛣 Gestão inteligente de rotas\
👨‍✈️ Gestão de motoristas\
🔧 Manutenção preditiva\
📊 Análise de desempenho\
🌐 Operação resiliente em baixa conectividade
🏗 Arquitetura
        Dispositivos IoT / GPS
                  │
                  ▼
          API Gateway (Spring Cloud)
                  │
                  ▼
        Microserviços (Spring WebFlux)
                  │
                  ▼
         Event Streaming (Kafka)
                  │
        ┌─────────┴─────────┐
        ▼                   ▼
  Processadores        Cache (Redis)
        │                   │
        ▼                   │
Banco Relacional       Resposta Rápida
 (MySQL)                    │
        │                   ▼
        └──────► TimescaleDB (Séries Temporais)
                           │
                           ▼
                    Frontend (React + Mapas)
🔧 Tech Stack
Backend
Java 17+
Spring Boot
Spring WebFlux (Reativo)
Spring Data JPA
JWT + RBAC
WebSocket/STOMP
Quartz Scheduler
Rate Limiting
Banco de Dados
MySQL (dados relacionais)
TimescaleDB (séries temporais)
Redis (cache + pub/sub)
Mensageria
Apache Kafka
RabbitMQ (alternativo)
Observabilidade
Prometheus
Grafana
ELK Stack
Spring Actuator
Infraestrutura
Docker
Docker Compose
🚀 Principais Funcionalidades
📡 Telemetria em Tempo Real
Atualização instantânea via WebSocket
Processamento de sensores
Persistência otimizada para séries temporais
API IoT dedicada
🛣 Gestão de Rotas
Planejamento de rotas
Estimativa de chegada (ETA)
Detecção automática de desvios
Alertas inteligentes
🔐 Segurança
JWT com refresh token
RBAC (ADMIN, GESTOR, OPERADOR, MOTORISTA)
MFA para administradores
Auditoria completa
Criptografia em trânsito e repouso
🔥 Funcionalidades Avançadas
Roteamento inteligente (peso, pedágios, trânsito)
Monitoramento de carga (temperatura, umidade, impacto)
Comunicação motorista ↔ gestor
Controle de jornada (conformidade legal)
🛡 Resiliência e Performance
Implementado para operar em ambientes adversos:

Buffer local offline com sincronização posterior
Compressão de dados
Retry com backoff exponencial
Redução adaptativa de frequência
Priorização de eventos críticos
Backpressure
Cache warming
Rate limiting
📊 KPIs (Em evolução)
Eficiência da frota
Consumo médio
Ociosidade
Custo por veículo
Alertas de manutenção
Excesso de velocidade
🧪 Ambiente de Teste
Simulador de GPS
Rotas entre capitais brasileiras
Eventos simulados
Cargas e consumo simulados
🗺 Roadmap
✅ Fase 1 -- MVP
Telemetria básica
Rastreamento GPS
Persistência em banco
Autenticação JWT
🔄 Fase 2 -- Escala e Performance
Integração Kafka
Cache Redis
WebSocket tempo real
Observabilidade completa
🚧 Fase 3 -- Inteligência
Manutenção preditiva
Algoritmo de roteamento inteligente
Análise comportamental de motoristas
🎯 Fase 4 -- Expansão
Machine Learning
Integração com ERPs
Multi-tenant
Internacionalização LATAM
🏆 Diferenciais
🌎 Foco Brasil / LATAM
📡 Offline-first
⚡ Arquitetura preparada para alta escala
💰 Compatível com dispositivos GPS de baixo custo
🎯 Interface simplificada
📌 Status Atual
✔ MVP funcional
✔ MVP robusto
✔ Arquitetura escalável
✔ Preparado para crescimento

Relatório Técnico – Sistema de Telemetria para Frotas de Caminhões
Este projeto tem como objetivo propor e implementar uma arquitetura escalável para um sistema de telemetria voltado a empresas de transporte rodoviário, permitindo o monitoramento eficiente de grandes volumes de dados provenientes de dispositivos embarcados em caminhões.

1. Processamento Concorrente
Foi implementado um modelo baseado em threads, possibilitando a execução paralela de múltiplas tarefas dentro do mesmo processo. Essa abordagem melhora significativamente:

O desempenho geral do sistema

A responsividade das requisições

A eficiência no uso de recursos computacionais

1. Processamento Assíncrono e Fila de Execução
Também foi adotado o processamento assíncrono para execução de tarefas de longa duração, como o tratamento e análise dos dados telemétricos enviados pelos dispositivos embarcados.

Considerando um cenário com até 100.000 caminhões transmitindo dados simultaneamente, a arquitetura foi projetada para evitar sobrecarga imediata do sistema. Para isso, foi implementado um mecanismo de fila de processamento, permitindo o desacoplamento entre a ingestão dos dados e seu processamento efetivo.

Essa estratégia evita gargalos e mantém a estabilidade mesmo sob alta taxa de eventos.

1. Streaming de Eventos
Para suportar alto volume de dados em tempo real, foi integrado o Apache Kafka como plataforma de streaming distribuído.

O uso do Kafka permite:

Alta taxa de throughput (milhões de eventos)

Escalabilidade horizontal

Persistência confiável dos eventos

Processamento paralelo por meio de particionamento

Essa abordagem é adequada para sistemas de telemetria que exigem ingestão contínua e processamento distribuído.

1. Cache em Memória
Foi implementado o Redis como mecanismo de cache em memória.

Em funções de pré-processamento, como preprocessarDadosUrbanos, o sistema consulta primeiramente o Redis antes de acessar o banco de dados relacional. Isso reduz:

Latência de resposta

Carga no banco de dados

Reprocessamento desnecessário

Essa estratégia melhora significativamente o desempenho em cenários de alta leitura.

1. Controle de Fluxo (Rate Limiting)
Foi implementado um mecanismo de rate limiting, responsável por controlar a quantidade de requisições ou eventos permitidos dentro de um intervalo de tempo específico.

Esse mecanismo garante:

Proteção contra sobrecarga

Estabilidade operacional

Controle de fluxo

Isolamento de falhas causadas por dispositivos com comportamento anômalo

📘 Relatório Técnico
Sistema de Telemetria para Frotas de Caminhões com Integração ao OSRM
1️⃣ Visão Geral do OSRM
O OSRM (Open Source Routing Machine) é um motor de cálculo de rotas open-source baseado nos dados do:

➡ OpenStreetMap

Ele fornece funcionalidades similares a serviços como Google Maps e Mapbox, porém com a vantagem de ser self-hosted e sem limites comerciais quando executado em infraestrutura própria.

Principais funcionalidades
✔ Cálculo da melhor rota

✔ Distância total

✔ Tempo estimado (ETA)

✔ Geometria detalhada da via (GeoJSON)

✔ Snap-to-road

✔ Map Matching

✔ Matriz de distâncias (table)

2️⃣ Funcionamento Interno
O fluxo de processamento do OSRM ocorre em seis etapas principais:

Download do mapa em formato .osm.pbf

Execução do osrm-extract

Aplicação de perfil de roteamento (ex: car.lua)

Execução do osrm-contract (Hierarquia de Contração - CH)

Inicialização do servidor (osrm-routed)

Consumo via API HTTP

Pipeline técnico
OSM (.pbf)
↓
Extract
↓
Contract (CH)
↓
Servidor HTTP
↓
Backend Telemetria

3️⃣ Exemplo de Chamada HTTP
Endpoint:
GET /route/v1/driving/lon1,lat1;lon2,lat2

Exemplo público:
<https://router.project-osrm.org/route/v1/driving/-56.0974,-15.6014;-56.1200,-15.6500?overview=full&geometries=geojson>

Resposta simplificada:
{
"routes": [
{
"distance": 12450.3,
"duration": 845.2,
"geometry": {
"coordinates": [
[-56.0974, -15.6014],
[-56.0980, -15.6020]
]
}
}
]
}

4️⃣ Aplicação no Sistema de Telemetria
Integração direta com o serviço de detecção de desvio de rota.

Problema tradicional
Sem motor de roteamento:

Comparação por linha reta

Falsos positivos de desvio

GPS impreciso

Dificuldade em calcular ETA real

Com OSRM integrado
✔ Rota real baseada na malha viária
✔ Snap do veículo à via correta
✔ Cálculo preciso de ETA
✔ Detecção real de desvio
✔ Correção de ruído de GPS

5️⃣ Principais Modos de Operação
🔹 1. route
Calcula rota entre dois ou mais pontos.

Uso principal:

ETA

Planejamento

Visualização de trajeto

🔹 2. nearest
Retorna a via mais próxima de uma coordenada.

Uso principal:

Snap-to-road

Correção de erro de GPS

🔹 3. match (Map Matching)
Endpoint:

/match/v1/driving/lon1,lat1;lon2,lat2;lon3,lat3

Função:

Ajusta sequência de pontos GPS na via correta
Aplicação crítica para telemetria:

✔ Corrige imprecisão de GPS
✔ Evita falsos alertas de desvio
✔ Reconstrói trajetória real
✔ Base para auditoria de percurso

Esse é o modo mais poderoso para frotas.

6️⃣ Arquitetura Recomendada
Estrutura modular
[Dispositivos GPS]
↓
[Telemetria Service]
↓
[Roteamento Service]
↓
[OSRM]

Separar o roteamento em microserviço permite:

Escalabilidade independente

Cache dedicado

Controle de carga

Evolução futura (ex: perfis caminhão pesado)

7️⃣ Requisitos de Infraestrutura
O OSRM carrega em memória:

Grafo da malha viária

Índices espaciais (R-tree)

Hierarquia de contração

RAM impacta diretamente:
Latência

Throughput

Estabilidade

Porém, RAM não é suficiente
Também é necessário:

CPU multi-core (8–32 cores ideal)

SSD NVMe

Backend assíncrono

Pool de conexões HTTP otimizado

Estratégia de cache

8️⃣ Escalabilidade
Para suportar ~1000 veículos simultâneos:

Recomenda-se:

Cache Redis para rotas repetidas

Rate limiting interno

Monitoramento (CPU/RAM)

Instâncias paralelas do OSRM

Balanceador de carga

9️⃣ Conclusão Técnica
A integração do OSRM transforma o sistema de telemetria de:

Rastreamento básico

Para:

Plataforma inteligente de análise logística

Ele possibilita:

Monitoramento avançado

Detecção precisa de desvio

Cálculo real de desempenho operacional

Base tecnológica para expansão comercial

*Gerado em: 10/03/2026 | Sistema de Telemetria para Frotas v3.0*
