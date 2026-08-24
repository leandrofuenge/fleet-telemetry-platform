# Segurança de transporte HTTPS

`integration-engine` e `integration-engine-telemetry-service` suportam dois modelos de implantação:

1. **TLS direto no serviço:** configure `SERVER_SSL_ENABLED=true`, o caminho do
   keystore PKCS12, sua senha e, opcionalmente, o alias. O serviço passa a escutar
   HTTPS na própria porta e `SECURITY_REQUIRE_HTTPS` é ativado por padrão.
2. **TLS no proxy reverso/ingress:** mantenha `SERVER_SSL_ENABLED=false`, configure
   `SECURITY_REQUIRE_HTTPS=true` e faça o proxy enviar `Forwarded: proto=https` ou
   `X-Forwarded-Proto: https`. O proxy deve remover cabeçalhos encaminhados vindos
   da Internet antes de adicionar os seus.

## Variáveis por serviço

```text
SERVER_SSL_ENABLED=true
SERVER_SSL_KEY_STORE=file:/run/secrets/https/server.p12
SERVER_SSL_KEY_STORE_PASSWORD=<segredo>
SERVER_SSL_KEY_STORE_TYPE=PKCS12
SERVER_SSL_KEY_ALIAS=<alias-opcional>
SERVER_SSL_ENABLED_PROTOCOLS=TLSv1.3,TLSv1.2
SECURITY_REQUIRE_HTTPS=true
SECURITY_HSTS_ENABLED=true
SECURITY_HSTS_MAX_AGE_SECONDS=31536000
SECURITY_HSTS_INCLUDE_SUBDOMAINS=true
```

Para comunicação interna, configure o cliente do `integration-engine-telemetry-service` com a URL
HTTPS do motor:

```text
INTEGRATION_SERVICE_URL=https://integration-engine:9060
```

Se o certificado interno vier de uma CA privada, importe a CA no truststore da
JVM do `integration-engine-telemetry-service`. Nunca desative a validação de certificado ou hostname.
O mTLS já usado nas integrações SEFAZ é independente do certificado HTTPS de
entrada e permanece configurado pelas variáveis `SEFAZ_CERT_*` e
`SEFAZ_TRUSTSTORE_*`.

Os dois serviços enviam HSTS somente em respostas HTTPS, `X-Content-Type-Options`,
`X-Frame-Options: DENY` e `Referrer-Policy: no-referrer`.
