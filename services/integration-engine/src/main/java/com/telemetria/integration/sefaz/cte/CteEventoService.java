package com.telemetria.integration.sefaz.cte;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.telemetria.integration.config.SefazProperties;
import com.telemetria.integration.security.XmlSigner;
import com.telemetria.integration.sefaz.cte.evento.CteEventoBuilder;

@Service
public class CteEventoService {

    private final CteEventoBuilder eventoBuilder;
    private final XmlSigner xmlSigner;
    private final CteClient cteClient;
    private final SefazProperties sefazProperties;

    @Value("${sefaz.uf-codigo:51}") // 51 = MT
    private String cUF;

    public CteEventoService(CteEventoBuilder eventoBuilder, XmlSigner xmlSigner, CteClient cteClient,
            SefazProperties sefazProperties) {
        this.eventoBuilder = eventoBuilder;
        this.xmlSigner = xmlSigner;
        this.cteClient = cteClient;
        this.sefazProperties = sefazProperties;
    }

    /**
     * Executa o fluxo completo de cancelamento do CT-e (Gera XML -> Assina -> Envia para a SEFAZ).
     *
     * @param chaveCte    Chave de acesso (44 dígitos)
     * @param nProt       Número do Protocolo da autorização
     * @param xJust       Motivo do cancelamento (mín. 15 caracteres)
     * @param cnpjEmissor CNPJ da empresa emitente (14 dígitos)
     * @return Resposta XML bruta do WebService da SEFAZ
     */
    public String cancelarCte(String chaveCte, String nProt, String xJust, String cnpjEmissor) {
        // 1. Constrói o XML do evento
        String xmlEventoBruto = eventoBuilder.buildXmlCancelamento(chaveCte, nProt, xJust, cnpjEmissor,
                sefazProperties.getCte().ambienteCte().codigo(), cUF);

        // 2. Assina digitalmente o nó <infEvento> com a chave privada do certificado A1
        String xmlEventoAssinado = xmlSigner.assinarXml(xmlEventoBruto, "infEvento");

        // 3. Transmite o envelope SOAP para a SEFAZ
        return cteClient.enviarEvento(xmlEventoAssinado);
    }
}
