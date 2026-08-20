package com.telemetria.integration.sefaz.cte.consulta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.telemetria.integration.sefaz.cte.exception.CteException;

/**
 * Construtor do XML da estrutura {@code <consSitCTe>} para consulta de situação do CT-e v4.00.
 */
@Component
public class CteConsultaBuilder {

    private static final Logger log = LoggerFactory.getLogger(CteConsultaBuilder.class);

    private static final String NAMESPACE_CTE = "http://www.portalfiscal.inf.br/cte";
    private static final String VERSAO_CTE = "4.00";
    private static final String X_SERV_CONSULTAR = "CONSULTAR";

    /**
     * Monta o XML de consulta da situação do CT-e.
     *
     * @param chaveCte Chave de acesso contendo exatamente 44 dígitos numéricos
     * @param tpAmb    Tipo de ambiente (1 = Produção, 2 = Homologação)
     * @return String contendo o XML {@code <consSitCTe>} formatado segundo o Manual de Orientação do Contribuinte
     */
    public String buildXmlConsulta(String chaveCte, String tpAmb) {
        log.debug("Gerando XML <consSitCTe> para a chave {} no ambiente {}", chaveCte, tpAmb);

        validarEntradas(chaveCte, tpAmb);

        return """
                <consSitCTe xmlns="%s" versao="%s">\
                <tpAmb>%s</tpAmb>\
                <xServ>%s</xServ>\
                <chCTe>%s</chCTe>\
                </consSitCTe>\
                """.formatted(NAMESPACE_CTE, VERSAO_CTE, tpAmb, X_SERV_CONSULTAR, chaveCte);
    }

    private void validarEntradas(String chaveCte, String tpAmb) {
        if (chaveCte == null || !chaveCte.matches("\\d{44}")) {
            throw new CteException("Chave de acesso do CT-e inválida. Deve conter exatamente 44 dígitos numéricos.");
        }
        if (tpAmb == null || (!"1".equals(tpAmb) && !"2".equals(tpAmb))) {
            throw new CteException("Tipo de ambiente inválido para consulta do CT-e. Deve ser '1' (Produção) ou '2' (Homologação).");
        }
    }
}
