package com.telemetria.integration.workflow.application;

import org.springframework.stereotype.Service;

import com.telemetria.integration.antt.rntrc.RntrcService;
import com.telemetria.integration.sefaz.cte.status.CteStatusRequest;
import com.telemetria.integration.sefaz.cte.status.CteStatusResponse;
import com.telemetria.integration.sefaz.cte.status.CteStatusService;
import com.telemetria.integration.senatran.serpro.SerproConsultaService;
import com.telemetria.integration.workflow.domain.ViagemWorkflowRequest;
import com.telemetria.integration.workflow.domain.ViagemWorkflowResponse;

@Service
public class InicioViagemService {

    private final SerproConsultaService serproConsultaService;
    private final RntrcService rntrcService;
    private final CteStatusService cteStatusService;

    public InicioViagemService(SerproConsultaService serproConsultaService, RntrcService rntrcService,
            CteStatusService cteStatusService) {
        this.serproConsultaService = serproConsultaService;
        this.rntrcService = rntrcService;
        this.cteStatusService = cteStatusService;
    }

    public ViagemWorkflowResponse executar(ViagemWorkflowRequest request) {
        ViagemWorkflowResponse response = new ViagemWorkflowResponse(
                request != null ? request.getViagemId() : null,
                "EM_ANALISE"
        );
        if (request == null) {
            response.getPendencias().add("Dados da viagem não informados");
            response.setStatus("BLOQUEADA");
            return response;
        }

        validarMotorista(request, response);
        validarVeiculoSenatran(request, response);
        validarTransportadorAntt(request, response);
        validarSefaz(request, response);
        concluir(response);
        return response;
    }

    private void validarMotorista(ViagemWorkflowRequest request, ViagemWorkflowResponse response) {
        String cpf = apenasNumeros(request.getMotoristaCpf());
        boolean valido = cpf != null && cpf.length() == 11;
        response.setMotoristaValido(valido);
        registrar(response, valido, "MOTORISTA_VALIDADO", "CPF do motorista inválido ou ausente");
    }

    private void validarVeiculoSenatran(ViagemWorkflowRequest request, ViagemWorkflowResponse response) {
        String placa = sanitizarPlaca(request.getVeiculoPlaca());
        boolean formatoValido = placa != null && placa.matches("[A-Z0-9]{7}");
        if (!formatoValido) {
            response.setVeiculoValido(false);
            response.getPendencias().add("Placa do veículo inválida ou ausente");
            return;
        }

        if (request.getRenavam() == null || request.getRenavam().isBlank()) {
            response.setVeiculoValido(true);
            response.getEtapasConcluidas().add("VEICULO_VALIDADO");
            response.getEtapasConcluidas().add("VEICULO_VALIDADO_LOCALMENTE");
            return;
        }

        try {
            boolean apto = serproConsultaService.isVeiculoAptoParaViagem(placa, request.getRenavam());
            response.setVeiculoValido(apto);
            if (apto) {
                response.getEtapasConcluidas().add("VEICULO_VALIDADO");
            }
            registrar(response, apto, "SENATRAN_VEICULO_APTO", "Veículo possui pendências no SENATRAN/RADAR");
        } catch (RuntimeException e) {
            response.setVeiculoValido(false);
            response.getPendencias().add("Falha na validação SENATRAN: " + e.getMessage());
        }
    }

    private void validarTransportadorAntt(ViagemWorkflowRequest request, ViagemWorkflowResponse response) {
        if (request.getTransportadorDocumento() == null || request.getTransportadorDocumento().isBlank()) {
            response.setAnttRegular(true);
            response.getEtapasConcluidas().add("ANTT_NAO_APLICAVEL");
            return;
        }

        try {
            boolean regular = rntrcService.validarRegularidade(
                    request.getVeiculoPlaca(), request.getTransportadorDocumento());
            response.setAnttRegular(regular);
            registrar(response, regular, "ANTT_RNTRC_REGULAR", "Transportador sem confirmação de regularidade na ANTT");
        } catch (RuntimeException e) {
            response.setAnttRegular(false);
            response.getPendencias().add("Falha na validação ANTT: " + e.getMessage());
        }
    }

    private void validarSefaz(ViagemWorkflowRequest request, ViagemWorkflowResponse response) {
        CteStatusResponse status = cteStatusService.consultar(
                new CteStatusRequest(request.getUfOrigem(), null)
        );
        boolean disponivel = status != null && status.isDisponivel();
        response.setSefazDisponivel(disponivel);
        registrar(response, disponivel, "SEFAZ_CTE_OPERACIONAL", "SEFAZ CT-e indisponível no momento");
    }

    private void concluir(ViagemWorkflowResponse response) {
        boolean liberada = response.isMotoristaValido()
                && response.isVeiculoValido()
                && response.isAnttRegular()
                && response.isSefazDisponivel();
        response.setStatus(liberada ? "LIBERADA" : "BLOQUEADA");
        if (liberada) {
            response.getEtapasConcluidas().add("VIAGEM_LIBERADA");
        }
    }

    private void registrar(ViagemWorkflowResponse response, boolean sucesso, String etapa, String pendencia) {
        if (sucesso) {
            response.getEtapasConcluidas().add(etapa);
        } else {
            response.getPendencias().add(pendencia);
        }
    }

    private String sanitizarPlaca(String placa) {
        return placa != null ? placa.toUpperCase().replaceAll("[^A-Z0-9]", "") : null;
    }

    private String apenasNumeros(String valor) {
        return valor != null ? valor.replaceAll("\\D", "") : null;
    }
}
