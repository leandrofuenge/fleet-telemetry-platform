package com.telemetria.integration.nfe.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import com.telemetria.integration.nfe.dom.enuns.EventosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCancelamento;
import com.telemetria.integration.nfe.schemas_eventos.TEventoCancelamento;

class CancelamentoUtilTeste {

    private static final String CHAVE = "52230309158456000159550010000731791567812345";
    private static final String CNPJ  = "09158456000159";
    private static final String PROTOCOLO = "352230000123456";
    private static final String MOTIVO    = "Cancelamento por erro na emissao";

    private ConfiguracoesNfe config;

    @BeforeEach
    void setUp() {
        config = new ConfiguracoesNfe();
        config.setEstado(EstadosEnum.GO);
        config.setAmbiente(AmbienteEnum.HOMOLOGACAO);
        config.setZoneId(ZoneId.of("America/Sao_Paulo"));
    }

    private Evento novoEvento() {
        Evento e = new Evento();
        e.setChave(CHAVE);
        e.setCnpj(CNPJ);
        e.setProtocolo(PROTOCOLO);
        e.setMotivo(MOTIVO);
        e.setDataEvento(LocalDateTime.of(2024, 1, 15, 10, 0, 0));
        return e;
    }

    @Test
    void montaCancelamento_unico_retornaTEnvEvento() throws ExcecaoNfe {
        TEnvEventoCancelamento resultado = CancelamentoUtil.montaCancelamento(novoEvento(), config);
        assertNotNull(resultado);
        assertEquals(1, resultado.getEvento().size());
    }

    @Test
    void montaCancelamento_tpEvento_ehCancelamento() throws ExcecaoNfe {
        TEnvEventoCancelamento resultado = CancelamentoUtil.montaCancelamento(novoEvento(), config);
        TEventoCancelamento.InfEvento info = resultado.getEvento().get(0).getInfEvento();
        assertEquals(EventosEnum.CANCELAMENTO.getCodigo(), info.getTpEvento());
    }

    @Test
    void montaCancelamento_chaveNFe_preenchida() throws ExcecaoNfe {
        TEnvEventoCancelamento resultado = CancelamentoUtil.montaCancelamento(novoEvento(), config);
        assertEquals(CHAVE, resultado.getEvento().get(0).getInfEvento().getChNFe());
    }

    @Test
    void montaCancelamento_idComecaComID() throws ExcecaoNfe {
        TEnvEventoCancelamento resultado = CancelamentoUtil.montaCancelamento(novoEvento(), config);
        String id = resultado.getEvento().get(0).getInfEvento().getId();
        assertTrue(id.startsWith("ID"));
    }

    @Test
    void montaCancelamento_protocolo_preenchido() throws ExcecaoNfe {
        TEnvEventoCancelamento resultado = CancelamentoUtil.montaCancelamento(novoEvento(), config);
        assertEquals(PROTOCOLO, resultado.getEvento().get(0).getInfEvento().getDetEvento().getNProt());
    }

    @Test
    void montaCancelamento_justificativa_preenchida() throws ExcecaoNfe {
        TEnvEventoCancelamento resultado = CancelamentoUtil.montaCancelamento(novoEvento(), config);
        assertEquals(MOTIVO, resultado.getEvento().get(0).getInfEvento().getDetEvento().getXJust());
    }

    @Test
    void montaCancelamento_lote_retornaMultiplosEventos() throws ExcecaoNfe {
        List<Evento> lista = new ArrayList<>();
        lista.add(novoEvento());
        lista.add(novoEvento());

        TEnvEventoCancelamento resultado = CancelamentoUtil.montaCancelamento(lista, config);
        assertEquals(2, resultado.getEvento().size());
    }

    @Test
    void montaCancelamento_loteAcimaDe20_lancaExcecao() {
        List<Evento> lista = new ArrayList<>();
        for (int i = 0; i < 21; i++) lista.add(novoEvento());

        assertThrows(ExcecaoNfe.class, () -> CancelamentoUtil.montaCancelamento(lista, config));
    }

    @Test
    void montaCancelamento_ambienteHomologacao_preenchido() throws ExcecaoNfe {
        TEnvEventoCancelamento resultado = CancelamentoUtil.montaCancelamento(novoEvento(), config);
        assertEquals(AmbienteEnum.HOMOLOGACAO.getCodigo(),
                resultado.getEvento().get(0).getInfEvento().getTpAmb());
    }
}
