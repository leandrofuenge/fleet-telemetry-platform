# Escopo do Integration Engine

O serviço mantém exclusivamente as integrações abaixo:

- **CT-e** — autorização, consulta, eventos, status, assinatura e transporte SOAP SEFAZ.
- **NF-e** — autorização, eventos, inutilização e distribuição de documentos fiscais eletrônicos.
- **SERPRO** — consulta veicular pela integração SENATRAN/Infosimples.

## Organização

```text
API HTTP
  ├── /api/integracoes/sefaz/cte       -> CT-e
  ├── /api/integracoes/sefaz/nfe       -> NF-e
  └── /api/integracoes/senatran/serpro -> SERPRO

Controller -> serviço de aplicação -> rota/processadores Camel -> cliente externo
```

As configurações preservadas cobrem certificados e TLS SEFAZ, endpoints CT-e/NF-e, credenciais SERPRO, segurança HTTP, persistência, logs e monitoramento. Não há fluxos ativos de ANTT, MDF-e, NFC-e, transferência Base64, workflow de viagem ou consumo de telemetria.
