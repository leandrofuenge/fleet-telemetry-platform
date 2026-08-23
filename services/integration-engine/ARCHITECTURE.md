# Integration Engine

O catálogo completo está em [`docs/flow-catalog.md`](docs/flow-catalog.md).

## Direção das dependências

`api -> application/service -> validation/guard -> client/transport -> sistema externo`

Rotas Camel orquestram etapas, auditoria e integração assíncrona. Regras fiscais
ficam nos services e guards, nunca em controllers ou rotas.

## Regras

1. Controllers validam o contrato HTTP e delegam.
2. Rotas Camel orquestram; não implementam regras fiscais.
3. Guards autorizam operações com efeito fiscal.
4. Builders criam payloads; validators não os alteram.
5. Clients representam sistemas externos; transports fazem I/O.
6. Parsers convertem respostas em objetos de domínio.
7. Repositories são a fronteira de persistência.
8. Rotas experimentais exigem habilitação explícita.
9. Logs não contêm credenciais, certificados ou XML fiscal completo.

Todo componente exclusivo de CT-e fica abaixo de
`com.telemetria.integration.sefaz.cte`.
