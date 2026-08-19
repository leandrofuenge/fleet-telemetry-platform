package com.telemetria.integration.sefaz.cte;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.telemetria.integration.config.SefazProperties;
import com.telemetria.integration.sefaz.certificate.CertificadoLoader;

class CteFiscalOperationGuardTests {

    private SefazProperties properties;
    private CertificadoLoader loader;
    private CteFiscalOperationGuard guard;

    @BeforeEach
    void setUp() {
        properties = new SefazProperties();
        loader = mock(CertificadoLoader.class);
        guard = new CteFiscalOperationGuard(properties, loader);
    }

    @Test
    void deveManterAutorizacaoBloqueadaPorPadrao() {
        assertThrows(CteOperationBlockedException.class, guard::exigirAutorizacaoPermitida);
    }

    @Test
    void deveManterCancelamentoBloqueadoPorPadrao() {
        assertThrows(CteOperationBlockedException.class, guard::exigirCancelamentoPermitido);
    }

    @Test
    void deveBloquearSemConfirmacaoDaMassaFiscal() {
        properties.getCte().getOperations().setAuthorizationEnabled(true);

        assertThrows(CteOperationBlockedException.class, guard::exigirAutorizacaoPermitida);
    }

    @Test
    void deveBloquearSemCertificadoMesmoComFlagsAtivas() {
        properties.getCte().getOperations().setCancellationEnabled(true);
        properties.getCte().getOperations().setAuthorizedFiscalTestData(true);

        assertThrows(CteOperationBlockedException.class, guard::exigirCancelamentoPermitido);
    }

    @Test
    void deveLiberarSomenteComFlagMassaECertificadoValidos() throws Exception {
        properties.getCte().getOperations().setAuthorizationEnabled(true);
        properties.getCte().getOperations().setCancellationEnabled(true);
        properties.getCte().getOperations().setAuthorizedFiscalTestData(true);
        properties.getCertificado().setArquivo("sefaz-a1.p12");
        properties.getCertificado().setSenha("senha-real");

        KeyStore keyStore = mock(KeyStore.class);
        when(keyStore.aliases()).thenAnswer(ignored ->
                Collections.enumeration(Collections.singleton("a1")));
        when(keyStore.getKey(eq("a1"), any(char[].class))).thenReturn(mock(PrivateKey.class));
        when(keyStore.getCertificate("a1")).thenReturn(mock(X509Certificate.class));
        when(loader.carregarKeyStore("sefaz-a1.p12", "senha-real", "PKCS12")).thenReturn(keyStore);

        assertDoesNotThrow(guard::exigirAutorizacaoPermitida);
        assertDoesNotThrow(guard::exigirCancelamentoPermitido);
    }
}
