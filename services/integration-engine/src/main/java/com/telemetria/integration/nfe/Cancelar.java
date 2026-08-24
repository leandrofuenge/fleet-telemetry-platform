package com.telemetria.integration.nfe;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCancelamento;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCancelamento;

/**
 */
class Cancelar {

    static TRetEnvEventoCancelamento eventoCancelamento(ConfiguracoesNfe config, TEnvEventoCancelamento enviEvento, boolean valida, DocumentoEnum tipoDocumento)
            throws ExcecaoNfe {

        return EventoNfeSender.enviar(
                config,
                enviEvento,
                TRetEnvEventoCancelamento.class,
                ServicosEnum.CANCELAMENTO,
                tipoDocumento,
                valida);

    }

    static com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCancelamentoSubstituicao eventoCancelamentoSubstituicao(ConfiguracoesNfe config, com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCancelamentoSubstituicao enviEvento, boolean valida)
            throws ExcecaoNfe {

        return EventoNfeSender.enviar(
                config,
                enviEvento,
                com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCancelamentoSubstituicao.class,
                ServicosEnum.CANCELAMENTO_SUBSTITUICAO,
                DocumentoEnum.NFCE,
                valida);

    }

}
