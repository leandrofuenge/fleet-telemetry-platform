package com.telemetria.integration.nfe.dom.enums;

import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.codigo.gerado.schemas.DistDFeInt;
import com.telemetria.integration.nfe.codigo.gerado.schemas.RetDistDFeInt;
import com.telemetria.integration.nfe.codigo.gerado.schemas.TConsCad;
import com.telemetria.integration.nfe.codigo.gerado.schemas.TConsReciNFe;
import com.telemetria.integration.nfe.codigo.gerado.schemas.TConsSitNFe;
import com.telemetria.integration.nfe.codigo.gerado.schemas.TConsStatServ;
import com.telemetria.integration.nfe.codigo.gerado.schemas.TEnviNFe;
import com.telemetria.integration.nfe.codigo.gerado.schemas.TInutNFe;
import com.telemetria.integration.nfe.codigo.gerado.schemas.TNFe;
import com.telemetria.integration.nfe.codigo.gerado.schemas.TNfeProc;
import com.telemetria.integration.nfe.codigo.gerado.schemas.TProcInutNFe;
import com.telemetria.integration.nfe.codigo.gerado.schemas.TRetConsCad;
import com.telemetria.integration.nfe.codigo.gerado.schemas.TRetConsReciNFe;
import com.telemetria.integration.nfe.codigo.gerado.schemas.TRetConsSitNFe;
import com.telemetria.integration.nfe.codigo.gerado.schemas.TRetConsStatServ;
import com.telemetria.integration.nfe.codigo.gerado.schemas.TRetEnviNFe;
import com.telemetria.integration.nfe.codigo.gerado.schemas.TRetInutNFe;

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
    PROT_NFE(com.telemetria.integration.nfe.codigo.gerado.schemas.TProtNFe.class, "protNFe"),

    //Inutilização
    INUT_NFE(TInutNFe.class, "inutNFe"),
    PROC_INUT_NFE(TProcInutNFe.class, "procInutNFe"),
    RET_INUT_NFE(TRetInutNFe.class, "retInutNFe"),

    //Cancelamento
    CANC_PROC_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TProcEventoCancelamento.class, "procEventoNFe"),
    CANC_ENV_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TEnvEventoCancelamento.class, "envEvento"),
    CANC_RET_ENV_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TRetEnvEventoCancelamento.class, "retEnvEvento"),

    //Cancelamento Substituicao
    CANC_SUBS_PROC_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TProcEventoCancelamentoSubstituicao.class, "procEventoNFe"),
    CANC_SUBS_ENV_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TEnvEventoCancelamentoSubstituicao.class, "envEvento"),
    CANC_SUBS_RET_ENV_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TRetEnvEventoCancelamentoSubstituicao.class, "retEnvEvento"),

    //Ator Interessado
    ATOR_INTER_PROC_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TProcEventoAtorInteressado.class, "procEventoNFe"),
    ATOR_INTER_ENV_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TEnvEventoAtorInteressado.class, "envEvento"),
    ATOR_INTER_RET_ENV_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TRetEnvEventoAtorInteressado.class, "retEnvEvento"),

    //Carta Correcao
    CCE_PROC_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TProcEventoCartaCorrecao.class, "procEventoNFe"),
    CCE_ENV_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TEnvEventoCartaCorrecao.class, "envEvento"),
    CCE_RET_ENV_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TRetEnvEventoCartaCorrecao.class, "retEnvEvento"),

    //EPEC
    EPEC_PROC_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TProcEventoEpec.class, "procEventoNFe"),
    EPEC_ENV_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TEnvEventoEpec.class, "envEvento"),
    EPEC_RET_ENV_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TRetEnvEventoEpec.class, "retEnvEvento"),

    //MANIFESTACAO
    MAN_PROC_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TProcEventoManifestacao.class, "procEventoNFe"),
    MAN_ENV_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TEnvEventoManifestacao.class, "envEvento"),
    MAN_RET_ENV_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TRetEnvEventoManifestacao.class, "retEnvEvento"),

    //INSUCESSO
    INS_PROC_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TProcEventoInsucessoEntrega.class, "procEventoNFe"),
    INS_ENV_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TEnvEventoInsucessoEntrega.class, "envEvento"),
    INS_RET_ENV_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TRetEnvEventoInsucessoEntrega.class, "retEnvEvento"),

    //CANC INSUCESSO
    _PROC_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TProcEventoCancelamentoInsucessoEntrega.class, "procEventoNFe"),
    _ENV_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TEnvEventoCancelamentoInsucessoEntrega.class, "envEvento"),
    _RET_ENV_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TRetEnvEventoCancelamentoInsucessoEntrega.class, "retEnvEvento"),

    //ECONF
    ECONF_PROC_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TProcEventoConciliacaoFinanceira.class, "procEventoNFe"),
    ECONF_ENV_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TEnvEventoConciliacaoFinanceira.class, "envEvento"),
    ECONF_RET_ENV_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TRetEnvEventoConciliacaoFinanceira.class, "retEnvEvento"),

    //CANC ECONF
    CANC_ECONF_PROC_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TProcEventoCancelamentoConciliacaoFinanceira.class, "procEventoNFe"),
    CANC_ECONF_ENV_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TEnvEventoCancelamentoConciliacaoFinanceira.class, "envEvento"),
    CANC_ECONF_RET_ENV_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TRetEnvEventoCancelamentoConciliacaoFinanceira.class, "retEnvEvento"),

    //EVENTO GENERICO
    EVENTO_GENERICO_PROC_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TProcEventoGenerico.class, "procEventoNFe"),
    EVENTO_GENERICO_ENV_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TEnvEventoGenerico.class, "envEvento"),
    EVENTO_GENERICO_RET_ENV_EVENTO(com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TRetEnvEventoGenerico.class, "retEnvEvento"),

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

    public static XsdEnum getByClassName(String simpleClassName) throws ExcecaoNfe {
        for (XsdEnum e : values()) {
            if (e.clazz.getName().equals(simpleClassName)) return e;
        }
        throw new ExcecaoNfe("Xsd Não mapeado: " + simpleClassName);
    }
}
