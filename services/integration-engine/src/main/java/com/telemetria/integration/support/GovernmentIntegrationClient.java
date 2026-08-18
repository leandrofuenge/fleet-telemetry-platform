package com.telemetria.integration.support;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.telemetria.integration.antt.seguro.SeguroClient;
import com.telemetria.integration.antt.valepedagio.ValePedagioClient;
import com.telemetria.integration.sefaz.cte.portal.PortalCteClient;
import com.telemetria.integration.sefaz.evento.EventoFiscalClient;
import com.telemetria.integration.sefaz.mdfe.portal.PortalMdfeClient;
import com.telemetria.integration.sefaz.nfce.NfceClient;
import com.telemetria.integration.senatran.crlve.CrlveClient;
import com.telemetria.integration.senatran.renach.RenachClient;
import com.telemetria.integration.senatran.renainf.RenainfClient;
import com.telemetria.integration.senatran.renavam.RenavamClient;

/** Adaptador provisório configurável; cada operação é roteada para seu endpoint autorizado. */
@Component
public class GovernmentIntegrationClient extends ConfigurableIntegrationClient implements
        ValePedagioClient, SeguroClient, RenavamClient, RenainfClient, CrlveClient, RenachClient,
        EventoFiscalClient, NfceClient, PortalMdfeClient, PortalCteClient {

    @Value("${external-integrations.antt.vale-pedagio-url:}") private String valePedagioUrl;
    @Value("${external-integrations.antt.seguro-url:}") private String seguroUrl;
    @Value("${external-integrations.antt.token:}") private String anttToken;
    @Value("${external-integrations.senatran.renavam-url:}") private String renavamUrl;
    @Value("${external-integrations.senatran.renainf-url:}") private String renainfUrl;
    @Value("${external-integrations.senatran.crlve-url:}") private String crlveUrl;
    @Value("${external-integrations.senatran.renach-url:}") private String renachUrl;
    @Value("${external-integrations.senatran.token:}") private String senatranToken;
    @Value("${external-integrations.sefaz.evento-url:}") private String eventoUrl;
    @Value("${external-integrations.sefaz.nfce-url:}") private String nfceUrl;
    @Value("${external-integrations.sefaz.portal-mdfe-url:}") private String portalMdfeUrl;
    @Value("${external-integrations.sefaz.portal-cte-url:}") private String portalCteUrl;
    @Value("${external-integrations.sefaz.token:}") private String sefazToken;

    public GovernmentIntegrationClient(RestTemplate restTemplate) {
        super(restTemplate);
    }

    @Override
    public IntegrationResponse execute(IntegrationRequest request) {
        String operation = request.operation().toLowerCase();
        if (operation.startsWith("antt.vale-pedagio.")) return post(valePedagioUrl, anttToken, request);
        if (operation.startsWith("antt.seguro.")) return post(seguroUrl, anttToken, request);
        if (operation.startsWith("senatran.renavam.")) return post(renavamUrl, senatranToken, request);
        if (operation.startsWith("senatran.renainf.")) return post(renainfUrl, senatranToken, request);
        if (operation.startsWith("senatran.crlve.")) return post(crlveUrl, senatranToken, request);
        if (operation.startsWith("senatran.renach.")) return post(renachUrl, senatranToken, request);
        if (operation.startsWith("sefaz.evento.")) return post(eventoUrl, sefazToken, request);
        if (operation.startsWith("sefaz.nfce.")) return post(nfceUrl, sefazToken, request);
        if (operation.startsWith("sefaz.portal-mdfe.")) return post(portalMdfeUrl, sefazToken, request);
        if (operation.startsWith("sefaz.portal-cte.")) return post(portalCteUrl, sefazToken, request);
        throw new IllegalArgumentException("Operação de integração não suportada: " + request.operation());
    }
}
