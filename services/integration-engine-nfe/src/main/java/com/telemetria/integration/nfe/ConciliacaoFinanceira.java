package com.telemetria.integration.nfe;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enums.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enums.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TEnvEventoConciliacaoFinanceira;
import com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TRetEnvEventoConciliacaoFinanceira;

/**
 */
class ConciliacaoFinanceira {

    private ConciliacaoFinanceira() {
    }

    static TRetEnvEventoConciliacaoFinanceira eventoEConf(ConfiguracoesNfe config, TEnvEventoConciliacaoFinanceira enviEvento, DocumentoEnum documento, boolean valida)
            throws ExcecaoNfe {

        return EventoNfeSender.enviar(
                config,
                enviEvento,
                TRetEnvEventoConciliacaoFinanceira.class,
                ServicosEnum.ECONF,
                documento,
                valida);

    }

}
