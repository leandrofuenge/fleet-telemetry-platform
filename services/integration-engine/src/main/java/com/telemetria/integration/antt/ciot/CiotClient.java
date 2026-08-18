package com.telemetria.integration.antt.ciot;

/**
 * Contrato de integração: ANTT - CIOT (Documento obrigatório da viagem).
 * A comunicação geralmente ocorre via REST/JSON com a API de uma IPEF homologada.
 */
public interface CiotClient {

    /**
     * Solicita a emissão (geração) de um novo CIOT para uma operação de transporte.
     *
     * @param request Dados do contratante, motorista, veículo e valores
     * @return Resposta contendo o número do CIOT gerado ou erro
     */
    CiotResponse gerarCiot(CiotRequest request);

    /**
     * Solicita o encerramento ou cancelamento de um CIOT existente (Fim da viagem).
     *
     * @param numeroCiot Número do CIOT gerado anteriormente
     * @return Resposta confirmando o encerramento
     */
    CiotResponse encerrarCiot(String numeroCiot);
}