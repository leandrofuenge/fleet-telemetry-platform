# Integration Engine

## Fluxo técnico

Cada funcionalidade deve seguir a mesma direção de dependências:

`api/controller -> application/service -> client -> sistema externo`

Rotas Camel ficam em `route` e são usadas para encadeamento, auditoria, retentativas e integração assíncrona. Regras de negócio ficam em `application` ou no `Service` do domínio, nunca dentro do controller ou da rota.

## Estrutura

- `config`: configuração compartilhada do Spring e Camel.
- `datatransfer`: utilitários de transporte de payloads e SOAP.
- `workflow`: casos de uso que coordenam mais de uma integração.
  - `api`: endpoints HTTP.
  - `application`: serviços de aplicação e decisão de negócio.
  - `domain`: contratos de entrada e saída.
  - `route`: rotas Camel de orquestração.
- `antt`, `sefaz`, `senatran`: integrações agrupadas por órgão e produto.
- `security`: assinatura e segurança de documentos.
- `support`: processadores transversais, auditoria e erros.
- `util`: funções puras reutilizáveis.

## Início de viagem

1. O controller recebe a solicitação.
2. A rota Camel registra a execução e delega ao serviço de aplicação.
3. O motorista é validado localmente.
4. O veículo é consultado no SENATRAN/RADAR quando o RENAVAM é informado.
5. O transportador é validado na ANTT/RNTRC quando seu documento é informado.
6. A disponibilidade do CT-e é verificada na SEFAZ.
7. A viagem é liberada somente quando todas as validações aplicáveis forem aprovadas.

Os campos `renavam` e `transportadorDocumento` são opcionais para manter compatibilidade com consumidores existentes. Quando ausentes, as respectivas integrações externas não são chamadas.
