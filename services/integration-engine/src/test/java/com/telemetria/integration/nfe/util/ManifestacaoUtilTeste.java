package com.telemetria.integration.nfe.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.Evento;
import com.telemetria.integration.nfe.dom.enuns.AmbienteEnum;
import com.telemetria.integration.nfe.dom.enuns.EstadosEnum;
import com.telemetria.integration.nfe.dom.enuns.ManifestacaoEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoManifestacao;
import com.telemetria.integration.nfe.schemas_eventos.TEventoManifestacao;

class ManifestacaoUtilTeste {

    private static final String CHAVE = "52230309158456000159550010000731791567812345";
    private static final String CNPJ  = "09158456000159";

    private ConfiguracoesNfe config;

    @BeforeEach
    void setUp() {
        config = new ConfiguracoesNfe();
        config.setEstado(EstadosEnum.GO);
        config.setAmbiente(AmbienteEnum.HOMOLOGACAO);
        config.setZoneId(ZoneId.of("America/Sao_Paulo"));
    }

    private Evento novoEvento(ManifestacaoEnum tipo) {
        Evento e = new Evento();
        e.setChave(CHAVE);
        e.setCnpj(CNPJ);
        e.setTipoManifestacao(tipo);
        e.setSequencia(1);
        e.setDataEvento(LocalDateTime.of(2024, 4, 1, 12, 0, 0));
        return e;
    }

    @Test
    void montaManifestacao_confirmacao_retornaTEnvEvento() throws ExcecaoNfe {
        TEnvEventoManifestacao resultado = ManifestacaoUtil.montaManifestacao(
                novoEvento(ManifestacaoEnum.CONFIRMACAO_DA_OPERACAO), config);
        assertNotNull(resultado);
        assertEquals(1, resultado.getEvento().size());
    }

    @Test
    void montaManifestacao_tpEvento_confirmacao() throws ExcecaoNfe {
        TEnvEventoManifestacao resultado = ManifestacaoUtil.montaManifestacao(
                novoEvento(ManifestacaoEnum.CONFIRMACAO_DA_OPERACAO), config);
        TEventoManifestacao.InfEvento info = resultado.getEvento().get(0).getInfEvento();
        assertEquals(ManifestacaoEnum.CONFIRMACAO_DA_OPERACAO.getCodigo(), info.getTpEvento());
    }

    @Test
    void montaManifestacao_ciencia_tpEvento() throws ExcecaoNfe {
        TEnvEventoManifestacao resultado = ManifestacaoUtil.montaManifestacao(
                novoEvento(ManifestacaoEnum.CIENCIA_DA_OPERACAO), config);
        assertEquals(ManifestacaoEnum.CIENCIA_DA_OPERACAO.getCodigo(),
                resultado.getEvento().get(0).getInfEvento().getTpEvento());
    }

    @Test
    void montaManifestacao_desconhecimento_tpEvento() throws ExcecaoNfe {
        TEnvEventoManifestacao resultado = ManifestacaoUtil.montaManifestacao(
                novoEvento(ManifestacaoEnum.DESCONHECIMENTO_DA_OPERACAO), config);
        assertEquals(ManifestacaoEnum.DESCONHECIMENTO_DA_OPERACAO.getCodigo(),
                resultado.getEvento().get(0).getInfEvento().getTpEvento());
    }

    @Test
    void montaManifestacao_operacaoNaoRealizada_comJustificativa() throws ExcecaoNfe {
        Evento e = novoEvento(ManifestacaoEnum.OPERACAO_NAO_REALIZADA);
        e.setMotivo("Mercadoria nao recebida");

        TEnvEventoManifestacao resultado = ManifestacaoUtil.montaManifestacao(e, config);
        TEventoManifestacao.InfEvento.DetEventoManifestacao det = resultado.getEvento().get(0).getInfEvento().getDetEvento();
        assertEquals("Mercadoria nao recebida", det.getXJust());
    }

    @Test
    void montaManifestacao_sequenciaZero_usaUm() throws ExcecaoNfe {
        Evento e = novoEvento(ManifestacaoEnum.CONFIRMACAO_DA_OPERACAO);
        e.setSequencia(0); // deve ser corrigido para 1

        TEnvEventoManifestacao resultado = ManifestacaoUtil.montaManifestacao(e, config);
        assertEquals("1", resultado.getEvento().get(0).getInfEvento().getNSeqEvento());
    }

    @Test
    void montaManifestacao_lote_retornaMultiplosEventos() throws ExcecaoNfe {
        List<Evento> lista = new ArrayList<>();
        lista.add(novoEvento(ManifestacaoEnum.CONFIRMACAO_DA_OPERACAO));
        lista.add(novoEvento(ManifestacaoEnum.CIENCIA_DA_OPERACAO));

        TEnvEventoManifestacao resultado = ManifestacaoUtil.montaManifestacao(lista, config);
        assertEquals(2, resultado.getEvento().size());
    }

    @Test
    void montaManifestacao_loteAcimaDe20_lancaExcecao() {
        List<Evento> lista = new ArrayList<>();
        for (int i = 0; i < 21; i++)
            lista.add(novoEvento(ManifestacaoEnum.CIENCIA_DA_OPERACAO));

        assertThrows(ExcecaoNfe.class, () -> ManifestacaoUtil.montaManifestacao(lista, config));
    }

    @Test
    void montaManifestacao_chaveNFe_preenchida() throws ExcecaoNfe {
        TEnvEventoManifestacao resultado = ManifestacaoUtil.montaManifestacao(
                novoEvento(ManifestacaoEnum.CONFIRMACAO_DA_OPERACAO), config);
        assertEquals(CHAVE, resultado.getEvento().get(0).getInfEvento().getChNFe());
    }
}
