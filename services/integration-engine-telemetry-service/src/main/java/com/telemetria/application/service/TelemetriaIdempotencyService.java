package com.telemetria.application.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.telemetria.domain.entity.Telemetria;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;

@Service
public class TelemetriaIdempotencyService {

    private final TelemetriaRepository repository;

    public TelemetriaIdempotencyService(TelemetriaRepository repository) {
        this.repository = repository;
    }

    public boolean eventAlreadyProcessed(Long tenantId, String eventId) {
        return repository.existsByTenantIdAndEventId(tenantId, eventId);
    }

    public SequenceCheck checkSequence(
            Long tenantId,
            Long veiculoId,
            String deviceId,
            Long sequenceNumber) {
        if (sequenceNumber == null) {
            return SequenceCheck.notProvided();
        }

        boolean duplicate = deviceId != null
                ? repository.existsByTenantIdAndDeviceIdAndSequenceNumber(tenantId, deviceId, sequenceNumber)
                : repository.existsByTenantIdAndVeiculoIdAndDeviceIdIsNullAndSequenceNumber(
                        tenantId, veiculoId, sequenceNumber);
        if (duplicate) {
            return new SequenceCheck(true, false, 0L, null);
        }

        Optional<Telemetria> latest = deviceId != null
                ? repository.findTopByVeiculoIdAndDeviceIdAndSequenceNumberIsNotNullOrderBySequenceNumberDesc(
                        veiculoId, deviceId)
                : repository.findTopByVeiculoIdAndSequenceNumberIsNotNullOrderBySequenceNumberDesc(veiculoId);

        if (latest.isEmpty() || latest.get().getSequenceNumber() == null) {
            return new SequenceCheck(false, false, 0L, null);
        }

        long previous = latest.get().getSequenceNumber();
        boolean outOfOrder = sequenceNumber < previous;
        long gap = sequenceNumber > previous + 1 ? sequenceNumber - previous - 1 : 0;
        return new SequenceCheck(false, outOfOrder, gap, previous);
    }

    public record SequenceCheck(boolean duplicate, boolean outOfOrder, long gap, Long previousSequence) {
        public static SequenceCheck notProvided() {
            return new SequenceCheck(false, false, 0L, null);
        }
    }
}
