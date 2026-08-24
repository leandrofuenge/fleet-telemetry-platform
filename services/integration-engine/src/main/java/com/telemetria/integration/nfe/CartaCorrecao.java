package com.telemetria.integration.nfe;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCartaCorrecao;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCartaCorrecao;

/**
 */
class CartaCorrecao {

    static TRetEnvEventoCartaCorrecao eventoCCe(ConfiguracoesNfe config, TEnvEventoCartaCorrecao enviEvento, boolean valida)
            throws ExcecaoNfe {

        return EventoNfeSender.enviar(
                config,
                enviEvento,
                TRetEnvEventoCartaCorrecao.class,
                ServicosEnum.CCE,
                DocumentoEnum.NFE,
                valida);

    }
}
