package com.telemetria.domain.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.ApiClient;
import com.telemetria.domain.entity.Webhook;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.infrastructure.persistence.ApiClientRepository;
import com.telemetria.infrastructure.persistence.WebhookRepository;

@Service
public class ApiIntegracaoService {
    private final ApiClientRepository clientes;
    private final WebhookRepository webhooks;

    public ApiIntegracaoService(ApiClientRepository clientes, WebhookRepository webhooks) {
        this.clientes = clientes;
        this.webhooks = webhooks;
    }

    @Transactional
    public Credencial criarCliente(ApiClient cliente) {
        String secret = UUID.randomUUID().toString().replace("-", "");
        cliente.setClientId("cli_" + UUID.randomUUID());
        cliente.setSecretHash(hash(secret));
        clientes.save(cliente);
        return new Credencial(cliente.getClientId(), secret, cliente.getScopes());
    }

    @Transactional
    public Webhook registrarWebhook(Webhook webhook, String segredo) {
        if (webhook.getTenantId() == null
                || webhook.getUrl() == null
                || !webhook.getUrl().startsWith("https://")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Webhook deve usar HTTPS e ter tenant");
        }
        webhook.setSecretHash(hash(segredo));
        return webhooks.save(webhook);
    }

    public String assinatura(String segredo, String payload) {
        return "sha256=" + hash(segredo + payload);
    }

    private String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte valueByte : bytes) {
                hex.append(String.format("%02x", valueByte));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record Credencial(String clientId, String clientSecret, String scopes) {
    }
}
