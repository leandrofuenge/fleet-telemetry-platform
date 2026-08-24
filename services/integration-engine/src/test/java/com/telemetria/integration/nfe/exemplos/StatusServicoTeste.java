/**
 *
 */
package com.telemetria.integration.nfe.exemplos;

import com.telemetria.integration.nfe.Nfe;
import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.AmbienteEnum;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.EstadosEnum;
import com.telemetria.integration.nfe.schemas.TRetConsStatServ;

/**
 */
public class StatusServicoTeste {

    public static void main(String[] args) {

        try {
            // Inicia As Configurações
            ConfiguracoesNfe config = ConfiguracaoTeste.iniciaConfiguracoes(EstadosEnum.MG, AmbienteEnum.PRODUCAO);

            try {
                //Efetua Consulta
                TRetConsStatServ retorno = Nfe.statusServico(config, DocumentoEnum.NFE);

                //Resultado
                System.out.println();
                System.out.println("# Status: " + retorno.getCStat() + " - " + retorno.getXMotivo());
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("# Erro: " + e.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("# Erro: " + e.getMessage());
        }
    }
}
