package com.app.telemetria.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // ================= SUCESSO (2xx) =================
    SUCCESS("200", "Operação realizada com sucesso", HttpStatus.OK),
    CREATED("201", "Recurso criado com sucesso", HttpStatus.CREATED),
    ACCEPTED("202", "Requisição aceita para processamento", HttpStatus.ACCEPTED),
    NO_CONTENT("204", "Operação realizada sem conteúdo para retornar", HttpStatus.NO_CONTENT),

    // ================= ERROS DE VALIDAÇÃO (400) =================
    VALIDATION_ERROR("400-001", "Erro de validação nos dados enviados", HttpStatus.BAD_REQUEST),
    INVALID_FIELD("400-002", "Campo inválido ou mal formatado", HttpStatus.BAD_REQUEST),
    MISSING_FIELD("400-003", "Campo obrigatório não informado", HttpStatus.BAD_REQUEST),
    INVALID_DATE("400-004", "Data inválida ou formato incorreto", HttpStatus.BAD_REQUEST),
    INVALID_COORDINATES("400-005", "Coordenadas geográficas inválidas", HttpStatus.BAD_REQUEST),

    // ================= AUTENTICAÇÃO/AUTORIZAÇÃO (401-403) =================
    UNAUTHORIZED("401-001", "Não autenticado. Faça login para continuar", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS("401-002", "Login ou senha inválidos", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("401-003", "Token expirado. Faça login novamente", HttpStatus.UNAUTHORIZED),
    
    FORBIDDEN("403-001", "Acesso negado. Você não tem permissão", HttpStatus.FORBIDDEN),
    TOKEN_INVALID("403-002", "Token inválido ou corrompido", HttpStatus.FORBIDDEN),
    ACCOUNT_DISABLED("403-003", "Conta desabilitada. Contate o administrador", HttpStatus.FORBIDDEN),
    ACCOUNT_LOCKED("403-004", "Conta bloqueada por tentativas excessivas", HttpStatus.FORBIDDEN),

    // ================= RECURSOS NÃO ENCONTRADOS (404) =================
    RESOURCE_NOT_FOUND("404-000", "Recurso não encontrado", HttpStatus.NOT_FOUND),
    
    // Motorista
    MOTORISTA_NOT_FOUND("404-001", "Motorista não encontrado", HttpStatus.NOT_FOUND),
    
    // Veículo
    VEICULO_NOT_FOUND("404-002", "Veículo não encontrado", HttpStatus.NOT_FOUND),
    
    // Rota
    ROTA_NOT_FOUND("404-003", "Rota não encontrada", HttpStatus.NOT_FOUND),
    
    // Viagem
    VIAGEM_NOT_FOUND("404-004", "Viagem não encontrada", HttpStatus.NOT_FOUND),
    
    // Usuário
    USER_NOT_FOUND("404-005", "Usuário não encontrado", HttpStatus.NOT_FOUND),
    
    // Alerta
    ALERTA_NOT_FOUND("404-006", "Alerta não encontrado", HttpStatus.NOT_FOUND),
    
    // Desvio
    DESVIO_NOT_FOUND("404-007", "Desvio de rota não encontrado", HttpStatus.NOT_FOUND),
    
    // Telemetria
    TELEMETRIA_NOT_FOUND("404-008", "Dados de telemetria não encontrados", HttpStatus.NOT_FOUND),

    // ================= CONFLITOS (409) =================
    DUPLICATE_RESOURCE("409-000", "Recurso já existe no sistema", HttpStatus.CONFLICT),
    
    // Motorista
    MOTORISTA_DUPLICATE("409-001", "Já existe um motorista com este CPF/CNH", HttpStatus.CONFLICT),
    
    // Veículo
    VEICULO_DUPLICATE("409-002", "Já existe um veículo com esta placa", HttpStatus.CONFLICT),
    
    // Rota
    ROTA_DUPLICATE("409-003", "Já existe uma rota com este nome", HttpStatus.CONFLICT),
    
    // Usuário
    USER_DUPLICATE("409-004", "Já existe um usuário com este login/email", HttpStatus.CONFLICT),

    // ================= ERROS DE NEGÓCIO (422) =================
    BUSINESS_ERROR("422-000", "Erro na regra de negócio", HttpStatus.UNPROCESSABLE_ENTITY),
    ROTA_INVALID("422-001", "Dados da rota inválidos ou inconsistentes", HttpStatus.UNPROCESSABLE_ENTITY),
    VIAGEM_INVALID("422-002", "Viagem não pode ser iniciada/finalizada", HttpStatus.UNPROCESSABLE_ENTITY),
    VEICULO_INDISPONIVEL("422-003", "Veículo indisponível para esta operação", HttpStatus.UNPROCESSABLE_ENTITY),
    MOTORISTA_INDISPONIVEL("422-004", "Motorista indisponível para esta operação", HttpStatus.UNPROCESSABLE_ENTITY),

    // ================= INTEGRAÇÃO EXTERNA (424-429) =================
    DEPENDENCY_FAILED("424-001", "Falha em serviço externo", HttpStatus.FAILED_DEPENDENCY),
    WEATHER_API_ERROR("424-002", "Erro ao consultar API de clima", HttpStatus.FAILED_DEPENDENCY),
    MAPS_API_ERROR("424-003", "Erro ao consultar API de mapas", HttpStatus.FAILED_DEPENDENCY),
    
    TOO_MANY_REQUESTS("429-001", "Muitas requisições. Aguarde e tente novamente", HttpStatus.TOO_MANY_REQUESTS),

    // ================= ERROS DO SERVIDOR (500) =================
    INTERNAL_ERROR("500-000", "Erro interno do servidor", HttpStatus.INTERNAL_SERVER_ERROR),
    DATABASE_ERROR("500-001", "Erro no banco de dados", HttpStatus.INTERNAL_SERVER_ERROR),
    CACHE_ERROR("500-002", "Erro no sistema de cache", HttpStatus.INTERNAL_SERVER_ERROR),
    MESSAGE_QUEUE_ERROR("500-003", "Erro na fila de mensagens", HttpStatus.INTERNAL_SERVER_ERROR),
    
    // Kafka
    KAFKA_PRODUCER_ERROR("500-004", "Erro ao publicar mensagem no Kafka", HttpStatus.INTERNAL_SERVER_ERROR),
    KAFKA_CONSUMER_ERROR("500-005", "Erro ao consumir mensagem do Kafka", HttpStatus.INTERNAL_SERVER_ERROR),
    
    // MQTT
    MQTT_CONNECTION_ERROR("500-006", "Erro na conexão MQTT", HttpStatus.INTERNAL_SERVER_ERROR),
    MQTT_PUBLISH_ERROR("500-007", "Erro ao publicar mensagem MQTT", HttpStatus.INTERNAL_SERVER_ERROR),

    // ================= ERROS DE TIMEOUT (504) =================
    GATEWAY_TIMEOUT("504-001", "Tempo limite da requisição excedido", HttpStatus.GATEWAY_TIMEOUT),
    EXTERNAL_SERVICE_TIMEOUT("504-002", "Serviço externo não respondeu a tempo", HttpStatus.GATEWAY_TIMEOUT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() { return code; }
    public String getMessage() { return message; }
    public HttpStatus getHttpStatus() { return httpStatus; }
}