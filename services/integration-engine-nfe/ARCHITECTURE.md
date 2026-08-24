# NF-e Integration Engine

Módulo Maven responsável pelas integrações com os serviços de NF-e da SEFAZ.

O código-fonte está centralizado em `src/main/java` e utiliza o namespace-base
`com.telemetria.integration.nfe`.

Principais áreas:

- `api`: interface HTTP e tratamento de erros;
- `application`: casos de uso, clientes e DTOs de entrada/saída;
- `config` e `validation`: configurações e regras de proteção;
- `dom` (incluindo `dom.enums`), `dto` e `exception`: modelo legado de NF-e e
  seus contratos;
- `soap`, `ws` e `wsdl`: comunicação SOAP com a SEFAZ;
- `schema`, `schemas` e `schemas_eventos`: classes JAXB geradas a partir dos
  layouts fiscais;
- `impressao` e `util`: recursos auxiliares e impressão de documentos fiscais.

Classes geradas em `schema*` e `wsdl` devem ser alteradas somente quando houver
atualização do layout fiscal ou do WSDL de origem. Os subpacotes `wsdl` mantêm
as maiúsculas dos nomes oficiais dos serviços para preservar os contratos
gerados.
