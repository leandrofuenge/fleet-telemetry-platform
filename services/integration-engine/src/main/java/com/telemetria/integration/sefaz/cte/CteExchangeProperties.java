package com.telemetria.integration.sefaz.cte;

/**
 * Propriedades utilizadas entre os processors do fluxo Camel.
 */
public final class CteExchangeProperties {

    private CteExchangeProperties() {
    }

    public static final String CTE_METADATA =
            "cte.metadata";

    public static final String CTE_XML_NORMALIZADO =
            "cte.xml.normalizado";

    public static final String CTE_CHAVE =
            "cte.chave";

    public static final String CTE_NUMERO =
            "cte.numero";

    public static final String CTE_SERIE =
            "cte.serie";

    public static final String CTE_MODELO =
            "cte.modelo";

    public static final String CTE_PROCESSAMENTO_FALHOU =
            "cte.processamento.falhou";

    public static final String CTE_ERRO =
            "cte.erro";
}