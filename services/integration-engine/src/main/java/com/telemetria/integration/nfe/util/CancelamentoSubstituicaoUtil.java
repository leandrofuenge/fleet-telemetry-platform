package com.telemetria.integration.nfe.util;

import java.util.Collections;
import java.util.List;

import com.telemetria.integration.nfe.Assinar;
import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.Evento;
import com.telemetria.integration.nfe.dom.enuns.AssinaturaEnum;
import com.telemetria.integration.nfe.dom.enuns.EventosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCancelamentoSubstituicao;
import com.telemetria.integration.nfe.schemas_eventos.TEventoCancelamentoSubstituicao;
import com.telemetria.integration.nfe.schemas_eventos.TProcEventoCancelamentoSubstituicao;
import com.telemetria.integration.nfe.schemas_eventos.TRetEventoCancelamentoSubstituicao;

import jakarta.xml.bind.JAXBException;

/**
 * @author Samuel Oliveira - samuk.exe@hotmail.com
 * Data: 02/03/2019 - 22:51
 */
public class CancelamentoSubstituicaoUtil {

    private CancelamentoSubstituicaoUtil() {}

    /**
     * MOnta o Evento de cancelamento unico
     *
     * @param cancela
     * @param configuracao
     * @return
     * @throws ExcecaoNfe
     */
    public static TEnvEventoCancelamentoSubstituicao montaCancelamento(Evento cancela, ConfiguracoesNfe configuracao) throws ExcecaoNfe {
        return montaCancelamento(Collections.singletonList(cancela), configuracao);
    }

    /**
     * MOnta o Evento de cancelamento Lote
     *
     * @param listaCancela
     * @param configuracao
     * @return
     * @throws ExcecaoNfe
     */
    public static TEnvEventoCancelamentoSubstituicao montaCancelamento(List<Evento> listaCancela, ConfiguracoesNfe configuracao) throws ExcecaoNfe {

        if (listaCancela.size() > 20) {
            throw new ExcecaoNfe("Podem ser enviados no máximo 20 eventos no Lote.");
        }

        TEnvEventoCancelamentoSubstituicao enviEvento = new TEnvEventoCancelamentoSubstituicao();
        enviEvento.setVersao(ConstantesUtil.VERSAO.EVENTO_CANCELAMENTO_SUBSTIUICAO);
        enviEvento.setIdLote("1");

        listaCancela.forEach(evento -> {
            String id = "ID" + EventosEnum.CANCELAMENTO_SUBSTITUICAO.getCodigo() + evento.getChave() + "01";

            TEventoCancelamentoSubstituicao eventoCancela = new TEventoCancelamentoSubstituicao();
            eventoCancela.setVersao(ConstantesUtil.VERSAO.EVENTO_CANCELAMENTO_SUBSTIUICAO);

            TEventoCancelamentoSubstituicao.InfEvento infoEvento = new TEventoCancelamentoSubstituicao.InfEvento();
            infoEvento.setId(id);
            infoEvento.setCOrgao(String.valueOf(configuracao.getEstado().getCodigoUF()));
            infoEvento.setTpAmb(configuracao.getAmbiente().getCodigo());

            infoEvento.setCPF(evento.getCpf());
            infoEvento.setCNPJ(evento.getCnpj());

            infoEvento.setChNFe(evento.getChave());

            infoEvento.setDhEvento(XmlNfeUtil.dataNfe(evento.getDataEvento(), configuracao.getZoneId()));
            infoEvento.setTpEvento(EventosEnum.CANCELAMENTO_SUBSTITUICAO.getCodigo());
            infoEvento.setNSeqEvento("1");
            infoEvento.setVerEvento(ConstantesUtil.VERSAO.EVENTO_CANCELAMENTO_SUBSTIUICAO);

            TEventoCancelamentoSubstituicao.InfEvento.DetEventoCancelamentoSubstituicao detEvento = new TEventoCancelamentoSubstituicao.InfEvento.DetEventoCancelamentoSubstituicao();
            detEvento.setVersao(ConstantesUtil.VERSAO.EVENTO_CANCELAMENTO_SUBSTIUICAO);
            detEvento.setDescEvento("Cancelamento por substituicao");
            detEvento.setCOrgaoAutor(String.valueOf(configuracao.getEstado().getCodigoUF()));
            detEvento.setTpAutor("1");
            detEvento.setVerAplic(ConstantesUtil.VERSAO.EVENTO_CANCELAMENTO_SUBSTIUICAO);
            detEvento.setNProt(evento.getProtocolo());
            detEvento.setXJust(evento.getMotivo());
            detEvento.setChNFeRef(evento.getChaveSusbstituta());
            infoEvento.setDetEvento(detEvento);
            eventoCancela.setInfEvento(infoEvento);
            enviEvento.getEvento().add(eventoCancela);
        });

        return enviEvento;
    }

    /**
     * Cria o ProcEvento de Cancelamento
     *
     * @param config
     * @param enviEvento
     * @param retorno
     * @return
     * @throws JAXBException
     * @throws ExcecaoNfe
     */
    public static String criaProcEventoCancelamento(ConfiguracoesNfe config, TEnvEventoCancelamentoSubstituicao enviEvento, TRetEventoCancelamentoSubstituicao retorno) throws JAXBException, ExcecaoNfe {

        String xml = XmlNfeUtil.objectToXml(enviEvento, config.getEncode());
        xml = xml.replace(" xmlns:ns2=\"http://www.w3.org/2000/09/xmldsig#\"", "").replace("<evento v", "<evento xmlns=\"http://www.portalfiscal.inf.br/nfe\" v");

        String assinado = Assinar.assinaNfe(ConfiguracoesUtil.iniciaConfiguracoes(config), xml, AssinaturaEnum.EVENTO);

        TProcEventoCancelamentoSubstituicao procEvento = new TProcEventoCancelamentoSubstituicao();
        procEvento.setVersao(ConstantesUtil.VERSAO.EVENTO_CANCELAMENTO);
        procEvento.setEvento(XmlNfeUtil.xmlToObject(assinado, TEnvEventoCancelamentoSubstituicao.class).getEvento().get(0));
        procEvento.setRetEvento(retorno);

        return XmlNfeUtil.objectToXml(procEvento, config.getEncode());
    }

}
