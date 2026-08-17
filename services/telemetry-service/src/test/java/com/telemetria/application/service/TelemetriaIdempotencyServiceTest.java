package com.telemetria.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.telemetria.domain.entity.Telemetria;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;

@ExtendWith(MockitoExtension.class)
class TelemetriaIdempotencyServiceTest {

    @Mock
    private TelemetriaRepository repository;

    private TelemetriaIdempotencyService service;

    @BeforeEach
    void setUp() {
        service = new TelemetriaIdempotencyService(repository);
    }

    @Test
    void shouldDetectDuplicateSequenceBeforeInsert() {
        when(repository.existsByTenantIdAndDeviceIdAndSequenceNumber(1L, "dev-1", 10L)).thenReturn(true);

        var result = service.checkSequence(1L, 2L, "dev-1", 10L);

        assertThat(result.duplicate()).isTrue();
        assertThat(result.outOfOrder()).isFalse();
    }

    @Test
    void shouldMarkLateSequenceAndPreserveIt() {
        Telemetria latest = new Telemetria();
        latest.setSequenceNumber(20L);
        when(repository.existsByTenantIdAndDeviceIdAndSequenceNumber(1L, "dev-1", 18L)).thenReturn(false);
        when(repository.findTopByVeiculoIdAndDeviceIdAndSequenceNumberIsNotNullOrderBySequenceNumberDesc(2L, "dev-1"))
                .thenReturn(Optional.of(latest));

        var result = service.checkSequence(1L, 2L, "dev-1", 18L);

        assertThat(result.duplicate()).isFalse();
        assertThat(result.outOfOrder()).isTrue();
        assertThat(result.previousSequence()).isEqualTo(20L);
    }

    @Test
    void shouldCalculateMissingSequenceGap() {
        Telemetria latest = new Telemetria();
        latest.setSequenceNumber(20L);
        when(repository.existsByTenantIdAndDeviceIdAndSequenceNumber(1L, "dev-1", 25L)).thenReturn(false);
        when(repository.findTopByVeiculoIdAndDeviceIdAndSequenceNumberIsNotNullOrderBySequenceNumberDesc(2L, "dev-1"))
                .thenReturn(Optional.of(latest));

        var result = service.checkSequence(1L, 2L, "dev-1", 25L);

        assertThat(result.gap()).isEqualTo(4);
        assertThat(result.outOfOrder()).isFalse();
    }
}
