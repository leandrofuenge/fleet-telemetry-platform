package com.telemetria.integration.antt.ciot;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.telemetria.integration.sefaz.cte.exception.CteException;

/**
 * Serviço de orquestração do CIOT (Código Identificador da Operação de Transportes).
 * Trata regras de negócio da ANTT, validações prévias e sanitização de dados antes do envio.
 */
@Service
public class CiotService {

    private final CiotClient client;

    public CiotService(CiotClient client) {
        this.client = client;
    }

    /**
     * Orquestra a emissão do CIOT com validação rigorosa dos dados da viagem.
     *
     * @param request DTO com informações da viagem/frete
     * @return DTO com o resultado da solicitação (número do CIOT ou motivo da rejeição)
     */
    public CiotResponse emitirCiot(CiotRequest request) {
        // 1. Sanitiza os dados de entrada
        CiotRequest requestSanitizado = sanitizarRequest(request);

        // 2. Executa validações de regras de negócio antes de chamar a API externa
        validarRequestEmissao(requestSanitizado);

        // 3. Executa a chamada no client REST/SOAP
        CiotResponse response = client.gerarCiot(requestSanitizado);

        // 4. Valida a resposta do parceiro/ANTT
        if (!response.sucesso() || response.numeroCiot() == null || response.numeroCiot().isBlank()) {
            throw new CteException("Falha ao gerar o CIOT junto à ANTT: " + response.mensagemErro());
        }

        return response;
    }

    /**
     * Orquestra o encerramento do CIOT ao finalizar a prestação do serviço de transporte.
     *
     * @param numeroCiot Número do CIOT a ser encerrado
     * @return Resposta com o status do encerramento
     */
    public CiotResponse encerrarCiot(String numeroCiot) {
        if (numeroCiot == null || numeroCiot.isBlank()) {
            throw new CteException("O número do CIOT é obrigatório para realizar o encerramento.");
        }

        String ciotLimpo = numeroCiot.replaceAll("\\D", "");
        return client.encerrarCiot(ciotLimpo);
    }

    /**
     * Regra de Negócio ANTT: Verifica se a operação exige obrigatoriamente a geração do CIOT.
     * O CIOT é obrigatório para contratação de TAC (Transportador Autônomo de Cargas) 
     * ou equiparados (ETC/CTC com até 3 veículos cadastrados).
     */
    public boolean isCiotObrigatorio(boolean ehTransportadorAutonomo, BigDecimal valorFrete) {
        return ehTransportadorAutonomo && valorFrete != null && valorFrete.compareTo(BigDecimal.ZERO) > 0;
    }

    // --- Métodos Privados de Sanitização e Validação ---

    private CiotRequest sanitizarRequest(CiotRequest r) {
        if (r == null) {
            throw new CteException("Os dados para emissão do CIOT não podem ser nulos.");
        }

        return new CiotRequest(
                apenasNumeros(r.cnpjContratante()),
                apenasNumeros(r.cpfCnpjContratado()),
                apenasNumeros(r.cpfMotorista()),
                r.placaVeiculo() != null ? r.placaVeiculo().toUpperCase().replaceAll("[^A-Z0-9]", "") : null,
                apenasNumeros(r.renavam()),
                apenasNumeros(r.cepOrigem()),
                apenasNumeros(r.cepDestino()),
                r.valorFrete(),
                r.valorPedagio() != null ? r.valorPedagio() : BigDecimal.ZERO,
                r.tipoPagamento() != null ? r.tipoPagamento() : "TRANSFERENCIA_BANCARIA"
        );
    }

    private void validarRequestEmissao(CiotRequest req) {
        if (req.cnpjContratante() == null || req.cnpjContratante().length() != 14) {
            throw new CteException("CNPJ do Contratante do frete inválido.");
        }
        if (req.cpfCnpjContratado() == null || (req.cpfCnpjContratado().length() != 11 && req.cpfCnpjContratado().length() != 14)) {
            throw new CteException("CPF/CNPJ do Contratado inválido.");
        }
        if (req.cpfMotorista() == null || req.cpfMotorista().length() != 11) {
            throw new CteException("CPF do Motorista é obrigatório e deve ter 11 dígitos.");
        }
        if (req.placaVeiculo() == null || req.placaVeiculo().length() != 7) {
            throw new CteException("Placa do Veículo é obrigatória e deve possuir 7 caracteres.");
        }
        if (req.valorFrete() == null || req.valorFrete().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CteException("O Valor do Frete deve ser superior a zero.");
        }
    }

    private String apenasNumeros(String input) {
        return Objects.nonNull(input) ? input.replaceAll("\\D", "") : null;
    }
}
