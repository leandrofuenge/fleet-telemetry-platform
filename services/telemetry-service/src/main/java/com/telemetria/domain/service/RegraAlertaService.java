package com.telemetria.domain.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.Alerta;
import com.telemetria.domain.entity.RegraAlerta;
import com.telemetria.domain.entity.Telemetria;
import com.telemetria.domain.enums.SeveridadeAlerta;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.infrastructure.persistence.AlertaRepository;
import com.telemetria.infrastructure.persistence.RegraAlertaRepository;

/** Avalia regras de alertas configuradas e aplica cooldown, agrupamento e quarentena. */
@Service
public class RegraAlertaService {
    private final RegraAlertaRepository regraRepository;
    private final AlertaRepository alertaRepository;
    private final StringRedisTemplate redis;

    public RegraAlertaService(RegraAlertaRepository regraRepository, AlertaRepository alertaRepository,
                              StringRedisTemplate redis) {
        this.regraRepository = regraRepository;
        this.alertaRepository = alertaRepository;
        this.redis = redis;
    }

    @Transactional
    public RegraAlerta salvar(RegraAlerta regra) {
        validar(regra);
        return regraRepository.save(regra);
    }

    public List<RegraAlerta> listar(Long tenantId) { return regraRepository.findByTenantIdOrderByNomeAsc(tenantId); }

    public void avaliar(Telemetria telemetria) {
        if (telemetria.getTenantId() == null || telemetria.getVeiculoId() == null) return;
        for (RegraAlerta regra : regraRepository.findByTenantIdAndAtivoTrue(telemetria.getTenantId())) {
            if (atingiu(regra, telemetria)) dispararSePermitido(regra, telemetria);
        }
    }

    private void dispararSePermitido(RegraAlerta regra, Telemetria telemetria) {
        String base = "alerta:regra:" + regra.getId() + ":veiculo:" + telemetria.getVeiculoId();
        // A janela contabiliza todas as ocorrências, inclusive as suprimidas pelo cooldown.
        // Caso contrário, uma regra com cooldown longo jamais chegaria ao agrupamento/quarentena.
        String janela = base + ":janela";
        Long ocorrencias = redis.opsForValue().increment(janela);
        if (Long.valueOf(1).equals(ocorrencias)) redis.expire(janela, Duration.ofMinutes(5));
        if (ocorrencias != null && ocorrencias > 100) {
            redis.opsForValue().set(base + ":quarentena", "1", Duration.ofHours(1));
            return;
        }
        if (Boolean.TRUE.equals(redis.hasKey(base + ":quarentena"))) return;

        boolean agrupado = ocorrencias != null && ocorrencias > 10;
        if (agrupado && Boolean.FALSE.equals(redis.opsForValue().setIfAbsent(base + ":agrupado", "1", Duration.ofMinutes(5)))) return;
        if (!agrupado && Boolean.FALSE.equals(redis.opsForValue().setIfAbsent(base + ":cooldown", "1",
                Duration.ofMinutes(regra.getCooldownMinutos())))) return;
        SeveridadeAlerta severidade = agrupado ? SeveridadeAlerta.CRITICO : regra.getSeveridade();
        String mensagem = agrupado
                ? "Mais de 10 ocorrências em cinco minutos: " + regra.getNome()
                : "Regra disparada: " + regra.getNome();
        alertaRepository.save(Alerta.builder().tenantId(telemetria.getTenantId())
                .veiculoId(telemetria.getVeiculoId()).veiculoUuid(telemetria.getVeiculoUuid())
                .telemetriaId(telemetria.getId()).regraId(regra.getId()).tipo(regra.getTipo())
                .severidade(severidade).mensagem(mensagem).latitude(telemetria.getLatitude())
                .longitude(telemetria.getLongitude()).dataHora(LocalDateTime.now())
                .canaisNotificados(regra.getCanais()).dadosContexto(Map.of("campo", regra.getCampo(),
                        "operador", regra.getOperador(), "valorLimite", regra.getValorLimite())).build());
    }

    private boolean atingiu(RegraAlerta regra, Telemetria t) {
        Double valor = switch (regra.getCampo()) {
            case "velocidade" -> t.getVelocidade();
            case "hdop" -> t.getHdop();
            case "temperatura_carga" -> t.getTemperaturaCarga();
            case "sinal_gsm" -> t.getSinalGsm();
            case "pressao_oleo" -> t.getPressaoOleo();
            default -> null;
        };
        if (valor == null || regra.getValorLimite() == null) return false;
        return switch (regra.getOperador()) {
            case ">" -> valor > regra.getValorLimite();
            case ">=" -> valor >= regra.getValorLimite();
            case "<" -> valor < regra.getValorLimite();
            case "<=" -> valor <= regra.getValorLimite();
            case "=" -> Double.compare(valor, regra.getValorLimite()) == 0;
            default -> false;
        };
    }

    private void validar(RegraAlerta regra) {
        if (regra == null || regra.getTenantId() == null || regra.getNome() == null || regra.getNome().isBlank()
                || regra.getTipo() == null || regra.getSeveridade() == null || regra.getCampo() == null
                || regra.getOperador() == null || regra.getValorLimite() == null
                || regra.getCooldownMinutos() == null || regra.getCooldownMinutos() < 1
                || !List.of(">", ">=", "<", "<=", "=").contains(regra.getOperador())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Regra de alerta inválida");
        }
    }
}
