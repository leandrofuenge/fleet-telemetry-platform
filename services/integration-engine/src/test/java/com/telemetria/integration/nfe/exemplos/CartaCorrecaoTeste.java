package com.telemetria.integration.nfe.exemplos;

import java.time.LocalDateTime;

import com.telemetria.integration.nfe.Assinar;
import com.telemetria.integration.nfe.Nfe;
import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.Evento;
import com.telemetria.integration.nfe.dom.enuns.AmbienteEnum;
import com.telemetria.integration.nfe.dom.enuns.AssinaturaEnum;
import com.telemetria.integration.nfe.dom.enuns.EstadosEnum;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCartaCorrecao;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCartaCorrecao;
import com.telemetria.integration.nfe.util.CartaCorrecaoUtil;
import com.telemetria.integration.nfe.util.RetornoUtil;
import com.telemetria.integration.nfe.util.XmlNfeUtil;

/**
 * @author Samuel Oliveira
 *
 */
public class CartaCorrecaoTeste {

	public static void main(String[] args) {

		try {

            // Inicia As Configurações
            ConfiguracoesNfe config = ConfiguracaoTeste.iniciaConfiguracoes(EstadosEnum.GO, AmbienteEnum.HOMOLOGACAO);

            //Agora o evento pode aceitar uma lista de cancelaemntos para envio em Lote.
            //Para isso Foi criado o Objeto Cancela
            Evento cce = new Evento();
            //Informe a chave da Nota a ser feita a CArta de Correção
            cce.setChave("52250810732644000128550010000927501960446967");
            //Informe o CNPJ do emitente
            cce.setCnpj("10732644000128");
            //Informe o Texto da Carta de Correção
            cce.setMotivo("Teste de Carta de Correção");
            //Informe a data da Carta de Correção
            cce.setDataEvento(LocalDateTime.now());
            //Informe a sequencia do Evento
            cce.setSequencia(1);

			// Monta o Evento
            TEnvEventoCartaCorrecao envEvento = CartaCorrecaoUtil.montaCCe(cce,config);

            System.out.println(XmlNfeUtil.objectToXml(envEvento));

//            //Envia a CCe
            TRetEnvEventoCartaCorrecao retorno = Nfe.cce(config, envEvento, true);

            //Valida o Retorno do Carta de Correção
            RetornoUtil.validaCartaCorrecao(retorno);

            //Resultado
            System.out.println();
            retorno.getRetEvento().forEach( resultado -> {
                System.out.println("# Chave: " + resultado.getInfEvento().getChNFe());
                System.out.println("# Status: " + resultado.getInfEvento().getCStat() + " - " + resultado.getInfEvento().getXMotivo());
                System.out.println("# Protocolo: " + resultado.getInfEvento().getNProt());
            });

            String xml = XmlNfeUtil.objectToXml(envEvento);
            xml = xml.replaceAll(" xmlns:ns2=\"http://www.w3.org/2000/09/xmldsig#\"", "");
            xml = xml.replaceAll("<evento v", "<evento xmlns=\"http://www.portalfiscal.inf.br/nfe\" v");
            xml = Assinar.assinaNfe(config, xml, AssinaturaEnum.EVENTO);
            envEvento = XmlNfeUtil.xmlToObject(xml, TEnvEventoCartaCorrecao.class);

            //Cria ProcEvento da CCe
            String proc = CartaCorrecaoUtil.criaProcEventoCCe(config, envEvento, retorno);
            System.out.println();
            System.out.println("# ProcEvento : " + proc);

		} catch (Exception e) {
            System.err.println();
            System.err.println(e.getMessage());
		}

	}

}
