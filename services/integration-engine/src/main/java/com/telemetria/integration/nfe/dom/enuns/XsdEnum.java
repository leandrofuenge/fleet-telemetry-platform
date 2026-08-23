package com.telemetria.integration.nfe.dom.enuns;

import com.telemetria.integration.nfe.exception.NfeException;
import com.telemetria.integration.nfe.schemas.DistDFeInt;
import com.telemetria.integration.nfe.schemas.RetDistDFeInt;
import com.telemetria.integration.nfe.schemas.TConsCad;
import com.telemetria.integration.nfe.schemas.TConsReciNFe;
import com.telemetria.integration.nfe.schemas.TConsSitNFe;
import com.telemetria.integration.nfe.schemas.TConsStatServ;
import com.telemetria.integration.nfe.schemas.TEnviNFe;
import com.telemetria.integration.nfe.schemas.TInutNFe;
import com.telemetria.integration.nfe.schemas.TNFe;
import com.telemetria.integration.nfe.schemas.TNfeProc;
import com.telemetria.integration.nfe.schemas.TProcInutNFe;
import com.telemetria.integration.nfe.schemas.TRetConsCad;
import com.telemetria.integration.nfe.schemas.TRetConsReciNFe;
import com.telemetria.integration.nfe.schemas.TRetConsSitNFe;
import com.telemetria.integration.nfe.schemas.TRetConsStatServ;
import com.telemetria.integration.nfe.schemas.TRetEnviNFe;
import com.telemetria.integration.nfe.schemas.TRetInutNFe;

public enum XsdEnum {

    //Consulta Cadastro
    CONS_CAD(TConsCad.class, "ConsCad"),
    RET_CONS_CAD(TRetConsCad.class, "retConsCad"),

    //Consulta Status Serviço
    CONS_STAT_SERV(TConsStatServ.class, "consStatServ"),
    RET_STAT_SERV(TRetConsStatServ.class, "retConsStatServ"),

    //Consulta Recibo
    CONS_RECI_NFE(TConsReciNFe.class, "consReciNFe"),
    RET_CONS_RECI_NFE(TRetConsReciNFe.class, "retConsReciNFe"),

    //Consulta Situacao
    CONS_SIT_NFE(TConsSitNFe.class, "consSitNFe"),
    RET_CONS_SIT_NFE(TRetConsSitNFe.class, "retConsSitNFe"),

    //NFe
    NFE(TNFe.class, "NFe"),
    ENVI_NFE(TEnviNFe.class, "enviNFe"),
    RET_ENVI_NFE(TRetEnviNFe.class, "retEnviNFe"),
    NFE_PROC(TNfeProc.class, "nfeProc"),
    PROT_NFE(com.telemetria.integration.nfe.schemas.TProtNFe.class, "protNFe"),

    //Inutilização
    INUT_NFE(TInutNFe.class, "inutNFe"),
    PROC_INUT_NFE(TProcInutNFe.class, "procInutNFe"),
    RET_INUT_NFE(TRetInutNFe.class, "retInutNFe"),

    //Cancelamento
    CANC_PROC_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TProcEventoCancelamento.class, "procEventoNFe"),
    CANC_ENV_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCancelamento.class, "envEvento"),
    CANC_RET_ENV_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCancelamento.class, "retEnvEvento"),

    //Cancelamento Substituicao
    CANC_SUBS_PROC_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TProcEventoCancelamentoSubstituicao.class, "procEventoNFe"),
    CANC_SUBS_ENV_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCancelamentoSubstituicao.class, "envEvento"),
    CANC_SUBS_RET_ENV_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCancelamentoSubstituicao.class, "retEnvEvento"),

    //Ator Interessado
    ATOR_INTER_PROC_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TProcEventoAtorInteressado.class, "procEventoNFe"),
    ATOR_INTER_ENV_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TEnvEventoAtorInteressado.class, "envEvento"),
    ATOR_INTER_RET_ENV_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoAtorInteressado.class, "retEnvEvento"),

    //Carta Correcao
    CCE_PROC_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TProcEventoCartaCorrecao.class, "procEventoNFe"),
    CCE_ENV_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCartaCorrecao.class, "envEvento"),
    CCE_RET_ENV_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCartaCorrecao.class, "retEnvEvento"),

    //EPEC
    EPEC_PROC_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TProcEventoEpec.class, "procEventoNFe"),
    EPEC_ENV_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TEnvEventoEpec.class, "envEvento"),
    EPEC_RET_ENV_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoEpec.class, "retEnvEvento"),

    //MANIFESTACAO
    MAN_PROC_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TProcEventoManifestacao.class, "procEventoNFe"),
    MAN_ENV_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TEnvEventoManifestacao.class, "envEvento"),
    MAN_RET_ENV_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoManifestacao.class, "retEnvEvento"),

    //INSUCESSO
    INS_PROC_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TProcEventoInsucessoEntrega.class, "procEventoNFe"),
    INS_ENV_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TEnvEventoInsucessoEntrega.class, "envEvento"),
    INS_RET_ENV_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoInsucessoEntrega.class, "retEnvEvento"),

    //CANC INSUCESSO
    _PROC_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TProcEventoCancelamentoInsucessoEntrega.class, "procEventoNFe"),
    _ENV_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCancelamentoInsucessoEntrega.class, "envEvento"),
    _RET_ENV_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCancelamentoInsucessoEntrega.class, "retEnvEvento"),

    //ECONF
    ECONF_PROC_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TProcEventoConciliacaoFinanceira.class, "procEventoNFe"),
    ECONF_ENV_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TEnvEventoConciliacaoFinanceira.class, "envEvento"),
    ECONF_RET_ENV_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoConciliacaoFinanceira.class, "retEnvEvento"),

    //CANC ECONF
    CANC_ECONF_PROC_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TProcEventoCancelamentoConciliacaoFinanceira.class, "procEventoNFe"),
    CANC_ECONF_ENV_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCancelamentoConciliacaoFinanceira.class, "envEvento"),
    CANC_ECONF_RET_ENV_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCancelamentoConciliacaoFinanceira.class, "retEnvEvento"),

    //EVENTO GENERICO
    EVENTO_GENERICO_PROC_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TProcEventoGenerico.class, "procEventoNFe"),
    EVENTO_GENERICO_ENV_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TEnvEventoGenerico.class, "envEvento"),
    EVENTO_GENERICO_RET_ENV_EVENTO(com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoGenerico.class, "retEnvEvento"),

    //DistDfe
    DIST_DFE_INT(DistDFeInt.class, "distDFeInt"),
    RET_DIST_DFE_INT(RetDistDFeInt.class, "retDistDFeInt");

    private final Class<?> clazz;
    private final String name;

    XsdEnum(Class<?> clazz, String name) {
        this.clazz = clazz;
        this.name = name;
    }

    public Class<?> getClazz() { return clazz; }
    public String getName() { return name; }

    public static XsdEnum getByClassName(String simpleClassName) throws NfeException {
        for (XsdEnum e : values()) {
            if (e.clazz.getName().equals(simpleClassName)) return e;
        }
        throw new NfeException("Xsd Não mapeado: " + simpleClassName);
    }
}
