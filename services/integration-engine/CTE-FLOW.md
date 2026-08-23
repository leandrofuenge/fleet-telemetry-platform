# Integração CT-e

## Organização

- `cte.api`: endpoint REST e tratamento de erros.
- `cte.autorizacao`: cliente de autorização e comunicação fiscal.
- `cte.config`: validação de configuração e health indicator do certificado.
- `cte.consulta`: construção e orquestração de consultas.
- `cte.domain`: contexto imutável, metadados, estados e resultados.
- `cte.evento`: construção e orquestração de eventos, como cancelamento.
- `cte.exception`: exceções funcionais e bloqueios de segurança.
- `cte.persistence`: repositórios JDBC e histórico.
- `cte.pipeline`: processors do fluxo operacional Camel.
- `cte.soap`: contrato, transporte e parser SOAP.
- `cte.validation`: validações XML, XSD, fiscais e de segurança.
- `cte.status`: contrato e serviço da consulta de disponibilidade.
- `cte.retorno`: resultados tipados e códigos de retorno da SEFAZ.
- `cte.infosimples` e `cte.portal`: integrações externas complementares.
- `cte.route`: rotas experimentais, habilitadas somente por propriedade.
- `cte.util`: utilitários sem estado.
- pacote `cte`: namespace raiz, sem classes concretas misturadas.

## Fluxo operacional em lote

`direct:processarCteLote` executa, para cada documento:

1. `CteItemProcessor`: normaliza, faz parsing seguro, extrai metadados e cria `CteContext`.
2. `CtePersistenceProcessor`: registra a recepção e devolve o contexto com ID e tentativa.
3. `CteXsdValidator`: valida o leiaute oficial.
4. `CteBusinessValidator`: valida chave, modelo, número e série.
5. `CteSignatureProcessor`: assina `infCte` e troca o contexto por uma nova versão imutável.
6. `CteSefazSender`: prepara o envio fiscal.
7. `CteSefazResponseProcessor`: classifica o retorno e persiste autorização ou rejeição.

O estado do documento é transportado exclusivamente em `CteContext`, armazenado na
propriedade `CteExchangeProperties.CTE_CONTEXT` do Camel Exchange.

## Fluxo de status

`GET /api/integracoes/sefaz/cte/status` envia `CteStatusRequest` para
`direct:cte-status`, que delega a consulta ao `CteStatusService`. A implementação
real de autorização continua separada no cliente e no transporte SOAP.

As rotas de `CteExperimentalRoutes` não fazem parte do fluxo padrão e só são ativadas
com `integration.experimental-routes.enabled=true`.
