# Integration Engine SENATRAN

Módulo Maven responsável pela consulta veicular SENATRAN/SERPRO. O namespace
base é `com.telemetria.integration.senatran.serpro`.

- `api`: endpoint, filtros e tratamento de falhas HTTP;
- `application`: caso de uso e porta do cliente de consulta;
- `domain`: contratos, validações e exceções de negócio;
- `infrastructure`: cliente InfoSimples, cache, configuração, observabilidade
  e política de resiliência.

Tokens e dados veiculares não devem ser registrados integralmente em logs. A
configuração de credenciais deve ser fornecida por ambiente, nunca por código.
