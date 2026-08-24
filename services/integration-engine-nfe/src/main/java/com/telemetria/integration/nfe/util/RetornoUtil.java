package com.telemetria.integration.nfe.util;

import com.telemetria.integration.nfe.dom.enums.StatusEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas.TRetConsReciNFe;
import com.telemetria.integration.nfe.schemas.TRetEnviNFe;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCancelamento;

/**
 * Data: 02/03/2019 - 23:05
 */
public class RetornoUtil {

    /**
     * Valida Retorno Assincrono Trasmissão de Contingencia!
     *
     * @param retorno
     * @throws ExcecaoNfe
     */
    public static void validaSincronoTrasmissaoContingencia(TRetEnviNFe retorno) throws ExcecaoNfe {

        if (!retorno.getCStat().equals(StatusEnum.LOTE_RECEBIDO.getCodigo()) && !retorno.getCStat().equals(StatusEnum.LOTE_PROCESSADO.getCodigo())) {
            throw new ExcecaoNfe(retorno.getCStat() + " - " + retorno.getXMotivo());
        }

        if (!retorno.getProtNFe().getInfProt().getCStat().equals(StatusEnum.AUTORIZADO.getCodigo()) && !retorno.getProtNFe().getInfProt().getCStat().equals(StatusEnum.AUTORIZADO_FORA_PRAZO.getCodigo())) {
            throw new ExcecaoNfe(retorno.getProtNFe().getInfProt().getCStat() + " - " + retorno.getProtNFe().getInfProt().getXMotivo());
        }
    }

    /**
     * Valida o Retorno Do Cancelamento
     *
     * @param retorno
     * @throws ExcecaoNfe
     */
    public static void validaCancelamento(TRetEnvEventoCancelamento retorno) throws ExcecaoNfe {
        if (!StatusEnum.LOTE_EVENTO_PROCESSADO.getCodigo().equals(retorno.getCStat())) {
            throw new ExcecaoNfe(retorno.getCStat() + " - " + retorno.getXMotivo());
        }

        final String[] erro = {""};
        retorno.getRetEvento().forEach(retEvento -> {
            if (!StatusEnum.EVENTO_VINCULADO.getCodigo().equals(retEvento.getInfEvento().getCStat()) &&
                !StatusEnum.CANCELAMENTO_FORA_PRAZO.getCodigo().equals(retEvento.getInfEvento().getCStat())) {
                erro[0] += retEvento.getInfEvento().getChNFe() + " - " + retEvento.getInfEvento().getCStat() + " - " + retEvento.getInfEvento().getXMotivo() + System.lineSeparator();
            }
        });

        if (ObjetoUtil.verifica(erro[0]).isPresent()) {
            throw new ExcecaoNfe(erro[0]);
        }
    }

    /**
     * Valida o Retorno Do Cancelamento Substituicao
     *
     * @param retorno
     * @throws ExcecaoNfe
     */
    public static void validaCancelamentoSubstituicao(com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCancelamentoSubstituicao retorno) throws ExcecaoNfe {
        if (!StatusEnum.LOTE_EVENTO_PROCESSADO.getCodigo().equals(retorno.getCStat())) {
            throw new ExcecaoNfe(retorno.getCStat() + " - " + retorno.getXMotivo());
        }

        final String[] erro = {""};
        retorno.getRetEvento().forEach(retEvento -> {
            if (!StatusEnum.EVENTO_VINCULADO.getCodigo().equals(retEvento.getInfEvento().getCStat())) {
                erro[0] += retEvento.getInfEvento().getChNFe() + " - " + retEvento.getInfEvento().getCStat() + " - " + retEvento.getInfEvento().getXMotivo() + System.lineSeparator();
            }
        });

        if (ObjetoUtil.verifica(erro[0]).isPresent()) {
            throw new ExcecaoNfe(erro[0]);
        }
    }

    /**
     * Valida o Retorno Do Evento Generico
     *
     * @param retorno
     * @throws ExcecaoNfe
     */
    public static void validaEventoGenerico(com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoGenerico retorno) throws ExcecaoNfe {
        if (!StatusEnum.LOTE_EVENTO_PROCESSADO.getCodigo().equals(retorno.getCStat())) {
            throw new ExcecaoNfe(retorno.getCStat() + " - " + retorno.getXMotivo());
        }

        final String[] erro = {""};
        retorno.getRetEvento().forEach(retEvento -> {
            if (!StatusEnum.EVENTO_VINCULADO.getCodigo().equals(retEvento.getInfEvento().getCStat())) {
                erro[0] += retEvento.getInfEvento().getChNFe() + " - " + retEvento.getInfEvento().getCStat() + " - " + retEvento.getInfEvento().getXMotivo() + System.lineSeparator();
            }
        });

        if (ObjetoUtil.verifica(erro[0]).isPresent()) {
            throw new ExcecaoNfe(erro[0]);
        }
    }

    /**
     * Valida o Retorno Da Manifestação
     *
     * @param retorno
     * @throws ExcecaoNfe
     */
    public static void validaManifestacao(com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoManifestacao retorno) throws ExcecaoNfe {
        if (!StatusEnum.LOTE_EVENTO_PROCESSADO.getCodigo().equals(retorno.getCStat())) {
            throw new ExcecaoNfe(retorno.getCStat() + " - " + retorno.getXMotivo());
        }

        final String[] erro = {""};
        retorno.getRetEvento().forEach(retEvento -> {
            if (!StatusEnum.EVENTO_VINCULADO.getCodigo().equals(retEvento.getInfEvento().getCStat()) && !StatusEnum.EVENTO_REGISTRADO_NAO_VINCULADO.getCodigo().equals(retEvento.getInfEvento().getCStat())) {
                erro[0] += retEvento.getInfEvento().getChNFe() + " - " + retEvento.getInfEvento().getCStat() + " - " + retEvento.getInfEvento().getXMotivo() + System.lineSeparator();
            }
        });

        if (ObjetoUtil.verifica(erro[0]).isPresent()) {
            throw new ExcecaoNfe(erro[0]);
        }

    }

    /**
     * Valida o Retorno Da Carta de Correção
     *
     * @param retorno
     * @throws ExcecaoNfe
     */
    public static void validaCartaCorrecao(com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCartaCorrecao retorno) throws ExcecaoNfe {
        if (!StatusEnum.LOTE_EVENTO_PROCESSADO.getCodigo().equals(retorno.getCStat())) {
            throw new ExcecaoNfe(retorno.getCStat() + " - " + retorno.getXMotivo());
        }
        final String[] erro = {""};
        retorno.getRetEvento().forEach(retEvento -> {
            if (!StatusEnum.EVENTO_VINCULADO.getCodigo().equals(retEvento.getInfEvento().getCStat())) {
                erro[0] += retEvento.getInfEvento().getChNFe() + " - " + retEvento.getInfEvento().getCStat() + " - " + retEvento.getInfEvento().getXMotivo() + System.lineSeparator();
            }
        });

        if (ObjetoUtil.verifica(erro[0]).isPresent()) {
            throw new ExcecaoNfe(erro[0]);
        }
    }

    /**
     * Valida o Retorno Do EPEC
     *
     * @param retorno
     * @throws ExcecaoNfe
     */
    public static void validaEpec(com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoEpec retorno) throws ExcecaoNfe {
        if (!StatusEnum.LOTE_EVENTO_PROCESSADO.getCodigo().equals(retorno.getCStat())) {
            throw new ExcecaoNfe(retorno.getCStat() + " - " + retorno.getXMotivo());

        }

        final String[] erro = {""};
        retorno.getRetEvento().forEach(retEvento -> {
            if (!StatusEnum.EVENTO_VINCULADO.getCodigo().equals(retEvento.getInfEvento().getCStat())) {
                erro[0] += retEvento.getInfEvento().getChNFe() + " - " + retEvento.getInfEvento().getCStat() + " - " + retEvento.getInfEvento().getXMotivo() + System.lineSeparator();
            }
        });

        if (ObjetoUtil.verifica(erro[0]).isPresent()) {
            throw new ExcecaoNfe(erro[0]);
        }
    }

    /**
     * Valida o Retorno Da Consulta Cadastro
     *
     * @param retorno
     * @throws ExcecaoNfe
     */
    public static void validaConsultaCadastro(com.telemetria.integration.nfe.schemas.TRetConsCad retorno) throws ExcecaoNfe {
        if (!retorno.getInfCons().getCStat().equals(StatusEnum.CADASTRO_ENCONTRADO.getCodigo())) {
            throw new ExcecaoNfe(retorno.getInfCons().getCStat() + " - " + retorno.getInfCons().getXMotivo());
        }
    }

    /**
     * Valida o Retorno Da Inutilização
     *
     * @param retorno
     * @throws ExcecaoNfe
     */
    public static void validaInutilizacao(com.telemetria.integration.nfe.schemas.TRetInutNFe retorno) throws ExcecaoNfe {
        if (!retorno.getInfInut().getCStat().equals(StatusEnum.INUTILIZADO.getCodigo())) {
            throw new ExcecaoNfe(retorno.getInfInut().getCStat() + " - " + retorno.getInfInut().getXMotivo());
        }
    }

    /**
     * Verifica se Restorno é Sincrono ou Assincrono
     *
     * @param retorno
     * @return
     */
    public static boolean isRetornoAssincrono(TRetEnviNFe retorno) throws ExcecaoNfe {
        if (!retorno.getCStat().equals(StatusEnum.LOTE_RECEBIDO.getCodigo()) && !retorno.getCStat().equals(StatusEnum.LOTE_PROCESSADO.getCodigo())) {
            throw new ExcecaoNfe(retorno.getCStat() + " - " + retorno.getXMotivo());
        }

        return retorno.getCStat().equals(StatusEnum.LOTE_RECEBIDO.getCodigo());
    }

    /**
     * Valida Retorno Assincrono
     *
     * @param retorno
     * @throws ExcecaoNfe
     */
    public static void validaAssincrono(TRetConsReciNFe retorno) throws ExcecaoNfe {

        if (!retorno.getCStat().equals(StatusEnum.LOTE_PROCESSADO.getCodigo())) {
            throw new ExcecaoNfe(retorno.getCStat() + " - " + retorno.getXMotivo());
        }

        final String[] erro = {""};
        retorno.getProtNFe().forEach(protNFe -> {
            if (!StatusEnum.AUTORIZADO.getCodigo().equals(protNFe.getInfProt().getCStat()) && !StatusEnum.AUTORIZADO_FORA_PRAZO.getCodigo().equals(protNFe.getInfProt().getCStat())) {
                erro[0] += protNFe.getInfProt().getChNFe() + " - " + protNFe.getInfProt().getCStat() + " - " + protNFe.getInfProt().getXMotivo() + System.lineSeparator();
            }
        });

        if (ObjetoUtil.verifica(erro[0]).isPresent()) {
            throw new ExcecaoNfe(erro[0]);
        }

    }

    /**
     * Valida Retorno Assincrono
     *
     * @param retorno
     * @throws ExcecaoNfe
     */
    public static void validaSincrono(TRetEnviNFe retorno) throws ExcecaoNfe {

        if (!retorno.getCStat().equals(StatusEnum.LOTE_RECEBIDO.getCodigo()) && !retorno.getCStat().equals(StatusEnum.LOTE_PROCESSADO.getCodigo())) {
            throw new ExcecaoNfe(retorno.getCStat() + " - " + retorno.getXMotivo());
        }

        if (!retorno.getProtNFe().getInfProt().getCStat().equals(StatusEnum.AUTORIZADO.getCodigo()) &&
            !retorno.getProtNFe().getInfProt().getCStat().equals(StatusEnum.AUTORIZADO_FORA_PRAZO.getCodigo())) {
            throw new ExcecaoNfe(retorno.getProtNFe().getInfProt().getCStat() + " - " + retorno.getProtNFe().getInfProt().getXMotivo());
        }
    }

}
