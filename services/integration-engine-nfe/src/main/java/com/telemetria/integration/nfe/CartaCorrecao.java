package com.telemetria.integration.nfe;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enums.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enums.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TEnvEventoCartaCorrecao;
import com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TRetEnvEventoCartaCorrecao;

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
