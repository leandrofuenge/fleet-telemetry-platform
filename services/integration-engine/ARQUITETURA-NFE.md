# Organização dos pacotes NF-e

O `integration-engine` usa duas camadas NF-e com responsabilidades distintas.

| Pacote | Responsabilidade |
| --- | --- |
| `com.telemetria.integration.sefaz.nfe` | API REST, regras de segurança, Base64, configuração e tratamento de erros do serviço. |
| `com.telemetria.integration.sefaz.nfe.soap` | Envelope SOAP 1.2, transporte mTLS, SOAPAction e validação de respostas da SEFAZ. |
| `com.telemetria.integration.nfe` | Modelos JAXB, validação, assinatura e particularidades fiscais incorporadas ao serviço. |
| `com.telemetria.integration.nfe.schemas` e `schemas_eventos` | Classes JAXB geradas a partir dos XSDs NF-e. Não editar manualmente. |
| `com.telemetria.integration.nfe.wsdl` | Stubs Axis2 gerados a partir de WSDLs. Não editar manualmente. |

Todos os fontes do módulo usam o namespace `com.telemetria.integration`. Os modelos de schema são usados pela camada Spring para montar consultas à SEFAZ; o transporte permanece centralizado no cliente SOAP seguro.

Os XSDs externos estão em `schemas/` e são incluídos no JAR em `schemas/` pelo Maven. Para uso com a biblioteca incorporada, configure `ConfiguracoesNfe.pastaSchemas` com um diretório extraído/montado no ambiente; o validador de XSD usa caminho de arquivo, não `classpath:`.
