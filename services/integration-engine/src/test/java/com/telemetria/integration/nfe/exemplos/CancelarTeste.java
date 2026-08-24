/**
 *
 */
package com.telemetria.integration.nfe.exemplos;

import java.time.LocalDateTime;

import com.telemetria.integration.nfe.Nfe;
import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.Evento;
import com.telemetria.integration.nfe.dom.enuns.AmbienteEnum;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.EstadosEnum;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCancelamento;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCancelamento;
import com.telemetria.integration.nfe.util.CancelamentoUtil;
import com.telemetria.integration.nfe.util.RetornoUtil;
import com.telemetria.integration.nfe.util.XmlNfeUtil;

/**
 */
public class CancelarTeste {

    public static void main(String[] args) {

        try {

            // Inicia As Configurações
            ConfiguracoesNfe config = ConfiguracaoTeste.iniciaConfiguracoes(EstadosEnum.GO, AmbienteEnum.HOMOLOGACAO);

            //Agora o evento pode aceitar uma lista de cancelaemntos para envio em Lote.
            //Para isso Foi criado o Objeto Cancela
            Evento cancela = new Evento();
            //Informe a chave da Nota a ser Cancelada
            cancela.setChave("52260610732644000128550010000927581162933910");
            //Informe o protocolo da Nota a ser Cancelada
            cancela.setProtocolo("152260027418507");
            //Informe o CNPJ do emitente
            cancela.setCnpj("10732644000128");
            //Informe o Motivo do Cancelamento
            cancela.setMotivo("Teste de Cancelamento");
            //Informe a data do Cancelamento
            cancela.setDataEvento(LocalDateTime.now());

            //Monta o Evento de Cancelamento
            TEnvEventoCancelamento enviEvento = CancelamentoUtil.montaCancelamento(cancela, config);

            System.out.println(XmlNfeUtil.objectToXml(enviEvento));

            //Envia o Evento de Cancelamento
            TRetEnvEventoCancelamento retorno = Nfe.cancelarNfe(config, enviEvento, true, DocumentoEnum.NFCE);

            //Valida o Retorno do Cancelamento
            RetornoUtil.validaCancelamento(retorno);

            //Resultado
            System.out.println();
            retorno.getRetEvento().forEach(resultado -> {
                System.out.println("# Chave: " + resultado.getInfEvento().getChNFe());
                System.out.println("# Status: " + resultado.getInfEvento().getCStat() + " - " + resultado.getInfEvento().getXMotivo());
                System.out.println("# Protocolo: " + resultado.getInfEvento().getNProt());
            });

            //Cria ProcEvento de Cacnelamento
            String proc = CancelamentoUtil.criaProcEventoCancelamento(config, enviEvento, retorno.getRetEvento().get(0));
            System.out.println();
            System.out.println("# ProcEvento : " + proc);

        } catch (Exception e) {
            System.err.println();
            System.err.println("# Erro: " + e.getMessage());
        }

    }

}
