package com.telemetria.integration.sefaz.cte.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.telemetria.integration.sefaz.cte.autorizacao.CteClient;
import com.telemetria.integration.sefaz.cte.dto.CteProcessingResult;
import com.telemetria.integration.sefaz.cte.exception.CteException;
import com.telemetria.integration.sefaz.cte.retorno.CteAutorizacaoResultado;
import com.telemetria.integration.sefaz.cte.security.CteXmlSigner;
import com.telemetria.integration.sefaz.cte.validation.CteXmlValidator;

@Service
public class CteDocumentProcessor {

    private static final Logger log =
            LoggerFactory.getLogger(CteDocumentProcessor.class);

    private final CteXmlValidator xmlValidator;
    private final CteXmlSigner xmlSigner;
    private final CteClient cteClient;

    public CteDocumentProcessor(
            CteXmlValidator xmlValidator,
            CteXmlSigner xmlSigner,
            CteClient cteClient
    ) {
        this.xmlValidator = xmlValidator;
        this.xmlSigner = xmlSigner;
        this.cteClient = cteClient;
    }

    public CteProcessingResult process(String xml) {

        /*
         * 1. Validação básica do XML
         */
        xmlValidator.validarCte(xml);

        log.debug("XML do CT-e validado.");

        /*
         * 2. Assinatura digital
         */
        String xmlAssinado =
                xmlSigner.assinarXml(xml, "infCte");

        log.debug("XML do CT-e assinado.");

        /*
         * 3. Comunicação com SEFAZ
         */
        CteAutorizacaoResultado response =
                cteClient.autorizarCteComResultado(xmlAssinado);

        /*
         * 4. Validação da resposta fiscal
         */
        if (!response.autorizado()) {

            throw new CteException(
                    String.valueOf(response.codigo()),
                    response.motivo()
            );
        }

        /*
         * 5. Resultado final
         */
        return new CteProcessingResult(
                response.chaveAcesso(),
                response.protocolo(),
                String.valueOf(response.codigo()),
                response.motivo()
        );
    }
}
