/**
 *
 */
package com.telemetria.integration.nfe.exemplos;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.AmbienteEnum;
import com.telemetria.integration.nfe.dom.enuns.EstadosEnum;

import br.com.swconsultoria.certificado.Certificado;
import br.com.swconsultoria.certificado.CertificadoService;

/**
 *
 */
public class ConfiguracaoTeste {

    public static ConfiguracoesNfe iniciaConfiguracoes(EstadosEnum estado, AmbienteEnum ambiente) throws Exception {

        boolean habilitaLog = true;
        if (habilitaLog) {
            Logger.getLogger("").setLevel(Level.ALL);
        } else {
            Logger.getLogger("").setLevel(Level.WARNING);
        }

        Certificado certificado = CertificadoService.certificadoPfx("d:/teste/certificado.pfx", "123456");

        return ConfiguracoesNfe.criarConfiguracoes(estado, ambiente, certificado, "./schemas");
    }

}
