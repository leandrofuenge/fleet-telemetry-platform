package com.telemetria.integration.nfe;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enums.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enums.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCancelamentoInsucessoEntrega;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCancelamentoInsucessoEntrega;

/**
 */
class CancInsucessoEntrega {

    private CancInsucessoEntrega() {
    }

    static TRetEnvEventoCancelamentoInsucessoEntrega eventoCancInsuccessoEntrega(ConfiguracoesNfe config, TEnvEventoCancelamentoInsucessoEntrega enviEvento, boolean valida)
            throws ExcecaoNfe {

        return EventoNfeSender.enviar(
                config,
                enviEvento,
                TRetEnvEventoCancelamentoInsucessoEntrega.class,
                ServicosEnum.CANC_INSUCESSO_ENTREGA,
                DocumentoEnum.NFE,
                valida);

    }

}
