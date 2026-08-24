# Integration Engine CT-e

Módulo Maven dedicado às integrações CT-e da SEFAZ. O namespace público é
`com.telemetria.integration.sefaz.cte`.

- `api` e `application`: entrada HTTP e casos de uso;
- `autorizacao`, `consulta`, `evento`, `status` e `portal`: operações fiscais;
- `pipeline` e `route`: orquestração Apache Camel;
- `soap`: transporte e envelopes SOAP;
- `domain`, `retorno` e `infosimples`: contratos de domínio e respostas;
- `persistence`, `security`, `validation`, `config` e `util`: infraestrutura.

O módulo usa componentes compartilhados do `integration-engine`. A extração
futura desses componentes para um módulo `integration-engine-core` permitirá
remover essa dependência transitória sem duplicar código.
