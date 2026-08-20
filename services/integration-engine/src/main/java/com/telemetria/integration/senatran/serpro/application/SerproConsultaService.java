package com.telemetria.integration.senatran.serpro.application;
import com.telemetria.integration.senatran.serpro.domain.InvalidVehicleQueryException;
import com.telemetria.integration.senatran.serpro.infrastructure.cache.SerproConsultaCache;
import com.telemetria.integration.senatran.serpro.domain.SerproIntegrationException;
import com.telemetria.integration.senatran.serpro.infrastructure.config.SerproProperties;
import com.telemetria.integration.senatran.serpro.domain.SerproVeiculoRequest;
import com.telemetria.integration.senatran.serpro.domain.SerproVeiculoResponse;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

@Service
public class SerproConsultaService {
    private static final Set<String> KNOWN_NON_BLOCKING_STATUSES = Set.of(
            "PAGO", "CANCELADO", "ARQUIVADO", "BAIXADO", "ENCERRADO");
    private final SerproConsultaClient client;
    private final SerproProperties properties;
    private final SerproConsultaCache cache;

    public SerproConsultaService(SerproConsultaClient client, SerproProperties properties,
            SerproConsultaCache cache) {
        this.client = client;
        this.properties = properties;
        this.cache = cache;
    }

    public SerproVeiculoResponse consultarVeiculo(String placa, String renavam) {
        String normalizedPlate = normalizePlate(placa);
        String normalizedRenavam = digitsOnly(renavam);
        validate(normalizedPlate, normalizedRenavam);
        SerproVeiculoResponse cached = cache.get(normalizedPlate, normalizedRenavam);
        if (cached != null) return cached;
        SerproVeiculoResponse response = client.consultarVeiculo(
                new SerproVeiculoRequest(normalizedPlate, normalizedRenavam));
        if (response == null || !response.isSucesso()) {
            String message = response != null ? response.mensagemErro() : "Resposta nula recebida do cliente.";
            throw new SerproIntegrationException("Falha na consulta do veículo no SERPRO/RADAR: " + message);
        }
        cache.put(normalizedPlate, normalizedRenavam, response);
        return response;
    }

    public boolean isVeiculoAptoParaViagem(String placa, String renavam) {
        SerproVeiculoResponse response = consultarVeiculo(placa, renavam);
        if (response.data() == null || response.data().isEmpty()) return false;
        Set<String> blocking = properties.getSerpro().getBlockingStatuses().stream()
                .map(value -> value.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
        return response.data().stream()
                .flatMap(v -> v.infracoes() == null ? Stream.empty() : v.infracoes().stream())
                .map(SerproVeiculoResponse.InfracaoData::situacao)
                .noneMatch(status -> isBlocking(status, blocking));
    }

    private boolean isBlocking(String status, Set<String> blocking) {
        if (status == null || status.isBlank()) return properties.getSerpro().isUnknownStatusBlocks();
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (blocking.contains(normalized)) return true;
        return properties.getSerpro().isUnknownStatusBlocks() && !KNOWN_NON_BLOCKING_STATUSES.contains(normalized);
    }

    private void validate(String plate, String renavam) {
        if (plate == null || !plate.matches("(?:[A-Z]{3}[0-9]{4}|[A-Z]{3}[0-9][A-Z][0-9]{2})")) {
            throw new InvalidVehicleQueryException("Placa inválida. Informe uma placa brasileira antiga ou Mercosul.");
        }
        if (renavam == null || !renavam.matches("\\d{9,11}") || !hasValidRenavamCheckDigit(renavam)) {
            throw new InvalidVehicleQueryException("RENAVAM inválido, inclusive o dígito verificador.");
        }
    }

    static boolean hasValidRenavamCheckDigit(String value) {
        String padded = "0".repeat(11 - value.length()) + value;
        int[] weights = {3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < 10; i++) sum += Character.digit(padded.charAt(i), 10) * weights[i];
        int digit = 11 - (sum % 11);
        if (digit >= 10) digit = 0;
        return digit == Character.digit(padded.charAt(10), 10);
    }

    private String normalizePlate(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }
    private String digitsOnly(String value) { return value == null ? null : value.replaceAll("\\D", ""); }
}
