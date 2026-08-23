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
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCartaCorrecao;
import com.telemetria.integration.nfe.schemas_eventos.TEventoCartaCorrecao;

class CartaCorrecaoUtilTeste {

    private static final String CHAVE     = "52230309158456000159550010000731791567812345";
    private static final String CNPJ      = "09158456000159";
    private static final String CORRECAO  = "Correcao no campo de endereco do destinatario";

    private ConfiguracoesNfe config;

    @BeforeEach
    void setUp() {
        config = new ConfiguracoesNfe();
        config.setEstado(EstadosEnum.GO);
        config.setAmbiente(AmbienteEnum.HOMOLOGACAO);
        config.setZoneId(ZoneId.of("America/Sao_Paulo"));
    }

    private Evento novoCCe() {
        Evento e = new Evento();
        e.setChave(CHAVE);
        e.setCnpj(CNPJ);
        e.setMotivo(CORRECAO);
        e.setSequencia(1);
        e.setDataEvento(LocalDateTime.of(2024, 3, 10, 9, 0, 0));
        return e;
    }

    @Test
    void montaCCe_unico_retornaTEnvEvento() throws ExcecaoNfe {
        TEnvEventoCartaCorrecao resultado = CartaCorrecaoUtil.montaCCe(novoCCe(), config);
        assertNotNull(resultado);
        assertEquals(1, resultado.getEvento().size());
    }

    @Test
    void montaCCe_tpEvento_ehCCe() throws ExcecaoNfe {
        TEnvEventoCartaCorrecao resultado = CartaCorrecaoUtil.montaCCe(novoCCe(), config);
        TEventoCartaCorrecao.InfEvento info = resultado.getEvento().get(0).getInfEvento();
        assertEquals(EventosEnum.CCE.getCodigo(), info.getTpEvento());
    }

    @Test
    void montaCCe_chaveNFe_preenchida() throws ExcecaoNfe {
        TEnvEventoCartaCorrecao resultado = CartaCorrecaoUtil.montaCCe(novoCCe(), config);
        assertEquals(CHAVE, resultado.getEvento().get(0).getInfEvento().getChNFe());
    }

    @Test
    void montaCCe_correcao_preenchida() throws ExcecaoNfe {
        TEnvEventoCartaCorrecao resultado = CartaCorrecaoUtil.montaCCe(novoCCe(), config);
        assertEquals(CORRECAO, resultado.getEvento().get(0).getInfEvento().getDetEvento().getXCorrecao());
    }

    @Test
    void montaCCe_descEvento_cartaDeCorrecao() throws ExcecaoNfe {
        TEnvEventoCartaCorrecao resultado = CartaCorrecaoUtil.montaCCe(novoCCe(), config);
        assertEquals("Carta de Correcao",
                resultado.getEvento().get(0).getInfEvento().getDetEvento().getDescEvento());
    }

    @Test
    void montaCCe_lote_retornaMultiplosEventos() throws ExcecaoNfe {
        List<Evento> lista = new ArrayList<>();
        lista.add(novoCCe());
        lista.add(novoCCe());

        TEnvEventoCartaCorrecao resultado = CartaCorrecaoUtil.montaCCe(lista, config);
        assertEquals(2, resultado.getEvento().size());
    }

    @Test
    void montaCCe_loteAcimaDe20_lancaExcecao() {
        List<Evento> lista = new ArrayList<>();
        for (int i = 0; i < 21; i++) lista.add(novoCCe());

        assertThrows(ExcecaoNfe.class, () -> CartaCorrecaoUtil.montaCCe(lista, config));
    }

    @Test
    void montaCCe_idComecaComID() throws ExcecaoNfe {
        TEnvEventoCartaCorrecao resultado = CartaCorrecaoUtil.montaCCe(novoCCe(), config);
        assertTrue(resultado.getEvento().get(0).getInfEvento().getId().startsWith("ID"));
    }
}
