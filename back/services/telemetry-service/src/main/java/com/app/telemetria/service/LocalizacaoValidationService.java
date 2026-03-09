package com.app.telemetria.service;

import com.app.telemetria.entity.*;
import com.app.telemetria.repository.*;
import com.app.telemetria.util.DistanciaCalculator;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class LocalizacaoValidationService {

    private final IPLocateService ipLocateService;
    private final TelemetriaService telemetriaService;
    private final ViagemRepository viagemRepository;
    private final AlertaRepository alertaRepository;
    private final VeiculoRepository veiculoRepository;
    private final DistanciaCalculator distanciaCalculator;
    private final LocationClassifierService locationClassifierService;

    private static final double DISTANCIA_MAXIMA_IP_GPS = 50.0;
    private static final double DISTANCIA_MAXIMA_IP_ROTA = 100.0;

    public LocalizacaoValidationService(
            IPLocateService ipLocateService,
            TelemetriaService telemetriaService,
            ViagemRepository viagemRepository,
            AlertaRepository alertaRepository,
            VeiculoRepository veiculoRepository,
            DistanciaCalculator distanciaCalculator,
            LocationClassifierService locationClassifierService) {

        this.ipLocateService = ipLocateService;
        this.telemetriaService = telemetriaService;
        this.viagemRepository = viagemRepository;
        this.alertaRepository = alertaRepository;
        this.veiculoRepository = veiculoRepository;
        this.distanciaCalculator = distanciaCalculator;
        this.locationClassifierService = locationClassifierService;
    }

    public void validarLocalizacaoMotorista(Long motoristaId, Long veiculoId, String ip) {

        var localizacaoIP = ipLocateService.buscarLocalizacaoPorIP(ip);

        if (localizacaoIP.isEmpty()) {
            criarAlertaLocalizacao(veiculoId, motoristaId,
                    "LOCALIZACAO_DESCONHECIDA",
                    "Não foi possível determinar localização do IP: " + ip,
                    "MEDIA");
            return;
        }

        var ipInfo = localizacaoIP.get();

        if (ipInfo.usandoProxy()) {
            criarAlertaLocalizacao(veiculoId, motoristaId,
                    "PROXY_DETECTADO",
                    "Uso de proxy/VPN detectado: " + ip,
                    "ALTA");
        }

        confrontarComTelemetria(veiculoId, motoristaId, ipInfo);
        confrontarComRotaAtiva(veiculoId, motoristaId, ipInfo);
        validarTipoViaComOSM(veiculoId, motoristaId);
    }

    private void confrontarComTelemetria(Long veiculoId, Long motoristaId,
                                         IPLocateService.LocalizacaoInfo ipInfo) {

        var ultimaTelemetria = telemetriaService.buscarUltimaPorVeiculo(veiculoId);

        if (ultimaTelemetria.isEmpty()) return;

        var telemetria = ultimaTelemetria.get();

        double distancia = distanciaCalculator.calcularDistancia(
                ipInfo.latitude(), ipInfo.longitude(),
                telemetria.getLatitude(), telemetria.getLongitude()
        );

        if (distancia > DISTANCIA_MAXIMA_IP_GPS) {
            criarAlertaLocalizacao(veiculoId, motoristaId,
                    "DISCREPANCIA_LOCALIZACAO",
                    "IP distante do GPS do veículo: " + distancia + " km",
                    distancia > 100 ? "ALTA" : "MEDIA"
            );
        }
    }

    private void confrontarComRotaAtiva(Long veiculoId, Long motoristaId,
                                        IPLocateService.LocalizacaoInfo ipInfo) {

        Optional<Viagem> viagemAtiva =
                viagemRepository.findByVeiculoIdAndStatus(veiculoId, "EM_ANDAMENTO");

        if (viagemAtiva.isEmpty()) return;

        Rota rota = viagemAtiva.get().getRota();

        double distancia = distanciaCalculator.calcularDistanciaAteRota(
                ipInfo.latitude(), ipInfo.longitude(),
                rota.getLatitudeOrigem(), rota.getLongitudeOrigem(),
                rota.getLatitudeDestino(), rota.getLongitudeDestino()
        );

        if (distancia > DISTANCIA_MAXIMA_IP_ROTA) {
            criarAlertaLocalizacao(veiculoId, motoristaId,
                    "LOCALIZACAO_INESPERADA",
                    "IP fora da rota ativa: " + distancia + " km",
                    distancia > 200 ? "ALTA" : "MEDIA"
            );
        }
    }

    private void validarTipoViaComOSM(Long veiculoId, Long motoristaId) {

        var ultimaTelemetria = telemetriaService.buscarUltimaPorVeiculo(veiculoId);
        if (ultimaTelemetria.isEmpty()) return;

        var telemetria = ultimaTelemetria.get();

        String classificacao = locationClassifierService.classify(
                telemetria.getLatitude(),
                telemetria.getLongitude()
        );

        criarAlertaLocalizacao(veiculoId, motoristaId,
                "CLASSIFICACAO_VIA",
                "Veículo classificado como: " + classificacao,
                "INFO");
    }

    private void criarAlertaLocalizacao(Long veiculoId, Long motoristaId,
                                        String tipo, String mensagem, String gravidade) {

        veiculoRepository.findById(veiculoId).ifPresent(veiculo -> {

            Alerta alerta = new Alerta();
            alerta.setVeiculo(veiculo);

            if (motoristaId != null) {
                Motorista motorista = new Motorista();
                motorista.setId(motoristaId);
                alerta.setMotorista(motorista);
            }

            alerta.setTipo(tipo);
            alerta.setGravidade(gravidade);
            alerta.setMensagem(mensagem);
            alerta.setDataHora(LocalDateTime.now());
            alerta.setLido(false);
            alerta.setResolvido(false);

            alertaRepository.save(alerta);

            System.out.println("📍 [" + gravidade + "] " + mensagem);
        });
    }
}