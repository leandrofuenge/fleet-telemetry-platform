package com.telemetria.integration.workflow.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.telemetria.integration.antt.rntrc.RntrcService;
import com.telemetria.integration.sefaz.cte.status.CteStatusRequest;
import com.telemetria.integration.sefaz.cte.status.CteStatusResponse;
import com.telemetria.integration.sefaz.cte.status.CteStatusService;
import com.telemetria.integration.senatran.serpro.application.SerproConsultaService;
import com.telemetria.integration.workflow.domain.ViagemWorkflowRequest;
import com.telemetria.integration.workflow.domain.ViagemWorkflowResponse;

class InicioViagemServiceTests {

    @Mock
    private SerproConsultaService serproConsultaService;
    @Mock
    private RntrcService rntrcService;
    @Mock
    private CteStatusService cteStatusService;

    private InicioViagemService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new InicioViagemService(serproConsultaService, rntrcService, cteStatusService);
    }

    @Test
    void liberaViagemComValidacoesBasicasQuandoConsultasOpcionaisNaoForemSolicitadas() {
        when(cteStatusService.consultar(org.mockito.ArgumentMatchers.any(CteStatusRequest.class)))
                .thenReturn(new CteStatusResponse("homologacao", "MT", true, "107", "Operacional", 1L));

        ViagemWorkflowResponse response = service.executar(new ViagemWorkflowRequest(
                "VGM-1", "ABC1D23", "12345678900", "MT", "SP"
        ));

        assertThat(response.getStatus()).isEqualTo("LIBERADA");
        assertThat(response.getEtapasConcluidas()).contains(
                "MOTORISTA_VALIDADO", "VEICULO_VALIDADO_LOCALMENTE", "ANTT_NAO_APLICAVEL",
                "SEFAZ_CTE_OPERACIONAL", "VIAGEM_LIBERADA"
        );
        verify(serproConsultaService, never()).isVeiculoAptoParaViagem(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(rntrcService, never()).validarRegularidade(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void bloqueiaViagemQuandoSenatranOuAnttApontamPendencia() {
        when(serproConsultaService.isVeiculoAptoParaViagem("ABC1D23", "12345678901"))
                .thenReturn(false);
        when(rntrcService.validarRegularidade("ABC1D23", "12345678901"))
                .thenReturn(false);
        when(cteStatusService.consultar(org.mockito.ArgumentMatchers.any(CteStatusRequest.class)))
                .thenReturn(new CteStatusResponse("homologacao", "MT", true, "107", "Operacional", 1L));

        ViagemWorkflowResponse response = service.executar(new ViagemWorkflowRequest(
                "VGM-2", "ABC1D23", "12345678900", "MT", "SP", "12345678901", "12345678901"
        ));

        assertThat(response.getStatus()).isEqualTo("BLOQUEADA");
        assertThat(response.getPendencias()).contains(
                "Veículo possui pendências no SENATRAN/RADAR",
                "Transportador sem confirmação de regularidade na ANTT"
        );
    }
}
