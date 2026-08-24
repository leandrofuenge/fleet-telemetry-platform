package com.telemetria.integration.nfe;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enums.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enums.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoAtorInteressado;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoAtorInteressado;

/**
 * Serviço responsável pelo evento de Ator Interessado.
 */
class AtorInteressado {

    private AtorInteressado() {
        // Classe utilitária.
    }

    static TRetEnvEventoAtorInteressado eventoAtorInteressado(
            ConfiguracoesNfe config,
            TEnvEventoAtorInteressado enviEvento,
            boolean valida) throws ExcecaoNfe {

        validarParametros(
                config,
                enviEvento
        );

        return EventoNfeSender.enviar(
                config,
                enviEvento,
                TRetEnvEventoAtorInteressado.class,
                ServicosEnum.ATOR_INTERESSADO,
                DocumentoEnum.NFE,
                valida);
    }

    private static void validarParametros(
            ConfiguracoesNfe config,
            TEnvEventoAtorInteressado enviEvento)
            throws ExcecaoNfe {

        if (config == null) {
            throw new ExcecaoNfe(
                    "Configurações da NFe não informadas."
            );
        }

        if (config.getCertificado() == null) {
            throw new ExcecaoNfe(
                    "Certificado digital não configurado."
            );
        }

        if (enviEvento == null) {
            throw new ExcecaoNfe(
                    "Evento de ator interessado não informado."
            );
        }

        if (config.getEncode() == null) {

            throw new ExcecaoNfe(
                    "Encoding da NFe não configurado."
            );
        }
    }
}
