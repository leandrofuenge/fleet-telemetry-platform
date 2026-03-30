package com.telemetria.application.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.telemetria.domain.entity.Alerta;
import com.telemetria.domain.entity.Rota;
import com.telemetria.domain.entity.Viagem;
import com.telemetria.domain.enums.SeveridadeAlerta;
import com.telemetria.domain.enums.TipoAlerta;
import com.telemetria.domain.service.TelemetriaService;
import com.telemetria.infrastructure.integration.geocoding.IPLocateService;
import com.telemetria.infrastructure.integration.geocoding.LocationClassifierService;
import com.telemetria.infrastructure.persistence.AlertaRepository;
import com.telemetria.infrastructure.persistence.ViagemRepository;
import com.telemetria.util.DistanciaCalculator;

@Service
public class LocalizacaoValidationService {

    private final IPLocateService ipLocateService;
    private final TelemetriaService telemetriaService;
    private final ViagemRepository viagemRepository;
    private final AlertaRepository alertaRepository;
    private final DistanciaCalculator distanciaCalculator;
    private final LocationClassifierService locationClassifierService;

    private static final double DISTANCIA_MAXIMA_IP_GPS = 50.0;
    private static final double DISTANCIA_MAXIMA_IP_ROTA = 100.0;

    public LocalizacaoValidationService(
            IPLocateService ipLocateService,
            TelemetriaService telemetriaService,
            ViagemRepository viagemRepository,
            AlertaRepository alertaRepository,
            DistanciaCalculator distanciaCalculator,
            LocationClassifierService locationClassifierService) {

        this.ipLocateService = ipLocateService;
        this.telemetriaService = telemetriaService;
        this.viagemRepository = viagemRepository;
        this.alertaRepository = alertaRepository;
        this.distanciaCalculator = distanciaCalculator;
        this.locationClassifierService = locationClassifierService;
    }

    public void validarLocalizacaoMotorista(Long motoristaId, Long veiculoId, String ip) {

        var localizacaoIP = ipLocateService.buscarLocalizacaoPorIP(ip);

        if (localizacaoIP.isEmpty()) {
            criarAlertaLocalizacao(veiculoId, motoristaId,
                    TipoAlerta.OUTRO, // ou LOCALIZACAO_DESCONHECIDA se existir
                    "Não foi possível determinar localização do IP: " + ip,
                    SeveridadeAlerta.MEDIO);
            return;
        }

        var ipInfo = localizacaoIP.get();

        if (ipInfo.usandoProxy()) {
            criarAlertaLocalizacao(veiculoId, motoristaId,
                    TipoAlerta.OUTRO, // ou PROXY_DETECTADO
                    "Uso de proxy/VPN detectado: " + ip,
                    SeveridadeAlerta.ALTO);
        }

        confrontarComTelemetria(veiculoId, motoristaId, ipInfo);
        confrontarComRotaAtiva(veiculoId, motoristaId, ipInfo);
        validarTipoViaComOSM(veiculoId, motoristaId);
    }

    private void confrontarComTelemetria(Long veiculoId, Long motoristaId,
            IPLocateService.LocalizacaoInfo ipInfo) {

        var ultimaTelemetria = telemetriaService.buscarUltimaPorVeiculo(veiculoId);

        if (ultimaTelemetria.isEmpty())
            return;

        var telemetria = ultimaTelemetria.get();

        double distancia = distanciaCalculator.calcularDistancia(
                ipInfo.latitude(), ipInfo.longitude(),
                telemetria.getLatitude(), telemetria.getLongitude());

        if (distancia > DISTANCIA_MAXIMA_IP_GPS) {
            criarAlertaLocalizacao(veiculoId, motoristaId,
                    TipoAlerta.OUTRO, // ou DISCREPANCIA_LOCALIZACAO
                    "IP distante do GPS do veículo: " + distancia + " km",
                    distancia > 100 ? SeveridadeAlerta.ALTO : SeveridadeAlerta.MEDIO);
        }
    }

    private void confrontarComRotaAtiva(Long veiculoId, Long motoristaId,
            IPLocateService.LocalizacaoInfo ipInfo) {

        Optional<Viagem> viagemAtiva = viagemRepository.findByVeiculoIdAndStatus(veiculoId, "EM_ANDAMENTO");

        if (viagemAtiva.isEmpty())
            return;

        Rota rota = viagemAtiva.get().getRota();

        double distancia = distanciaCalculator.calcularDistanciaAteRota(
                ipInfo.latitude(), ipInfo.longitude(),
                rota.getLatitudeOrigem(), rota.getLongitudeOrigem(),
                rota.getLatitudeDestino(), rota.getLongitudeDestino());

        if (distancia > DISTANCIA_MAXIMA_IP_ROTA) {
            criarAlertaLocalizacao(veiculoId, motoristaId,
                    TipoAlerta.OUTRO, // ou LOCALIZACAO_INESPERADA
                    "IP fora da rota ativa: " + distancia + " km",
                    distancia > 200 ? SeveridadeAlerta.ALTO : SeveridadeAlerta.MEDIO);
        }
    }

    private void validarTipoViaComOSM(Long veiculoId, Long motoristaId) {

        var ultimaTelemetria = telemetriaService.buscarUltimaPorVeiculo(veiculoId);
        if (ultimaTelemetria.isEmpty())
            return;

        var telemetria = ultimaTelemetria.get();

        String classificacao = locationClassifierService.classify(
                telemetria.getLatitude(),
                telemetria.getLongitude());

        criarAlertaLocalizacao(veiculoId, motoristaId,
                TipoAlerta.OUTRO, // ou CLASSIFICACAO_VIA
                "Veículo classificado como: " + classificacao,
                SeveridadeAlerta.BAIXO); // INFO substituído por BAIXO
    }

    private void criarAlertaLocalizacao(Long veiculoId, Long motoristaId,
            TipoAlerta tipo, String mensagem, SeveridadeAlerta severidade) {

        Alerta alerta = new Alerta();
        Long tenantId = telemetriaService.buscarUltimaPorVeiculo(veiculoId)
                .map(t -> t.getTenantId())
                .orElse(1L);
        alerta.setTenantId(tenantId);
        alerta.setVeiculoId(veiculoId);
        alerta.setMotoristaId(motoristaId);
        alerta.setTipo(tipo);
        alerta.setSeveridade(severidade);
        alerta.setMensagem(mensagem);
        alerta.setDataHora(LocalDateTime.now());
        alerta.setLido(false);
        alerta.setResolvido(false);

        alertaRepository.save(alerta);

        System.out.println("📍 [" + severidade + "] " + mensagem);
    }
}
