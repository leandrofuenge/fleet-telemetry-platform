# Catálogo de fluxos

## Fluxo padrão

```text
HTTP API
  -> caso de uso ou rota Camel
    -> serviço
      -> validação e guard
        -> client e transport
          -> sistema externo
      -> parser
    -> persistência e auditoria
  -> resposta HTTP
```

Domínio não conhece Spring, Camel ou HTTP. Clients não conhecem controllers,
rotas ou repositories.

## Pacotes

| Pacote | Responsabilidade |
|---|---|
| `antt` | CIOT, RNTRC, piso mínimo, seguro e vale-pedágio |
| `senatran` | CRLV-e, RENACH, RENAINF, RENAVAM e SERPRO/RADAR |
| `sefaz` | CT-e, NF-e, NFC-e, MDF-e, certificados e eventos |
| `workflow` | Coordenação entre mais de um órgão |
| `datatransfer` | Base64 e envelopes genéricos |
| `security` | Assinatura e validação criptográfica XML |
| `support` | Auditoria, erros e contratos transversais |

## CT-e

Todos os componentes exclusivos de CT-e estão sob
`com.telemetria.integration.sefaz.cte`:

```text
cte
  api            entrada HTTP e ProblemDetail
  autorizacao    cliente do ciclo fiscal
  consulta       construção e caso de uso de consulta
  evento         construção e caso de uso de eventos
  status         contrato legado de disponibilidade
  validation     XSD, XML, regras e guards fiscais
  soap           protocolo e transporte HTTPS/mTLS
  pipeline       etapas unitárias do processamento
  route          orquestração Camel
  retorno        resultados tipados e códigos SEFAZ
  domain         estado interno do processamento
  persistence    histórico e estado persistido
  config         configuração e health
```

### Lote estável

```text
direct:processarCteLote
  -> CteItemProcessor
  -> CtePersistenceProcessor
  -> CteXsdValidator
  -> CteBusinessValidator
  -> CteSignatureProcessor
  -> CteSefazSender
  -> CteSefazResponseProcessor
```

### Operações síncronas

```text
autorização: guard -> XML/XSD -> assinatura -> SOAP -> HTTPS/mTLS -> parser
consulta: chave -> builder -> CteClient -> HTTPS/mTLS -> parser
cancelamento: guard -> parâmetros -> builder -> assinatura -> CteClient -> parser
```

Autorização e cancelamento ficam bloqueados por padrão. A habilitação exige
flags fiscais, certificado válido e ambiente autorizado.

### Status CT-e

`GET /api/integracoes/sefaz/cte/status` passa por `direct:cte-status` e
`CteStatusService`. O webservice histórico de status foi descontinuado no CT-e
4.00. Com simulação retorna `107`; sem simulação retorna `000` e
`disponivel=false`. O resultado não comprova disponibilidade real da SEFAZ.

### Rotas experimentais

`CteExperimentalRoutes` só é carregada com
`INTEGRATION_EXPERIMENTAL_ROUTES_ENABLED=true`. Não deve ser habilitada em
produção antes de idempotência, persistência e retentativa completas.

## NF-e

```text
NfeController
  -> NfeApplicationService
    -> NfeFiscalOperationGuard (operações mutáveis)
    -> NfeXmlPayloadValidator e XmlSignatureValidator
    -> NfeClient
      -> NfeSoapGateway -> HTTPS/mTLS -> SEFAZ
      -> NfeSoapResponseValidator
```

`NfeApplicationService` concentra os casos de uso expostos pela API: status,
consulta por chave, recibo, autorização, eventos, inutilização e distribuição DFe.
Os pacotes `integration.nfe.schemas` e `integration.nfe.wsdl` são fontes gerados e
ficam fora da orquestração; não devem ser editados manualmente.

## Início de viagem

```text
POST /api/integracoes/workflow/iniciar-viagem
  -> direct:iniciar-viagem -> auditoria
  -> motorista local
  -> SENATRAN/SERPRO quando houver RENAVAM
  -> ANTT/RNTRC quando houver documento do transportador
  -> status CT-e -> LIBERADA ou BLOQUEADA -> auditoria
```

Fora da simulação, a etapa conservadora de status CT-e bloqueia o workflow.

## SERPRO/RADAR

```text
HTTP API -> correlação e API key -> service -> cache/resiliência
  -> client -> fornecedor -> resposta normalizada
```

## Flags operacionais

| Flag | Padrão | Efeito |
|---|---:|---|
| `INTEGRATION_SIMULATION_ENABLED` | `false` | Respostas simuladas explícitas |
| `INTEGRATION_EXPERIMENTAL_ROUTES_ENABLED` | `false` | Carrega protótipos Camel |
| `SEFAZ_CTE_AUTHORIZATION_ENABLED` | `false` | Libera autorização fiscal |
| `SEFAZ_CTE_CANCELLATION_ENABLED` | `false` | Libera cancelamento fiscal |
| `SERVER_SSL_ENABLED` | `false` | TLS direto no serviço |
| `SECURITY_REQUIRE_HTTPS` | valor de SSL | Exige canal seguro |

## Inclusão de fluxos

1. definir request e response;
2. criar caso de uso;
3. isolar I/O no client/transport;
4. aplicar guard antes de efeitos fiscais;
5. adicionar idempotência antes de retentativas;
6. registrar métricas sem payload ou segredo;
7. expor por controller ou rota;
8. testar contrato, falha e timeout.
