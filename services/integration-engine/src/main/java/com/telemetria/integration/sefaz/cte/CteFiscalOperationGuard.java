package com.telemetria.integration.sefaz.cte;

import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import org.springframework.stereotype.Component;

import com.telemetria.integration.config.SefazProperties;
import com.telemetria.integration.sefaz.certificate.CertificadoLoader;

/** Trava operações que alteram estado fiscal até sua liberação deliberada. */
@Component
public class CteFiscalOperationGuard {

    private final SefazProperties properties;
    private final CertificadoLoader certificadoLoader;

    public CteFiscalOperationGuard(SefazProperties properties, CertificadoLoader certificadoLoader) {
        this.properties = properties;
        this.certificadoLoader = certificadoLoader;
    }

    public void exigirAutorizacaoPermitida() {
        exigirLiberacao(properties.getCte().getOperations().isAuthorizationEnabled(), "emissão/autorização");
    }

    public void exigirCancelamentoPermitido() {
        exigirLiberacao(properties.getCte().getOperations().isCancellationEnabled(), "cancelamento");
    }

    private void exigirLiberacao(boolean operationEnabled, String operation) {
        if (!operationEnabled) {
            bloquear(operation, "a operação permanece desabilitada por configuração");
        }
        if (!properties.getCte().getOperations().isAuthorizedFiscalTestData()) {
            bloquear(operation, "a massa fiscal autorizada ainda não foi confirmada");
        }
        validarCertificado(operation);
    }

    private void validarCertificado(String operation) {
        var certificate = properties.getCertificado();
        if (!preenchido(certificate.getArquivo()) || !preenchido(certificate.getSenha())) {
            bloquear(operation, "certificado A1 e senha não foram fornecidos");
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
                if (key instanceof PrivateKey
                        && keyStore.getCertificate(alias) instanceof X509Certificate x509Certificate) {
                    x509Certificate.checkValidity();
                    return;
                }
            }
            bloquear(operation, "certificado A1 não possui chave privada utilizável");
        } catch (CteOperationBlockedException e) {
            throw e;
        } catch (Exception e) {
            throw new CteOperationBlockedException(
                    "Operação CT-e bloqueada (" + operation + "): certificado A1 inválido ou indisponível.", e);
        }
    }

    private boolean preenchido(String value) {
        return value != null && !value.isBlank() && !value.startsWith("CHANGE_ME");
    }

    private void bloquear(String operation, String reason) {
        throw new CteOperationBlockedException("Operação CT-e bloqueada (" + operation + "): " + reason + ".");
    }
}
