# Arquitetura atual

## Limites de serviço

| Serviço | Responsabilidade | Persistência própria |
| --- | --- | --- |
| `telemetry-service` | Ingestão de telemetria, alertas, posição atual e integrações IoT | `telemetria` |
| `routing-service` | Rotas, viagens, pontos de entrega e caches de roteamento | `fleet_routing` |

Os serviços se comunicam por APIs e eventos. Cada um mantém suas próprias migrations e não deve acessar o banco do outro diretamente.

## Organização interna

```text
api/             Adaptadores HTTP/WebSocket: controllers e DTOs
application/     Casos de uso, inicializadores e agendamentos
domain/          Entidades, regras e portas de negócio
infrastructure/  Persistência, cache, mensageria e clientes externos
```

A direção de dependência deve apontar para o domínio. Interfaces de repositório e gateways pertencem ao domínio ou à aplicação; suas implementações ficam em `infrastructure`.

## Desenvolvimento local

O ambiente compartilhado está em `deploy/compose/`. Os serviços não possuem Compose próprio para evitar instâncias duplicadas de PostgreSQL, Redis, Kafka e Mosquitto.

`database/reference-schemas/` contém esquemas históricos e de referência. Antes de usar qualquer um em produção, converta-o em uma migration versionada no serviço responsável.
