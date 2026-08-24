package com.telemetria.integration.nfe.exemplos;

import java.time.LocalDateTime;

import com.telemetria.integration.nfe.Nfe;
import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.AmbienteEnum;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.EstadosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas_eventos.DetEventoConciliacaoFinanceira;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoConciliacaoFinanceira;
import com.telemetria.integration.nfe.schemas_eventos.TEventoConciliacaoFinanceira;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoConciliacaoFinanceira;
import com.telemetria.integration.nfe.schemas_eventos.TUfEmi;
import com.telemetria.integration.nfe.util.XmlNfeUtil;

/**
 *
 */
public class EConfTeste {

    public static void main(String[] args) {

        try {

            // Inicia As Configurações
            ConfiguracoesNfe config = ConfiguracaoTeste.iniciaConfiguracoes(EstadosEnum.GO, AmbienteEnum.PRODUCAO);

            // Monta o Evento
            TEnvEventoConciliacaoFinanceira envEvento = new TEnvEventoConciliacaoFinanceira();
            envEvento.setVersao("1.00");
            envEvento.setIdLote("1");

            TEventoConciliacaoFinanceira evento = new TEventoConciliacaoFinanceira();
            evento.setVersao("1.00");
            TEventoConciliacaoFinanceira.InfEvento infEvento = new TEventoConciliacaoFinanceira.InfEvento();
            infEvento.setId("ID" + "110750" + "522511107326440001286509887049582437824" + "01");
            infEvento.setCOrgao("52");
            infEvento.setTpAmb("2");
            infEvento.setCNPJ("10732644000128");
            infEvento.setChNFe("5225111073264400012865009887049582437824");
            infEvento.setDhEvento(XmlNfeUtil.dataNfe(LocalDateTime.now()));
            infEvento.setTpEvento("110750");
            infEvento.setNSeqEvento("1");
            infEvento.setVerEvento("1.00");

            DetEventoConciliacaoFinanceira detEvento = new DetEventoConciliacaoFinanceira();
            detEvento.setVersao("1.00");
            detEvento.setDescEvento("ECONF");
            detEvento.setVerAplic("1.00");
            infEvento.setDetEvento(detEvento);
            evento.setInfEvento(infEvento);
            envEvento.getEvento().add(evento);

            DetEventoConciliacaoFinanceira.DetPag detPag = new DetEventoConciliacaoFinanceira.DetPag();
            detPag.setIndPag("1");
            detPag.setTPag("04");
            detPag.setVPag("500.00");
            detPag.setDPag("2025-11-04");
            detPag.setCNPJPag("10440482000154");
            detPag.setUFPag(TUfEmi.GO);
            detPag.setTBand("02");
            detPag.setCNPJIF("10440482000154");
            detPag.setCAut("JFMfVe");
            envEvento.getEvento().get(0).getInfEvento().getDetEvento().getDetPag().add(detPag);

            System.out.println(XmlNfeUtil.objectToXml(envEvento));

            //Envia a ECONF
            TRetEnvEventoConciliacaoFinanceira retorno = Nfe.econf(config, envEvento, DocumentoEnum.NFE, false);

            if (!retorno.getCStat().equals("128")) {
                throw new ExcecaoNfe(retorno.getCStat() + " - " + retorno.getXMotivo());
            }

            if (!retorno.getRetEvento().get(0).getInfEvento().getCStat().equals("135")) {
                throw new ExcecaoNfe(retorno.getRetEvento().get(0).getInfEvento().getCStat() + " - " + retorno.getRetEvento().get(0).getInfEvento().getXMotivo());
            }

            //Resultado
            System.out.println();
            retorno.getRetEvento().forEach(resultado -> {
                System.out.println("# Chave: " + resultado.getInfEvento().getChNFe());
                System.out.println("# Status: " + resultado.getInfEvento().getCStat() + " - " + resultado.getInfEvento().getXMotivo());
                System.out.println("# Protocolo: " + resultado.getInfEvento().getNProt());
            });

        } catch (Exception e) {
            System.err.println();
            System.err.println(e.getMessage());
        }

    }

}
