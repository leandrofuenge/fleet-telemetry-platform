package com.telemetria.integration.nfe.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import com.telemetria.integration.nfe.exception.ExcecaoNfe;

import br.com.swconsultoria.certificado.Certificado;
import br.com.swconsultoria.certificado.CertificadoService;
import br.com.swconsultoria.certificado.exception.CertificadoException;

@EnabledIfEnvironmentVariable(named = "NFE_TEST_CERT_PATH", matches = ".+")
class NFCeUtilTeste {

    @Test
    void geraHashCSRTSucesso() throws NoSuchAlgorithmException {
        String chave = "41180678393592000146558900000006041028190697";
        String csrt = "G8063VRTNDMO886SFNK5LDUDEI24XJ22YIPO";

        assertArrayEquals("aWv6LeEM4X6u4+qBI2OYZ8grigw=".getBytes(StandardCharsets.UTF_8), geraHashCSRTBase64(chave, csrt));
    }

    @Test
    void geraHashCSRTParametrosInvalidos() {
        String chave = "41180678393592000146558900000006041028190697";
        String csrt = "G8063VRTNDMO886SFNK5LDUDEI24XJ22YIPO";

        //Chave Nula
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, ()
                -> geraHashCSRTBase64(null, csrt)
        );
        assertEquals("Chave não deve ser nula ou vazia", exception.getMessage());

        //Chave Com menos Caracteres
        exception = assertThrows(IllegalArgumentException.class, ()
                -> geraHashCSRTBase64("123", csrt)
        );
        assertEquals("Chave deve conter 44 caracteres.", exception.getMessage());

        //CSRC Vazio
        exception = assertThrows(IllegalArgumentException.class, ()
                -> geraHashCSRTBase64(chave, "")
        );
        assertEquals("CSRT não deve ser nulo ou vazio", exception.getMessage());
    }

    private byte[] geraHashCSRTBase64(String chave, String csrt) throws NoSuchAlgorithmException {
        //O XSD é xs:base64Binary e já faz o base64, aqui é uma "simulação" para os testes
        return Base64.getEncoder().encode(NFCeUtil.geraHashCSRT(chave, csrt));
    }

    @Test
    void getCodeQRCodeContingenciaV3Sucesso() throws Exception {
        String chave = "41180678393592000146558900000006041028190697";
        String tpAmp = "2";
        String dhEmi = "2025-07-01T12:37:06-03:00";
        String vNF = "12.34";
        String tpDestRegraQrCode = "2";
        String cpf = "11111111111";
        String dadosAssinar = obterDadosAssinar(chave, tpAmp, dhEmi, vNF, tpDestRegraQrCode, cpf);
        Certificado certificado = obterCertificado();

        String qrcodeGerado = NFCeUtil.getCodeQRCodeContingenciaV3(chave, tpAmp, dhEmi, vNF, tpDestRegraQrCode, cpf,
                "https://fake.it", certificado);
        byte[] assinatura = Base64.getDecoder().decode(qrcodeGerado.split("\\|")[7]);

        assertTrue(isAssinaturaValida(certificado, dadosAssinar, assinatura));
    }

    /**
     * Neste caso da assinatura não bater a Sefaz retorna:
     * 583-Rejeicao: Valor da assinatura do qrCode difere do valor calculado
     */
    @Test
    void getCodeQRCodeContingenciaV3FalhaDadosDivergentes()
            throws CertificadoException, ExcecaoNfe, GeneralSecurityException {
        String chave = "41180678393592000146558900000006041028190697";
        String tpAmp = "2";
        String dhEmi = "2025-07-01T12:37:06-03:00";
        String vNF = "12.34";
        String tpDestRegraQrCode = "2";
        String cpf = "11111111111";

        String dadosAssinar = obterDadosAssinar(chave, tpAmp, dhEmi, vNF, tpDestRegraQrCode, "22222222222");
        Certificado certificado = obterCertificado();

        String qrcodeGerado = NFCeUtil.getCodeQRCodeContingenciaV3(chave, tpAmp, dhEmi, vNF, tpDestRegraQrCode, cpf,
                "https://fake.it", certificado);
        byte[] assinatura = Base64.getDecoder().decode(qrcodeGerado.split("\\|")[7]);

        assertFalse(isAssinaturaValida(certificado, dadosAssinar, assinatura));
    }

    private String obterDadosAssinar(String chave, String tpAmp, String dhEmi, String vNF, String tpDestRegraQrCode,
                                     String cpf) {
        //Conforme manual v6.0 QRCode - Assinar apenas Campos 1 ao 7, incluindo os separadores |.
        return String.format("%s|3|%s|%s|%s|%s|%s", chave, tpAmp, dhEmi.substring(8, 10), vNF, tpDestRegraQrCode, cpf);
    }

    private Certificado obterCertificado() throws CertificadoException {
        String caminhoCertificado = System.getenv("NFE_TEST_CERT_PATH");
        String senhaCertificado = System.getenv("NFE_TEST_CERT_PASSWORD");
        if (senhaCertificado == null || senhaCertificado.isBlank()) {
            throw new IllegalStateException("Defina NFE_TEST_CERT_PASSWORD para executar testes que assinam NFC-e.");
        }
        return CertificadoService.certificadoPfx(caminhoCertificado, senhaCertificado);
    }

    private boolean isAssinaturaValida(Certificado certificado, String dadosAssinar, byte[] assinatura)
            throws GeneralSecurityException, CertificadoException {
        Signature verifier = Signature.getInstance("SHA1withRSA");
        KeyStore keyStore = CertificadoService.getKeyStore(certificado);
        PublicKey publicKey = keyStore.getCertificate(certificado.getNome()).getPublicKey();

        verifier.initVerify(publicKey);
        verifier.update(dadosAssinar.getBytes(StandardCharsets.UTF_8));
        return verifier.verify(assinatura);
    }

}
