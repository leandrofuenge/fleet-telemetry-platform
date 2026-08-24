package com.telemetria.integration.nfe;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enums.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enums.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoEpec;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoEpec;

/**
 * Data: 28/09/2017 - 11:11
 */
class Epec {

    static TRetEnvEventoEpec eventoEpec(ConfiguracoesNfe config, TEnvEventoEpec enviEvento, boolean valida) throws ExcecaoNfe {

        return EventoNfeSender.enviar(
                config,
                enviEvento,
                TRetEnvEventoEpec.class,
                ServicosEnum.EPEC,
                DocumentoEnum.NFE,
                valida);

    }

}
