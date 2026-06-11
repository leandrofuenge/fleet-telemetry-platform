package com.telemetria.domain.dto;

import com.telemetria.domain.enums.SeveridadeAlerta;
import com.telemetria.domain.enums.TipoAlerta;

public record CriarAlertaCommand(
    Long tenantId,
    Long veiculoId,
    String veiculoUuid,
    Long viagemId,
    Long motoristaId,
    TipoAlerta tipo,
    SeveridadeAlerta severidade,
    String mensagem,
    Double latitude,
    Double longitude,
    Double velocidadeKmh,
    Double odometroKm
) {}