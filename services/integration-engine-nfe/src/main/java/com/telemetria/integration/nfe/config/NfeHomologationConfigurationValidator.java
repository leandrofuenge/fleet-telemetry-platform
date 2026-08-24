package com.telemetria.integration.nfe.config;

import java.net.URI;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.telemetria.integration.nfe.domain.exception.NfeException;
import com.telemetria.integration.nfe.security.CertificadoLoader;

/** Faz o serviço falhar cedo quando a homologação NF-e estiver incompleta ou insegura. */
@Component
@Profile("nfe-homologation")
public class NfeHomologationConfigurationValidator implements ApplicationRunner {
    private final NfeProperties nfe;
    private final SefazProperties sefaz;
    private final CertificadoLoader certificadoLoader;
    private final Environment environment;

    public NfeHomologationConfigurationValidator(NfeProperties nfe, SefazProperties sefaz,
            CertificadoLoader certificadoLoader, Environment environment) {
        this.nfe = nfe;
        this.sefaz = sefaz;
        this.certificadoLoader = certificadoLoader;
        this.environment = environment;
    }

    @Override public void run(ApplicationArguments args) {
        if (!"2".equals(nfe.getAmbiente())) fail("sefaz.nfe.ambiente deve ser 2.");
        if (!nfe.getCodigoUf().matches("\\d{2}")) fail("sefaz.nfe.codigo-uf deve possuir dois dígitos.");
        if (nfe.getTimeoutMillis() <= 0) fail("sefaz.nfe.timeout-millis deve ser maior que zero.");
        List.of(nfe.getEndpoints().getAutorizacao(), nfe.getEndpoints().getRetAutorizacao(),
                nfe.getEndpoints().getConsulta(), nfe.getEndpoints().getEvento(),
                nfe.getEndpoints().getInutilizacao(), nfe.getEndpoints().getStatusServico(),
                nfe.getEndpoints().getDistribuicaoDfe()).forEach(this::validarEndpoint);
        if (vazio(sefaz.getCertificado().getArquivo()) || vazio(sefaz.getCertificado().getSenha())) {
            fail("SEFAZ_CERT_PATH e SEFAZ_CERT_PASSWORD devem ser fornecidos.");
        }
        try {
            if (certificadoLoader.carregarKeyStore(sefaz.getCertificado().getArquivo(),
                    sefaz.getCertificado().getSenha(), sefaz.getCertificado().getTipo()).size() == 0) {
                fail("O certificado A1 configurado está vazio.");
            }
        } catch (NfeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new NfeException("Configuração NF-e homologation inválida: certificado A1 indisponível.", exception);
        }
        if (environment.getProperty("integration.simulation.enabled", Boolean.class, false)) {
            fail("integration.simulation.enabled deve ser false.");
        }
    }

    private void validarEndpoint(URI endpoint) {
        if (endpoint == null || !"https".equalsIgnoreCase(endpoint.getScheme()) || endpoint.getHost() == null
                || endpoint.toString().toLowerCase().matches(".*(change_me|localhost|example).*")) {
            fail("Todos os endpoints NF-e devem ser URLs HTTPS reais.");
        }
    }
    private boolean vazio(String value) { return value == null || value.isBlank() || value.startsWith("CHANGE_ME"); }
    private void fail(String message) { throw new NfeException("Configuração NF-e homologation inválida: " + message); }
}
