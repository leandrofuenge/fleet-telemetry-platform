# Fluxos do Integration Engine

## Princípios de organização

- As classes `api`/controllers recebem HTTP e não contêm regra de integração.
- As classes `application` coordenam o caso de uso, propagam correlação e gravam auditoria.
- As rotas Camel conectam etapas e tratam a observabilidade do fluxo.
- Processadores e serviços de domínio fazem uma única transformação ou validação.
- Clientes externos ficam nos pacotes da integração correspondente (`sefaz`, `antt` ou `senatran`).

## Transferência Base64 e SOAP

```text
HTTP POST /api/integracoes/transfer/base64
        |
        v
DataTransferController
        |
        v
DataTransferApplicationService
  - define X-Correlation-ID
  - chama direct:transfer-base64
  - persiste sucesso ou falha
        |
        v
DataTransferRoute
  - auditoria de início
  - Base64TransferProcessor
  - auditoria de fim
        |
        v
Base64TransferProcessor
  - TransferPayloadDecoder: texto/Base64/GZIP -> conteúdo
  - DocumentoFiscalXmlValidator: valida XML fiscal quando solicitado
  - TransferResponseFactory: Base64/GZIP/SOAP -> resposta
```

O conteúdo completo não é persistido na auditoria; é salvo apenas o hash SHA-256 e metadados de tamanho. A correlação enviada no cabeçalho `X-Correlation-ID` é devolvida na resposta e usada nos logs Camel.

## Demais integrações

| Contexto | Entrada | Orquestração | Saída externa |
| --- | --- | --- | --- |
| SEFAZ CT-e | `SefazCteController` | `CteRoute` e pipeline CT-e | SOAP SEFAZ |
| SEFAZ NF-e | `NfeController` | serviço de aplicação NF-e | SOAP/serviço SEFAZ |
| ANTT | rotas `CiotRouteBuilder` e `RntrcRouteBuilder` | Camel | serviços ANTT/SEFAZ |
| SENATRAN/Serpro | `SerproConsultaController` | `SerproConsultaService` | API Serpro |
| Jornada de viagem | `WorkflowController` | `InicioViagemWorkflowRoute` | telemetria e serviços associados |

Novos fluxos devem seguir esta sequência: **controller -> application service -> route/processors -> client externo**, sem acesso HTTP ou banco diretamente dentro de uma rota quando houver um serviço de aplicação apropriado.

## Orquestração com o telemetry-service

```text
Dispositivo/API -> telemetria-raw -> telemetry-service
                                      |
                                      | persiste telemetria + outbox na mesma transação
                                      v
                                telemetria-events (Kafka)
                                      |
                                      v
                         integration-engine / grupo independente
                                      |
                                      +-> valida contrato e elimina duplicidade por eventId
                                      +-> registra receipt no PostgreSQL
                                      +-> direct:telemetria-integration-event (Camel)
```

O tópico é `telemetria-events`, a chave Kafka é o identificador do veículo e o grupo consumidor padrão é `integration-engine-telemetry-v1`. O consumo pode ser ativado com `INTEGRATION_TELEMETRY_EVENTS_ENABLED=true`; ele já é ativado no `docker-compose.yml`.
