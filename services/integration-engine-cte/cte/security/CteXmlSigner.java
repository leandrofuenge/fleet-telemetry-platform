package com.telemetria.integration.sefaz.cte.security;

import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import org.springframework.stereotype.Component;

import com.telemetria.integration.config.SefazProperties;
import com.telemetria.integration.security.XmlSecurityException;
import com.telemetria.integration.security.XmlSigner;
import com.telemetria.integration.sefaz.certificate.CertificadoLoader;
import com.telemetria.integration.sefaz.cte.exception.CteException;

/** Resolve o certificado CT-e e delega a assinatura XMLDSig ao componente genérico. */
@Component
public class CteXmlSigner {

    private final XmlSigner xmlSigner;
    private final CertificadoLoader certificadoLoader;
    private final SefazProperties sefazProperties;

    public CteXmlSigner(
            XmlSigner xmlSigner,
            CertificadoLoader certificadoLoader,
            SefazProperties sefazProperties) {
        this.xmlSigner = xmlSigner;
        this.certificadoLoader = certificadoLoader;
        this.sefazProperties = sefazProperties;
    }

    public String assinarXml(String xml, String nomeElemento) {
        try {
            return xmlSigner.assinarXml(xml, nomeElemento, carregarCertificado());
        } catch (XmlSecurityException e) {
            throw new CteException("Não foi possível assinar digitalmente o XML do CT-e.", e);
        }
    }

    private KeyStore.PrivateKeyEntry carregarCertificado() {
        try {
            String arquivo = sefazProperties.getCertificado().getArquivo();
            String senha = sefazProperties.getCertificado().getSenha();
            KeyStore keyStore = certificadoLoader.carregarKeyStore(
                    arquivo, senha, sefazProperties.getCertificado().getTipo());
            String senhaChave = sefazProperties.getCertificado().getSenhaChave();
            String senhaEfetiva = senhaChave == null || senhaChave.isBlank() ? senha : senhaChave;
            char[] password = senhaEfetiva == null ? new char[0] : senhaEfetiva.toCharArray();

            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Key key = keyStore.getKey(alias, password);
                if (key instanceof PrivateKey && keyStore.getCertificate(alias) instanceof X509Certificate) {
                    return new KeyStore.PrivateKeyEntry(
                            (PrivateKey) key,
                            keyStore.getCertificateChain(alias));
                }
            }
            throw new CteException("Certificado A1 não contém uma chave privada utilizável.");
        } catch (CteException e) {
            throw e;
        } catch (Exception e) {
            throw new CteException("Não foi possível carregar o certificado do CT-e.", e);
        }
    }
}
