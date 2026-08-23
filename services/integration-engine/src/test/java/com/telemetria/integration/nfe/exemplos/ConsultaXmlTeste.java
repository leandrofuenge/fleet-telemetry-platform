package com.telemetria.integration.nfe.exemplos;

import com.telemetria.integration.nfe.Nfe;
import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.AmbienteEnum;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.EstadosEnum;
import com.telemetria.integration.nfe.schemas.TRetConsSitNFe;
import com.telemetria.integration.nfe.util.XmlNfeUtil;

/**
 * @author Samuel Oliveira
 *
 */
public class ConsultaXmlTeste {

	public static void main(String[] args) {

		try {

            // Inicia As Configurações
            ConfiguracoesNfe config = ConfiguracaoTeste.iniciaConfiguracoes(EstadosEnum.GO, AmbienteEnum.HOMOLOGACAO);

            //Informe a chave a ser Consultada
            String chave = "52260610732644000128550010000927561865874165";

            //Efetua a consulta
			TRetConsSitNFe retorno = Nfe.consultaXml(config, chave, DocumentoEnum.NFE);

            //Resultado
            System.out.println("XML: "+ XmlNfeUtil.objectToXml(retorno));
            System.out.println("# Status: " + retorno.getCStat() + " - " + retorno.getXMotivo());


		} catch (Exception e) {
			System.err.println();
			System.err.println(e.getMessage());
		}
	}
}
