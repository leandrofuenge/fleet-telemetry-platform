package com.telemetria.domain.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.telemetria.domain.entity.Telemetria;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.infrastructure.integration.geocoding.LocationClassifierService;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;
import com.telemetria.util.DistanciaCalculator;

@Service
public class GpsValidationService {

    private final TelemetriaRepository telemetriaRepository;
    private final DistanciaCalculator distanciaCalculator;
    private final AlertaService alertaService;
    private final LocationClassifierService locationClassifierService;

    // Configurações
    @Value("${gps.velocidade.max:300}")
    private double velocidadeMaxima;

    @Value("${gps.velocidade.consecutivos:2}")
    private int velocidadeConsecutivos;

    @Value("${gps.salto.distancia:100}")
    private double saltoDistanciaKm;

    @Value("${gps.salto.tempo:60}")
    private int saltoTempoSegundos;

    @Value("${gps.hdop.limite:10}")
    private double hdopLimite;

    @Value("${gps.hdop.tempo_minutos:5}")
    private int hdopTempoMinutos;

    @Value("${gps.satelites.limite:4}")
    private int satelitesLimite;

    @Value("${gps.satelites.tempo_minutos:10}")
    private int satelitesTempoMinutos;

    // Estado por veículo
    private final ConcurrentHashMap<Long, Integer> velocidadeAltaCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, LocalDateTime> hdopAltoInicio = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, LocalDateTime> satelitesBaixoInicio = new ConcurrentHashMap<>();

    @Autowired
    public GpsValidationService(TelemetriaRepository telemetriaRepository,
                                DistanciaCalculator distanciaCalculator,
                                AlertaService alertaService,
                                LocationClassifierService locationClassifierService) {
        this.telemetriaRepository = telemetriaRepository;
        this.distanciaCalculator = distanciaCalculator;
        this.alertaService = alertaService;
        this.locationClassifierService = locationClassifierService;
    }

    /**
     * Valida GPS e detecta adulteração (RN-TEL-002)
     */
    public void validarGps(Telemetria telemetriaAtual, Optional<Telemetria> telemetriaAnterior) {
        // 1. Velocidade impossível (> 300 km/h por 2 eventos consecutivos)
        validarVelocidadeImpossivel(telemetriaAtual, telemetriaAnterior);

        // 2. Salto de posição > 100 km em < 60 segundos
        validarSaltoPosicao(telemetriaAtual, telemetriaAnterior);

        // 3. HDOP > 10 por mais de 5 minutos
        validarHdop(telemetriaAtual);

        // 4. Satélites < 4 em área aberta por mais de 10 minutos
        validarSatelites(telemetriaAtual);
    }

    private void validarVelocidadeImpossivel(Telemetria atual, Optional<Telemetria> anterior) {
        if (atual.getVelocidade() == null) return;

        Long veiculoId = atual.getVeiculoId();

        if (atual.getVelocidade() > velocidadeMaxima) {
            // Incrementa contador de eventos consecutivos
            int count = velocidadeAltaCount.getOrDefault(veiculoId, 0) + 1;
            velocidadeAltaCount.put(veiculoId, count);

            if (count >= velocidadeConsecutivos) {
                // Marca adulteração e gera alerta
                atual.setAdulteracaoGps(true);
                alertaService.criarAlertaVelocidadeImpossivel(atual);
                // Reseta contador após gerar alerta (opcional)
                velocidadeAltaCount.remove(veiculoId);
            }
        } else {
            // Reset contador se velocidade voltou ao normal
            velocidadeAltaCount.remove(veiculoId);
        }
    }

    private void validarSaltoPosicao(Telemetria atual, Optional<Telemetria> anterior) {
        if (anterior.isEmpty()) return;

        Telemetria prev = anterior.get();

        double distanciaKm = distanciaCalculator.calcularDistancia(
                prev.getLatitude(), prev.getLongitude(),
                atual.getLatitude(), atual.getLongitude());

        long segundos = Duration.between(prev.getDataHora(), atual.getDataHora()).getSeconds();

        if (distanciaKm > saltoDistanciaKm && segundos < saltoTempoSegundos) {
            // Gera alerta CRÍTICO e descarta o evento
            alertaService.criarAlertaSaltoPosicao(atual, distanciaKm, segundos);
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                String.format("Salto GPS impossível: %.1fkm em %ds", distanciaKm, segundos));
        }
    }

    private void validarHdop(Telemetria atual) {
        if (atual.getHdop() == null) return;

        Long veiculoId = atual.getVeiculoId();

        if (atual.getHdop() > hdopLimite) {
            LocalDateTime inicio = hdopAltoInicio.get(veiculoId);
            if (inicio == null) {
                // Primeiro evento com HDOP alto
                hdopAltoInicio.put(veiculoId, atual.getDataHora());
            } else {
                // Já estava com HDOP alto, verificar duração
                long minutos = Duration.between(inicio, atual.getDataHora()).toMinutes();
                if (minutos >= hdopTempoMinutos) {
                    atual.setImpreciso(true);
                    alertaService.criarAlertaHdopAlto(atual);
                    // Opcional: resetar para não gerar alertas repetidos (podemos manter flag)
                }
            }
        } else {
            // HDOP normal, limpar estado
            hdopAltoInicio.remove(veiculoId);
        }
    }

    private void validarSatelites(Telemetria atual) {
        if (atual.getSatelites() == null) return;

        Long veiculoId = atual.getVeiculoId();

        // Só aplica regra se for área aberta
        boolean areaAberta = isAreaAberta(atual.getLatitude(), atual.getLongitude());

        if (areaAberta && atual.getSatelites() < satelitesLimite) {
            LocalDateTime inicio = satelitesBaixoInicio.get(veiculoId);
            if (inicio == null) {
                // Primeiro evento com satélites baixos
                satelitesBaixoInicio.put(veiculoId, atual.getDataHora());
            } else {
                // Já estava baixo, verificar duração
                long minutos = Duration.between(inicio, atual.getDataHora()).toMinutes();
                if (minutos >= satelitesTempoMinutos) {
                    alertaService.criarAlertaSatelitesBaixos(atual, minutos);
                    // Opcional: resetar para não gerar alertas repetidos
                    satelitesBaixoInicio.remove(veiculoId);
                }
            }
        } else {
            // Se saiu da área aberta ou satélites normalizados, limpar estado
            satelitesBaixoInicio.remove(veiculoId);
        }
    }

    private boolean isAreaAberta(double latitude, double longitude) {
        try {
            String classificacao = locationClassifierService.classify(latitude, longitude);
            // "RODOVIA" geralmente é área aberta; "AREA_URBANA" não.
            return "RODOVIA".equals(classificacao);
        } catch (Exception e) {
            return false; // Em caso de erro, assume que não é área aberta
        }
    }

    public Optional<Telemetria> buscarAnterior(Long veiculoId, LocalDateTime dataHora) {
        return telemetriaRepository.findUltimaTelemetriaAntes(veiculoId, dataHora);
    }
}