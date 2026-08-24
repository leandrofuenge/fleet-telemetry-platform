# Regeneracao dos stubs Axis2

Este fluxo baixa os WSDLs diretamente dos endpoints publicados no Portal Nacional da NF-e e gera novos clientes com Apache Axis2 2.0.1.

Ele **nao sobrescreve** `src/main/java/.../wsdl`. Os resultados ficam em `target/regenerated-axis2`, para que as diferencas sejam revisadas antes de qualquer substituicao.

## Pre-requisitos

- JDK 17 ou superior;
- distribuicao binaria do Apache Axis2 2.0.1;
- certificado ICP-Brasil A1 (`.pfx` ou `.p12`) autorizado para acessar os endpoints.

## Executar

```powershell
$senha = Read-Host 'Senha do certificado' -AsSecureString
.\tools\wsdl\regenerate-stubs.ps1 `
  -CertificatePath 'C:\certificados\empresa.pfx' `
  -CertificatePassword $senha `
  -Axis2Home 'C:\ferramentas\axis2-2.0.1'
```

Para apenas renovar os WSDLs:

```powershell
.\tools\wsdl\regenerate-stubs.ps1 `
  -CertificatePath 'C:\certificados\empresa.pfx' `
  -CertificatePassword $senha `
  -DownloadOnly
```

Os contratos e seus pacotes Java estao declarados em `contracts.csv`. Atualize as URLs a partir da pagina oficial antes de uma nova geracao:

- https://www.nfe.fazenda.gov.br/portal/webservices.aspx

## Revisao

Compare cada pasta em `target/regenerated-axis2` com o pacote correspondente em `src/main/java/com/telemetria/integration/nfe/wsdl`.

Os stubs atuais possuem variantes especificas de SP, MS, RS e CE. Nao troque uma variante por outra apenas porque os nomes dos servicos coincidem: WSDLs de autorizadores diferentes podem produzir assinaturas e envelopes SOAP distintos.

Depois da comparacao, aplique novamente somente as adaptacoes necessarias ao Axis2 2.0.1/Axiom 2.0.0 e execute:

```powershell
mvn test
mvn -DskipTests package
```

Nunca versione o certificado, sua senha ou arquivos privados. `target` permanece descartavel e deve ser regenerado quando houver mudanca oficial de contrato, nao a cada reinicio da aplicacao.
