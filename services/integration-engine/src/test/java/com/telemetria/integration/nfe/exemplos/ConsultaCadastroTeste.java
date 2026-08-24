package com.telemetria.integration.nfe.exemplos;

import com.telemetria.integration.nfe.Nfe;
import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.AmbienteEnum;
import com.telemetria.integration.nfe.dom.enuns.EstadosEnum;
import com.telemetria.integration.nfe.dom.enuns.PessoaEnum;
import com.telemetria.integration.nfe.schemas.TRetConsCad;
import com.telemetria.integration.nfe.util.RetornoUtil;
import com.telemetria.integration.nfe.util.XmlNfeUtil;

/**
 */
public class ConsultaCadastroTeste {

    public static void main(String[] args) {

        try {

            // Inicia As Configurações
            ConfiguracoesNfe config = ConfiguracaoTeste.iniciaConfiguracoes(EstadosEnum.MG, AmbienteEnum.HOMOLOGACAO);

            try {
                //Envia a Consulta
                TRetConsCad retorno = Nfe.consultaCadastro(config, PessoaEnum.JURIDICA, "30081357000617", EstadosEnum.GO);

                //Valida o Retorno da Consulta Cadastro
                RetornoUtil.validaConsultaCadastro(retorno);

                //Resultado
                System.out.println();
                System.out.println("# Status: " + retorno.getInfCons().getCStat() + " - " + retorno.getInfCons().getXMotivo());
                System.out.println();
                retorno.getInfCons().getInfCad().forEach(cadastro -> {
                    System.out.println("# Razão Social: " + cadastro.getXNome());
                    System.out.println("# Cnpj: " + cadastro.getCNPJ());
                    System.out.println("# Ie: " + cadastro.getIE());
                });

                System.out.println(XmlNfeUtil.objectToXml(retorno));

            } catch (Exception a) {
                System.err.println();
                System.err.println(a.getMessage());
            }

            Thread.sleep(1000);

        } catch (Exception e) {
            System.err.println();
            System.err.println(e.getMessage());
        }

    }

}
