package com.telemetria.integration.sefaz.cte;

/**
 * DTO com o resultado estruturado da resposta da SEFAZ.
 */
public class CteResultadoParse {

    private final String cStat;
    private final String xMotivo;
    private final String nProt;

    public CteResultadoParse(String cStat, String xMotivo, String nProt) {
        this.cStat = cStat;
        this.xMotivo = xMotivo;
        this.nProt = nProt;
    }

    public String getcStat() {
        return cStat;
    }

    public String getxMotivo() {
        return xMotivo;
    }

    public String getnProt() {
        return nProt;
    }

    /**
     * Verifica se a operação foi autorizada/processada com sucesso pela SEFAZ.
     * <p>
     * Códigos de sucesso comuns:
     * <ul>
     *   <li><b>100</b>: Autorizado o uso do CT-e</li>
     *   <li><b>101</b>: Cancelamento de CT-e homologado</li>
     *   <li><b>103</b>: Lote recebido com sucesso</li>
     * </ul>
     */
    public boolean isSucesso() {
        return "100".equals(cStat) || "101".equals(cStat) || "104".equals(cStat)
                || "107".equals(cStat) || "113".equals(cStat)
                || "135".equals(cStat) || "136".equals(cStat) || "155".equals(cStat);
    }

    @Override
    public String toString() {
        return "CteResultadoParse{" +
                "cStat='" + cStat + '\'' +
                ", xMotivo='" + xMotivo + '\'' +
                ", nProt='" + nProt + '\'' +
                '}';
    }
}
