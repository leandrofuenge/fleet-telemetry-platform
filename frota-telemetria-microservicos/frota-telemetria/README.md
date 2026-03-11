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

*Gerado em: 10/03/2026 | Sistema de Telemetria para Frotas v3.0*
