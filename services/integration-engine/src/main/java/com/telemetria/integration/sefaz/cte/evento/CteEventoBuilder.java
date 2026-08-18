package com.telemetria.integration.sefaz.cte.evento;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.telemetria.integration.sefaz.cte.CteException;

/**
 * Construtor do XML para Eventos do CT-e v4.00 (Cancelamento, CC-e, etc.).
 */
@Component
public class CteEventoBuilder {

    private static final String NAMESPACE_CTE = "http://www.portalfiscal.inf.br/cte";
    private static final String CODIGO_EVENTO_CANCELAMENTO = "110111";

    /**
     * Monta o XML do Evento de Cancelamento do CT-e (pronto para ser assinado pelo XmlSigner).
     *
     * @param chaveCte     Chave de acesso de 44 dígitos do CT-e a ser cancelado
     * @param nProt        Número do protocolo de autorização do CT-e
     * @param xJust        Justificativa do cancelamento (mínimo 15 caracteres)
     * @param cnpjEmissor  CNPJ do emitente do CT-e (apenas números)
     * @param tpAmb        1 = Produção, 2 = Homologação
     * @param cUF          Código IBGE da UF do emitente (ex: "51" para MT)
     * @return XML string não assinado do eventoCTe
     */
    public String buildXmlCancelamento(String chaveCte, String nProt, String xJust, String cnpjEmissor, String tpAmb, String cUF) {
        validarParametrosCancelamento(chaveCte, nProt, xJust, cnpjEmissor);

        String idEvento = "ID" + CODIGO_EVENTO_CANCELAMENTO + chaveCte + "01";
        String dhEvento = LocalDateTime.now(ZoneId.of("America/Cuiaba"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));

        StringBuilder xml = new StringBuilder();
        xml.append("<eventoCTe xmlns=\"").append(NAMESPACE_CTE).append("\" versao=\"4.00\">");
        xml.append("<infEvento Id=\"").append(idEvento).append("\">");
        xml.append("<cOrgao>").append(cUF).append("</cOrgao>");
        xml.append("<tpAmb>").append(tpAmb).append("</tpAmb>");
        xml.append("<CNPJ>").append(cnpjEmissor).append("</CNPJ>");
        xml.append("<chCTe>").append(chaveCte).append("</chCTe>");
        xml.append("<dhEvento>").append(dhEvento).append("</dhEvento>");
        xml.append("<tpEvento>").append(CODIGO_EVENTO_CANCELAMENTO).append("</tpEvento>");
        xml.append("<nSeqEvento>1</nSeqEvento>");
        xml.append("<detEvento versaoEvento=\"4.00\">");
        xml.append("<evCancCTe>");
        xml.append("<descEvento>Cancelamento</descEvento>");
        xml.append("<nProt>").append(nProt).append("</nProt>");
        xml.append("<xJust>").append(sanitizarTexto(xJust)).append("</xJust>");
        xml.append("</evCancCTe>");
        xml.append("</detEvento>");
        xml.append("</infEvento>");
        xml.append("</eventoCTe>");

        return xml.toString();
    }

    private void validarParametrosCancelamento(String chaveCte, String nProt, String xJust, String cnpjEmissor) {
        if (chaveCte == null || chaveCte.length() != 44) {
            throw new CteException("Chave de acesso do CT-e deve conter exatamente 44 dígitos.");
        }
        if (nProt == null || nProt.isBlank()) {
            throw new CteException("Número do protocolo de autorização é obrigatório para cancelamento.");
        }
        if (xJust == null || xJust.trim().length() < 15) {
            throw new CteException("Justificativa do cancelamento deve possuir no mínimo 15 caracteres.");
        }
        if (cnpjEmissor == null || cnpjEmissor.length() != 14) {
            throw new CteException("CNPJ do emitente deve conter exatamente 14 dígitos.");
        }
    }

    private String sanitizarTexto(String texto) {
        return texto.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}