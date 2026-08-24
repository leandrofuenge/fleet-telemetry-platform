# Plataforma de Telemetria de Frotas

Backend para monitoramento de frotas, composto atualmente pelos serviços de telemetria e roteamento.

## Estrutura

```text
services/       Serviços Spring Boot independentes
deploy/compose/ Ambiente local compartilhado (Docker Compose e configurações)
database/       Esquemas SQL de referência; migrations executáveis ficam no serviço dono
docs/           Arquitetura, visão de produto e relatórios
var/            Estado local, logs e metadados de IDE — não versionados
```

## Serviços implementados

- `integration-engine-telemetry-service` — ingestão e processamento de telemetria, alertas e integrações MQTT/Kafka.
- `routing-service` — cálculo e gestão de rotas, viagens e caches de roteamento.

## Subir o ambiente local

1. Copie `deploy/compose/.env.example` para `deploy/compose/.env` e preencha valores locais fortes.
2. Execute:

   ```bash
   docker compose --env-file deploy/compose/.env -f deploy/compose/docker-compose.yml up --build
   ```

O Compose central inicia PostgreSQL, Redis, Kafka, Mosquitto e os dois serviços. Os logs locais ficam em `var/logs/`.

## Convenções

- Cada serviço é responsável por suas migrations em `src/main/resources/db/migration`.
- Contratos entre serviços devem ser explícitos; não compartilhe entidades JPA entre eles.
- `domain` não depende de implementações de banco, mensageria ou HTTP; essas adaptações pertencem a `infrastructure`.
- Segredos ficam em `.env` local ou no gerenciador de segredos do ambiente, nunca no repositório.

Consulte [a arquitetura atual](docs/architecture.md) e [a visão de arquitetura planejada](docs/architecture-vision.md).

O estado de implementação dos requisitos de cadastros base está em [RF01–RF04](docs/implementacao-rf01-rf04.md).
## Implementações funcionais

As entregas concluídas estão documentadas em [RF01–RF04](docs/implementacao-rf01-rf04.md), [RF05–RF12](docs/implementacao-rf05-rf12.md), [RF14–RF21](docs/implementacao-rf14-rf21.md) e [RF22–RF31](docs/implementacao-rf22-rf31.md).
