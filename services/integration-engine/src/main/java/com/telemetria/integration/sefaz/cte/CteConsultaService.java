package com.telemetria.integration.sefaz.cte;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.telemetria.integration.config.SefazProperties;
import com.telemetria.integration.sefaz.cte.infosimples.InfosimplesCteClient;
import com.telemetria.integration.sefaz.cte.infosimples.InfosimplesCteRequest;
import com.telemetria.integration.sefaz.cte.infosimples.InfosimplesCteResponse;

/**
 * Serviço de orquestração para consultas de CT-e.
 * Suporta consulta leve de situação via SOAP (SEFAZ) e consulta enriquecida via REST (Infosimples).
 */
@Service
public class CteConsultaService {

    private static final Logger log = LoggerFactory.getLogger(CteConsultaService.class);

    private final CteConsultaBuilder consultaBuilder;
    private final CteClient cteClient;
    private final InfosimplesCteClient infosimplesClient;
    private final SefazProperties sefazProperties;

    public CteConsultaService(CteConsultaBuilder consultaBuilder, 
                              CteClient cteClient, 
                              InfosimplesCteClient infosimplesClient,
                              SefazProperties sefazProperties) {
        this.consultaBuilder = consultaBuilder;
        this.cteClient = cteClient;
        this.infosimplesClient = infosimplesClient;
        this.sefazProperties = sefazProperties;
    }

    /**
     * Executa consulta SOAP leve da situação fiscal do CT-e diretamente nos servidores da SEFAZ.
     *
     * @param chaveCte Chave de acesso contendo 44 dígitos numéricos
     * @return XML contendo a resposta da SEFAZ (estrutura {@code <retConsSitCTe>})
     */
    public String consultarSituacaoSefaz(String chaveCte) {
        log.info("Iniciando consulta SOAP de situação fiscal na SEFAZ para o CT-e: {}", chaveCte);

        String ambienteCodigo = sefazProperties.getCte().ambienteCte().codigo();
        String xmlConsulta = consultaBuilder.buildXmlConsulta(chaveCte, ambienteCodigo);

        log.debug("XML de consulta SEFAZ gerado para o CT-e {}. Transmitindo requisição...", chaveCte);
        String respostaXml = cteClient.consultarCte(xmlConsulta);

        log.info("Consulta SOAP SEFAZ finalizada para o CT-e: {}", chaveCte);
        return respostaXml;
    }

    /**
     * Executa consulta REST enriquecida via Infosimples para obter XML completo, CIOT, RNTRC e motoristas.
     *
     * @param chaveCte   Chave de acesso contendo 44 dígitos numéricos
     * @param certBase64 Certificado digital A1 codificado em Base64
     * @param senhaCert  Senha do certificado digital
     * @return Objeto DTO estruturado com os dados enriquecidos do CT-e
     */
    public InfosimplesCteResponse consultarDadosCompletosInfosimples(String chaveCte, String certBase64, String senhaCert) {
        log.info("Iniciando consulta REST de dados completos via Infosimples para o CT-e: {}", chaveCte);

        validarParametrosInfosimples(chaveCte, certBase64, senhaCert);

        InfosimplesCteRequest request = new InfosimplesCteRequest(chaveCte, certBase64, senhaCert);
        InfosimplesCteResponse response = infosimplesClient.consultarCteCompleto(request);

        if (response == null) {
            log.error("Retorno nulo recebido do cliente Infosimples para a chave {}", chaveCte);
            throw new CteException("Falha ao consultar dados do CT-e na Infosimples. O serviço retornou uma resposta vazia.");
        }

        log.info("Consulta de dados completos Infosimples finalizada com sucesso para o CT-e: {}", chaveCte);
        return response;
    }

    private void validarParametrosInfosimples(String chaveCte, String certBase64, String senhaCert) {
        if (chaveCte == null || !chaveCte.matches("\\d{44}")) {
            throw new CteException("Chave de acesso do CT-e inválida. Deve conter exatamente 44 dígitos numéricos.");
        }
        if (certBase64 == null || certBase64.isBlank()) {
            throw new CteException("O certificado digital A1 em Base64 é obrigatório para consulta na Infosimples.");
        }
        if (senhaCert == null || senhaCert.isBlank()) {
            throw new CteException("A senha do certificado digital é obrigatória para consulta na Infosimples.");
        }
    }
}