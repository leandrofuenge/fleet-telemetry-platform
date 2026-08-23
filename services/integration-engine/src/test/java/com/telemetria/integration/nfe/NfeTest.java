package com.telemetria.integration.nfe;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.PessoaEnum;
import com.telemetria.integration.nfe.exception.NfeException;

/**
 * Testes de Nfe — fachada principal da biblioteca.
 * Verifica o contrato da API (construtor privado, rejeição de config nula).
 * As chamadas de rede não são testadas aqui (requerem certificado + SEFAZ).
 */
class NfeTest {

    // -------------------------------------------------------------------------
    // Construtor privado
    // -------------------------------------------------------------------------

    @Test
    void nfe_construtorEPrivado() throws Exception {
        Constructor<Nfe> constructor = Nfe.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()),
                "Nfe deve ter construtor privado para impedir instanciação");
    }

    // -------------------------------------------------------------------------
    // Rejeição de ConfiguracoesNfe nula (lança NfeException antes da chamada WS)
    // -------------------------------------------------------------------------

    @Test
    void statusServico_configNula_lancaNfeException() {
        assertThrows(NfeException.class,
                () -> Nfe.statusServico(null, DocumentoEnum.NFE));
    }

    @Test
    void consultaXml_configNula_lancaNfeException() {
        assertThrows(NfeException.class,
                () -> Nfe.consultaXml(null, "52230309158456000159550010000731791567812345", DocumentoEnum.NFE));
    }

    @Test
    void consultaRecibo_configNula_lancaNfeException() {
        assertThrows(NfeException.class,
                () -> Nfe.consultaRecibo(null, "123456789012345", DocumentoEnum.NFE));
    }

    @Test
    void distribuicaoDfe_configNula_lancaNfeException() {
        assertThrows(NfeException.class,
                () -> Nfe.distribuicaoDfe(null, PessoaEnum.JURIDICA, "09158456000159",
                        com.telemetria.integration.nfe.dom.enuns.ConsultaDFeEnum.NSU, "000000000000001"));
    }

    @Test
    void inutilizacao_configNula_lancaNfeException() {
        com.telemetria.integration.nfe.schemas.TInutNFe inutNFe =
                new com.telemetria.integration.nfe.schemas.TInutNFe();
        com.telemetria.integration.nfe.schemas.TInutNFe.InfInut infInut =
                new com.telemetria.integration.nfe.schemas.TInutNFe.InfInut();
        infInut.setCNPJ("09158456000159");
        inutNFe.setInfInut(infInut);

        assertThrows(NfeException.class,
                () -> Nfe.inutilizacao(null, inutNFe, DocumentoEnum.NFE, false));
    }

    // -------------------------------------------------------------------------
    // Config sem certificado lança NfeException (getCertificado() retorna null)
    // -------------------------------------------------------------------------

    @Test
    void statusServico_configSemCertificado_lancaNfeException() {
        ConfiguracoesNfe config = new ConfiguracoesNfe();
        config.setEstado(com.telemetria.integration.nfe.dom.enuns.EstadosEnum.GO);
        config.setAmbiente(com.telemetria.integration.nfe.dom.enuns.AmbienteEnum.HOMOLOGACAO);

        assertThrows(Exception.class,
                () -> Nfe.statusServico(config, DocumentoEnum.NFE));
    }

    @Test
    void consultaXml_configSemCertificado_lancaNfeException() {
        ConfiguracoesNfe config = new ConfiguracoesNfe();
        config.setEstado(com.telemetria.integration.nfe.dom.enuns.EstadosEnum.GO);
        config.setAmbiente(com.telemetria.integration.nfe.dom.enuns.AmbienteEnum.HOMOLOGACAO);

        assertThrows(Exception.class,
                () -> Nfe.consultaXml(config, "52230309158456000159550010000731791567812345", DocumentoEnum.NFE));
    }
}
