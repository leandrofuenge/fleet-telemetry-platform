package com.telemetria.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telemetria.domain.entity.Telemetria;
import com.telemetria.domain.entity.VeiculoCache;
import com.telemetria.domain.exception.TelemetriaMessageException;
import com.telemetria.infrastructure.messaging.dto.KafkaMessageMetadata;
import com.telemetria.infrastructure.messaging.dto.TelemetriaEnvelope;

class TelemetriaMessageMapperTest {

    private final TelemetriaMessageMapper mapper = new TelemetriaMessageMapper(new ObjectMapper(), 4096, "UTC");
    private final KafkaMessageMetadata metadata = new KafkaMessageMetadata("telemetria-raw", 2, 42L);

    @Test
    void shouldMapContractAndKeepEventTimeSeparateFromIngestionTime() {
        String payload = """
                {
                  "event_id":"evt-123",
                  "sequence_number":17,
                  "tenant_id":8,
                  "veiculo_id":99,
                  "device_id":"tracker-1",
                  "latitude":-15.601,
                  "longitude":-56.097,
                  "velocidade":72.5,
                  "timestamp":1704067200000,
                  "hdop":2.1,
                  "satelites":9,
                  "ignicao":true
                }
                """;

        TelemetriaEnvelope envelope = mapper.parse(payload, metadata);
        VeiculoCache vehicle = vehicle(99L, 8L);
        Telemetria telemetry = mapper.toEntity(envelope, vehicle);

        assertThat(envelope.eventId()).isEqualTo("evt-123");
        assertThat(envelope.sequenceNumber()).isEqualTo(17L);
        assertThat(telemetry.getDataHora()).isEqualTo(LocalDateTime.of(2024, 1, 1, 0, 0));
        assertThat(telemetry.getRecebidoEm()).isNull();
        assertThat(telemetry.getIgnicao()).isTrue();
        assertThat(telemetry.getHdop()).isEqualTo(2.1);
    }

    @Test
    void shouldDeriveStableEventIdFromKafkaOffsetWhenDeviceDoesNotSendOne() {
        TelemetriaEnvelope envelope = mapper.parse(
                "{\"veiculo_id\":1,\"latitude\":-15.6,\"longitude\":-56.1}", metadata);

        assertThat(envelope.eventId()).isEqualTo("kafka:telemetria-raw:2:42");
    }

    @Test
    void shouldRejectMalformedCoordinates() {
        assertThatThrownBy(() -> mapper.parse(
                "{\"veiculo_id\":1,\"latitude\":91,\"longitude\":-56.1}", metadata))
                .isInstanceOf(TelemetriaMessageException.class)
                .hasMessageContaining("latitude");
    }

    @Test
    void shouldRejectTextThatIsNotNumericInsteadOfSilentlyConvertingToZero() {
        assertThatThrownBy(() -> mapper.parse(
                "{\"veiculo_id\":1,\"latitude\":\"invalid\",\"longitude\":-56.1}", metadata))
                .isInstanceOf(TelemetriaMessageException.class)
                .hasMessageContaining("latitude deve ser numérico");
    }

    private VeiculoCache vehicle(Long id, Long tenantId) {
        VeiculoCache vehicle = new VeiculoCache();
        vehicle.setId(id);
        vehicle.setTenantId(tenantId);
        vehicle.setUuid("00000000-0000-0000-0000-000000000099");
        vehicle.setAtivo(true);
        return vehicle;
    }
}
