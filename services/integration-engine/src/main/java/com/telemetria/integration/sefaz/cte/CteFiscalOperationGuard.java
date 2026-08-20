package com.telemetria.integration.sefaz.cte;

import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.telemetria.integration.config.SefazProperties;
import com.telemetria.integration.sefaz.certificate.CertificadoLoader;

/**
 * Trava de segurança para operações fiscais com alteração de estado no CT-e.
 * Impede emissão e cancelamento caso as flags de controle estejam desativadas
 * ou o certificado A1 esteja ausente, expirado ou inválido.
 */
@Component
public class CteFiscalOperationGuard {

    private static final Logger log = LoggerFactory.getLogger(CteFiscalOperationGuard.class);

    private final SefazProperties properties;
    private final CertificadoLoader certificadoLoader;

    public CteFiscalOperationGuard(SefazProperties properties, CertificadoLoader certificadoLoader) {
        this.properties = properties;
        this.certificadoLoader = certificadoLoader;
    }

    /**
     * Valida se a autorização/emissão de CT-e está liberada para execução.
     */
    public void exigirAutorizacaoPermitida() {
        var cte = obterConfiguracaoCte();
        boolean isEnabled = cte.getOperations() != null && cte.getOperations().isAuthorizationEnabled();
        exigirLiberacao(isEnabled, "emissão/autorização");
    }

    /**
     * Valida se o cancelamento de CT-e está liberado para execução.
     */
    public void exigirCancelamentoPermitido() {
        var cte = obterConfiguracaoCte();
        boolean isEnabled = cte.getOperations() != null && cte.getOperations().isCancellationEnabled();
        exigirLiberacao(isEnabled, "cancelamento");
    }

    private void exigirLiberacao(boolean operationEnabled, String operation) {
        if (!operationEnabled) {
            bloquear(operation, "a operação permanece desabilitada por configuração");
        }

        var cte = obterConfiguracaoCte();
        boolean isAuthorizedData = cte.getOperations() != null && cte.getOperations().isAuthorizedFiscalTestData();
        if (!isAuthorizedData) {
            bloquear(operation, "a massa fiscal autorizada ainda não foi confirmada");
        }

        validarCertificado(operation);
        log.debug("Guarda de segurança liberou a execução da operação de {}.", operation);
    }

    private void validarCertificado(String operation) {
        var certificate = Optional.ofNullable(properties.getCertificado())
                .orElseThrow(() -> new CteOperationBlockedException(
                        "Operação CT-e bloqueada (" + operation + "): bloco de configuração do certificado ausente."));

        if (!preenchido(certificate.getArquivo()) || !preenchido(certificate.getSenha())) {
            bloquear(operation, "caminho do certificado A1 ou senha não foram fornecidos");
        }

        try {
            KeyStore keyStore = certificadoLoader.carregarKeyStore(
                    certificate.getArquivo(), certificate.getSenha(), certificate.getTipo());

            String keyPassword = preenchido(certificate.getSenhaChave())
                    ? certificate.getSenhaChave() : certificate.getSenha();

            Enumeration<String> aliases = keyStore.aliases();

            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Key key = keyStore.getKey(alias, keyPassword.toCharArray());

                if (key instanceof PrivateKey && keyStore.getCertificate(alias) instanceof X509Certificate x509Cert) {
                    try {
                        x509Cert.checkValidity();
                        return; // Certificado e chave válidos encontrados
                    } catch (CertificateExpiredException e) {
                        bloquear(operation, "o certificado A1 expirou em " + x509Cert.getNotAfter());
                    } catch (CertificateNotYetValidException e) {
                        bloquear(operation, "o certificado A1 só é válido a partir de " + x509Cert.getNotBefore());
                    }
                }
            }

            bloquear(operation, "certificado A1 não possui par de chave privada e certificado X.509 utilizáveis");

        } catch (CteOperationBlockedException e) {
            throw e;
        } catch (Exception e) {
            throw new CteOperationBlockedException(
                    "Operação CT-e bloqueada (" + operation + "): falha ao validar certificado A1.", e);
        }
    }

    /**
     * Recupera o objeto de configuração CT-e raiz, protegendo contra nulos.
     */
    private SefazProperties.Cte obterConfiguracaoCte() {
        return Optional.ofNullable(properties.getCte())
                .orElseThrow(() -> new CteOperationBlockedException(
                        "Operação CT-e bloqueada: parâmetros 'sefaz.cte' não configurados."));
    }

    private boolean preenchido(String value) {
        return value != null && !value.isBlank() && !value.startsWith("CHANGE_ME");
    }

    private void bloquear(String operation, String reason) {
        String msg = "Operação CT-e bloqueada (" + operation + "): " + reason + ".";
        log.warn(msg);
        throw new CteOperationBlockedException(msg);
    }
}