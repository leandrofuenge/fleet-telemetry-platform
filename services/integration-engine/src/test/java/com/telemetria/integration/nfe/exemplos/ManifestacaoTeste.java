/**
 *
 */
package com.telemetria.integration.nfe.exemplos;

import java.time.LocalDateTime;

import com.telemetria.integration.nfe.Nfe;
import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.Evento;
import com.telemetria.integration.nfe.dom.enuns.AmbienteEnum;
import com.telemetria.integration.nfe.dom.enuns.EstadosEnum;
import com.telemetria.integration.nfe.dom.enuns.ManifestacaoEnum;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoManifestacao;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoManifestacao;
import com.telemetria.integration.nfe.util.ManifestacaoUtil;
import com.telemetria.integration.nfe.util.RetornoUtil;

/**
 */
public class ManifestacaoTeste {

    public static void main(String[] args) {

        try {

            // Inicia As Configurações
            ConfiguracoesNfe config = ConfiguracaoTeste.iniciaConfiguracoes(EstadosEnum.GO, AmbienteEnum.HOMOLOGACAO);
            //Para isso Foi criado o Objeto Manifestada
            Evento manifesta = new Evento();
            //Informe a chave da Nota a ser Manifestada
            manifesta.setChave("52200237874385000126550020000447071000447081");
            //Informe o CNPJ do emitente
            manifesta.setCnpj("10732644000128");
            //Caso o Tipo de manifestação seja OPERAÇÂO Não REALIZADA, Informe o Motivo do Manifestacao
//            manifesta.setMotivo("Teste Operação Não Realizada");
            //Informe a data do Manifestacao
            manifesta.setDataEvento(LocalDateTime.now());
            //Informe o Tipo da Manifestação
            manifesta.setTipoManifestacao(ManifestacaoEnum.CIENCIA_DA_OPERACAO);

            //Monta o Evento de Manifestação
            TEnvEventoManifestacao enviEvento = ManifestacaoUtil.montaManifestacao(manifesta, config);

            //Envia o Evento de Manifestação
            TRetEnvEventoManifestacao retorno = Nfe.manifestacao(config, enviEvento, false);

            //Valida o Retorno do Cancelamento
            RetornoUtil.validaManifestacao(retorno);

            //Resultado
            System.out.println();
            retorno.getRetEvento().forEach( resultado -> {
                System.out.println("# Chave: " + resultado.getInfEvento().getChNFe());
                System.out.println("# Status: " + resultado.getInfEvento().getCStat() + " - " + resultado.getInfEvento().getXMotivo());
                System.out.println("# Protocolo: " + resultado.getInfEvento().getNProt());
            });

            //Cria ProcEvento de Manifestacao
            String proc = ManifestacaoUtil.criaProcEventoManifestacao(config, enviEvento, retorno.getRetEvento().get(0));
            System.out.println();
            System.out.println("# ProcEvento : " + proc);

        } catch (Exception e) {
            System.err.println();
            System.err.println("# Erro: "+e.getMessage());
        }


    }

}
