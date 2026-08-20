package com.telemetria.integration.sefaz.certificate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.telemetria.integration.config.SefazProperties;
import com.telemetria.integration.sefaz.cte.exception.CteException;

class SefazTlsContextFactoryTests {

    @Test
    void deveUsarContextoPadraoSomenteQuandoCertificadoNaoFoiConfigurado() throws Exception {
        CertificadoLoader loader = Mockito.mock(CertificadoLoader.class);
        SefazTlsContextFactory factory = new SefazTlsContextFactory(loader, new SefazProperties());

        assertNotNull(factory.criar());
        verify(loader, never()).carregarKeyStore(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void deveFalharQuandoCertificadoConfiguradoNaoPodeSerCarregado() throws Exception {
        SefazProperties properties = new SefazProperties();
        properties.getCertificado().setArquivo("certificado-inexistente.p12");
        properties.getCertificado().setSenha("senha");
        CertificadoLoader loader = Mockito.mock(CertificadoLoader.class);
        when(loader.carregarKeyStore("certificado-inexistente.p12", "senha", "PKCS12"))
                .thenThrow(new IllegalArgumentException("arquivo inexistente"));

        assertThrows(CteException.class, () -> new SefazTlsContextFactory(loader, properties).criar());
    }
}
