package com.telemetria.application.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemetria.domain.entity.Telemetria;
import com.telemetria.domain.entity.VeiculoCache;
import com.telemetria.domain.exception.TelemetriaMessageException;
import com.telemetria.infrastructure.messaging.dto.KafkaMessageMetadata;
import com.telemetria.infrastructure.messaging.dto.TelemetriaEnvelope;

@Component
public class TelemetriaMessageMapper {

    private final ObjectMapper objectMapper;
    private final int maxPayloadBytes;
    private final ZoneId eventTimeZone;

    public TelemetriaMessageMapper(
            ObjectMapper objectMapper,
            @Value("${telemetria.ingestion.max-payload-bytes:1048576}") int maxPayloadBytes,
            @Value("${telemetria.event-time-zone:America/Sao_Paulo}") String eventTimeZone) {
        this.objectMapper = objectMapper;
        this.maxPayloadBytes = maxPayloadBytes;
        this.eventTimeZone = ZoneId.of(eventTimeZone);
    }

    public TelemetriaEnvelope parse(String payload, KafkaMessageMetadata metadata) {
        if (payload == null || payload.isBlank()) {
            throw new TelemetriaMessageException("Payload de telemetria vazio");
        }
        if (payload.getBytes(StandardCharsets.UTF_8).length > maxPayloadBytes) {
            throw new TelemetriaMessageException("Payload excede o limite de " + maxPayloadBytes + " bytes");
        }

        try {
            JsonNode json = objectMapper.readTree(payload);
            if (json == null || !json.isObject()) {
                throw new TelemetriaMessageException("Payload deve ser um objeto JSON");
            }

            Long veiculoId = readLong(json, "veiculo_id", "veiculoId");
            if (veiculoId == null && json.path("veiculo").isObject()) {
                veiculoId = readLong(json.path("veiculo"), "id");
            }
            if (veiculoId == null || veiculoId <= 0) {
                throw new TelemetriaMessageException("veiculo_id é obrigatório e deve ser positivo");
            }

            Double latitude = readDouble(json, "latitude");
            Double longitude = readDouble(json, "longitude");
            if (latitude == null || latitude < -90 || latitude > 90) {
                throw new TelemetriaMessageException("latitude deve estar entre -90 e 90");
            }
            if (longitude == null || longitude < -180 || longitude > 180) {
                throw new TelemetriaMessageException("longitude deve estar entre -180 e 180");
            }

            String eventId = readText(json, "event_id", "eventId");
            if (eventId == null || eventId.isBlank()) {
                eventId = metadata.deterministicEventId();
            }
            if (eventId.length() > 128) {
                throw new TelemetriaMessageException("event_id deve possuir no máximo 128 caracteres");
            }

            Long sequence = readLong(json, "sequence_number", "sequence", "sequencia");
            if (sequence != null && sequence < 0) {
                throw new TelemetriaMessageException("sequence_number não pode ser negativo");
            }

            return new TelemetriaEnvelope(
                    json,
                    payload,
                    veiculoId,
                    readLong(json, "tenant_id", "tenantId"),
                    eventId,
                    sequence);
        } catch (JsonProcessingException e) {
            throw new TelemetriaMessageException("JSON de telemetria inválido", e);
        }
    }

    public Telemetria toEntity(TelemetriaEnvelope envelope, VeiculoCache veiculo) {
        JsonNode json = envelope.json();
        Telemetria telemetria = new Telemetria();
        telemetria.setTenantId(veiculo.getTenantId());
        telemetria.setVeiculoId(veiculo.getId());
        telemetria.setVeiculoUuid(veiculo.getUuid());
        telemetria.setEventId(envelope.eventId());
        telemetria.setSequenceNumber(envelope.sequenceNumber());
        telemetria.setMotoristaId(readLong(json, "motorista_id", "motoristaId"));
        telemetria.setViagemId(readLong(json, "viagem_id", "viagemId"));
        telemetria.setDeviceId(readText(json, "device_id", "deviceId"));
        telemetria.setImeiDispositivo(readText(json, "imei_dispositivo", "imei"));
        telemetria.setLatitude(readDouble(json, "latitude"));
        telemetria.setLongitude(readDouble(json, "longitude"));
        telemetria.setVelocidade(defaultValue(readDouble(json, "velocidade"), 0.0));
        telemetria.setDataHora(readEventTime(json));
        mapOptionalFields(telemetria, json);
        return telemetria;
    }

    private void mapOptionalFields(Telemetria t, JsonNode json) {
        setDouble(json, "altitude", t::setAltitude);
        setDouble(json, "direcao", t::setDirecao);
        setDouble(json, "hdop", t::setHdop);
        setInteger(json, "satelites", t::setSatelites);
        setDouble(json, "precisao_gps", t::setPrecisaoGps);
        setDouble(json, "lat_snap", t::setLatSnap);
        setDouble(json, "lng_snap", t::setLngSnap);
        setText(json, "nome_via", t::setNomeVia);

        setBoolean(json, "ignicao", t::setIgnicao);
        setDouble(json, "rpm", t::setRpm);
        setDouble(json, "carga_motor", t::setCargaMotor);
        setDouble(json, "torque_motor", t::setTorqueMotor);
        setDouble(json, "temperatura_motor", t::setTemperaturaMotor);
        setDouble(json, "pressao_oleo", t::setPressaoOleo);
        setDouble(json, "tensao_bateria", t::setTensaoBateria);
        setDouble(json, "odometro", t::setOdometro);
        setDouble(json, "horas_motor", t::setHorasMotor);
        setDouble(json, "aceleracao", t::setAceleracao);
        setDouble(json, "inclinacao", t::setInclinacao);

        setDouble(json, "nivel_combustivel", t::setNivelCombustivel);
        setDouble(json, "consumo_combustivel", t::setConsumoCombustivel);
        setDouble(json, "consumo_acumulado", t::setConsumoAcumulado);
        setInteger(json, "tempo_ocioso", t::setTempoOcioso);
        setInteger(json, "tempo_motor_ligado", t::setTempoMotorLigado);

        setBoolean(json, "frenagem_brusca", t::setFrenagemBrusca);
        setInteger(json, "numero_frenagens", t::setNumeroFrenagens);
        setInteger(json, "numero_aceleracoes_bruscas", t::setNumeroAceleracoesBruscas);
        setBoolean(json, "excesso_velocidade", t::setExcessoVelocidade);
        setDouble(json, "velocidade_limite_via", t::setVelocidadeLimiteVia);
        setBoolean(json, "curva_brusca", t::setCurvaBrusca);
        setInteger(json, "pontuacao_motorista", t::setPontuacaoMotorista);

        setBoolean(json, "colisao_detectada", t::setColisaoDetectada);
        setBoolean(json, "geofence_violada", t::setGeofenceViolada);
        setLong(json, "geofence_id", t::setGeofenceId);
        setBoolean(json, "cinto_seguranca", t::setCintoSeguranca);
        setBoolean(json, "porta_aberta", t::setPortaAberta);
        setBoolean(json, "botao_panico", t::setBotaoPanico);
        setBoolean(json, "adulteracao_gps", t::setAdulteracaoGps);
        setBoolean(json, "impreciso", t::setImpreciso);
        setBoolean(json, "preservar_dados", t::setPreservarDados);

        setDouble(json, "temperatura_carga", t::setTemperaturaCarga);
        setDouble(json, "umidade_carga", t::setUmidadeCarga);
        setDouble(json, "peso_carga_kg", t::setPesoCargaKg);
        setBoolean(json, "porta_bau_aberta", t::setPortaBauAberta);
        setBoolean(json, "impacto_carga", t::setImpactoCarga);
        setDouble(json, "g_force_impacto", t::setGForceImpacto);
        setJson(json, "pressao_pneus_json", t::setPressaoPneusJson);
        setBoolean(json, "alerta_pneu", t::setAlertaPneu);

        setBoolean(json, "fadiga_detectada", t::setFadigaDetectada);
        setBoolean(json, "distracao_detectada", t::setDistracaoDetectada);
        setBoolean(json, "uso_celular_detectado", t::setUsoCelularDetectado);
        setBoolean(json, "uso_celular", t::setUsoCelular);
        setBoolean(json, "cigarro_detectado", t::setCigarroDetectado);
        setBoolean(json, "ausencia_cinto_dms", t::setAusenciaCintoDms);
        setInteger(json, "score_dms", t::setScoreDms);

        setDouble(json, "temperatura_externa", t::setTemperaturaExterna);
        setDouble(json, "umidade_externa", t::setUmidadeExterna);
        setBoolean(json, "chuva_detectada", t::setChuvaDetectada);
        setText(json, "condicao_pista", t::setCondicaoPista);

        setDouble(json, "sinal_gsm", t::setSinalGsm);
        setDouble(json, "sinal_gps", t::setSinalGps);
        setText(json, "tecnologia_rede", t::setTecnologiaRede);
        setText(json, "firmware_versao", t::setFirmwareVersao);
        setBoolean(json, "modo_offline", t::setModoOffline);
        setInteger(json, "delay_sincronizacao_s", t::setDelaySincronizacaoS);

        setText(json, "tacografo_status", t::setTacografoStatus);
        setDouble(json, "tacografo_velocidade", t::setTacografoVelocidade);
        setDouble(json, "tacografo_distancia", t::setTacografoDistancia);
        setDouble(json, "horas_direcao_acumuladas", t::setHorasDirecaoAcumuladas);
        setBoolean(json, "manutencao_pendente", t::setManutencaoPendente);
        setDouble(json, "desgaste_freio", t::setDesgasteFreio);
        setJson(json, "dtc_codes", t::setDtcCodes);

        JsonNode revisao = json.get("proxima_revisao");
        if (revisao != null && !revisao.isNull()) {
            try {
                t.setProximaRevisao(LocalDateTime.parse(revisao.asText()));
            } catch (DateTimeParseException e) {
                throw new TelemetriaMessageException("proxima_revisao inválida", e);
            }
        }

        JsonNode payload = json.get("payload");
        if (payload != null && payload.isObject()) {
            Map<String, Object> value = objectMapper.convertValue(payload, new TypeReference<Map<String, Object>>() { });
            t.setPayload(value);
        }
    }

    private LocalDateTime readEventTime(JsonNode json) {
        JsonNode timestamp = first(json, "timestamp", "event_time", "data_hora", "dataHora");
        if (timestamp == null || timestamp.isNull()) {
            return LocalDateTime.now(eventTimeZone);
        }
        if (timestamp.isNumber()) {
            long value = timestamp.asLong();
            Instant instant = Math.abs(value) < 100_000_000_000L
                    ? Instant.ofEpochSecond(value)
                    : Instant.ofEpochMilli(value);
            return LocalDateTime.ofInstant(instant, eventTimeZone);
        }

        String value = timestamp.asText();
        try {
            return LocalDateTime.ofInstant(Instant.parse(value), eventTimeZone);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(value).atZoneSameInstant(eventTimeZone).toLocalDateTime();
            } catch (DateTimeParseException ignoredOffset) {
                try {
                    return LocalDateTime.parse(value);
                } catch (DateTimeParseException e) {
                    throw new TelemetriaMessageException("timestamp/event_time inválido", e);
                }
            }
        }
    }

    private JsonNode first(JsonNode json, String... names) {
        for (String name : names) {
            JsonNode value = json.get(name);
            if (value != null && !value.isNull()) return value;
        }
        return null;
    }

    private Long readLong(JsonNode json, String... names) {
        JsonNode value = first(json, names);
        if (value == null) return null;
        if (value.isIntegralNumber()) return value.longValue();
        if (value.isTextual()) {
            try {
                return Long.valueOf(value.asText().trim());
            } catch (NumberFormatException e) {
                throw invalidNumber(names[0], e);
            }
        }
        throw invalidNumber(names[0], null);
    }

    private Double readDouble(JsonNode json, String... names) {
        JsonNode value = first(json, names);
        if (value == null) return null;
        double result;
        if (value.isNumber()) {
            result = value.doubleValue();
        } else if (value.isTextual()) {
            try {
                result = Double.parseDouble(value.asText().trim());
            } catch (NumberFormatException e) {
                throw invalidNumber(names[0], e);
            }
        } else {
            throw invalidNumber(names[0], null);
        }
        if (!Double.isFinite(result)) throw invalidNumber(names[0], null);
        return result;
    }

    private String readText(JsonNode json, String... names) {
        JsonNode value = first(json, names);
        return value == null ? null : value.asText();
    }

    private <T> T defaultValue(T value, T fallback) {
        return value == null ? fallback : value;
    }

    private void setLong(JsonNode json, String name, java.util.function.Consumer<Long> setter) {
        Long value = readLong(json, name); if (value != null) setter.accept(value);
    }
    private void setInteger(JsonNode json, String name, java.util.function.Consumer<Integer> setter) {
        Long value = readLong(json, name);
        if (value != null) {
            try {
                setter.accept(Math.toIntExact(value));
            } catch (ArithmeticException e) {
                throw invalidNumber(name, e);
            }
        }
    }
    private void setDouble(JsonNode json, String name, java.util.function.Consumer<Double> setter) {
        Double value = readDouble(json, name); if (value != null) setter.accept(value);
    }
    private void setBoolean(JsonNode json, String name, java.util.function.Consumer<Boolean> setter) {
        JsonNode value = first(json, name);
        if (value == null) return;
        if (value.isBoolean()) {
            setter.accept(value.booleanValue());
            return;
        }
        if (value.isTextual() && ("true".equalsIgnoreCase(value.asText()) || "false".equalsIgnoreCase(value.asText()))) {
            setter.accept(Boolean.valueOf(value.asText()));
            return;
        }
        throw new TelemetriaMessageException(name + " deve ser booleano");
    }
    private void setText(JsonNode json, String name, java.util.function.Consumer<String> setter) {
        String value = readText(json, name); if (value != null) setter.accept(value);
    }
    private void setJson(JsonNode json, String name, java.util.function.Consumer<Object> setter) {
        JsonNode value = first(json, name);
        if (value != null) setter.accept(objectMapper.convertValue(value, Object.class));
    }

    private TelemetriaMessageException invalidNumber(String field, Exception cause) {
        String message = field + " deve ser numérico";
        return cause == null
                ? new TelemetriaMessageException(message)
                : new TelemetriaMessageException(message, cause);
    }
}
