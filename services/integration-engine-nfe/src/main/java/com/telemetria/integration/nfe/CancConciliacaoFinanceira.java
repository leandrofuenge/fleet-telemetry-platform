package com.telemetria.integration.nfe;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enums.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enums.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TEnvEventoCancelamentoConciliacaoFinanceira;
import com.telemetria.integration.nfe.codigo.gerado.schemas_eventos.TRetEnvEventoCancelamentoConciliacaoFinanceira;

/**
 */
class CancConciliacaoFinanceira {

    private CancConciliacaoFinanceira() {
    }

    static TRetEnvEventoCancelamentoConciliacaoFinanceira eventoEConf(ConfiguracoesNfe config, TEnvEventoCancelamentoConciliacaoFinanceira enviEvento, boolean valida)
            throws ExcecaoNfe {

        return EventoNfeSender.enviar(
                config,
                enviEvento,
                TRetEnvEventoCancelamentoConciliacaoFinanceira.class,
                ServicosEnum.CANC_ECONF,
                DocumentoEnum.NFE,
                valida);

    }

}
