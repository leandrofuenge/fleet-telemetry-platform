package com.telemetria.domain.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.DesvioRota;
import com.telemetria.domain.entity.PontoRota;
import com.telemetria.domain.entity.Rota;
import com.telemetria.domain.entity.Telemetria;
import com.telemetria.domain.entity.Veiculo;
import com.telemetria.domain.enums.SeveridadeAlerta;
import com.telemetria.domain.enums.TipoAlerta;
import com.telemetria.domain.enums.TipoVia;
import com.telemetria.infrastructure.integration.geocoding.GeocodingService;
import com.telemetria.infrastructure.integration.routing.OSRMMapMatchingService;
import com.telemetria.infrastructure.persistence.AlertaRepository;
import com.telemetria.infrastructure.persistence.DesvioRotaRepository;
import com.telemetria.infrastructure.persistence.RotaRepository;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;
import com.telemetria.infrastructure.persistence.VeiculoCacheRepository;
import com.telemetria.infrastructure.persistence.VeiculoRepository;
import com.telemetria.util.DistanciaCalculator;

/**
 * RN-ROT-002 - Detector de desvios de rota com thresholds por tipo de via
 */
@Service
public class DetectorDesvioRotaService {

    private static final Logger log = LoggerFactory.getLogger(DetectorDesvioRotaService.class);

    // RN-ROT-002: Configurações
    private static final double MAX_KM_EXTRAS_ALERTA = 2.0; // km acumulados para alerta crítico
    private static final double METROS_POR_GRAU = 111320.0; // Aproximadamente 111.32 km por grau

    // RN-DEV-001: Configuracoes de Map Matching
    private static final int EVENTOS_CONSECUTIVOS_DESVIO = 3;
    private static final double THRESHOLD_CONFIDENCIA_MATCH = 50.0; // metros

    private static final int LIMITE_HISTORICO_TELEMETRIA = 20;
    
    // RN-DEV-001: Cache em memória de curta duração
    private final Map<Long, Integer> contadorDesviosConsecutivos = new ConcurrentHashMap<>();
    private final Map<Long, List<Telemetria>> historicoTelemetria = new ConcurrentHashMap<>();
    
    private final RotaRepository rotaRepository;
    private final TelemetriaRepository telemetriaRepository;
    private final DesvioRotaRepository desvioRotaRepository;
    private final AlertaRepository alertaRepository;
    private final ClassificacaoTipoViaService classificacaoTipoViaService;
    private final OSRMMapMatchingService mapMatchingService;
    private final DistanciaCalculator distanciaCalculator;

    public DetectorDesvioRotaService(
            RotaRepository rotaRepository,
            TelemetriaRepository telemetriaRepository,
            DesvioRotaRepository desvioRotaRepository,
            VeiculoRepository veiculoRepository,
            AlertaRepository alertaRepository,
            ClassificacaoTipoViaService classificacaoTipoViaService,
            DistanciaCalculator distanciaCalculator,
            GeocodingService geocodingService,
            OSRMMapMatchingService mapMatchingService,
            VeiculoCacheRepository veiculoCacheRepository) {
        this.rotaRepository = rotaRepository;
        this.telemetriaRepository = telemetriaRepository;
        this.desvioRotaRepository = desvioRotaRepository;
        this.alertaRepository = alertaRepository;
        this.classificacaoTipoViaService = classificacaoTipoViaService;
        this.distanciaCalculator = distanciaCalculator;
        this.mapMatchingService = mapMatchingService;
    }

    /**
     * Remove @Transactional daqui para evitar travar o banco em loops longos.
     */
    public void verificarDesviosAtivos() {
        log.debug("🔍 Verificando desvios ativos...");

        List<Rota> rotasAtivas = rotaRepository.findByStatus("EM_ANDAMENTO");
        log.debug("📊 Rotas ativas encontradas: {}", rotasAtivas.size());

        for (Rota rota : rotasAtivas) {
            try {
                // Chama o processamento isolado em uma transação própria por rota
                verificarDesvioParaRotaIsolado(rota);
            } catch (Exception e) {
                log.error("❌ Erro ao verificar desvio para rota {}: {}", rota.getId(), e.getMessage(), e);
            }
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void verificarDesvioParaRotaIsolado(Rota rota) {
        Veiculo veiculo = rota.getVeiculo();

        if (veiculo == null || veiculo.getId() == null) {
            log.warn("⚠️ Rota {} sem veículo associado", rota.getId());
            return;
        }

        Long veiculoId = veiculo.getId();
        Optional<Telemetria> telemetriaOpt = telemetriaRepository.findUltimaTelemetriaByVeiculoId(veiculoId);

        if (telemetriaOpt.isEmpty()) {
            log.debug("ℹ️ Nenhuma telemetria encontrada para veículo {}", veiculoId);
            return;
        }

        Telemetria ultimaTelemetria = telemetriaOpt.get();
        double latitude = ultimaTelemetria.getLatitude();
        double longitude = ultimaTelemetria.getLongitude();

        // RN-DEV-001: Map Matching OSRM otimizado
        double distanciaAteRota = calcularDistanciaAteRotaComMapMatching(latitude, longitude, rota);

        // RN-ROT-002: Classificar tipo de via
        TipoVia tipoVia = classificacaoTipoViaService.classificarTipoVia(latitude, longitude);
        double tolerancia = tipoVia.getToleranciaMetros();

        log.debug("📍 Rota {} - Distância: {}m, Tipo via: {}, Tolerância: {}m",
                rota.getId(), String.format("%.2f", distanciaAteRota), tipoVia.getDescricao(), tolerancia);

        if (distanciaAteRota > tolerancia) {
            processarDesvio(rota, ultimaTelemetria, distanciaAteRota, tipoVia, veiculoId);
        } else {
            processarVeiculoDentroDaRota(rota, ultimaTelemetria, veiculoId);
        }
    }

    private void processarDesvio(Rota rota, Telemetria telemetria, double distanciaAteRota, TipoVia tipoVia, Long veiculoId) {
        int contador = contadorDesviosConsecutivos.merge(veiculoId, 1, Integer::sum);
        adicionarAoHistorico(veiculoId, telemetria);

        log.debug("🔴 Veículo {} fora da rota - Evento {}/{}", veiculoId, contador, EVENTOS_CONSECUTIVOS_DESVIO);

        if (contador >= EVENTOS_CONSECUTIVOS_DESVIO) {
            log.info("🚨 Desvio confirmado para veículo {} após {} eventos consecutivos", veiculoId, contador);
            registrarDesvio(rota, telemetria, distanciaAteRota, tipoVia);
        }
    }

    private void processarVeiculoDentroDaRota(Rota rota, Telemetria telemetria, Long veiculoId) {
        if (contadorDesviosConsecutivos.containsKey(veiculoId)) {
            log.debug("✅ Veículo {} retornou à rota - Resetando contador de desvios", veiculoId);
            contadorDesviosConsecutivos.remove(veiculoId);
            historicoTelemetria.remove(veiculoId);
        }
        verificarRetornoRota(rota, telemetria);
    }

    private void adicionarAoHistorico(Long veiculoId, Telemetria telemetria) {
        List<Telemetria> historico = historicoTelemetria.computeIfAbsent(veiculoId, k -> new ArrayList<>());
        historico.add(telemetria);

        if (historico.size() > LIMITE_HISTORICO_TELEMETRIA) {
            historico.remove(0);
        }
    }

    /**
     * RN-DEV-001: Otimizado com tratamento de fallback correto e cálculo quadrático.
     */
    private double calcularDistanciaAteRotaComMapMatching(double lat, double lng, Rota rota) {
        Optional<OSRMMapMatchingService.MatchResult> match = mapMatchingService.matchSinglePoint(lat, lng);
        
        if (match.isPresent()) {
            OSRMMapMatchingService.MatchResult result = match.get();
            
            // Se o match for confiável, usa as coordenadas "snapadas"
            if (result.distanceToMatch() <= THRESHOLD_CONFIDENCIA_MATCH) {
                return calcularDistanciaAteRota(result.matchedLat(), result.matchedLon(), rota);
            }
            
            log.warn("⚠️ Map Matching com baixa confianca ({}m). Usando coordenadas originais.", result.distanceToMatch());
        } else {
            log.warn("⚠️ Map Matching falhou, usando coordenadas originais");
        }
        
        return calcularDistanciaAteRota(lat, lng, rota);
    }

    private double calcularDistanciaAteRota(double lat, double lng, Rota rota) {
        List<PontoRota> pontos = rota.getPontosRota();
        if (pontos == null || pontos.size() < 2) {
            return Double.MAX_VALUE;
        }

        double menorDistanciaAoQuadrado = Double.MAX_VALUE;

        for (int i = 0; i < pontos.size() - 1; i++) {
            PontoRota p1 = pontos.get(i);
            PontoRota p2 = pontos.get(i + 1);

            double distAoQuadrado = calcularDistanciaQuadráticaPontoParaSegmento(
                    lat, lng,
                    p1.getLatitude(), p1.getLongitude(),
                    p2.getLatitude(), p2.getLongitude());

            if (distAoQuadrado < menorDistanciaAoQuadrado) {
                menorDistanciaAoQuadrado = distAoQuadrado;
            }
        }

        return Math.sqrt(menorDistanciaAoQuadrado) * METROS_POR_GRAU;
    }

    private double calcularDistanciaQuadráticaPontoParaSegmento(
            double px, double py,
            double x1, double y1,
            double x2, double y2) {

        double vX = x2 - x1; 
        double vY = y2 - y1; 
        double wX = px - x1; 
        double wY = py - y1; 

        double produtoEscalar = wX * vX + wY * vY;
        double comprimentoSegmentoAoQuadrado = vX * vX + vY * vY;

        if (comprimentoSegmentoAoQuadrado == 0) {
            return wX * wX + wY * wY;
        }

        double t = produtoEscalar / comprimentoSegmentoAoQuadrado;
        double pontoMaisProximoX;
        double pontoMaisProximoY;

        if (t < 0.0) {
            pontoMaisProximoX = x1;
            pontoMaisProximoY = y1;
        } else if (t > 1.0) {
            pontoMaisProximoX = x2;
            pontoMaisProximoY = y2;
        } else {
            pontoMaisProximoX = x1 + t * vX;
            pontoMaisProximoY = y1 + t * vY;
        }

        double dx = px - pontoMaisProximoX;
        double dy = py - pontoMaisProximoY;

        return dx * dx + dy * dy;
    }

    private void registrarDesvio(Rota rota, Telemetria telemetria, double distancia, TipoVia tipoVia) {
        Optional<DesvioRota> desvioAtivo = desvioRotaRepository.findByRotaIdAndResolvidoFalse(rota.getId());

        if (desvioAtivo.isEmpty()) {
            log.info("🚨 Novo desvio detectado para rota {} - Tipo via: {}", rota.getId(), tipoVia.getDescricao());
            Long tenantId = telemetria.getTenantId();

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
                    .tipoVia(tipoVia.name())
                    .build();

            desvioRotaRepository.save(desvio);
            log.info("✅ Desvio registrado com sucesso. ID: {}, Tenant: {}", desvio.getId(), tenantId);
            notificarDesvio(desvio, rota, tipoVia, false);
            
        } else {
            DesvioRota desvio = desvioAtivo.get();
            double kmExtrasAdicionais = calcularKmExtrasAdicionais(desvio, telemetria);
            double novoKmExtras = desvio.getKmExtras() + kmExtrasAdicionais;
            desvio.setKmExtras(novoKmExtras);
            
            if (desvio.getDataHoraDesvio() != null) {
                long duracaoMinutos = ChronoUnit.MINUTES.between(desvio.getDataHoraDesvio(), LocalDateTime.now());
                desvio.setDuracaoMin((int) duracaoMinutos);
            }
            
            desvioRotaRepository.save(desvio);
            log.debug("📊 Desvio atualizado - Km extras: {}km, Duração: {} min", String.format("%.3f", novoKmExtras), desvio.getDuracaoMin());
            
            if (novoKmExtras >= MAX_KM_EXTRAS_ALERTA && !desvio.getAlertaEnviado()) {
                log.warn("⚠️ Alerta crítico: Veículo atingiu {}km fora da rota!", String.format("%.2f", novoKmExtras));
                notificarDesvio(desvio, rota, tipoVia, true);
                desvio.setAlertaEnviado(true);
                desvioRotaRepository.save(desvio);
                criarAlertaCritico(desvio, rota, novoKmExtras);
            }
        }
    }
    
    private double calcularKmExtrasAdicionais(DesvioRota desvio, Telemetria telemetriaAtual) {
        Optional<Telemetria> ultimaTelemetria = telemetriaRepository.findUltimaTelemetriaByVeiculoId(desvio.getVeiculoId());
        
        if (ultimaTelemetria.isEmpty()) {
            return 0.0;
        }
        
        Telemetria anterior = ultimaTelemetria.get();
        return distanciaCalculator.calcularDistancia(
                anterior.getLatitude(), anterior.getLongitude(),
                telemetriaAtual.getLatitude(), telemetriaAtual.getLongitude()
        );
    }
    
    private void criarAlertaCritico(DesvioRota desvio, Rota rota, double kmExtras) {
        try {
            com.telemetria.domain.entity.Alerta alerta = com.telemetria.domain.entity.Alerta.builder()
                    .tenantId(desvio.getTenantId())
                    .veiculoId(desvio.getVeiculoId())
                    .veiculoUuid(desvio.getVeiculoUuid())
                    .viagemId(desvio.getViagemId())
                    .tipo(TipoAlerta.DESVIO_ROTA_CRITICO)
                    .severidade(SeveridadeAlerta.CRITICO)
                    .mensagem(String.format(
                        "⚠️ ALERTA CRÍTICO: Veículo está há %.2fkm fora da rota '%s'. Tipo de via: %s. Tolerância excedida.",
                        kmExtras, rota.getNome(), desvio.getTipoVia()))
                    .latitude(desvio.getLatitudeDesvio())
                    .longitude(desvio.getLongitudeDesvio())
                    .velocidadeKmh(desvio.getVelocidadeKmh())
                    .odometroKm(desvio.getKmExtras())
                    .dataHora(LocalDateTime.now())
                    .lido(false)
                    .resolvido(false)
                    .build();
            
            alertaRepository.save(alerta);
            log.info("🚨 Alerta crítico criado para desvio ID: {}", desvio.getId());
        } catch (Exception e) {
            log.error("❌ Erro ao criar alerta crítico: {}", e.getMessage());
        }
    }

    private Long buscarViagemAtiva(Long veiculoId) {
        return null;
    }

    private void verificarRetornoRota(Rota rota, Telemetria telemetria) {
        Optional<DesvioRota> desvioAtivo = desvioRotaRepository.findByRotaIdAndResolvidoFalse(rota.getId());

        if (desvioAtivo.isPresent()) {
            DesvioRota desvio = desvioAtivo.get();
            desvio.setResolvido(true);
            desvio.setDataHoraRetorno(LocalDateTime.now());

            if (desvio.getDataHoraDesvio() != null) {
                long duracaoMinutos = ChronoUnit.MINUTES.between(desvio.getDataHoraDesvio(), desvio.getDataHoraRetorno());
                desvio.setDuracaoMin((int) duracaoMinutos);
            }

            log.info("✅ Veículo retornou à rota {}. Desvio {} resolvido. Km extras totais: {}km, Duração: {} min", 
                    rota.getId(), desvio.getId(), String.format("%.3f", desvio.getKmExtras()), desvio.getDuracaoMin());

            desvioRotaRepository.save(desvio);
            notificarRetorno(rota, desvio);
        }
    }

    private void notificarDesvio(DesvioRota desvio, Rota rota, TipoVia tipoVia, boolean isCritico) {
        String nivel = isCritico ? "🚨 CRÍTICO" : "⚠️ ALERTA";
        String mensagem = String.format(
                "%s DESVIO DE ROTA DETECTADO!\n" +
                "Rota: %s\n" +
                "Veículo ID: %d\n" +
                "Distância: %.2f metros\n" +
                "Tipo de via: %s (tolerância: %.0fm)\n" +
                "Local: %.6f, %.6f\n" +
                "Km extras acumulados: %.3fkm",
                nivel, rota.getNome(), desvio.getVeiculoId(), desvio.getDistanciaMetros(),
                tipoVia.getDescricao(), tipoVia.getToleranciaMetros(),
                desvio.getLatitudeDesvio(), desvio.getLongitudeDesvio(), desvio.getKmExtras());

        log.info("\n{}", mensagem);
    }

    private void notificarRetorno(Rota rota, DesvioRota desvio) {
        String mensagem = String.format(
                "✅ VEÍCULO RETORNOU À ROTA!\n" +
                "Rota: %s\n" +
                "Veículo: %s\n" +
                "Km extras totais: %.3fkm\n" +
                "Duração do desvio: %d minutos",
                rota.getNome(),
                rota.getVeiculo() != null ? rota.getVeiculo().getPlaca() : "N/A",
                desvio.getKmExtras(), desvio.getDuracaoMin());

        log.info("\n{}", mensagem);
    }
}
