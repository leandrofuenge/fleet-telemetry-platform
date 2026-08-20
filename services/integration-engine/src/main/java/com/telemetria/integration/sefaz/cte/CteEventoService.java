package com.telemetria.integration.sefaz.cte;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.telemetria.integration.config.SefazProperties;
import com.telemetria.integration.security.XmlSigner;
import com.telemetria.integration.sefaz.cte.evento.CteEventoBuilder;

/**
 * Serviço responsável pelo orquestramento e envio de eventos fiscais do CT-e (ex: Cancelamento).
 */
@Service
public class CteEventoService {

    private static final Logger log = LoggerFactory.getLogger(CteEventoService.class);

    private final CteEventoBuilder eventoBuilder;
    private final XmlSigner xmlSigner;
    private final CteClient cteClient;
    private final SefazProperties sefazProperties;
    private final CteFiscalOperationGuard fiscalOperationGuard;

    @Value("${sefaz.uf-codigo:51}") // 51 = MT
    private String cUF;

    public CteEventoService(CteEventoBuilder eventoBuilder,
                            XmlSigner xmlSigner,
                            CteClient cteClient,
                            SefazProperties sefazProperties,
                            CteFiscalOperationGuard fiscalOperationGuard) {
        this.eventoBuilder = eventoBuilder;
        this.xmlSigner = xmlSigner;
        this.cteClient = cteClient;
        this.sefazProperties = sefazProperties;
        this.fiscalOperationGuard = fiscalOperationGuard;
    }

    /**
     * Executa o fluxo completo de cancelamento do CT-e:
     * Validação das regras de negócio -> Trava de segurança -> Construção do XML -> Assinatura A1 -> Transmissão SOAP.
     *
     * @param chaveCte    Chave de acesso de 44 dígitos
     * @param nProt       Número do protocolo de autorização
     * @param xJust       Justificativa do cancelamento (mín. 15 e máx. 255 caracteres)
     * @param cnpjEmissor CNPJ do emitente do documento (14 dígitos)
     * @return Resposta XML bruta devolvida pela SEFAZ
     */
    public String cancelarCte(String chaveCte, String nProt, String xJust, String cnpjEmissor) {
        log.info("Solicitação de cancelamento recebida para o CT-e chave: {}", chaveCte);

        // 1. Aciona a guarda de operação fiscal (impede disparos acidentais ou sem certificado válido)
        fiscalOperationGuard.exigirCancelamentoPermitido();

        // 2. Validação estrita de parâmetros segundo regras da SEFAZ
        validarParametrosCancelamento(chaveCte, nProt, xJust, cnpjEmissor);

        String ambienteCodigo = sefazProperties.getCte().ambienteCte().codigo();

        // 3. Constrói o XML do evento de cancelamento
        log.debug("Construindo XML de cancelamento para o CT-e {}", chaveCte);
        String xmlEventoBruto = eventoBuilder.buildXmlCancelamento(chaveCte, nProt, xJust.trim(), cnpjEmissor, ambienteCodigo, cUF);

        // 4. Assina digitalmente o nó <infEvento> com a chave privada do certificado A1
        log.debug("Assinando digitalmente o evento de cancelamento do CT-e {}", chaveCte);
        String xmlEventoAssinado = xmlSigner.assinarXml(xmlEventoBruto, "infEvento");

        // 5. Transmite o envelope SOAP para a SEFAZ
        log.info("Enviando evento de cancelamento para a SEFAZ (Chave: {})", chaveCte);
        String respostaSefaz = cteClient.enviarEvento(xmlEventoAssinado);

        log.info("Evento de cancelamento do CT-e {} concluído e enviado à SEFAZ.", chaveCte);
        return respostaSefaz;
    }

    private void validarParametrosCancelamento(String chaveCte, String nProt, String xJust, String cnpjEmissor) {
        if (chaveCte == null || !chaveCte.matches("\\d{44}")) {
            throw new CteException("Chave de acesso do CT-e inválida. Deve conter exatamente 44 dígitos numéricos.");
        }
        if (nProt == null || nProt.isBlank()) {
            throw new CteException("Número do protocolo de autorização é obrigatório para efetuar o cancelamento.");
        }
        if (xJust == null || xJust.trim().length() < 15 || xJust.trim().length() > 255) {
            throw new CteException("A justificativa de cancelamento deve ter entre 15 e 255 caracteres.");
        }
        if (cnpjEmissor == null || !cnpjEmissor.matches("\\d{14}")) {
            throw new CteException("CNPJ do emissor inválido. Deve conter exatamente 14 dígitos numéricos.");
        }
    }
}