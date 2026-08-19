# Estado de homologação

O módulo compila e executa a suíte automatizada, mas uma integração só deve ser habilitada em produção depois que seu contrato externo for validado.

## Proteções padrão

- `INTEGRATION_SIMULATION_ENABLED=false`: respostas simuladas ficam desligadas.
- `INTEGRATION_EXPERIMENTAL_ROUTES_ENABLED=false`: rotas com contratos ainda não homologados não são registradas.
- Tokens e senhas devem ser fornecidos por variáveis de ambiente ou cofre de segredos.

## Adaptadores implementados

Os módulos Vale-Pedágio, Seguro, RENAVAM, RENAINF, CRLV-e, RENACH, eventos fiscais,
NFC-e e portais CT-e/MDF-e possuem contratos internos, serviços Spring e um adaptador
HTTP configurável. Cada chamada exige endpoint e token; configuração ausente interrompe
o fluxo antes de qualquer acesso de rede. O objeto `operation` deve usar um dos prefixos:

- `antt.vale-pedagio.*` ou `antt.seguro.*`
- `senatran.renavam.*`, `senatran.renainf.*`, `senatran.crlve.*` ou `senatran.renach.*`
- `sefaz.evento.*`, `sefaz.nfce.*`, `sefaz.portal-cte.*` ou `sefaz.portal-mdfe.*`

O mapeamento definitivo dos campos de `data` continua condicionado ao contrato oficial
de cada órgão. O adaptador não fabrica respostas quando o serviço não está disponível.

## Dependências externas pendentes

### SEFAZ CT-e

- Certificado A1 de homologação e respectiva cadeia de confiança.
- Massa de testes para autorização, consulta e eventos.
- Credenciamento e liberação de rede perante o autorizador da UF.

#### Pacote pré-pronto para homologação CT-e

O perfil `cte-homologation` valida no início os endpoints HTTPS por operação, o
ambiente, certificado A1, truststore, senhas, XMLDSig e XSD oficiais. Para executar
sem gravar certificados na imagem ou no repositório:

1. Copie `services/integration-engine/cte-homologation.env.example` para
   `.env.cte-homologation` (arquivo não versionado).
2. Informe caminhos absolutos do host em `SEFAZ_CERT_HOST_PATH` e
   `SEFAZ_TRUSTSTORE_HOST_PATH`, além das senhas e URLs reais de homologação.
3. Valide o Compose com
   `docker compose --env-file .env.cte-homologation -f docker-compose.yml -f docker-compose.cte-homologation.yml config`.
4. Suba o serviço usando os mesmos argumentos e acrescente
   `up --build integration-engine`.
5. Execute
   `powershell -ExecutionPolicy Bypass -File services/integration-engine/scripts/smoke-cte-homologation.ps1`.

O override monta certificado e truststore separadamente em `/run/secrets`, somente
para leitura, e habilita `no-new-privileges`. O smoke test consulta somente health e
status do serviço; ele falha se detectar simulação e não transmite CT-e ou evento.
Em servidores Linux, os arquivos também precisam ter proprietário/permissões que
permitam leitura pelo usuário não-root do contêiner. Senhas devem vir do cofre de
segredos da plataforma, nunca do arquivo versionado.

Emissão/autorização e cancelamento permanecem bloqueados por padrão, inclusive com
o perfil ativo. A liberação exige simultaneamente certificado A1 válido, confirmação
da massa fiscal autorizada (`SEFAZ_CTE_AUTHORIZED_FISCAL_TEST_DATA=true`) e a flag da
operação (`SEFAZ_CTE_AUTHORIZATION_ENABLED=true` ou
`SEFAZ_CTE_CANCELLATION_ENABLED=true`). Consulta e status não dependem dessas flags.

### SENATRAN/SERPRO

- Decisão formal entre API oficial SERPRO e fornecedor InfoSimples.
- Credenciamento, contrato, token/certificado e catálogo autorizado.
- Exemplos oficiais de request, response e códigos de erro.

#### Pacote pré-pronto para homologação

O pacote `com.telemetria.integration.senatran.serpro` possui perfil fail-fast,
endpoint protegido, health indicator, métricas, cache anonimizado, retry, circuit
breaker, rate limit e contrato OpenAPI. Para ativar:

1. Copie `serpro-homologation.env.example` para um arquivo `.env` não versionado ou
   cadastre as mesmas chaves no cofre de segredos.
2. Substitua apenas `INFOSIMPLES_SERPRO_RADAR_URL`, `INFOSIMPLES_TOKEN` e
   `INFOSIMPLES_SERPRO_INTERNAL_API_KEY`.
3. Ative `SPRING_PROFILES_ACTIVE=docker,homologation` (ou
   `INTEGRATION_PROFILES=docker,homologation` no Compose).
4. Suba com `docker compose up --build integration-engine`.
5. Verifique `/actuator/health`, `/actuator/prometheus` e `/openapi-serpro.yaml`.
6. Execute `scripts/smoke-serpro-homologation.ps1` com uma massa autorizada.

O perfil `homologation` recusa inicialização quando uma variável obrigatória está
ausente, contém placeholder, possui menos de 16 caracteres, quando os dois segredos
são iguais ou quando o endpoint externo não é uma URL HTTPS absoluta.

### ANTT

- WSDL/OpenAPI e namespaces oficiais para RNTRC e CIOT.
- Credenciais de homologação e produção.
- Códigos de retorno, regras de idempotência e massa de testes.

## Critério para habilitar uma rota experimental

1. Remover placeholders e namespaces de exemplo.
2. Adicionar teste de contrato com resposta aprovada, rejeitada e indisponível.
3. Validar timeout, retry e idempotência.
4. Confirmar que logs não expõem CPF, CNPJ, RENAVAM, XML assinado ou segredos.
5. Executar homologação no ambiente do órgão ou fornecedor.
6. Somente então definir `INTEGRATION_EXPERIMENTAL_ROUTES_ENABLED=true` no ambiente autorizado.
