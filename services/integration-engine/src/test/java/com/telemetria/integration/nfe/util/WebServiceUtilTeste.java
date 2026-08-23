package com.telemetria.integration.nfe.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTeste;
import org.junit.jupiter.params.provider.MethodSource;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.AmbienteEnum;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.EstadosEnum;
import com.telemetria.integration.nfe.dom.enuns.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;

class WebServiceUtilTeste {

    private static Stream<Object[]> provideStateAndServiceCombinationsNfe() {
        return Stream.of(EstadosEnum.values())
                .flatMap(estado ->
                        Stream.of(ServicosEnum.values())
                                .filter(servico ->
                                        servico != ServicosEnum.CONSULTA_CADASTRO &&
                                        servico != ServicosEnum.URL_CONSULTANFCE &&
                                        servico != ServicosEnum.PROC &&
                                        servico != ServicosEnum.URL_QRCODE)
                                .flatMap(servico ->
                                        Stream.of(AmbienteEnum.values())
                                                .map(ambiente -> new Object[]{estado, servico, ambiente})));
    }

    private static Stream<Object[]> provideStateAndServiceCombinationsNfce() {
        return Stream.of(EstadosEnum.values())
                .flatMap(estado ->
                        Stream.of(ServicosEnum.values())
                                .filter(servico ->
                                        servico != ServicosEnum.CONSULTA_CADASTRO &&
                                        servico != ServicosEnum.PROC)
                                .flatMap(servico ->
                                        Stream.of(AmbienteEnum.values())
                                                .map(ambiente -> new Object[]{estado, servico, ambiente})));
    }

    @Test
    void testGetUrlWithCustomFile() throws ExcecaoNfe, IOException {
        String TEMP_WS_FILE = "temp_WebServicesNfe.ini";

        // Cria uma cópia temporária do arquivo INI para testes
        try (InputStream is = UtilitarioServicoWeb.class.getResourceAsStream("/WebServicesNfe.ini")) {
            assertNotNull(is);
            Files.copy(is, Paths.get(TEMP_WS_FILE), StandardCopyOption.REPLACE_EXISTING);
        }

        ConfiguracoesNfe config = new ConfiguracoesNfe();
        config.setEstado(EstadosEnum.SP);
        config.setAmbiente(AmbienteEnum.PRODUCAO);
        config.setArquivoWebService(TEMP_WS_FILE);

        String url = UtilitarioServicoWeb.getUrl(config, DocumentoEnum.NFE, ServicosEnum.STATUS_SERVICO);
        assertNotNull(url);

        // Remove o arquivo temporário após os testes
        Files.deleteIfExists(Paths.get(TEMP_WS_FILE));
    }

    @Test
    void testGetUrlWithFileNotFound() {
        ConfiguracoesNfe config = new ConfiguracoesNfe();
        config.setEstado(EstadosEnum.SP);
        config.setAmbiente(AmbienteEnum.PRODUCAO);
        config.setArquivoWebService("nonexistent_file.ini");

        assertThrows(ExcecaoNfe.class, () -> UtilitarioServicoWeb.getUrl(config, DocumentoEnum.NFE, ServicosEnum.STATUS_SERVICO));
    }

    @ParameterizedTeste
    @MethodSource("provideStateAndServiceCombinationsNfe")
    void testGetUrlForStateServiceCombinationsNfe(EstadosEnum estado, ServicosEnum servico, AmbienteEnum ambienteEnum) throws ExcecaoNfe {
        ConfiguracoesNfe config = new ConfiguracoesNfe();
        config.setEstado(estado);
        config.setAmbiente(ambienteEnum);

        String url = UtilitarioServicoWeb.getUrl(config,DocumentoEnum.NFE, servico);
        assertNotNull(url);
        assertTrue(url.startsWith("http"));
    }

    @ParameterizedTeste
    @MethodSource("provideStateAndServiceCombinationsNfce")
    void testGetUrlForStateServiceCombinationsNfce(EstadosEnum estado, ServicosEnum servico, AmbienteEnum ambienteEnum) throws ExcecaoNfe {
        ConfiguracoesNfe config = new ConfiguracoesNfe();
        config.setEstado(estado);
        config.setAmbiente(ambienteEnum);

        String url = UtilitarioServicoWeb.getUrl(config,DocumentoEnum.NFCE, servico);
        assertNotNull(url);
    }

    @Test
    void testGetUrlForDistribuicaoDFe() throws ExcecaoNfe {
        ConfiguracoesNfe config = new ConfiguracoesNfe();
        config.setEstado(EstadosEnum.SP); // Qualquer estado serve para AN
        config.setAmbiente(AmbienteEnum.PRODUCAO);

        String url = UtilitarioServicoWeb.getUrl(config, DocumentoEnum.NFE, ServicosEnum.DISTRIBUICAO_DFE);
        assertNotNull(url);
        assertTrue(url.startsWith("https://www1.nfe.fazenda.gov.br/NFeDistribuicaoDFe/NFeDistribuicaoDFe.asmx"));
    }

    @Test
    void testGetUrlForContingenciaSVC() throws ExcecaoNfe {
        ConfiguracoesNfe config = new ConfiguracoesNfe();
        config.setEstado(EstadosEnum.SP);
        config.setAmbiente(AmbienteEnum.PRODUCAO);
        config.setContigenciaSVC(true);

        // SP no SVC deve usar SVRS
        String url = UtilitarioServicoWeb.getUrl(config,DocumentoEnum.NFE, ServicosEnum.STATUS_SERVICO);
        assertNotNull(url);
        assertTrue(url.contains("sefazvirtual"));
    }

}