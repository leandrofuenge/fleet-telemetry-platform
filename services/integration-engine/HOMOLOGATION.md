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
- URLs por UF, ambiente e operação.
- Schemas XSD oficiais versionados.
- Massa de testes para autorização, consulta e eventos.
- Validação da assinatura XMLDSig contra o autorizador.

### SENATRAN/SERPRO

- Decisão formal entre API oficial SERPRO e fornecedor InfoSimples.
- Credenciamento, contrato, token/certificado e catálogo autorizado.
- Exemplos oficiais de request, response e códigos de erro.

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
