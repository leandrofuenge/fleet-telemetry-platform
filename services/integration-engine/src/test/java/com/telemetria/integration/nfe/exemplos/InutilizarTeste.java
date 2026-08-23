/**
 *
 */
package com.telemetria.integration.nfe.exemplos;

import java.time.LocalDateTime;

import com.telemetria.integration.nfe.Nfe;
import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.AmbienteEnum;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.EstadosEnum;
import com.telemetria.integration.nfe.schemas.TInutNFe;
import com.telemetria.integration.nfe.schemas.TRetInutNFe;
import com.telemetria.integration.nfe.util.InutilizacaoUtil;
import com.telemetria.integration.nfe.util.RetornoUtil;
import com.telemetria.integration.nfe.util.XmlNfeUtil;

/**
 * @author Samuel Oliveira
 *
 */
public class InutilizarTeste {

	public static void main(String[] args) {

		try {

			/// Inicia As Configurações
            ConfiguracoesNfe config = ConfiguracaoTeste.iniciaConfiguracoes(EstadosEnum.GO, AmbienteEnum.HOMOLOGACAO);

            //Informe o CNPJ do emitente
            String cnpj = "10732644000128";
            //Informe a serie
            int serie = 1;
            //Informe a numeracao Inicial
            int numeroInicial = 243;
            //Informe a numeracao Final
            int numeroFinal = 244;
            //Informe a Justificativa da Inutilizacao
            String justificativa = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
            //Informe a data do Cancelamento
            LocalDateTime dataCancelamento = LocalDateTime.now();

            //MOnta Inutilização
            TInutNFe inutNFe = InutilizacaoUtil.montaInutilizacao(DocumentoEnum.NFCE,cnpj,serie,numeroInicial,numeroFinal,justificativa,dataCancelamento,config);

            System.out.println(XmlNfeUtil.objectToXml(inutNFe));
//            //Envia Inutilização
			TRetInutNFe retorno = Nfe.inutilizacao(config,inutNFe, DocumentoEnum.NFCE,true);

            //Valida o Retorno da Inutilização
            RetornoUtil.validaInutilizacao(retorno);

            //Resultado
            System.out.println();
            System.out.println("# Status: " + retorno.getInfInut().getCStat() + " - " + retorno.getInfInut().getXMotivo());
            System.out.println("# Protocolo: " + retorno.getInfInut().getNProt());

            //Cria ProcEvento da Inutilização
            String proc = InutilizacaoUtil.criaProcInutilizacao(config,inutNFe, retorno);
            System.out.println();
            System.out.println("# ProcInutilizacao : " + proc);


		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

	}

}
