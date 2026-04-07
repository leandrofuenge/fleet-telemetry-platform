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
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.DesvioRota;
import com.telemetria.domain.entity.PontoRota;
import com.telemetria.domain.entity.Rota;
import com.telemetria.domain.entity.Telemetria;
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
    
    // RN-DEV-001: Cache para contagem de eventos consecutivos de desvio por veiculo
    private final Map<Long, Integer> contadorDesviosConsecutivos = new ConcurrentHashMap<>();
    private final Map<Long, List<Telemetria>> historicoTelemetria = new ConcurrentHashMap<>();
    
    private final RotaRepository rotaRepository;
    private final TelemetriaRepository telemetriaRepository;
    private final DesvioRotaRepository desvioRotaRepository;
    private final VeiculoRepository veiculoRepository;
    private final AlertaRepository alertaRepository;
    private final ClassificadorViaService classificadorViaService;
    private final DistanciaCalculator distanciaCalculator;
    private final GeocodingService geocodingService;
    private final OSRMMapMatchingService mapMatchingService;
    private final VeiculoCacheRepository veiculoCacheRepository;

    public DetectorDesvioRotaService(
            RotaRepository rotaRepository,
            TelemetriaRepository telemetriaRepository,
            DesvioRotaRepository desvioRotaRepository,
            VeiculoRepository veiculoRepository,
            AlertaRepository alertaRepository,
            ClassificadorViaService classificadorViaService,
            DistanciaCalculator distanciaCalculator,
            GeocodingService geocodingService,
            OSRMMapMatchingService mapMatchingService,
            VeiculoCacheRepository veiculoCacheRepository) {
        this.rotaRepository = rotaRepository;
        this.telemetriaRepository = telemetriaRepository;
        this.desvioRotaRepository = desvioRotaRepository;
        this.veiculoRepository = veiculoRepository;
        this.alertaRepository = alertaRepository;
        this.classificadorViaService = classificadorViaService;
        this.distanciaCalculator = distanciaCalculator;
        this.geocodingService = geocodingService;
        this.mapMatchingService = mapMatchingService;
        this.veiculoCacheRepository = veiculoCacheRepository;
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

        Long veiculoId = rota.getVeiculo().getId();
        Optional<Telemetria> optTelemetria = telemetriaRepository
                .findUltimaTelemetriaByVeiculoId(veiculoId);

        if (optTelemetria.isEmpty()) {
            log.debug("ℹ️ Nenhuma telemetria encontrada para veículo {}", veiculoId);
            return;
        }

        Telemetria ultimaTelemetria = optTelemetria.get();

        // RN-DEV-001: Map Matching OSRM para eliminar ruido GPS
        double distanciaAteRota = calcularDistanciaAteRotaComMapMatching(
                ultimaTelemetria.getLatitude(),
                ultimaTelemetria.getLongitude(),
                rota);

        // RN-ROT-002: Classificar tipo de via
        TipoVia tipoVia = classificadorViaService.classificar(
                ultimaTelemetria.getLatitude(),
                ultimaTelemetria.getLongitude());
        
        double tolerancia = tipoVia.getToleranciaMetros();

        log.debug("📍 Rota {} - Distancia (apos Map Matching): {:.2f}m, Tipo via: {}, Tolerancia: {}m",
                 rota.getId(), distanciaAteRota, tipoVia.getDescricao(), tolerancia);

        // RN-DEV-001: Verificar se esta fora da rota
        boolean estaForaDaRota = distanciaAteRota > tolerancia;
        
        if (estaForaDaRota) {
            // Incrementar contador de eventos consecutivos
            int contador = contadorDesviosConsecutivos.getOrDefault(veiculoId, 0) + 1;
            contadorDesviosConsecutivos.put(veiculoId, contador);
            
            // Armazenar telemetria no historico
            historicoTelemetria.computeIfAbsent(veiculoId, k -> new ArrayList<>()).add(ultimaTelemetria);
            
            log.debug("🔴 Veiculo {} fora da rota - Evento {}/{}", veiculoId, contador, EVENTOS_CONSECUTIVOS_DESVIO);
            
            // RN-DEV-001: So registrar desvio apos 3 eventos consecutivos
            if (contador >= EVENTOS_CONSECUTIVOS_DESVIO) {
                log.info("🚨 Desvio confirmado para veiculo {} apos {} eventos consecutivos", 
                        veiculoId, contador);
                registrarDesvio(rota, ultimaTelemetria, distanciaAteRota, tipoVia);
            }
        } else {
            // Veiculo dentro da rota - resetar contador
            if (contadorDesviosConsecutivos.containsKey(veiculoId)) {
                log.debug("✅ Veiculo {} retornou a rota - Resetando contador de desvios", veiculoId);
                contadorDesviosConsecutivos.remove(veiculoId);
                historicoTelemetria.remove(veiculoId);
            }
            verificarRetornoRota(rota, ultimaTelemetria);
        }
    }

    /**
     * RN-DEV-001: Calcula distancia ate a rota usando OSRM Map Matching
     * Elimina ruido GPS antes de calcular a distancia
     */
    private double calcularDistanciaAteRotaComMapMatching(double lat, double lng, Rota rota) {
        // Aplicar Map Matching para obter coordenadas "snapadas" a rota mais provavel
        Optional<OSRMMapMatchingService.MatchResult> match = 
                mapMatchingService.matchSinglePoint(lat, lng);
        
        if (match.isPresent()) {
            OSRMMapMatchingService.MatchResult result = match.get();
            
            // Verificar se o match tem boa qualidade (confianca)
            if (result.distanceToMatch() > THRESHOLD_CONFIDENCIA_MATCH) {
                log.warn("⚠️ Map Matching com baixa confianca: {}m (threshold: {}m)", 
                        result.distanceToMatch(), THRESHOLD_CONFIDENCIA_MATCH);
            }
            
            // Usar coordenadas matchadas para calcular distancia ate a rota planejada
            return calcularDistanciaAteRota(result.matchedLat(), result.matchedLon(), rota);
        }
        
        // Fallback: usar coordenadas originais se Map Matching falhar
        log.warn("⚠️ Map Matching falhou, usando coordenadas originais");
        return calcularDistanciaAteRota(lat, lng, rota);
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

        return Math.sqrt(dx * dx + dy * dy) * METROS_POR_GRAU;
    }

    @Transactional
    private void registrarDesvio(Rota rota, Telemetria telemetria, double distancia, TipoVia tipoVia) {
        Optional<DesvioRota> desvioAtivo = desvioRotaRepository
                .findByRotaIdAndResolvidoFalse(rota.getId());

        if (desvioAtivo.isEmpty()) {
            // Novo desvio
            log.info("🚨 Novo desvio detectado para rota {} - Tipo via: {}", rota.getId(), tipoVia.getDescricao());

            Long tenantId = 1L; // Valor padrão

            if (rota.getVeiculo() != null && rota.getVeiculo().getCliente() != null) {
                // tenantId = rota.getVeiculo().getCliente().getTenantId();
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
                    .tipoVia(tipoVia.name())
                    .build();

            desvioRotaRepository.save(desvio);
            log.info("✅ Desvio registrado com sucesso. ID: {}, Tenant: {}", desvio.getId(), tenantId);

            // Enviar alerta de desvio (não crítico)
            notificarDesvio(desvio, rota, tipoVia, false);
            
        } else {
            // Desvio já existe - atualizar km extras
            DesvioRota desvio = desvioAtivo.get();
            
            // Calcular km extras adicionais desde a última telemetria
            double kmExtrasAdicionais = calcularKmExtrasAdicionais(desvio, telemetria);
            double novoKmExtras = desvio.getKmExtras() + kmExtrasAdicionais;
            desvio.setKmExtras(novoKmExtras);
            
            // Atualizar duração do desvio
            if (desvio.getDataHoraDesvio() != null) {
                long duracaoMinutos = ChronoUnit.MINUTES.between(
                    desvio.getDataHoraDesvio(), LocalDateTime.now());
                desvio.setDuracaoMin((int) duracaoMinutos);
            }
            
            desvioRotaRepository.save(desvio);
            
            log.debug("📊 Desvio atualizado - Km extras: {:.3f}km, Duração: {} min", 
                     novoKmExtras, desvio.getDuracaoMin());
            
            // RN-ROT-002: Verificar se atingiu 2km extras para alerta crítico
            if (novoKmExtras >= MAX_KM_EXTRAS_ALERTA && !desvio.getAlertaEnviado()) {
                log.warn("⚠️ Alerta crítico: Veículo atingiu {:.2f}km fora da rota!", novoKmExtras);
                notificarDesvio(desvio, rota, tipoVia, true);
                desvio.setAlertaEnviado(true);
                desvioRotaRepository.save(desvio);
                
                // Criar alerta crítico no sistema
                criarAlertaCritico(desvio, rota, novoKmExtras);
            }
        }
    }
    
    /**
     * Calcula km extras percorridos desde a última telemetria registrada
     */
    private double calcularKmExtrasAdicionais(DesvioRota desvio, Telemetria telemetriaAtual) {
        // Buscar última telemetria do veículo
        Optional<Telemetria> ultimaTelemetria = telemetriaRepository
                .findUltimaTelemetriaByVeiculoId(desvio.getVeiculoId());
        
        if (ultimaTelemetria.isEmpty()) {
            return 0.0;
        }
        
        Telemetria anterior = ultimaTelemetria.get();
        
        // Calcular distância percorrida entre as duas telemetrias
        double distanciaKm = distanciaCalculator.calcularDistancia(
            anterior.getLatitude(), anterior.getLongitude(),
            telemetriaAtual.getLatitude(), telemetriaAtual.getLongitude()
        );
        
        return distanciaKm;
    }
    
    /**
     * Cria alerta crítico no sistema quando atinge 2km extras
     */
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
                        "⚠️ ALERTA CRÍTICO: Veículo está há %.2fkm fora da rota '%s'. " +
                        "Tipo de via: %s. Tolerância excedida.",
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
        // Implementar busca de viagem ativa
        return null;
    }

    @Transactional
    private void verificarRetornoRota(Rota rota, Telemetria telemetria) {
        Optional<DesvioRota> desvioAtivo = desvioRotaRepository
                .findByRotaIdAndResolvidoFalse(rota.getId());

        if (desvioAtivo.isPresent()) {
            DesvioRota desvio = desvioAtivo.get();
            desvio.setResolvido(true);
            desvio.setDataHoraRetorno(LocalDateTime.now());

            // Calcular duração total
            if (desvio.getDataHoraDesvio() != null) {
                long duracaoMinutos = ChronoUnit.MINUTES.between(
                    desvio.getDataHoraDesvio(), desvio.getDataHoraRetorno());
                desvio.setDuracaoMin((int) duracaoMinutos);
            }

            log.info("✅ Veículo retornou à rota {}. Desvio {} resolvido. " +
                    "Km extras totais: {:.3f}km, Duração: {} min", 
                    rota.getId(), desvio.getId(), desvio.getKmExtras(), desvio.getDuracaoMin());

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
                nivel,
                rota.getNome(),
                desvio.getVeiculoId(),
                desvio.getDistanciaMetros(),
                tipoVia.getDescricao(),
                tipoVia.getToleranciaMetros(),
                desvio.getLatitudeDesvio(),
                desvio.getLongitudeDesvio(),
                desvio.getKmExtras());

        log.info("\n{}", mensagem);
        
        // TODO: Enviar via WebSocket para o frontend
        // messagingTemplate.convertAndSend("/topic/desvios", mensagem);
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
                desvio.getKmExtras(),
                desvio.getDuracaoMin());

        log.info("\n{}", mensagem);
    }
}