-- =============================================================================
-- AUTH SERVICE — auth_db
-- Responsável: Autenticação JWT, RBAC, MFA, SSO, refresh tokens, auditoria
-- Porta: 8081
-- =============================================================================

CREATE DATABASE IF NOT EXISTS auth_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE auth_db;

-- ---------------------------------------------------------------------------
-- TABELA: tenants
-- Registro de cada transportadora/empresa contratante do SaaS
-- ---------------------------------------------------------------------------
CREATE TABLE tenants (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid          CHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    nome          VARCHAR(255) NOT NULL,
    cnpj          VARCHAR(20)  NOT NULL UNIQUE,
    plano         ENUM('STARTER','PRO','ENTERPRISE','CUSTOM') NOT NULL DEFAULT 'STARTER',
    status        ENUM('ATIVO','INATIVO','BLOQUEADO','TRIAL') NOT NULL DEFAULT 'TRIAL',
    max_veiculos  INT NOT NULL DEFAULT 10,
    trial_expira  DATE,
    criado_em     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_cnpj   (cnpj),
    INDEX idx_tenant_status (status),
    INDEX idx_tenant_plano  (plano)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: roles
-- Papéis globais do RBAC
-- ---------------------------------------------------------------------------
CREATE TABLE roles (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nome        VARCHAR(50) NOT NULL UNIQUE COMMENT 'SUPER_ADMIN, ADMIN_TENANT, GESTOR, OPERADOR, MOTORISTA, API_CLIENT',
    descricao   VARCHAR(255),
    nivel       INT NOT NULL DEFAULT 5 COMMENT '1=maior privilégio'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO roles (nome, descricao, nivel) VALUES
    ('SUPER_ADMIN',   'Acesso total à plataforma, todos os tenants', 1),
    ('ADMIN_TENANT',  'Administrador do tenant próprio',             2),
    ('GESTOR',        'Gestor de frota designada',                   3),
    ('OPERADOR',      'Leitura e resposta a alertas',                4),
    ('MOTORISTA',     'Acesso apenas ao app mobile',                 5),
    ('API_CLIENT',    'Integrações externas via OAuth2',             5);

-- ---------------------------------------------------------------------------
-- TABELA: permissoes
-- Permissões granulares por recurso/ação
-- ---------------------------------------------------------------------------
CREATE TABLE permissoes (
    id       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    recurso  VARCHAR(100) NOT NULL COMMENT 'ex: veiculos, rotas, alertas',
    acao     VARCHAR(50)  NOT NULL COMMENT 'READ, WRITE, DELETE, EXPORT',
    descricao VARCHAR(255),
    UNIQUE KEY uk_recurso_acao (recurso, acao)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: role_permissoes
-- Vínculo N:N entre roles e permissões
-- ---------------------------------------------------------------------------
CREATE TABLE role_permissoes (
    role_id      BIGINT UNSIGNED NOT NULL,
    permissao_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (role_id, permissao_id),
    CONSTRAINT fk_rp_role      FOREIGN KEY (role_id)      REFERENCES roles(id)      ON DELETE CASCADE,
    CONSTRAINT fk_rp_permissao FOREIGN KEY (permissao_id) REFERENCES permissoes(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: usuarios
-- Usuários do sistema (gestores, operadores, motoristas, APIs)
-- ---------------------------------------------------------------------------
CREATE TABLE usuarios (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid                CHAR(36) NOT NULL UNIQUE DEFAULT (UUID()),
    tenant_id           BIGINT UNSIGNED NOT NULL,
    nome                VARCHAR(255) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    senha_hash          VARCHAR(255) NOT NULL COMMENT 'BCrypt rounds=12',
    cpf                 VARCHAR(14),
    telefone            VARCHAR(20),
    role_id             BIGINT UNSIGNED NOT NULL,
    ativo               BOOLEAN NOT NULL DEFAULT TRUE,
    email_verificado    BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_habilitado      BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_secret          VARCHAR(64) COMMENT 'TOTP secret (criptografado)',
    mfa_tipo            ENUM('TOTP','SMS','EMAIL') DEFAULT 'TOTP',
    ip_whitelist        JSON    COMMENT 'Lista de IPs permitidos para ADMIN',
    ultimo_acesso       DATETIME,
    tentativas_login    TINYINT UNSIGNED NOT NULL DEFAULT 0,
    bloqueado_ate       DATETIME COMMENT 'Bloqueio temporário após tentativas falhas',
    token_recuperacao   VARCHAR(128),
    token_recuperacao_exp DATETIME,
    token_ativacao      VARCHAR(128),
    criado_em           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_usuario_email_tenant (tenant_id, email),
    INDEX idx_usuario_tenant (tenant_id),
    INDEX idx_usuario_cpf    (cpf),
    INDEX idx_usuario_role   (role_id),
    CONSTRAINT fk_usuario_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_usuario_role   FOREIGN KEY (role_id)   REFERENCES roles(id)   ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: refresh_tokens
-- Tokens de renovação de sessão com TTL de 7 dias
-- ---------------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    usuario_id  BIGINT UNSIGNED NOT NULL,
    tenant_id   BIGINT UNSIGNED NOT NULL,
    token_hash  VARCHAR(255) NOT NULL UNIQUE COMMENT 'SHA-256 do token',
    device_info VARCHAR(255) COMMENT 'User-Agent do dispositivo',
    ip_origem   VARCHAR(45),
    expira_em   DATETIME NOT NULL,
    revogado    BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_rt_usuario (usuario_id),
    INDEX idx_rt_expira  (expira_em),
    CONSTRAINT fk_rt_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: api_clients
-- Credenciais OAuth2 para integrações (ERPs, seguradoras)
-- ---------------------------------------------------------------------------
CREATE TABLE api_clients (
    id            BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id     BIGINT UNSIGNED NOT NULL,
    client_id     VARCHAR(64) NOT NULL UNIQUE,
    client_secret VARCHAR(255) NOT NULL COMMENT 'Hash BCrypt',
    nome          VARCHAR(100) NOT NULL,
    scopes        JSON NOT NULL COMMENT 'Lista de scopes permitidos',
    ativo         BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_apiclient_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    INDEX idx_apiclient_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: audit_log
-- Log imutável de todas as ações de autenticação
-- ---------------------------------------------------------------------------
CREATE TABLE audit_log (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tenant_id   BIGINT UNSIGNED,
    usuario_id  BIGINT UNSIGNED,
    email       VARCHAR(255),
    acao        VARCHAR(50) NOT NULL COMMENT 'LOGIN_OK, LOGIN_FAIL, LOGOUT, MFA_OK, MFA_FAIL, SENHA_ALTERADA, TOKEN_REVOGADO',
    ip          VARCHAR(45) NOT NULL,
    user_agent  VARCHAR(512),
    dados       JSON COMMENT 'Dados adicionais do evento',
    sucesso     BOOLEAN NOT NULL,
    criado_em   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_tenant  (tenant_id),
    INDEX idx_audit_usuario (usuario_id),
    INDEX idx_audit_acao    (acao),
    INDEX idx_audit_criado  (criado_em)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- TABELA: sessoes_ativas
-- Controle de sessões concorrentes por usuário
-- ---------------------------------------------------------------------------
CREATE TABLE sessoes_ativas (
    id          BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    usuario_id  BIGINT UNSIGNED NOT NULL,
    tenant_id   BIGINT UNSIGNED NOT NULL,
    jti         VARCHAR(64) NOT NULL UNIQUE COMMENT 'JWT ID para revogação',
    ip          VARCHAR(45),
    device_info VARCHAR(255),
    expira_em   DATETIME NOT NULL,
    criado_em   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sessao_usuario  (usuario_id),
    INDEX idx_sessao_expira   (expira_em),
    CONSTRAINT fk_sessao_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
