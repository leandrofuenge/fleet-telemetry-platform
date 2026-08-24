package com.telemetria.domain.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.api.dto.response.RouteResponse;
import com.telemetria.domain.entity.Rota;
import com.telemetria.domain.entity.Telemetria;
import com.telemetria.domain.entity.Viagem;
import com.telemetria.domain.enums.SeveridadeAlerta;
import com.telemetria.domain.enums.TipoAlerta;
import com.telemetria.infrastructure.integration.geocoding.LocationClassifierService;
import com.telemetria.infrastructure.integration.routing.RoutingClient;
import com.telemetria.infrastructure.persistence.AlertaRepository;

@Service
public class TelemetriaRegrasService {

    private static final Logger log = LoggerFactory.getLogger(TelemetriaRegrasService.class);
    
    // Constantes sincronizadas rigorosamente com as suas definições de domínio
    private static final double VELOCIDADE_MAXIMA = 110.0;
    private static final double VELOCIDADE_MINIMA = 10.0;
    private static final int TEMPO_PARADA_MAXIMO = 30;
    private static final int NIVEL_COMBUSTIVEL_MINIMO = 15;
    private static final int TEMPO_DIRECAO_MAXIMO = 240;

    private final AlertaRepository alertaRepository;
    private final LocationClassifierService locationClassifierService;
    private final RoutingClient routingClient;
    private final AlertaBaseService alertaBaseService; // Serviço interno que encapsula o seu criarAlerta legado

    public TelemetriaRegrasService(AlertaRepository alertaRepository, 
                                  LocationClassifierService locationClassifierService, 
                                  RoutingClient routingClient,
                                  AlertaBaseService alertaBaseService) {
        this.alertaRepository = alertaRepository;
        this.locationClassifierService = locationClassifierService;
        this.routingClient = routingClient;
        this.alertaBaseService = alertaBaseService;
    }

    // ================= EXCESSO DE VELOCIDADE =================
    public void verificarExcessoVelocidade(Telemetria telemetria) {
        Double velocidade = telemetria.getVelocidade();
        if (velocidade == null || velocidade <= VELOCIDADE_MAXIMA) return;

        executarCriacaoAlertaExcesso(telemetria);
    }

    @Transactional
    protected void executarCriacaoAlertaExcesso(Telemetria telemetria) {
        boolean alertaRecente = alertaRepository
                .findPrimeiroByVeiculoIdAndTipoOrderByDataHoraDesc(telemetria.getVeiculoId(), TipoAlerta.EXCESSO_VELOCIDADE)
                .map(ultimo -> Duration.between(ultimo.getDataHora(), LocalDateTime.now()).toMinutes() <= 5)
                .orElse(false);

        if (alertaRecente) return;

        double diferenca = telemetria.getVelocidade() - VELOCIDADE_MAXIMA;
        String msg = "Veículo " + String.format("%.2f", diferenca) + " km/h acima do limite (" + VELOCIDADE_MAXIMA + " km/h)";
        
        alertaBaseService.salvarAlertaMapeado(telemetria, TipoAlerta.EXCESSO_VELOCIDADE, SeveridadeAlerta.ALTO, msg, null, null);
    }

    // ================= VELOCIDADE BAIXA =================
    public void verificarVelocidadeBaixa(Telemetria telemetria, Viagem viagem) {
        Double velocidade = telemetria.getVelocidade();
        if (velocidade == null || velocidade >= VELOCIDADE_MINIMA || velocidade <= 0) return;

        // Utiliza o serviço correto da sua infraestrutura para checagem de malha urbana
        boolean emAreaUrbana = locationClassifierService.verificarAreaUrbana(telemetria.getLatitude(), telemetria.getLongitude());
        if (emAreaUrbana) return;

        executarCriacaoAlertaVelocidadeBaixa(telemetria, viagem);
    }

    @Transactional
    protected void executarCriacaoAlertaVelocidadeBaixa(Telemetria telemetria, Viagem viagem) {
        String msg = "Velocidade muito baixa: " + String.format("%.1f", telemetria.getVelocidade()) + " km/h";
        alertaBaseService.salvarAlertaMapeado(telemetria, TipoAlerta.VELOCIDADE_BAIXA, SeveridadeAlerta.MEDIO, msg, 
                viagem != null ? viagem.getMotoristaId() : null, viagem != null ? viagem.getId() : null);
    }

    // ================= NÍVEL DE COMBUSTÍVEL =================
    public void verificarNivelCombustivel(Telemetria telemetria, Viagem viagem) {
        Double nivel = telemetria.getNivelCombustivel();
        if (nivel == null || nivel >= NIVEL_COMBUSTIVEL_MINIMO) return;

        executarCriacaoAlertaCombustivel(telemetria, viagem, nivel);
    }

    @Transactional
    protected void executarCriacaoAlertaCombustivel(Telemetria telemetria, Viagem viagem, Double nivel) {
        boolean alertaAtivo = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalse(telemetria.getVeiculoId(), TipoAlerta.NIVEL_COMBUSTIVEL_BAIXO);
        if (alertaAtivo) return;

        String msg = "Nível de combustível baixo: " + String.format("%.0f", nivel) + "%";
        alertaBaseService.salvarAlertaMapeado(telemetria, TipoAlerta.NIVEL_COMBUSTIVEL_BAIXO, SeveridadeAlerta.MEDIO, msg,
                viagem != null ? viagem.getMotoristaId() : null, viagem != null ? viagem.getId() : null);
    }

    @Transactional
    public void resolverAlertaCombustivelSeNecessario(Long veiculoId, Double nivelCombustivel) {
        if (veiculoId == null || nivelCombustivel == null || nivelCombustivel < NIVEL_COMBUSTIVEL_MINIMO) return;

        if (!alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalse(veiculoId, TipoAlerta.NIVEL_COMBUSTIVEL_BAIXO)) return;

        alertaRepository.findPrimeiroByVeiculoIdAndTipoAndResolvidoFalse(veiculoId, TipoAlerta.NIVEL_COMBUSTIVEL_BAIXO)
                .ifPresent(alerta -> {
                    alerta.setResolvido(true);
                    alertaRepository.save(alerta);
                });
    }

    // ================= GPS SEM SINAL =================
    public void verificarGpsSemSinal(Long veiculoId, Telemetria ultimaTelemetria) {
        if (veiculoId == null || ultimaTelemetria == null || ultimaTelemetria.getDataHora() == null) return;

        long minutosSemSinal = Duration.between(ultimaTelemetria.getDataHora(), LocalDateTime.now()).toMinutes();
        if (minutosSemSinal <= 15) return; // Parâmetro de tolerância de sinal padrão

        executarCriacaoAlertaGpsSemSinal(veiculoId, ultimaTelemetria, minutosSemSinal);
    }

    @Transactional
    protected void executarCriacaoAlertaGpsSemSinal(Long veiculoId, Telemetria ultimaTelemetria, long minutosSemSinal) {
        boolean alertaAtivo = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalse(veiculoId, TipoAlerta.GPS_SEM_SINAL);
        if (alertaAtivo) return;

        String msg = "Veículo sem sinal GPS há " + minutosSemSinal + " minutos";
        alertaBaseService.salvarAlertaMapeado(ultimaTelemetria, TipoAlerta.GPS_SEM_SINAL, SeveridadeAlerta.ALTO, msg, null, null);
    }

    @Transactional
    public void resolverAlertaGpsSeNecessario(Long veiculoId) {
        if (veiculoId == null) return;
        if (!alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalse(veiculoId, TipoAlerta.GPS_SEM_SINAL)) return;

        alertaRepository.findPrimeiroByVeiculoIdAndTipoAndResolvidoFalse(veiculoId, TipoAlerta.GPS_SEM_SINAL)
                .ifPresent(alerta -> {
                    alerta.setResolvido(true);
                    alertaRepository.save(alerta);
                });
    }

    // ================= TEMPO DE DIREÇÃO =================
    public void verificarTempoDirecao(Viagem viagem, Telemetria ultimaTelemetria) {
        if (viagem == null || viagem.getMotoristaId() == null || viagem.getDataSaida() == null) return;

        long minutosDirigindo = Duration.between(viagem.getDataSaida(), LocalDateTime.now()).toMinutes();
        if (minutosDirigindo <= TEMPO_DIRECAO_MAXIMO) return;

        executarCriacaoAlertaDirecao(viagem, ultimaTelemetria, minutosDirigindo);
    }

    @Transactional
    protected void executarCriacaoAlertaDirecao(Viagem viagem, Telemetria ultimaTelemetria, long minutosDirigindo) {
        boolean alertaAtivo = alertaRepository.existsByViagemIdAndTipoAndResolvidoFalse(viagem.getId(), TipoAlerta.TEMPO_DIRECAO);
        if (alertaAtivo) return;

        String msg = "Motorista dirigindo por " + minutosDirigindo + " minutos sem pausa";
        alertaBaseService.salvarAlertaMapeado(ultimaTelemetria, TipoAlerta.TEMPO_DIRECAO, SeveridadeAlerta.ALTO, msg, viagem.getMotoristaId(), viagem.getId());
    }

    // ================= ATRASO INTELIGENTE DE VIAGEM =================
    public void verificarAtrasoViagemInteligente(Viagem viagem, Telemetria ultimaTelemetria) {
        if (viagem == null || ultimaTelemetria == null || viagem.getDataChegadaPrevista() == null || viagem.getRota() == null) return;

        try {
            Rota rota = viagem.getRota();
            RouteResponse rotaCalculada = routingClient.calcular(
                    ultimaTelemetria.getLatitude(), ultimaTelemetria.getLongitude(),
                    rota.getLatitudeDestino(), rota.getLongitudeDestino()
            );

            if (rotaCalculada == null) return;

            LocalDateTime etaReal = LocalDateTime.now().plusMinutes((long) rotaCalculada.getDuracaoMinutos());
            if (!etaReal.isAfter(viagem.getDataChegadaPrevista())) return;

            long atrasoReal = Duration.between(viagem.getDataChegadaPrevista(), etaReal).toMinutes();
            executarCriacaoAlertaAtraso(viagem, ultimaTelemetria, atrasoReal);

        } catch (Exception e) {
            log.error("❌ Falha na integração do roteador para o veículo {}", viagem.getVeiculoId(), e);
        }
    }

    private void executarCriacaoAlertaAtraso(Viagem viagem, Telemetria ultimaTelemetria, long atrasoReal) {
		// TODO Auto-generated method stub
		
	}

	@Transactional
    protected void ejecutarCriacaoAlertaAtraso(Viagem viagem, Telemetria telemetria, long atrasoReal) {
        boolean alertaAtivo = alertaRepository.existsByViagemIdAndTipo(viagem.getId(), TipoAlerta.ATRASO_VIAGEM);
        if (alertaAtivo) return;

        String msg = "Atraso real estimado: " + atrasoReal + " minutos";
        alertaBaseService.salvarAlertaMapeado(telemetria, TipoAlerta.ATRASO_VIAGEM, SeveridadeAlerta.ALTO, msg, viagem.getMotoristaId(), viagem.getId());
    }
}