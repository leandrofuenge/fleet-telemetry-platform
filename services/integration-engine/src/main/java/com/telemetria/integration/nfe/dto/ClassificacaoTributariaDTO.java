package com.telemetria.integration.nfe.dto;

import java.io.Serializable;
import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representando uma Classificação Tributária do CFF.
 * Tolerante a mudanças: campos desconhecidos são ignorados automaticamente.
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClassificacaoTributariaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("cClassTrib")
    private String cClassTrib;

    public String getCClassTrib() { return cClassTrib; }
    public void setCClassTrib(String value) { cClassTrib = value; }

    @JsonProperty("DescricaoClassTrib")
    private String descricaoClassTrib;

    public String getDescricaoClassTrib() { return descricaoClassTrib; }
    public void setDescricaoClassTrib(String value) { descricaoClassTrib = value; }

    @JsonProperty("pRedIBS")
    private BigDecimal pRedIBS;

    public BigDecimal getPRedIBS() { return pRedIBS; }
    public void setPRedIBS(BigDecimal value) { pRedIBS = value; }

    @JsonProperty("pRedCBS")
    private BigDecimal pRedCBS;

    public BigDecimal getPRedCBS() { return pRedCBS; }
    public void setPRedCBS(BigDecimal value) { pRedCBS = value; }

    @JsonProperty("IndTribRegular")
    private Boolean indTribRegular;

    public Boolean getIndTribRegular() { return indTribRegular; }
    public void setIndTribRegular(Boolean value) { indTribRegular = value; }

    @JsonProperty("IndCredPresOper")
    private Boolean indCredPresOper;

    public Boolean getIndCredPresOper() { return indCredPresOper; }
    public void setIndCredPresOper(Boolean value) { indCredPresOper = value; }

    @JsonProperty("IndEstornoCred")
    private Boolean indEstornoCred;

    public Boolean getIndEstornoCred() { return indEstornoCred; }
    public void setIndEstornoCred(Boolean value) { indEstornoCred = value; }

    @JsonProperty("MonofasiaSujeitaRetencao")
    private Boolean monofasiaSujeitaRetencao;

    public Boolean getMonofasiaSujeitaRetencao() { return monofasiaSujeitaRetencao; }
    public void setMonofasiaSujeitaRetencao(Boolean value) { monofasiaSujeitaRetencao = value; }

    @JsonProperty("MonofasiaRetidaAnt")
    private Boolean monofasiaRetidaAnt;

    public Boolean getMonofasiaRetidaAnt() { return monofasiaRetidaAnt; }
    public void setMonofasiaRetidaAnt(Boolean value) { monofasiaRetidaAnt = value; }

    @JsonProperty("MonofasiaDiferimento")
    private Boolean monofasiaDiferimento;

    public Boolean getMonofasiaDiferimento() { return monofasiaDiferimento; }
    public void setMonofasiaDiferimento(Boolean value) { monofasiaDiferimento = value; }

    @JsonProperty("MonofasiaPadrao")
    private Boolean monofasiaPadrao;

    public Boolean getMonofasiaPadrao() { return monofasiaPadrao; }
    public void setMonofasiaPadrao(Boolean value) { monofasiaPadrao = value; }

    @JsonProperty("Publicacao")
    private String publicacao;

    public String getPublicacao() { return publicacao; }
    public void setPublicacao(String value) { publicacao = value; }

    @JsonProperty("InicioVigencia")
    private String inicioVigencia;

    public String getInicioVigencia() { return inicioVigencia; }
    public void setInicioVigencia(String value) { inicioVigencia = value; }

    @JsonProperty("FimVigencia")
    private String fimVigencia;

    public String getFimVigencia() { return fimVigencia; }
    public void setFimVigencia(String value) { fimVigencia = value; }

    @JsonProperty("TipoAliquota")
    private String tipoAliquota;

    public String getTipoAliquota() { return tipoAliquota; }
    public void setTipoAliquota(String value) { tipoAliquota = value; }

    @JsonProperty("IndNFe")
    private Boolean indNFe;

    public Boolean getIndNFe() { return indNFe; }
    public void setIndNFe(Boolean value) { indNFe = value; }

    @JsonProperty("IndNFCe")
    private Boolean indNFCe;

    public Boolean getIndNFCe() { return indNFCe; }
    public void setIndNFCe(Boolean value) { indNFCe = value; }

    @JsonProperty("IndCTe")
    private Boolean indCTe;

    public Boolean getIndCTe() { return indCTe; }
    public void setIndCTe(Boolean value) { indCTe = value; }

    @JsonProperty("IndCTeOS")
    private Boolean indCTeOS;

    public Boolean getIndCTeOS() { return indCTeOS; }
    public void setIndCTeOS(Boolean value) { indCTeOS = value; }

    @JsonProperty("IndBPe")
    private Boolean indBPe;

    public Boolean getIndBPe() { return indBPe; }
    public void setIndBPe(Boolean value) { indBPe = value; }

    @JsonProperty("IndNF3e")
    private Boolean indNF3e;

    public Boolean getIndNF3e() { return indNF3e; }
    public void setIndNF3e(Boolean value) { indNF3e = value; }

    @JsonProperty("IndNFCom")
    private Boolean indNFCom;

    public Boolean getIndNFCom() { return indNFCom; }
    public void setIndNFCom(Boolean value) { indNFCom = value; }

    @JsonProperty("IndNFSE")
    private Boolean indNFSE;

    public Boolean getIndNFSE() { return indNFSE; }
    public void setIndNFSE(Boolean value) { indNFSE = value; }

    @JsonProperty("IndBPeTM")
    private Boolean indBPeTM;

    public Boolean getIndBPeTM() { return indBPeTM; }
    public void setIndBPeTM(Boolean value) { indBPeTM = value; }

    @JsonProperty("IndBPeTA")
    private Boolean indBPeTA;

    public Boolean getIndBPeTA() { return indBPeTA; }
    public void setIndBPeTA(Boolean value) { indBPeTA = value; }

    @JsonProperty("IndNFAg")
    private Boolean indNFAg;

    public Boolean getIndNFAg() { return indNFAg; }
    public void setIndNFAg(Boolean value) { indNFAg = value; }

    @JsonProperty("IndNFSVIA")
    private Boolean indNFSVIA;

    public Boolean getIndNFSVIA() { return indNFSVIA; }
    public void setIndNFSVIA(Boolean value) { indNFSVIA = value; }

    @JsonProperty("IndNFABI")
    private Boolean indNFABI;

    public Boolean getIndNFABI() { return indNFABI; }
    public void setIndNFABI(Boolean value) { indNFABI = value; }

    @JsonProperty("IndNFGas")
    private Boolean indNFGas;

    public Boolean getIndNFGas() { return indNFGas; }
    public void setIndNFGas(Boolean value) { indNFGas = value; }

    @JsonProperty("IndDERE")
    private Boolean indDERE;

    public Boolean getIndDERE() { return indDERE; }
    public void setIndDERE(Boolean value) { indDERE = value; }

    @JsonProperty("IndCompraGov")
    private Boolean indCompraGov;

    public Boolean getIndCompraGov() { return indCompraGov; }
    public void setIndCompraGov(Boolean value) { indCompraGov = value; }

    @JsonProperty("Anexo")
    private Integer anexo;

    public Integer getAnexo() { return anexo; }
    public void setAnexo(Integer value) { anexo = value; }

    @JsonProperty("Link")
    private String link;

    public String getLink() { return link; }
    public void setLink(String value) { link = value; }
}
