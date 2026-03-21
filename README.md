# 🚛 Sistema de Monitoramento de Frotas

<div align="center">

![Java](https://img.shields.io/badge/Java-17+-blue?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-brightgreen?style=for-the-badge&logo=springboot)
![Kafka](https://img.shields.io/badge/Kafka-Streaming-black?style=for-the-badge&logo=apachekafka)
![Redis](https://img.shields.io/badge/Redis-Cache-red?style=for-the-badge&logo=redis)
![Docker](https://img.shields.io/badge/Docker-Container-blue?style=for-the-badge&logo=docker)
![MySQL](https://img.shields.io/badge/MySQL-Database-orange?style=for-the-badge&logo=mysql)
![Status](https://img.shields.io/badge/Status-MVP_Robusto-success?style=for-the-badge)

**Plataforma de telemetria e rastreamento em tempo real para frotas de caminhões e carretas, com foco no mercado brasileiro e LATAM.**

[Funcionalidades](#-funcionalidades) · [Arquitetura](#-arquitetura) · [Tech Stack](#-tech-stack) · [Banco de Dados](#-banco-de-dados) · [Roadmap](#-roadmap)

</div>

---

## 📌 Visão Geral

Sistema de monitoramento em tempo real projetado para empresas de transporte rodoviário, capaz de processar dados de até **100.000 veículos simultaneamente**. A plataforma combina telemetria IoT, roteamento inteligente via OSRM e uma arquitetura de microserviços orientada a eventos.

### Pilares do Sistema

| Pilar | Descrição |
|---|---|
| 📡 **Telemetria em Tempo Real** | Processamento contínuo de dados IoT (GPS, OBD-II, DMS) via WebSocket |
| 🛣️ **Roteamento Inteligente** | Integração com OSRM para rotas reais, ETA preciso e detecção de desvios |
| 🔧 **Manutenção Preditiva** | Análise comportamental e alertas preventivos baseados em sensores |
| 🌐 **Offline-First** | Buffer local com sincronização posterior para áreas de baixa conectividade |
| 🔐 **Segurança Corporativa** | JWT + RBAC + MFA + auditoria completa + criptografia em trânsito e repouso |
| 📊 **Observabilidade** | Stack completa com Prometheus, Grafana e ELK |

---

## 🏗 Arquitetura

```
┌─────────────────────────────────────────┐
│         Dispositivos IoT / GPS          │
│    (OBD-II · DMS · Rastreadores GPS)    │
└──────────────────┬──────────────────────┘
                   │ MQTT / HTTP
                   ▼
┌─────────────────────────────────────────┐
│        API Gateway (Spring Cloud)       │
│   Rate Limiting · Auth · Load Balance   │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│     Microserviços (Spring WebFlux)      │
│                                         │
│  telemetry │ routing │ alert │ vehicle  │
│  auth      │ driver  │ billing │ ota    │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│      Event Streaming (Apache Kafka)     │
└────────┬──────────────────┬────────────┘
         │                  │
         ▼                  ▼
┌────────────────┐  ┌───────────────────┐
│  Processadores │  │   Cache (Redis)   │
│   + Workers    │  │   Pub/Sub + TTL   │
└───────┬────────┘  └─────────┬─────────┘
        │                     │
        ▼                     ▼
┌────────────────┐  ┌───────────────────┐
│  MySQL (OLTP)  │  │  TimescaleDB      │
│  Dados         │  │  Séries Temporais │
│  Relacionais   │  │  (Telemetria)     │
└────────────────┘  └───────────────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │  Frontend React       │
              │  Mapas · Dashboard    │
              │  WebSocket Realtime   │
              └───────────────────────┘
```

---

## 🔧 Tech Stack

### Backend

- **Java 17+** com **Spring Boot 3.x**
- **Spring WebFlux** — programação reativa e não-bloqueante
- **Spring Data JPA** — persistência ORM
- **JWT + RBAC** — autenticação e autorização por papéis
- **WebSocket / STOMP** — comunicação bidirecional em tempo real
- **Quartz Scheduler** — agendamento de tarefas (ex: resumo diário às 01h)
- **Rate Limiting** — proteção contra sobrecarga por dispositivo

### Banco de Dados

- **MySQL 8** — dados relacionais e transacionais
- **TimescaleDB** — séries temporais de telemetria (alta performance)
- **Redis** — cache em memória + pub/sub + sessões

### Mensageria

- **Apache Kafka** — streaming distribuído de alto throughput
- **RabbitMQ** — alternativa para filas de tarefas pontuais

### Roteamento

- **OSRM (Open Source Routing Machine)** — motor de rotas self-hosted baseado em OpenStreetMap
  - Cálculo de rota real (não linha reta)
  - Map Matching para correção de GPS
  - ETA preciso
  - Snap-to-road

### Observabilidade

- **Prometheus** — coleta de métricas
- **Grafana** — dashboards operacionais
- **ELK Stack** — logs centralizados (Elasticsearch + Logstash + Kibana)
- **Spring Actuator** — health checks e métricas da JVM

### Infraestrutura

- **Docker + Docker Compose** — containerização e orquestração local
- **Spring Cloud Gateway** — API Gateway
- **JITP (Just-in-Time Provisioning)** — lifecycle de dispositivos IoT

---

## 🚀 Funcionalidades

### 📡 Telemetria em Tempo Real

- Ingestão de dados de sensores: GPS, velocidade, rotação, temperatura, combustível, impacto
- Atualização instantânea via WebSocket para o frontend
- Persistência otimizada para séries temporais no TimescaleDB
- API IoT dedicada com autenticação por dispositivo (JITP → revogação)
- Heartbeat e lifecycle completo dos rastreadores

### 🛣️ Gestão de Rotas (OSRM)

- Planejamento de rotas com perfis de veículo pesado
- ETA calculado sobre malha viária real
- Detecção automática de desvio com tolerância configurável
- Map Matching para reconstrução fidedigna do trajeto
- Prova de entrega (assinatura + foto) nos pontos de parada
- Relatório pós-viagem consolidado

### 🔐 Segurança

- **RBAC** com 4 papéis: `ADMIN`, `GESTOR`, `OPERADOR`, `MOTORISTA`
- **MFA** obrigatório para administradores
- JWT com refresh token automático
- Auditoria completa de ações
- Criptografia em trânsito (TLS) e em repouso (AES-256)
- **Multi-tenancy**: isolamento total de dados por empresa

### 🔥 Funcionalidades Avançadas

- Roteamento com restrições de peso, pedágios e trânsito
- Monitoramento de carga: temperatura, umidade e impacto
- Comunicação bidirecional motorista ↔ gestor
- Controle de jornada conforme **Lei 12.619/2012**
- Geofences configuráveis por tenant com alertas automáticos
- Resumo diário por veículo gerado automaticamente

---

## 🛡️ Resiliência e Performance

O sistema é projetado para ambientes adversos comuns no transporte rodoviário brasileiro:

| Mecanismo | Descrição |
|---|---|
| **Buffer Offline** | Armazena eventos localmente e sincroniza ao reconectar |
| **Retry com Backoff** | Exponential backoff para falhas de rede transitórias |
| **Compressão de Dados** | Reduz consumo de banda em conexões limitadas |
| **Frequência Adaptativa** | Reduz cadência de envio em baixa conectividade |
| **Priorização de Eventos** | Eventos críticos (acidente, desvio) têm preferência na fila |
| **Backpressure** | Kafka absorve picos sem derrubar consumidores |
| **Cache Warming** | Redis pré-carregado para rotas e veículos frequentes |
| **Rate Limiting** | Isola dispositivos com comportamento anômalo |

---

## 📊 KPIs Monitorados

- ⛽ Consumo médio de combustível por veículo/rota
- ⏱️ Ociosidade e tempo de parada
- 💰 Custo operacional por veículo
- 🚨 Alertas de manutenção preventiva
- 🏎️ Excesso de velocidade e eventos de aceleração brusca
- 📦 Eficiência de entrega (ETA vs. realizado)
- 🛣️ Desvios de rota por motorista

---

## 🗄️ Banco de Dados

### Estrutura dos Microserviços

| Microserviço | Banco | Principais Tabelas |
|---|---|---|
| `telemetry-service` | `telemetry_db` | `telemetria`, `posicao_atual`, `alertas`, `jornadas`, `geofences`, `dispositivos_iot` |
| `routing-service` | `routing_db` | `rotas`, `viagens`, `pontos_entrega`, `relatorio_viagem`, `osrm_route_cache` |
| `auth-service` | `auth_db` | `usuarios`, `tokens`, `auditoria`, `permissoes` |
| `vehicle-service` | `vehicle_db` | `veiculos`, `documentos`, `historico_manutencao` |
| `alert-service` | `alert_db` | `alertas`, `regras`, `notificacoes` |
| `maintenance-service` | `maintenance_db` | `ordens_servico`, `checklist`, `pecas` |
| `billing-service` | `billing_db` | `contratos`, `faturas`, `consumo` |
| `ota-service` | `ota_db` | `firmware`, `atualizacoes`, `dispositivos` |

### Sincronização via Kafka (Cache Local)

Cada microserviço mantém cópias locais (`veiculos_cache`, `motoristas_cache`) atualizadas via **Kafka Event Sourcing**:

```java
@KafkaListener(topics = "vehicle.events", groupId = "telemetry-service")
public void onVehicleEvent(VehicleEvent event) {
    VeiculoCache cache = veiculoCacheRepository
        .findById(event.getId())
        .orElse(new VeiculoCache());
    veiculoCacheRepository.save(mapper.toCache(event));
}
```

### Topics Kafka

| Topic | Producer | Consumers |
|---|---|---|
| `telemetry.raw` | MQTT Gateway | telemetry-service |
| `telemetry.processed` | telemetry-service | alert-service, ml-service |
| `route.events` | routing-service | telemetry-service, alert-service |
| `vehicle.events` | vehicle-service | telemetry-service, routing-service |
| `driver.events` | driver-service | telemetry-service, routing-service |
| `alerts.generated` | alert-service | notification-service |
| `deviation.detected` | telemetry-service | routing-service, alert-service |

---

## ⚙️ Configuração

### application.yml (telemetry-service)

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
      ddl-auto: validate        # Schema gerenciado pelo Flyway
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        default_batch_fetch_size: 50
  kafka:
    bootstrap-servers: ${KAFKA_SERVERS:localhost:9092}
    consumer:
      group-id: telemetry-service
      auto-offset-reset: earliest
```

### Dependências Maven

```xml
<!-- Spring Boot JPA -->
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

<!-- Hibernate JSON Types -->
<dependency>
    <groupId>io.hypersistence</groupId>
    <artifactId>hypersistence-utils-hibernate-63</artifactId>
    <version>3.7.0</version>
</dependency>
```

---

## 🧪 Ambiente de Testes

O sistema inclui um simulador completo para desenvolvimento e QA:

- **Simulador de GPS** — gera coordenadas reais entre capitais brasileiras
- **Eventos simulados** — acelerações, frenagens, desvios, alertas
- **Carga simulada** — variações de temperatura, umidade e impacto
- **Consumo simulado** — perfis de consumo realistas por tipo de rota

---

## 🗺️ Roadmap

### ✅ Fase 1 — MVP (Concluído)

- [x] Telemetria básica e rastreamento GPS
- [x] Persistência em banco relacional
- [x] Autenticação JWT com RBAC

### 🔄 Fase 2 — Escala e Performance (Em andamento)

- [x] Integração Apache Kafka
- [x] Cache Redis com pub/sub
- [x] WebSocket em tempo real
- [ ] Observabilidade completa (Prometheus + Grafana)

### 🚧 Fase 3 — Inteligência (Planejado)

- [ ] Manutenção preditiva com ML
- [ ] Algoritmo de roteamento com restrições de caminhão pesado
- [ ] Análise comportamental de motoristas

### 🎯 Fase 4 — Expansão (Futuro)

- [ ] Machine Learning para previsão de falhas
- [ ] Integração com ERPs (SAP, TOTVS)
- [ ] Multi-tenant SaaS completo
- [ ] Internacionalização LATAM (ES, EN)

---

## 🏆 Diferenciais

- 🌎 **Foco Brasil / LATAM** — conformidade com legislação brasileira (Lei 12.619, LGPD)
- 📡 **Offline-First** — operacional mesmo em rodovias sem sinal
- ⚡ **Alta Escala** — arquitetura preparada para 100k+ veículos simultâneos
- 💰 **Hardware Acessível** — compatível com rastreadores GPS de baixo custo
- 🗺️ **OSRM Self-Hosted** — sem custo por requisição, sem dependência de APIs externas
- 🎯 **Interface Simplificada** — UX focada no operador de frota, não no desenvolvedor

---

## 📌 Status Atual

| Item | Status |
|---|---|
| MVP Funcional | ✅ Concluído |
| MVP Robusto | ✅ Concluído |
| Arquitetura Escalável | ✅ Implementado |
| Testes e Simulador | ✅ Operacional |
| Observabilidade Completa | 🔄 Em andamento |
| Fase 3 — Inteligência | 🚧 Planejado |

---

<div align="center">

**Sistema de Telemetria para Frotas v3.0**
Java 17 · Spring Boot 3.x · MySQL · Kafka · Redis · OSRM

</div>
