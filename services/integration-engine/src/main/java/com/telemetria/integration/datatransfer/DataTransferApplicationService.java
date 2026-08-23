package com.telemetria.integration.datatransfer;

import java.util.UUID;

import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Service;

import com.telemetria.integration.support.AuditLogProcessor;

/**
 * Porta de entrada da aplicação para transferências de dados.
 * Centraliza a correlação, a chamada ao Camel e a auditoria persistida.
 */
@Service
public class DataTransferApplicationService {

    private final ProducerTemplate producerTemplate;
    private final TransferenciaDadosService transferenciaDadosService;

    public DataTransferApplicationService(
            ProducerTemplate producerTemplate,
            TransferenciaDadosService transferenciaDadosService) {
        this.producerTemplate = producerTemplate;
        this.transferenciaDadosService = transferenciaDadosService;
    }

    public TransferResult processar(Base64TransferRequest request, String correlationIdInformado) {
        String correlationId = correlationIdInformado == null || correlationIdInformado.isBlank()
                ? UUID.randomUUID().toString()
                : correlationIdInformado;
        try {
            Base64TransferResponse response = producerTemplate.requestBodyAndHeader(
                    DataTransferRoute.ROUTE_TRANSFER_BASE64,
                    request,
                    AuditLogProcessor.HEADER_CORRELATION_ID,
                    correlationId,
                    Base64TransferResponse.class);
            response.setCorrelationId(correlationId);
            transferenciaDadosService.registrarSucesso(correlationId, request, response);
            return new TransferResult(correlationId, response);
        } catch (RuntimeException exception) {
            transferenciaDadosService.registrarFalha(correlationId, request, exception);
            throw exception;
        }
    }

    public record TransferResult(String correlationId, Base64TransferResponse response) {
    }
}
