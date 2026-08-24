package com.telemetria.integration.nfe;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoInsucessoEntrega;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoInsucessoEntrega;

/**
 */
class InsucessoEntrega {

    private InsucessoEntrega() {
    }

    static TRetEnvEventoInsucessoEntrega eventoInsuccessoEntrega(ConfiguracoesNfe config, TEnvEventoInsucessoEntrega enviEvento, boolean valida)
            throws ExcecaoNfe {

        return EventoNfeSender.enviar(
                config,
                enviEvento,
                TRetEnvEventoInsucessoEntrega.class,
                ServicosEnum.INSUCESSO_ENTREGA,
                DocumentoEnum.NFE,
                valida);

    }

}
