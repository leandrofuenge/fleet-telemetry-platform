# Implementação RF01–RF04

## RF01 — Tenants e planos

- `POST /api/v1/tenants` cria um tenant com CNPJ validado por dígito verificador e trial de 14 dias.
- Os planos `STARTER`, `PRO`, `ENTERPRISE` e `CUSTOM` definem o limite de veículos e funcionalidades.
- O job `TenantTrialScheduler` expira trials e define retenção de dados por 30 dias.

## RF02 — Veículos

- A criação exige tenant ativo e respeita o limite do plano.
- Placas são normalizadas e validadas nos formatos Mercosul e antigo.
- A regra de tacógrafo, documentos e bloqueio de viagem já existente foi preservada.
- `POST /api/v1/veiculos/{veiculoId}/dispositivo-principal` realiza a troca segura do rastreador principal, com calibração obrigatória do odômetro e auditoria da diferença.

## RF03 — Motoristas

- CPF é normalizado, validado e único por tenant.
- O cadastro recebe vencimentos de CNH e ASO, MOPP, contato e tenant.
- As regras existentes de CNH, ASO, MOPP e score continuam sendo usadas antes de viagens.

## RF04 — Usuários, RBAC e MFA

- Senhas usam BCrypt com custo 12, expiram em 90 dias e não podem repetir as últimas cinco.
- Falhas de login são contadas em uma janela de 10 minutos; a quinta bloqueia a conta por 10 minutos.
- Sessões são limitadas a três por usuário.
- Admin e super-admin precisam de TOTP RFC 6238. Use `POST /api/v1/auth/mfa/setup` e depois `POST /api/v1/auth/mfa/confirm`; o login recebe `mfaCodigo`.
- Os endpoints de veículo, motorista e usuário passaram a exigir os papéis definidos na configuração de segurança.

## Banco de dados

`V3__tenants_and_user_scope.sql` adiciona o registro de tenant e o vínculo opcional do usuário ao tenant. O serviço ainda está configurado com Hibernate `ddl-auto=update`; antes de produção, habilite e baselinize o Flyway para que as migrations sejam a única fonte de schema.
