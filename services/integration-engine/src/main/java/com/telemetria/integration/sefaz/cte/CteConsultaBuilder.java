package com.telemetria.integration.sefaz.cte;

import org.springframework.stereotype.Component;

/**
 * Construtor do XML da estrutura <consSitCTe> para consulta de CT-e v4.00.
 */
@Component
public class CteConsultaBuilder {

    private static final String NAMESPACE_CTE = "http://www.portalfiscal.inf.br/cte";

    /**
     * Monta o XML de consulta da situação do CT-e.
     *
     * @param chaveCte Chave de acesso com 44 dígitos
     * @param tpAmb    1 = Produção, 2 = Homologação
     * @return String contendo o XML <consSitCTe>
     */
    public String buildXmlConsulta(String chaveCte, String tpAmb) {
        validarChave(chaveCte);

        StringBuilder xml = new StringBuilder();
        xml.append("<consSitCTe xmlns=\"").append(NAMESPACE_CTE).append("\" versao=\"4.00\">");
        xml.append("<tpAmb>").append(tpAmb).append("</tpAmb>");
        xml.append("<xServ>CONSULTAR</xServ>");
        xml.append("<chCTe>").append(chaveCte).append("</chCTe>");
        xml.append("</consSitCTe>");

        return xml.toString();
    }

    private void validarChave(String chaveCte) {
        if (chaveCte == null || !chaveCte.matches("\\d{44}")) {
            throw new CteException("Chave de acesso inválida. A chave deve conter exatamente 44 dígitos numéricos.");
        }
    }
}