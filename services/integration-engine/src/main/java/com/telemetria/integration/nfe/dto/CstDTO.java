package com.telemetria.integration.nfe.dto;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representando um CST (Código de Situação Tributária) do CFF.
 * Tolerante a mudanças: campos desconhecidos são ignorados automaticamente.
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CstDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("CST")
    private String cst;

    public String getCst() { return cst; }
    public void setCst(String value) { cst = value; }

    @JsonProperty("DescricaoCST")
    private String descricaoCST;

    public String getDescricaoCST() { return descricaoCST; }
    public void setDescricaoCST(String value) { descricaoCST = value; }

    @JsonProperty("IndIBSCBS")
    private Boolean indIBSCBS;

    public Boolean getIndIBSCBS() { return indIBSCBS; }
    public void setIndIBSCBS(Boolean value) { indIBSCBS = value; }

    @JsonProperty("IndRedBC")
    private Boolean indRedBC;

    public Boolean getIndRedBC() { return indRedBC; }
    public void setIndRedBC(Boolean value) { indRedBC = value; }

    @JsonProperty("IndRedAliq")
    private Boolean indRedAliq;

    public Boolean getIndRedAliq() { return indRedAliq; }
    public void setIndRedAliq(Boolean value) { indRedAliq = value; }

    @JsonProperty("IndTransfCred")
    private Boolean indTransfCred;

    public Boolean getIndTransfCred() { return indTransfCred; }
    public void setIndTransfCred(Boolean value) { indTransfCred = value; }

    @JsonProperty("IndDif")
    private Boolean indDif;

    public Boolean getIndDif() { return indDif; }
    public void setIndDif(Boolean value) { indDif = value; }

    @JsonProperty("IndAjusteCompet")
    private Boolean indAjusteCompet;

    public Boolean getIndAjusteCompet() { return indAjusteCompet; }
    public void setIndAjusteCompet(Boolean value) { indAjusteCompet = value; }

    @JsonProperty("IndIBSCBSMono")
    private Boolean indIBSCBSMono;

    public Boolean getIndIBSCBSMono() { return indIBSCBSMono; }
    public void setIndIBSCBSMono(Boolean value) { indIBSCBSMono = value; }

    @JsonProperty("IndCredPresIBSZFM")
    private Boolean indCredPresIBSZFM;

    public Boolean getIndCredPresIBSZFM() { return indCredPresIBSZFM; }
    public void setIndCredPresIBSZFM(Boolean value) { indCredPresIBSZFM = value; }

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

    @JsonProperty("classificacoesTributarias")
    private List<ClassificacaoTributariaDTO> classificacoesTributarias;

    public List<ClassificacaoTributariaDTO> getClassificacoesTributarias() { return classificacoesTributarias; }
    public void setClassificacoesTributarias(List<ClassificacaoTributariaDTO> value) { classificacoesTributarias = value; }
}
