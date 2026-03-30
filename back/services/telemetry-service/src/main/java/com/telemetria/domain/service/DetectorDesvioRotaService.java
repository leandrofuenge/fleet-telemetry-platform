package com.telemetria.domain.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.DesvioRota;
import com.telemetria.domain.entity.PontoRota;
import com.telemetria.domain.entity.Rota;
import com.telemetria.domain.entity.Telemetria;
import com.telemetria.infrastructure.integration.geocoding.GeocodingService;
import com.telemetria.infrastructure.persistence.DesvioRotaRepository;
import com.telemetria.infrastructure.persistence.RotaRepository;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;
import com.telemetria.infrastructure.persistence.VeiculoRepository;

@Service
public class DetectorDesvioRotaService {

    private static final Logger log = LoggerFactory.getLogger(DetectorDesvioRotaService.class);
    
    private final RotaRepository rotaRepository;
    private final TelemetriaRepository telemetriaRepository;
    private final DesvioRotaRepository desvioRotaRepository;
    private final VeiculoRepository veiculoRepository;
    
    private static final double TOLERANCIA_DESVIO = 50.0;

    public DetectorDesvioRotaService(
            RotaRepository rotaRepository,
            TelemetriaRepository telemetriaRepository,
            DesvioRotaRepository desvioRotaRepository,
            VeiculoRepository veiculoRepository,
            GeocodingService geocodingService) {
        this.rotaRepository = rotaRepository;
        this.telemetriaRepository = telemetriaRepository;
        this.desvioRotaRepository = desvioRotaRepository;
        this.veiculoRepository = veiculoRepository;
    }

    @Transactional
    public void verificarDesviosAtivos() {
        log.debug("🔍 Verificando desvios ativos...");
        
        List<Rota> rotasAtivas = rotaRepository.findByStatus("EM_ANDAMENTO");
        
        log.debug("📊 Rotas ativas encontradas: {}", rotasAtivas.size());

        for (Rota rota : rotasAtivas) {
            try {
                verificarDesvioParaRota(rota);
            } catch (Exception e) {
                log.error("❌ Erro ao verificar desvio para rota {}: {}", rota.getId(), e.getMessage(), e);
            }
        }
    }

    private void verificarDesvioParaRota(Rota rota) {
        if (rota.getVeiculo() == null || rota.getVeiculo().getId() == null) {
            log.warn("⚠️ Rota {} sem veículo associado", rota.getId());
            return;
        }

        Optional<Telemetria> optTelemetria = telemetriaRepository
                .findUltimaTelemetriaByVeiculoId(rota.getVeiculo().getId());

        if (optTelemetria.isEmpty()) {
            log.debug("ℹ️ Nenhuma telemetria encontrada para veículo {}", rota.getVeiculo().getId());
            return;
        }

        Telemetria ultimaTelemetria = optTelemetria.get();

        double distanciaAteRota = calcularDistanciaAteRota(
                ultimaTelemetria.getLatitude(),
                ultimaTelemetria.getLongitude(),
                rota);

        log.debug("📏 Distância até rota {}: {}m (tolerância: {}m)", 
                 rota.getId(), distanciaAteRota, TOLERANCIA_DESVIO);

        if (distanciaAteRota > TOLERANCIA_DESVIO) {
            registrarDesvio(rota, ultimaTelemetria, distanciaAteRota);
        } else {
            verificarRetornoRota(rota, ultimaTelemetria);
        }
    }

    private double calcularDistanciaAteRota(double lat, double lng, Rota rota) {
        double distanciaMinima = Double.MAX_VALUE;

        List<PontoRota> pontos = rota.getPontosRota();
        if (pontos == null || pontos.size() < 2) {
            return Double.MAX_VALUE;
        }

        for (int i = 0; i < pontos.size() - 1; i++) {
            PontoRota p1 = pontos.get(i);
            PontoRota p2 = pontos.get(i + 1);

            double distancia = distanciaPontoParaSegmento(
                    lat, lng,
                    p1.getLatitude(), p1.getLongitude(),
                    p2.getLatitude(), p2.getLongitude());

            distanciaMinima = Math.min(distanciaMinima, distancia);
        }

        return distanciaMinima;
    }

    private double distanciaPontoParaSegmento(
            double px, double py,
            double x1, double y1,
            double x2, double y2) {

        double A = px - x1;
        double B = py - y1;
        double C = x2 - x1;
        double D = y2 - y1;

        double dot = A * C + B * D;
        double len_sq = C * C + D * D;
        double param = len_sq != 0 ? dot / len_sq : -1;

        double xx, yy;

        if (param < 0) {
            xx = x1;
            yy = y1;
        } else if (param > 1) {
            xx = x2;
            yy = y2;
        } else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }

        double dx = px - xx;
        double dy = py - yy;

        return Math.sqrt(dx * dx + dy * dy) * 111320;
    }
    
    @Transactional
    private void registrarDesvio(Rota rota, Telemetria telemetria, double distancia) {
        Optional<DesvioRota> desvioAtivo = desvioRotaRepository
                .findByRotaIdAndResolvidoFalse(rota.getId());

        if (desvioAtivo.isEmpty()) {
            log.info("🚨 Novo desvio detectado para rota {}", rota.getId());
            
            Long tenantId = 1L; // Valor padrão
            
            if (rota.getVeiculo() != null && rota.getVeiculo().getCliente() != null) {
                // tenantId = rota.getVeiculo().getCliente().getTenantId(); // Descomente quando implementado
            }

            DesvioRota desvio = DesvioRota.builder()
                    .rotaId(rota.getId())
                    .veiculoId(telemetria.getVeiculoId())
                    .veiculoUuid(telemetria.getVeiculoUuid())
                    .viagemId(buscarViagemAtiva(telemetria.getVeiculoId()))
                    .latitudeDesvio(telemetria.getLatitude())
                    .longitudeDesvio(telemetria.getLongitude())
                    .velocidadeKmh(telemetria.getVelocidade())
                    .distanciaMetros(distancia)
                    .dataHoraDesvio(LocalDateTime.now())
                    .alertaEnviado(false)
                    .resolvido(false)
                    .kmExtras(0.0)
                    .tenantId(tenantId)
                    .build();

            desvioRotaRepository.save(desvio);
            log.info("✅ Desvio registrado com sucesso. ID: {}, Tenant: {}", desvio.getId(), tenantId);

            notificarDesvio(desvio, rota);
        } else {
            log.debug("ℹ️ Desvio já registrado para rota {}", rota.getId());
        }
    }

    private Long buscarViagemAtiva(Long veiculoId) {
        return null;
    }

    private void verificarRetornoRota(Rota rota, Telemetria telemetria) {
        Optional<DesvioRota> desvioAtivo = desvioRotaRepository
                .findByRotaIdAndResolvidoFalse(rota.getId());

        if (desvioAtivo.isPresent()) {
            DesvioRota desvio = desvioAtivo.get();
            desvio.setResolvido(true);
            desvio.setDataHoraRetorno(LocalDateTime.now());
            
            log.info("✅ Veículo retornou à rota {}. Desvio {} resolvido", rota.getId(), desvio.getId());

            desvioRotaRepository.save(desvio);
            notificarRetorno(rota);
        }
    }

    private void notificarDesvio(DesvioRota desvio, Rota rota) {
        String mensagem = String.format(
                "🚨 DESVIO DE ROTA DETECTADO!\n" +
                "Rota: %s\n" +
                "Veículo ID: %d\n" +
                "Distância: %.2f metros\n" +
                "Local: %.6f, %.6f",
                rota.getNome(),
                desvio.getVeiculoId(),
                desvio.getDistanciaMetros(),
                desvio.getLatitudeDesvio(),
                desvio.getLongitudeDesvio());

        log.info("\n{}", mensagem);
    }

    private void notificarRetorno(Rota rota) {
        String mensagem = String.format(
                "✅ VEÍCULO RETORNOU À ROTA!\n" +
                "Rota: %s\n" +
                "Veículo: %s",
                rota.getNome(),
                rota.getVeiculo() != null ? rota.getVeiculo().getPlaca() : "N/A");

        log.info("\n{}", mensagem);
    }
}