package com.telemetria.domain.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.api.dto.response.RouteResponse;
import com.telemetria.domain.entity.Alerta;
import com.telemetria.domain.entity.DispositivoIot;
import com.telemetria.domain.entity.Geofence;
import com.telemetria.domain.entity.HistoricoOdometro;
import com.telemetria.domain.entity.HistoricoScoreMotorista;
import com.telemetria.domain.entity.Motorista;
import com.telemetria.domain.entity.Rota;
import com.telemetria.domain.entity.Telemetria;
import com.telemetria.domain.entity.Veiculo;
import com.telemetria.domain.entity.Viagem;
import com.telemetria.domain.enums.SeveridadeAlerta;
import com.telemetria.domain.enums.StatusDispositivo;
import com.telemetria.domain.enums.TipoAlerta;
import com.telemetria.domain.enums.TipoDispositivo;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.domain.exception.VeiculoNotFoundException;
import com.telemetria.infrastructure.integration.geocoding.LocationClassifierService;
import com.telemetria.infrastructure.integration.routing.RoutingClient;
import com.telemetria.infrastructure.persistence.AlertaRepository;
import com.telemetria.infrastructure.persistence.DispositivoIotRepository;
import com.telemetria.infrastructure.persistence.HistoricoOdometroRepository;
import com.telemetria.infrastructure.persistence.HistoricoScoreMotoristaRepository;
import com.telemetria.infrastructure.persistence.MotoristaRepository;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;
import com.telemetria.infrastructure.persistence.VeiculoRepository;
import com.telemetria.infrastructure.persistence.ViagemRepository;

@Service
public class AlertaService {

    private static final Logger log = LoggerFactory.getLogger(AlertaService.class);
    
    private final AlertaRepository alertaRepository;
    private final ViagemRepository viagemRepository;
    private final TelemetriaRepository telemetriaRepository;
    private final LocationClassifierService locationClassifierService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RoutingClient routingClient;
    private final DispositivoIotRepository dispositivoRepository;
    private final VeiculoRepository veiculoRepository;
    private final HistoricoOdometroRepository historicoOdometroRepository;
    private final MotoristaRepository motoristaRepository;
    private final HistoricoScoreMotoristaRepository historicoScoreRepository;
    
    private static final double VELOCIDADE_MAXIMA = 110.0;
    private static final double VELOCIDADE_MINIMA = 10.0;
    private static final int TEMPO_PARADA_MAXIMO = 30;
    private static final int NIVEL_COMBUSTIVEL_MINIMO = 15;
    private static final int TEMPO_DIRECAO_MAXIMO = 240;

    
    @Value("${gps.hdop.tempo_minutos:5}")
    private int hdopTempoMinutos;

    @Value("${gps.satelites.tempo_minutos:10}")
    private int satelitesTempoMinutos;

    
    
    public AlertaService(
            AlertaRepository alertaRepository,
            ViagemRepository viagemRepository,
            TelemetriaRepository telemetriaRepository,
            LocationClassifierService locationClassifierService,
            SimpMessagingTemplate messagingTemplate,
            RoutingClient routingClient,
            DispositivoIotRepository dispositivoRepository,
            VeiculoRepository veiculoRepository,
            HistoricoOdometroRepository historicoOdometroRepository,
            MotoristaRepository motoristaRepository,
            HistoricoScoreMotoristaRepository historicoScoreRepository) {
        this.alertaRepository = alertaRepository;
        this.viagemRepository = viagemRepository;
        this.telemetriaRepository = telemetriaRepository;
        this.locationClassifierService = locationClassifierService;
        this.messagingTemplate = messagingTemplate;
        this.routingClient = routingClient;
        this.dispositivoRepository = dispositivoRepository;
        this.veiculoRepository = veiculoRepository;
        this.historicoOdometroRepository = historicoOdometroRepository;
        this.motoristaRepository = motoristaRepository;
        this.historicoScoreRepository = historicoScoreRepository;
        
        log.info("✅ AlertaService inicializado");
        log.debug("📊 Configurações - VelMax: {} km/h, VelMin: {} km/h, TempoParada: {} min, CombMin: {}%, TempoDireção: {} min",
                VELOCIDADE_MAXIMA, VELOCIDADE_MINIMA, TEMPO_PARADA_MAXIMO, 
                NIVEL_COMBUSTIVEL_MINIMO, TEMPO_DIRECAO_MAXIMO);
    }

    // ================ MÉTODOS PARA O CONTROLLER ================

    @Transactional(readOnly = true)
    public Page<Alerta> listarTodos(Pageable pageable) {
        log.debug("📋 Buscando todos alertas - página: {}, tamanho: {}", 
                 pageable.getPageNumber(), pageable.getPageSize());
        
        Page<Alerta> resultado = alertaRepository.findAll(pageable);
        
        log.debug("✅ Total de alertas: {}", resultado.getTotalElements());
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<Alerta> listarAtivos() {
        log.debug("🔴 Buscando alertas ativos (não resolvidos)");
        
        List<Alerta> resultado = alertaRepository.findByResolvidoFalseOrderByDataHoraDesc();
        
        log.debug("✅ Alertas ativos encontrados: {}", resultado.size());
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<Alerta> listarPorVeiculo(Long veiculoId) {
        log.debug("🚛 Buscando alertas do veículo ID: {}", veiculoId);
        
        List<Alerta> resultado = alertaRepository.findByVeiculoIdOrderByDataHoraDesc(veiculoId);
        
        log.debug("✅ Alertas do veículo {}: {}", veiculoId, resultado.size());
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<Alerta> listarPorMotorista(Long motoristaId) {
        log.debug("👤 Buscando alertas do motorista ID: {}", motoristaId);
        
        List<Alerta> resultado = alertaRepository.findByMotoristaIdOrderByDataHoraDesc(motoristaId);
        
        log.debug("✅ Alertas do motorista {}: {}", motoristaId, resultado.size());
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<Alerta> listarPorViagem(Long viagemId) {
        log.debug("🛣️ Buscando alertas da viagem ID: {}", viagemId);
        
        List<Alerta> resultado = alertaRepository.findByViagemIdOrderByDataHoraDesc(viagemId);
        
        log.debug("✅ Alertas da viagem {}: {}", viagemId, resultado.size());
        return resultado;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        log.debug("📊 Gerando dashboard de alertas");
        
        Map<String, Object> dashboard = new HashMap<>();
        
        try {
            List<Alerta> alertasAtivos = alertaRepository.findByResolvidoFalseOrderByDataHoraDesc();
            
            log.debug("📈 Total de alertas ativos: {}", alertasAtivos.size());
            
            dashboard.put("totalAtivos", alertasAtivos.size());

            long altaGravidade = alertasAtivos.stream()
                    .filter(a -> SeveridadeAlerta.ALTO.equals(a.getSeveridade()))
                    .count();
            long mediaGravidade = alertasAtivos.stream()
                    .filter(a -> SeveridadeAlerta.MEDIO.equals(a.getSeveridade()))
                    .count();
            long baixaGravidade = alertasAtivos.stream()
                    .filter(a -> SeveridadeAlerta.BAIXO.equals(a.getSeveridade()))
                    .count();

            log.debug("📊 Severidade - Alta: {}, Média: {}, Baixa: {}", 
                     altaGravidade, mediaGravidade, baixaGravidade);
            
            dashboard.put("altaGravidade", altaGravidade);
            dashboard.put("mediaGravidade", mediaGravidade);
            dashboard.put("baixaGravidade", baixaGravidade);

            Map<String, Long> alertasPorTipo = new HashMap<>();
            for (TipoAlerta tipo : TipoAlerta.values()) {
                long count = alertasAtivos.stream()
                        .filter(a -> tipo.equals(a.getTipo()))
                        .count();
                if (count > 0) {
                    alertasPorTipo.put(tipo.name(), count);
                    log.debug("📊 Tipo {}: {} alertas", tipo, count);
                }
            }
            dashboard.put("alertasPorTipo", alertasPorTipo);
            
            List<Map<String, Object>> ultimosAlertasSimplificado = alertasAtivos.stream()
                    .limit(10)
                    .map(alerta -> {
                        Map<String, Object> alertaMap = new HashMap<>();
                        alertaMap.put("id", alerta.getId());
                        alertaMap.put("tipo", alerta.getTipo() != null ? alerta.getTipo().name() : null);
                        alertaMap.put("severidade", alerta.getSeveridade() != null ? alerta.getSeveridade().name() : null);
                        alertaMap.put("mensagem", alerta.getMensagem());
                        alertaMap.put("dataHora", alerta.getDataHora());
                        alertaMap.put("lido", alerta.getLido());
                        alertaMap.put("resolvido", alerta.getResolvido());
                        alertaMap.put("veiculoId", alerta.getVeiculoId());
                        
                        try {
                            if (alerta.getVeiculo() != null) {
                                alertaMap.put("veiculoPlaca", alerta.getVeiculo().getPlaca());
                            }
                        } catch (Exception e) {
                            log.trace("Não foi possível carregar placa do veículo para alerta {}", alerta.getId());
                        }
                        
                        try {
                            if (alerta.getMotorista() != null) {
                                alertaMap.put("motoristaNome", alerta.getMotorista().getNome());
                            }
                        } catch (Exception e) {
                            log.trace("Não foi possível carregar nome do motorista para alerta {}", alerta.getId());
                        }
                        
                        return alertaMap;
                    })
                    .collect(Collectors.toList());

            dashboard.put("ultimosAlertas", ultimosAlertasSimplificado);

            log.debug("✅ Dashboard gerado com sucesso");
            
        } catch (Exception e) {
            log.error("❌ Erro ao gerar dashboard: {}", e.getMessage(), e);
            
            dashboard.put("totalAtivos", 0);
            dashboard.put("altaGravidade", 0L);
            dashboard.put("mediaGravidade", 0L);
            dashboard.put("baixaGravidade", 0L);
            dashboard.put("alertasPorTipo", new HashMap<>());
            dashboard.put("ultimosAlertas", List.of());
            dashboard.put("erro", "Erro ao gerar dashboard: " + e.getMessage());
        }
        
        return dashboard;
    }

    @Transactional
    public Alerta marcarComoLido(Long id) {
        log.debug("👁️ Marcando alerta {} como lido", id);
        
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("❌ Alerta {} não encontrado", id);
                    return new RuntimeException("Alerta não encontrado");
                });
        
        alerta.setLido(true);
        alerta.setDataHoraLeitura(LocalDateTime.now());
        Alerta resultado = alertaRepository.save(alerta);
        
        log.debug("✅ Alerta {} marcado como lido em {}", id, alerta.getDataHoraLeitura());
        return resultado;
    }

    @Transactional
    public Alerta resolverAlerta(Long id) {
        log.debug("✅ Resolvendo alerta {}", id);
        
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("❌ Alerta {} não encontrado", id);
                    return new RuntimeException("Alerta não encontrado");
                });
        
        alerta.setResolvido(true);
        alerta.setDataHoraResolucao(LocalDateTime.now());
        Alerta resultado = alertaRepository.save(alerta);
        
        log.debug("✅ Alerta {} resolvido em {}", id, alerta.getDataHoraResolucao());
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<Alerta> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        log.debug("📅 Buscando alertas entre {} e {}", inicio, fim);
        
        List<Alerta> resultado = alertaRepository.findByDataHoraBetweenOrderByDataHoraDesc(inicio, fim);
        
        log.debug("✅ Alertas encontrados no período: {}", resultado.size());
        return resultado;
    }

    // ================ ALERTAS DE VELOCIDADE ================

    @Transactional
    public void verificarExcessoVelocidade(Telemetria telemetria) {
        if (telemetria.getVelocidade() == null) {
            log.debug("⏭️ Velocidade nula, ignorando verificação de excesso");
            return;
        }

        log.debug("🔍 Verificando excesso de velocidade: {:.1f} km/h (limite: {} km/h)", 
                 telemetria.getVelocidade(), VELOCIDADE_MAXIMA);

        if (telemetria.getVelocidade() > VELOCIDADE_MAXIMA) {
            log.debug("⚠️ Excesso detectado! Velocidade: {:.1f} km/h", telemetria.getVelocidade());
            
            Optional<Alerta> alertaRecente = alertaRepository
                    .findPrimeiroByVeiculoIdAndTipoOrderByDataHoraDesc(
                            telemetria.getVeiculoId(), TipoAlerta.EXCESSO_VELOCIDADE);

            if (alertaRecente.isEmpty() ||
                    Duration.between(alertaRecente.get().getDataHora(), LocalDateTime.now()).toMinutes() > 5) {

                log.debug("🚨 Gerando alerta de excesso de velocidade");
                criarAlerta(
                        telemetria.getTenantId(),
                        telemetria.getVeiculoId(),
                        null,
                        null,
                        TipoAlerta.EXCESSO_VELOCIDADE,
                        SeveridadeAlerta.ALTO,
                        String.format("Veículo %.2f km/h acima do limite (%.0f km/h)",
                                telemetria.getVelocidade() - VELOCIDADE_MAXIMA, VELOCIDADE_MAXIMA),
                        telemetria.getLatitude(),
                        telemetria.getLongitude(),
                        telemetria.getVelocidade(),
                        telemetria.getOdometro());
            } else {
                log.debug("⏭️ Alerta recente já existe, ignorando");
            }
        } else {
            log.debug("✅ Velocidade normal: {:.1f} km/h", telemetria.getVelocidade());
        }
    }

    @Transactional
    public void verificarVelocidadeBaixa(Telemetria telemetria, Viagem viagem) {
        if (telemetria == null || viagem == null || telemetria.getVelocidade() == null) {
            log.debug("⏭️ Telemetria nula, viagem nula ou velocidade nula, ignorando verificação");
            return;
        }

        log.debug("🔍 Verificando velocidade baixa: {:.1f} km/h (limite: {} km/h)", 
                 telemetria.getVelocidade(), VELOCIDADE_MINIMA);

        if (telemetria.getVelocidade() < VELOCIDADE_MINIMA && telemetria.getVelocidade() > 0) {
            log.debug("⚠️ Velocidade baixa detectada: {:.1f} km/h", telemetria.getVelocidade());
            
            boolean emAreaUrbana = verificarAreaUrbana(telemetria.getLatitude(), telemetria.getLongitude());

            if (!emAreaUrbana) {
                log.debug("🚨 Gerando alerta de velocidade baixa (não está em área urbana)");
                criarAlerta(
                        telemetria.getTenantId(),
                        telemetria.getVeiculoId(),
                        viagem.getMotoristaId(),
                        viagem.getId(),
                        TipoAlerta.VELOCIDADE_BAIXA,
                        SeveridadeAlerta.MEDIO,
                        String.format("Velocidade muito baixa: %.1f km/h", telemetria.getVelocidade()),
                        telemetria.getLatitude(),
                        telemetria.getLongitude(),
                        telemetria.getVelocidade(),
                        telemetria.getOdometro());
            } else {
                log.debug("⏭️ Veículo em área urbana, ignorando alerta");
            }
        } else {
            log.debug("✅ Velocidade normal");
        }
    }

    // ================ ALERTAS DE PARADA ================

    @Transactional
    public void verificarParadaProlongada(Long veiculoId, LocalDateTime inicioParada) {
        if (inicioParada == null) {
            log.debug("⏭️ Início de parada nulo, ignorando verificação");
            return;
        }

        long minutosParado = Duration.between(inicioParada, LocalDateTime.now()).toMinutes();
        log.debug("🔍 Verificando parada prolongada: {} minutos parado (limite: {} min)", 
                 minutosParado, TEMPO_PARADA_MAXIMO);

        if (minutosParado > TEMPO_PARADA_MAXIMO) {
            log.debug("⚠️ Parada prolongada detectada: {} minutos", minutosParado);
            
            boolean alertaAtivo = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalse(
                    veiculoId, TipoAlerta.PARADA_PROLONGADA);

            if (!alertaAtivo) {
                log.debug("🚨 Gerando alerta de parada prolongada");
                criarAlerta(
                        null,
                        veiculoId,
                        null,
                        null,
                        TipoAlerta.PARADA_PROLONGADA,
                        SeveridadeAlerta.MEDIO,
                        String.format("Veículo parado por %d minutos", minutosParado),
                        null,
                        null,
                        0.0,
                        null);
            } else {
                log.debug("⏭️ Alerta de parada prolongada já existe");
            }
        } else {
            log.debug("✅ Tempo de parada normal");
        }
    }

    // ================ ALERTAS DE VIAGEM ================

    @Transactional
    public void verificarInicioViagem(Viagem viagem) {
        if (viagem == null || viagem.getStatus() == null) {
            log.debug("⏭️ Viagem nula ou status nulo, ignorando");
            return;
        }

        log.debug("🔍 Verificando início de viagem - Status: {}", viagem.getStatus());

        if ("EM_ANDAMENTO".equals(viagem.getStatus())) {
            Rota rota = viagem.getRota();
            if (rota != null) {
                log.debug("🚨 Gerando alerta de início de viagem: {} → {}", 
                         rota.getOrigem(), rota.getDestino());
                
                criarAlerta(
                        null,
                        viagem.getVeiculoId(),
                        viagem.getMotoristaId(),
                        viagem.getId(),
                        TipoAlerta.INICIO_VIAGEM,
                        SeveridadeAlerta.BAIXO,
                        String.format("Viagem iniciada: %s → %s",
                                rota.getOrigem(),
                                rota.getDestino()),
                        rota.getLatitudeOrigem(),
                        rota.getLongitudeOrigem(),
                        0.0,
                        null);
            } else {
                log.debug("⏭️ Rota nula, ignorando alerta de início");
            }
        }
    }

    @Transactional
    public void verificarFimViagem(Viagem viagem) {
        if (viagem == null || viagem.getStatus() == null) {
            log.debug("⏭️ Viagem nula ou status nulo, ignorando");
            return;
        }

        log.debug("🔍 Verificando fim de viagem - Status: {}", viagem.getStatus());

        if ("FINALIZADA".equals(viagem.getStatus())) {
            Rota rota = viagem.getRota();
            if (rota != null) {
                log.debug("🚨 Gerando alerta de fim de viagem: {} → {}", 
                         rota.getOrigem(), rota.getDestino());
                
                criarAlerta(
                        null,
                        viagem.getVeiculoId(),
                        viagem.getMotoristaId(),
                        viagem.getId(),
                        TipoAlerta.FIM_VIAGEM,
                        SeveridadeAlerta.BAIXO,
                        String.format("Viagem finalizada: %s → %s",
                                rota.getOrigem(),
                                rota.getDestino()),
                        rota.getLatitudeDestino(),
                        rota.getLongitudeDestino(),
                        0.0,
                        null);
            } else {
                log.debug("⏭️ Rota nula, ignorando alerta de fim");
            }
        }
    }

    @Transactional
    public void verificarAtrasoViagemInteligente(Viagem viagem, Telemetria ultimaTelemetria) {
        if (viagem == null || ultimaTelemetria == null) {
            log.debug("⏭️ Viagem ou telemetria nula, ignorando verificação de atraso");
            return;
        }

        Rota rota = viagem.getRota();
        if (rota == null) {
            log.debug("⏭️ Rota nula, ignorando verificação de atraso");
            return;
        }

        log.debug("🔍 Verificando atraso de viagem para veículo: {}", viagem.getVeiculoId());

        RouteResponse rotaCalculada = routingClient.calcular(
                ultimaTelemetria.getLatitude(),
                ultimaTelemetria.getLongitude(),
                rota.getLatitudeDestino(),
                rota.getLongitudeDestino());

        if (rotaCalculada == null) {
            log.warn("⚠️ Não foi possível calcular rota para verificação de atraso");
            return;
        }

        double minutosRestantes = rotaCalculada.getDuracaoMinutos();
        LocalDateTime etaReal = LocalDateTime.now().plusMinutes((long) minutosRestantes);

        log.debug("📊 ETA calculado: {} minutos restantes, chegada prevista: {}", 
                 minutosRestantes, etaReal);

        LocalDateTime dataChegadaPrevista = viagem.getDataChegadaPrevista();
        if (dataChegadaPrevista != null && etaReal.isAfter(dataChegadaPrevista)) {
            long atrasoReal = Duration.between(dataChegadaPrevista, etaReal).toMinutes();
            log.debug("⚠️ Atraso detectado: {} minutos", atrasoReal);

            criarAlerta(
                    null,
                    viagem.getVeiculoId(),
                    viagem.getMotoristaId(),
                    viagem.getId(),
                    TipoAlerta.ATRASO_VIAGEM,
                    SeveridadeAlerta.ALTO,
                    "Atraso real estimado: " + atrasoReal + " minutos",
                    ultimaTelemetria.getLatitude(),
                    ultimaTelemetria.getLongitude(),
                    ultimaTelemetria.getVelocidade(),
                    ultimaTelemetria.getOdometro());
        } else {
            log.debug("✅ Viagem no prazo");
        }
    }

    // ================ ALERTAS DE GPS ================

    @Transactional
    public void verificarGpsSemSinal(Long veiculoId, Telemetria ultimaTelemetria) {
        if (ultimaTelemetria == null) {
            log.debug("⏭️ Última telemetria nula, ignorando verificação de GPS");
            return;
        }

        LocalDateTime agora = LocalDateTime.now();
        long minutosSemSinal = Duration.between(ultimaTelemetria.getDataHora(), agora).toMinutes();

        log.debug("🔍 Verificando sinal GPS: {} minutos sem sinal", minutosSemSinal);

        if (minutosSemSinal > 15) {
            log.debug("⚠️ Veículo sem sinal GPS há {} minutos", minutosSemSinal);
            
            boolean alertaAtivo = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalse(
                    veiculoId, TipoAlerta.GPS_SEM_SINAL);

            if (!alertaAtivo) {
                log.debug("🚨 Gerando alerta de GPS sem sinal");
                criarAlerta(
                        null,
                        veiculoId,
                        null,
                        null,
                        TipoAlerta.GPS_SEM_SINAL,
                        SeveridadeAlerta.ALTO,
                        String.format("Veículo sem sinal GPS há %d minutos", minutosSemSinal),
                        ultimaTelemetria.getLatitude(),
                        ultimaTelemetria.getLongitude(),
                        ultimaTelemetria.getVelocidade(),
                        ultimaTelemetria.getOdometro());
            } else {
                log.debug("⏭️ Alerta de GPS sem sinal já existe");
            }
        } else {
            log.debug("✅ Sinal GPS OK");
        }
    }

    // ================ ALERTAS DE MOTORISTA ================

    @Transactional
    public void verificarTempoDirecao(Viagem viagem, Telemetria ultimaTelemetria) {
        if (viagem == null || viagem.getMotoristaId() == null) {
            log.debug("⏭️ Viagem nula ou motorista nulo, ignorando verificação");
            return;
        }

        LocalDateTime dataSaida = viagem.getDataSaida();
        if (dataSaida != null) {
            long minutosDirigindo = Duration.between(dataSaida, LocalDateTime.now()).toMinutes();
            log.debug("🔍 Verificando tempo de direção: {} minutos (limite: {} min)", 
                     minutosDirigindo, TEMPO_DIRECAO_MAXIMO);

            if (minutosDirigindo > TEMPO_DIRECAO_MAXIMO) {
                log.debug("⚠️ Tempo de direção excedido: {} minutos", minutosDirigindo);
                
                boolean alertaAtivo = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalse(
                        viagem.getVeiculoId(), TipoAlerta.TEMPO_DIRECAO);

                if (!alertaAtivo) {
                    log.debug("🚨 Gerando alerta de tempo de direção");
                    criarAlerta(
                            null,
                            viagem.getVeiculoId(),
                            viagem.getMotoristaId(),
                            viagem.getId(),
                            TipoAlerta.TEMPO_DIRECAO,
                            SeveridadeAlerta.ALTO,
                            String.format("Motorista dirigindo por %d minutos sem pausa", minutosDirigindo),
                            ultimaTelemetria != null ? ultimaTelemetria.getLatitude() : null,
                            ultimaTelemetria != null ? ultimaTelemetria.getLongitude() : null,
                            ultimaTelemetria != null ? ultimaTelemetria.getVelocidade() : null,
                            ultimaTelemetria != null ? ultimaTelemetria.getOdometro() : null);
                } else {
                    log.debug("⏭️ Alerta de tempo de direção já existe");
                }
            } else {
                log.debug("✅ Tempo de direção normal");
            }
        }
    }

    // ================ ALERTAS DE COMBUSTÍVEL ================

    @Transactional
    public void verificarNivelCombustivel(Telemetria telemetria, Viagem viagem) {
        if (telemetria.getNivelCombustivel() == null) {
            log.debug("⏭️ Nível de combustível nulo, ignorando verificação");
            return;
        }

        log.debug("🔍 Verificando nível de combustível: {:.1f}% (limite: {}%)", 
                 telemetria.getNivelCombustivel(), NIVEL_COMBUSTIVEL_MINIMO);

        if (telemetria.getNivelCombustivel() < NIVEL_COMBUSTIVEL_MINIMO) {
            log.debug("⚠️ Nível de combustível baixo: {:.1f}%", telemetria.getNivelCombustivel());
            
            criarAlerta(
                    telemetria.getTenantId(),
                    telemetria.getVeiculoId(),
                    viagem != null ? viagem.getMotoristaId() : null,
                    viagem != null ? viagem.getId() : null,
                    TipoAlerta.NIVEL_COMBUSTIVEL_BAIXO,
                    SeveridadeAlerta.MEDIO,
                    String.format("Nível de combustível baixo: %.0f%%", telemetria.getNivelCombustivel()),
                    telemetria.getLatitude(),
                    telemetria.getLongitude(),
                    telemetria.getVelocidade(),
                    telemetria.getOdometro());
        } else {
            log.debug("✅ Nível de combustível normal");
        }
    }

    // ================ MÉTODO PRINCIPAL ================

    @Transactional
    public CompletableFuture<String> processarTelemetria(Telemetria telemetria) {
        if (telemetria == null || telemetria.getVeiculoId() == null) {
            log.warn("⚠️ Telemetria inválida recebida para processamento");
            return CompletableFuture.completedFuture("Telemetria inválida");
        }

        long inicio = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        
        log.info("🔄 [Thread: {}] Iniciando processamento de alertas para veículo {}", 
                threadName, telemetria.getVeiculoId());

        try {
            log.debug("📊 Dados da telemetria - Vel: {} km/h, Lat: {}, Long: {}, Nível: {}%", 
                     telemetria.getVelocidade(), 
                     telemetria.getLatitude(), 
                     telemetria.getLongitude(),
                     telemetria.getNivelCombustivel());

            Viagem viagemAtiva = viagemRepository.findByVeiculoIdAndStatus(
                    telemetria.getVeiculoId(), "EM_ANDAMENTO").orElse(null);

            if (viagemAtiva != null) {
                log.debug("🛣️ Viagem ativa encontrada: ID {}", viagemAtiva.getId());
            } else {
                log.debug("🚫 Nenhuma viagem ativa para o veículo");
            }

            verificarExcessoVelocidade(telemetria);
            verificarVelocidadeBaixa(telemetria, viagemAtiva);
            verificarNivelCombustivel(telemetria, viagemAtiva);
            verificarGpsSemSinal(telemetria.getVeiculoId(), telemetria);

            if (viagemAtiva != null) {
                verificarTempoDirecao(viagemAtiva, telemetria);
                verificarAtrasoViagemInteligente(viagemAtiva, telemetria);
            }

            resolverAlertas(telemetria);

            long fim = System.currentTimeMillis();
            log.info("✅ [Thread: {}] Alertas processados em {}ms para veículo {}", 
                    threadName, (fim - inicio), telemetria.getVeiculoId());

            return CompletableFuture.completedFuture("Alertas processados com sucesso");

        } catch (Exception e) {
            log.error("❌ [Thread: {}] Erro no processamento de alertas para veículo {}: {}", 
                     threadName, telemetria.getVeiculoId(), e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Transactional
    public void processarMultiplasTelemetrias(List<Telemetria> telemetrias) {
        log.info("🔄 Processando lote de {} telemetrias", telemetrias.size());
        
        for (Telemetria t : telemetrias) {
            try {
                processarTelemetria(t);
            } catch (Exception e) {
                log.error("❌ Falha ao processar telemetria {}: {}", t.getId(), e.getMessage());
            }
        }
    }

    // ================ MÉTODOS AUXILIARES ================

    private Long resolverTenantId(Long veiculoId) {
        if (veiculoId == null) return 1L;
        return telemetriaRepository.findUltimaTelemetriaByVeiculoId(veiculoId)
                .map(Telemetria::getTenantId)
                .orElse(1L);
    }

    private void criarAlerta(Long tenantId, Long veiculoId, Long motoristaId, Long viagemId,
            TipoAlerta tipo, SeveridadeAlerta severidade, String mensagem,
            Double latitude, Double longitude, Double velocidadeKmh, Double odometroKm) {

        log.debug("🚨 Criando novo alerta - Tipo: {}, Severidade: {}, Mensagem: {}", 
                 tipo, severidade, mensagem);

        Alerta alerta = new Alerta();
        alerta.setTenantId(tenantId != null ? tenantId : resolverTenantId(veiculoId));
        alerta.setVeiculoId(veiculoId);
        alerta.setMotoristaId(motoristaId);
        alerta.setViagemId(viagemId);
        alerta.setTipo(tipo);
        alerta.setSeveridade(severidade);
        alerta.setMensagem(mensagem);
        alerta.setLatitude(latitude);
        alerta.setLongitude(longitude);
        alerta.setVelocidadeKmh(velocidadeKmh);
        alerta.setOdometroKm(odometroKm);
        alerta.setDataHora(LocalDateTime.now());
        alerta.setLido(false);
        alerta.setResolvido(false);

        alertaRepository.save(alerta);
        
        String threadName = Thread.currentThread().getName();
        log.info("🚨 [Thread: {}] ALERTA GERADO - {}: {}", threadName, tipo, mensagem);
        
        enviarAlertaWebSocket(alerta);
    }

    private void resolverAlertas(Telemetria telemetria) {
        if (telemetria.getVelocidade() != null && telemetria.getVelocidade() <= VELOCIDADE_MAXIMA) {
            log.debug("🔍 Verificando alertas de excesso para resolução - Velocidade atual: {:.1f} km/h", 
                     telemetria.getVelocidade());
            
            List<Alerta> alertasExcesso = alertaRepository
                    .findByVeiculoIdAndTipoAndResolvidoFalseOrderByDataHoraDesc(
                            telemetria.getVeiculoId(), TipoAlerta.EXCESSO_VELOCIDADE);

            if (!alertasExcesso.isEmpty()) {
                log.debug("✅ Resolvendo {} alertas de excesso de velocidade", alertasExcesso.size());
                
                for (Alerta alerta : alertasExcesso) {
                    alerta.setResolvido(true);
                    alerta.setDataHoraResolucao(LocalDateTime.now());
                    alertaRepository.save(alerta);
                    
                    log.debug("✅ Alerta {} resolvido", alerta.getId());
                    
                    messagingTemplate.convertAndSend("/topic/alertas/resolvidos", alerta);
                }
            }
        }
    }

    private boolean verificarAreaUrbana(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            log.debug("⏭️ Latitude ou longitude nula para verificação de área urbana");
            return false;
        }
        
        log.debug("🔍 Verificando se coordenadas ({}, {}) estão em área urbana", latitude, longitude);
        
        try {
            String classificacao = locationClassifierService.classify(latitude, longitude);
            boolean urbana = "AREA_URBANA".equals(classificacao);
            log.debug("📊 Classificação: {} - Urbana: {}", classificacao, urbana);
            return urbana;
        } catch (Exception e) {
            log.error("❌ Erro ao verificar área urbana: {}", e.getMessage(), e);
            return false;
        }
    }

    @Transactional
    public void verificarAreaUrbanaEAvisar(Double latitude, Double longitude, String placaVeiculo) {
        log.debug("🔍 Verificando área urbana para veículo {} em ({}, {})", 
                 placaVeiculo, latitude, longitude);
        
        boolean urbana = verificarAreaUrbana(latitude, longitude);
        if (urbana) {
            String mensagem = "Veículo " + placaVeiculo + " entrou em área urbana";
            log.info("🚗 {} - enviando WebSocket", mensagem);
            
            messagingTemplate.convertAndSend("/topic/alertas", mensagem);
            log.debug("✅ WebSocket enviado: {}", mensagem);
        } else {
            log.debug("⏭️ Veículo {} não está em área urbana", placaVeiculo);
        }
    }
    
    private void enviarAlertaWebSocket(Alerta alerta) {
        try {
            messagingTemplate.convertAndSend("/topic/alertas/" + alerta.getTipo(), alerta);
            log.debug("✅ Alerta {} enviado via WebSocket", alerta.getId());
        } catch (Exception e) {
            log.error("❌ Erro ao enviar alerta via WebSocket: {}", e.getMessage());
        }
    }
    
    // ================ RN-VEI-002 e RN-VEI-003 ================
    
    /**
     * RN-VEI-002: Cria alerta de vencimento de tacógrafo (30d ou 7d)
     */
    public void criarAlertaVencimentoTacografo(Veiculo veiculo, long diasAteVencimento) {
        log.info("📢 Criando alerta de vencimento de tacógrafo - Veículo: {}, Dias: {}", 
                veiculo.getPlaca(), diasAteVencimento);
        
        SeveridadeAlerta severidade = diasAteVencimento == 7 ? SeveridadeAlerta.CRITICO : SeveridadeAlerta.ALTO;
        
        Alerta alerta = Alerta.builder()
                .tenantId(veiculo.getTenantId())
                .veiculoId(veiculo.getId())
                .veiculoUuid(null)
                .tipo(TipoAlerta.TACOGRAFO_VENCIMENTO)
                .severidade(severidade)
                .mensagem(String.format(
                    "Tacógrafo do veículo %s (%s) vence em %d dias",
                    veiculo.getPlaca(), veiculo.getModelo(), diasAteVencimento))
                .dataHora(LocalDateTime.now())
                .lido(false)
                .resolvido(false)
                .build();
        
        alertaRepository.save(alerta);
        log.info("✅ Alerta de tacógrafo salvo com ID: {}", alerta.getId());
    }

    /**
     * RN-VEI-002: Cria alerta de tacógrafo já vencido
     */
    public void criarAlertaTacografoVencido(Veiculo veiculo) {
        log.warn("📢 Criando alerta de tacógrafo vencido - Veículo: {}", veiculo.getPlaca());
        
        Alerta alerta = Alerta.builder()
                .tenantId(veiculo.getTenantId())
                .veiculoId(veiculo.getId())
                .veiculoUuid(null)
                .tipo(TipoAlerta.TACOGRAFO_VENCIDO)
                .severidade(SeveridadeAlerta.CRITICO)
                .mensagem(String.format(
                    "Tacógrafo do veículo %s (%s) está vencido desde %s. " +
                    "Veículo não pode iniciar novas viagens.",
                    veiculo.getPlaca(), veiculo.getModelo(), veiculo.getDataVencimentoTacografo()))
                .dataHora(LocalDateTime.now())
                .lido(false)
                .resolvido(false)
                .build();
        
        alertaRepository.save(alerta);
        log.info("✅ Alerta de tacógrafo vencido salvo com ID: {}", alerta.getId());
    }

    /**
     * RN-VEI-003: Cria alerta de vencimento de documento (30d ou 7d)
     */
    public void criarAlertaVencimentoDocumento(Veiculo veiculo, String documento, long diasAteVencimento) {
        log.info("📢 Criando alerta de vencimento de {} - Veículo: {}, Dias: {}", 
                documento, veiculo.getPlaca(), diasAteVencimento);
        
        SeveridadeAlerta severidade = diasAteVencimento == 7 ? SeveridadeAlerta.ALTO : SeveridadeAlerta.MEDIO;
        TipoAlerta tipo = converterDocumentoParaTipoAlerta(documento, false);
        
        Alerta alerta = Alerta.builder()
                .tenantId(veiculo.getTenantId())
                .veiculoId(veiculo.getId())
                .veiculoUuid(null)
                .tipo(tipo)
                .severidade(severidade)
                .mensagem(String.format(
                    "%s do veículo %s (%s) vence em %d dias",
                    documento, veiculo.getPlaca(), veiculo.getModelo(), diasAteVencimento))
                .dataHora(LocalDateTime.now())
                .lido(false)
                .resolvido(false)
                .build();
        
        alertaRepository.save(alerta);
        log.info("✅ Alerta de {} salvo com ID: {}", documento, alerta.getId());
    }

    /**
     * RN-VEI-003: Cria alerta de documento já vencido
     */
    public void criarAlertaDocumentoVencido(Veiculo veiculo, String documento) {
        log.warn("📢 Criando alerta de {} vencido - Veículo: {}", documento, veiculo.getPlaca());
        
        TipoAlerta tipo = converterDocumentoParaTipoAlerta(documento, true);
        
        Alerta alerta = Alerta.builder()
                .tenantId(veiculo.getTenantId())
                .veiculoId(veiculo.getId())
                .veiculoUuid(null)
                .tipo(tipo)
                .severidade(SeveridadeAlerta.ALTO)
                .mensagem(String.format(
                    "%s do veículo %s (%s) está vencido. " +
                    "Veículo não pode iniciar novas viagens.",
                    documento, veiculo.getPlaca(), veiculo.getModelo()))
                .dataHora(LocalDateTime.now())
                .lido(false)
                .resolvido(false)
                .build();
        
        alertaRepository.save(alerta);
        log.info("✅ Alerta de {} vencido salvo com ID: {}", documento, alerta.getId());
    }

    /**
     * Converte o nome do documento para o enum TipoAlerta correspondente
     */
    private TipoAlerta converterDocumentoParaTipoAlerta(String documento, boolean isVencido) {
        switch (documento.toUpperCase()) {
            case "CRLV":
                return isVencido ? TipoAlerta.CRLV_VENCIDO : TipoAlerta.CRLV_VENCIMENTO;
            case "SEGURO":
                return isVencido ? TipoAlerta.SEGURO_VENCIDO : TipoAlerta.SEGURO_VENCIMENTO;
            case "DPVAT":
                return isVencido ? TipoAlerta.DPVAT_VENCIDO : TipoAlerta.DPVAT_VENCIMENTO;
            case "RCF":
                return isVencido ? TipoAlerta.RCF_VENCIDO : TipoAlerta.RCF_VENCIMENTO;
            case "VISTORIA":
                return isVencido ? TipoAlerta.VISTORIA_VENCIDO : TipoAlerta.VISTORIA_VENCIMENTO;
            case "RNTRC":
                return isVencido ? TipoAlerta.RNTRC_VENCIDO : TipoAlerta.RNTRC_VENCIMENTO;
            default:
                return isVencido ? TipoAlerta.DOCUMENTO_VENCIDO : TipoAlerta.DOCUMENTO_VENCIMENTO;
        }
    }
    
    // ================ RN-VEI-004, 005, 006 ================
    
    /**
     * RN-VEI-004: Vincular dispositivo ao veículo
     */
    @Transactional
    public void vincularDispositivo(Long veiculoId, String deviceId) {
        Veiculo veiculo = veiculoRepository.findById(veiculoId)
            .orElseThrow(() -> new VeiculoNotFoundException("Veículo não encontrado com ID: " + veiculoId));
        
        Optional<DispositivoIot> dispositivoExistente = dispositivoRepository.findByDeviceId(deviceId);
        
        if (dispositivoExistente.isPresent()) {
            DispositivoIot disp = dispositivoExistente.get();
            if (disp.getVeiculoId() != null && !disp.getVeiculoId().equals(veiculoId)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    String.format("Dispositivo %s já está vinculado ao veículo ID: %d. " +
                                  "Desvincule explicitamente antes de transferir.", 
                                  deviceId, disp.getVeiculoId()));
            }
        }
        
        DispositivoIot dispositivo = dispositivoExistente.orElse(new DispositivoIot());
        dispositivo.setDeviceId(deviceId);
        dispositivo.setVeiculoId(veiculoId);
        dispositivo.setTenantId(veiculo.getTenantId());
        dispositivo.setStatus(StatusDispositivo.ATIVO);
        
        if (dispositivo.getTipo() == null) {
            dispositivo.setTipo(TipoDispositivo.PRINCIPAL);
        }
        
        dispositivoRepository.save(dispositivo);
        log.info("✅ Dispositivo {} vinculado ao veículo {}", deviceId, veiculoId);
    }

    /**
     * RN-VEI-005: Adicionar dispositivo backup
     */
    @Transactional
    public void adicionarDispositivoBackup(Long veiculoId, String deviceIdBackup) {
        Veiculo veiculo = veiculoRepository.findById(veiculoId)
            .orElseThrow(() -> new VeiculoNotFoundException("Veículo não encontrado com ID: " + veiculoId));
        
        long countDispositivos = dispositivoRepository.countByVeiculoId(veiculoId);
        
        if (countDispositivos >= 2) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                "Veículo já possui 2 dispositivos (máximo permitido)");
        }
        
        Optional<DispositivoIot> principal = dispositivoRepository
            .findByVeiculoIdAndTipo(veiculoId, TipoDispositivo.PRINCIPAL);
        
        if (principal.isEmpty() && countDispositivos == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                "Adicione um dispositivo principal antes de adicionar backup");
        }
        
        DispositivoIot backup = new DispositivoIot();
        backup.setDeviceId(deviceIdBackup);
        backup.setVeiculoId(veiculoId);
        backup.setTenantId(veiculo.getTenantId());
        backup.setTipo(TipoDispositivo.BACKUP);
        backup.setStatus(StatusDispositivo.ATIVO);
        
        dispositivoRepository.save(backup);
        
        if (principal.isPresent()) {
            DispositivoIot dispPrincipal = principal.get();
            dispPrincipal.setSateliteAtivo(true);
            dispositivoRepository.save(dispPrincipal);
            log.info("📡 Satélite ativado no dispositivo principal do veículo {}", veiculoId);
        }
        
        log.info("✅ Dispositivo backup {} adicionado ao veículo {}", deviceIdBackup, veiculoId);
    }

    /**
     * RN-VEI-006: Trocar dispositivo com calibração de odômetro
     */
    @Transactional
    public void trocarDispositivo(Long veiculoId, String novoDeviceId, Double odometroAtualKm, Long usuarioId) {
        Veiculo veiculo = veiculoRepository.findById(veiculoId)
            .orElseThrow(() -> new VeiculoNotFoundException("Veículo não encontrado com ID: " + veiculoId));
        
        if (odometroAtualKm == null || odometroAtualKm < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                "Odômetro atual é obrigatório ao trocar dispositivo");
        }
        
        Optional<DispositivoIot> dispositivoAntigo = dispositivoRepository
            .findByVeiculoIdAndTipo(veiculoId, TipoDispositivo.PRINCIPAL);
        
        Double ultimoOdometro = null;
        Long dispositivoOrigemId = null;
        
        if (dispositivoAntigo.isPresent()) {
            DispositivoIot antigo = dispositivoAntigo.get();
            ultimoOdometro = obterUltimoOdometroDoDispositivo(veiculoId, antigo.getDeviceId());
            dispositivoOrigemId = antigo.getId();
            
            antigo.setVeiculoId(null);
            antigo.setStatus(StatusDispositivo.INATIVO);
            dispositivoRepository.save(antigo);
        }
        
        Optional<DispositivoIot> dispositivoExistente = dispositivoRepository.findByDeviceId(novoDeviceId);
        DispositivoIot novoDispositivo;
        
        if (dispositivoExistente.isPresent()) {
            novoDispositivo = dispositivoExistente.get();
            novoDispositivo.setVeiculoId(veiculoId);
            novoDispositivo.setTipo(TipoDispositivo.PRINCIPAL);
            novoDispositivo.setStatus(StatusDispositivo.ATIVO);
        } else {
            novoDispositivo = new DispositivoIot();
            novoDispositivo.setDeviceId(novoDeviceId);
            novoDispositivo.setVeiculoId(veiculoId);
            novoDispositivo.setTenantId(veiculo.getTenantId());
            novoDispositivo.setTipo(TipoDispositivo.PRINCIPAL);
            novoDispositivo.setStatus(StatusDispositivo.ATIVO);
        }
        
        DispositivoIot salvo = dispositivoRepository.save(novoDispositivo);
        
        double delta = 0;
        if (ultimoOdometro != null) {
            delta = odometroAtualKm - ultimoOdometro;
            
            HistoricoOdometro historico = HistoricoOdometro.builder()
                .veiculoId(veiculoId)
                .dispositivoOrigemId(dispositivoOrigemId)
                .dispositivoDestinoId(salvo.getId())
                .odometroAnteriorKm(ultimoOdometro)
                .odometroNovoKm(odometroAtualKm)
                .deltaKm(delta)
                .dataTroca(LocalDateTime.now())
                .usuarioId(usuarioId)
                .alertaInconsistencia(Math.abs(delta) > 500)
                .build();
            
            historicoOdometroRepository.save(historico);
            
            if (Math.abs(delta) > 500) {
                criarAlertaInconsistenciaOdometro(veiculo, delta, odometroAtualKm);
                log.warn("⚠️ Inconsistência de odômetro: veículo {}, delta: {} km", veiculoId, delta);
            }
        }
        
        log.info("✅ Dispositivo do veículo {} trocado. Delta odômetro: {} km", veiculoId, delta);
    }

    private Double obterUltimoOdometroDoDispositivo(Long veiculoId, String deviceId) {
        return telemetriaRepository.findTopByVeiculoIdAndDeviceIdOrderByDataHoraDesc(veiculoId, deviceId)
            .map(Telemetria::getOdometro)
            .orElse(null);
    }
    
    /**
     * RN-VEI-006: Cria alerta de inconsistência de odômetro (delta > 500 km)
     */
    public void criarAlertaInconsistenciaOdometro(Veiculo veiculo, double delta, Double odometroAtualKm) {
        log.warn("📢 Criando alerta de inconsistência de odômetro - Veículo: {}, Delta: {} km", 
                veiculo.getPlaca(), delta);
        
        Alerta alerta = Alerta.builder()
                .tenantId(veiculo.getTenantId())
                .veiculoId(veiculo.getId())
                .veiculoUuid(null)
                .tipo(TipoAlerta.ODOMETRO_INCONSISTENCIA)
                .severidade(SeveridadeAlerta.ALTO)
                .mensagem(String.format(
                    "Inconsistência de odômetro detectada no veículo %s (%s). " +
                    "Delta de %.0f km entre último registro e valor informado (%.0f km). " +
                    "Verificar possível adulteração ou erro de calibração.",
                    veiculo.getPlaca(), veiculo.getModelo(), delta, odometroAtualKm))
                .dataHora(LocalDateTime.now())
                .lido(false)
                .resolvido(false)
                .build();
        
        alertaRepository.save(alerta);
        log.info("✅ Alerta de inconsistência de odômetro salvo com ID: {}", alerta.getId());
    }

    // ================ RN-MOT-002: Alertas de CNH ================

    /**
     * RN-MOT-002: Cria alerta de vencimento de CNH (60d, 30d ou 7d)
     */
    public void criarAlertaVencimentoCnh(Motorista motorista, long diasAteVencimento) {
        log.info("📢 Criando alerta de vencimento de CNH - Motorista: {}, Dias: {}", 
                motorista.getNome(), diasAteVencimento);
        
        SeveridadeAlerta severidade;
        if (diasAteVencimento <= 7) {
            severidade = SeveridadeAlerta.CRITICO;
        } else if (diasAteVencimento <= 30) {
            severidade = SeveridadeAlerta.ALTO;
        } else {
            severidade = SeveridadeAlerta.MEDIO;
        }
        
        Alerta alerta = Alerta.builder()
                .tenantId(motorista.getTenantId())
                .motoristaId(motorista.getId())
                .tipo(TipoAlerta.CNH_VENCIMENTO)
                .severidade(severidade)
                .mensagem(String.format(
                    "CNH do motorista %s (CPF: %s) vence em %d dias. " +
                    "Categoria: %s. Providencie a renovação para não interromper as operações.",
                    motorista.getNome(), motorista.getCpf(), diasAteVencimento, motorista.getCategoriaCnh()))
                .dataHora(LocalDateTime.now())
                .lido(false)
                .resolvido(false)
                .build();
        
        alertaRepository.save(alerta);
        log.info("✅ Alerta de vencimento de CNH salvo com ID: {}", alerta.getId());
    }

    /**
     * RN-MOT-002: Cria alerta de CNH já vencida
     */
    public void criarAlertaCnhVencida(Motorista motorista) {
        log.warn("📢 Criando alerta de CNH vencida - Motorista: {}", motorista.getNome());
        
        Alerta alerta = Alerta.builder()
                .tenantId(motorista.getTenantId())
                .motoristaId(motorista.getId())
                .tipo(TipoAlerta.CNH_VENCIDA)
                .severidade(SeveridadeAlerta.CRITICO)
                .mensagem(String.format(
                    "CNH do motorista %s (CPF: %s) está vencida desde %s. " +
                    "Categoria: %s. Motorista bloqueado para novas viagens.",
                    motorista.getNome(), motorista.getCpf(), 
                    motorista.getDataVencimentoCnh(), motorista.getCategoriaCnh()))
                .dataHora(LocalDateTime.now())
                .lido(false)
                .resolvido(false)
                .build();
        
        alertaRepository.save(alerta);
        log.info("✅ Alerta de CNH vencida salvo com ID: {}", alerta.getId());
    }
    
    /**
     * RN-MOT-004: Cria alerta de score baixo (< 600)
     */
    public void criarAlertaScoreBaixo(Motorista motorista) {
        log.warn("📢 Criando alerta de score baixo - Motorista: {}, Score: {}", 
                motorista.getNome(), motorista.getScore());
        
        Alerta alerta = Alerta.builder()
                .tenantId(motorista.getTenantId())
                .motoristaId(motorista.getId())
                .tipo(TipoAlerta.SCORE_BAIXO)
                .severidade(SeveridadeAlerta.ALTO)
                .mensagem(String.format(
                    "Motorista %s (CPF: %s) está com score de comportamento baixo: %d. " +
                    "Score mínimo recomendado é 600. Monitorar comportamento do motorista.",
                    motorista.getNome(), motorista.getCpf(), motorista.getScore()))
                .dataHora(LocalDateTime.now())
                .lido(false)
                .resolvido(false)
                .build();
        
        alertaRepository.save(alerta);
        log.info("✅ Alerta de score baixo salvo com ID: {}", alerta.getId());
    }

    /**
     * RN-MOT-004: Cria alerta de score crítico (< 400)
     */
    public void criarAlertaScoreCritico(Motorista motorista) {
        log.error("📢 Criando alerta de score crítico - Motorista: {}, Score: {}", 
                motorista.getNome(), motorista.getScore());
        
        Alerta alerta = Alerta.builder()
                .tenantId(motorista.getTenantId())
                .motoristaId(motorista.getId())
                .tipo(TipoAlerta.SCORE_CRITICO)
                .severidade(SeveridadeAlerta.CRITICO)
                .mensagem(String.format(
                    "Motorista %s (CPF: %s) está com score de comportamento CRÍTICO: %d. " +
                    "Motorista bloqueado para novas viagens. Necessária intervenção imediata.",
                    motorista.getNome(), motorista.getCpf(), motorista.getScore()))
                .dataHora(LocalDateTime.now())
                .lido(false)
                .resolvido(false)
                .build();
        
        alertaRepository.save(alerta);
        log.info("✅ Alerta de score crítico salvo com ID: {}", alerta.getId());
    }
  
    /**
     * RN-MOT-004: Atualiza o score do motorista baseado em eventos
     */
    @Transactional
    public void atualizarScoreMotorista(Long motoristaId, String eventoTipo, Long viagemId) {
        Motorista motorista = motoristaRepository.findById(motoristaId)
            .orElseThrow(() -> new BusinessException(ErrorCode.MOTORISTA_NOT_FOUND, 
                "Motorista não encontrado com ID: " + motoristaId));
        
        int scoreAnterior = motorista.getScore();
        int novaPontuacao = scoreAnterior;
        String motivo = "";
        
        switch (eventoTipo) {
            case "FRENAGEM_BRUSCA":
                novaPontuacao -= 2;
                motivo = "Frenagem brusca";
                break;
            case "ACELERACAO_BRUSCA":
                novaPontuacao -= 2;
                motivo = "Aceleração brusca";
                break;
            case "EXCESSO_VELOCIDADE":
                novaPontuacao -= 5;
                motivo = "Excesso de velocidade";
                break;
            case "USO_CELULAR":
                novaPontuacao -= 10;
                motivo = "Uso de celular detectado";
                break;
            case "FADIGA":
                novaPontuacao -= 15;
                motivo = "Fadiga detectada";
                break;
            case "COLISAO":
                novaPontuacao -= 50;
                motivo = "Colisão detectada";
                break;
            case "VIAGEM_LIMPA":
                novaPontuacao += 5;
                motivo = "Viagem limpa (sem eventos negativos)";
                break;
            case "SETE_DIAS_SEM_ALERTA":
                novaPontuacao += 10;
                motivo = "7 dias sem alertas";
                break;
            default:
                log.debug("Evento não impacta score: {}", eventoTipo);
                return;
        }
        
        // Garantir que o score fique entre 0 e 1000
        novaPontuacao = Math.max(0, Math.min(1000, novaPontuacao));
        
        if (novaPontuacao != scoreAnterior) {
            motorista.setScore(novaPontuacao);
            motoristaRepository.save(motorista);
            
            // Registrar histórico
            HistoricoScoreMotorista historico = new HistoricoScoreMotorista();
            historico.setMotoristaId(motoristaId);
            historico.setData(LocalDate.now());
            historico.setScoreAnterior(scoreAnterior);
            historico.setScoreNovo(novaPontuacao);
            historico.setDiferenca(novaPontuacao - scoreAnterior);
            historico.setMotivo(motivo);
            historico.setViagemId(viagemId);
            historico.setEventoTipo(eventoTipo);
            historicoScoreRepository.save(historico);
            
            log.info("📊 Score do motorista {} atualizado: {} → {} ({})", 
                     motorista.getNome(), scoreAnterior, novaPontuacao, motivo);
            
            // Criar alertas se necessário
            if (novaPontuacao < 400) {
                criarAlertaScoreCritico(motorista);
            } else if (novaPontuacao < 600) {
                criarAlertaScoreBaixo(motorista);
            }
        }
    }
     
    /**
     * Cria alerta de geofence (entrada/saída)
     */
    public void criarAlertaGeofence(Telemetria telemetria, Geofence geofence, String mensagem) {
        Alerta alerta = new Alerta();
        alerta.setTenantId(telemetria.getTenantId());
        alerta.setVeiculoId(telemetria.getVeiculoId());
        alerta.setTipo(TipoAlerta.GEOFENCE);
        alerta.setSeveridade(SeveridadeAlerta.MEDIO);
        alerta.setMensagem(mensagem);
        alerta.setLatitude(telemetria.getLatitude());
        alerta.setLongitude(telemetria.getLongitude());
        alerta.setVelocidadeKmh(telemetria.getVelocidade());
        alerta.setOdometroKm(telemetria.getOdometro());
        alerta.setDataHora(LocalDateTime.now());
        alerta.setLido(false);
        alerta.setResolvido(false);

        viagemRepository.findByVeiculoIdAndStatus(telemetria.getVeiculoId(), "EM_ANDAMENTO")
            .ifPresent(viagem -> alerta.setViagemId(viagem.getId()));

        alertaRepository.save(alerta);
        log.info("Alerta de geofence criado: {}", mensagem);
        messagingTemplate.convertAndSend("/topic/alertas/geofence", alerta);
    }
    
 // ================ Alertas RN-TEL-002 ================

    public void criarAlertaVelocidadeImpossivel(Telemetria telemetria) {
        Alerta alerta = Alerta.builder()
                .tenantId(telemetria.getTenantId())
                .veiculoId(telemetria.getVeiculoId())
                .veiculoUuid(telemetria.getVeiculoUuid())
                .tipo(TipoAlerta.VELOCIDADE_IMPOSIVEL)
                .severidade(SeveridadeAlerta.CRITICO)
                .mensagem(String.format("Velocidade impossível detectada: %.1f km/h (acima de %d km/h consecutivamente)",
                        telemetria.getVelocidade(), 300))
                .latitude(telemetria.getLatitude())
                .longitude(telemetria.getLongitude())
                .velocidadeKmh(telemetria.getVelocidade())
                .odometroKm(telemetria.getOdometro())
                .dataHora(LocalDateTime.now())
                .lido(false)
                .resolvido(false)
                .build();
        alertaRepository.save(alerta);
        log.warn("🚨 ALERTA: Velocidade impossível - Veículo {}, Velocidade {} km/h", 
                 telemetria.getVeiculoId(), telemetria.getVelocidade());
        enviarAlertaWebSocket(alerta);
    }

    public void criarAlertaSaltoPosicao(Telemetria telemetria, double distanciaKm, long segundos) {
        Alerta alerta = Alerta.builder()
                .tenantId(telemetria.getTenantId())
                .veiculoId(telemetria.getVeiculoId())
                .veiculoUuid(telemetria.getVeiculoUuid())
                .tipo(TipoAlerta.SALTO_POSICAO)
                .severidade(SeveridadeAlerta.CRITICO)
                .mensagem(String.format("Salto de posição impossível: %.1f km em %d segundos",
                        distanciaKm, segundos))
                .latitude(telemetria.getLatitude())
                .longitude(telemetria.getLongitude())
                .velocidadeKmh(telemetria.getVelocidade())
                .odometroKm(telemetria.getOdometro())
                .dataHora(LocalDateTime.now())
                .lido(false)
                .resolvido(false)
                .build();
        alertaRepository.save(alerta);
        log.error("🚨 ALERTA CRÍTICO: Salto de posição - Veículo {}, Distância {} km em {}s",
                  telemetria.getVeiculoId(), distanciaKm, segundos);
        enviarAlertaWebSocket(alerta);
    }
    
 // ================ Alertas RN-TEL-002 ================


    public void criarAlertaHdopAlto(Telemetria telemetria) {
        Alerta alerta = Alerta.builder()
                .tenantId(telemetria.getTenantId())
                .veiculoId(telemetria.getVeiculoId())
                .veiculoUuid(telemetria.getVeiculoUuid())
                .tipo(TipoAlerta.HDOP_ALTO)
                .severidade(SeveridadeAlerta.MEDIO)
                .mensagem(String.format("HDOP elevado por mais de %d minutos: %.1f (precisão comprometida)",
                        hdopTempoMinutos, telemetria.getHdop()))
                .latitude(telemetria.getLatitude())
                .longitude(telemetria.getLongitude())
                .velocidadeKmh(telemetria.getVelocidade())
                .odometroKm(telemetria.getOdometro())
                .dataHora(LocalDateTime.now())
                .lido(false)
                .resolvido(false)
                .build();
        alertaRepository.save(alerta);
        log.info("⚠️ ALERTA: HDOP alto - Veículo {}, HDOP {:.1f}", telemetria.getVeiculoId(), telemetria.getHdop());
        enviarAlertaWebSocket(alerta);
    }

    public void criarAlertaSatelitesBaixos(Telemetria telemetria, long minutos) {
        Alerta alerta = Alerta.builder()
                .tenantId(telemetria.getTenantId())
                .veiculoId(telemetria.getVeiculoId())
                .veiculoUuid(telemetria.getVeiculoUuid())
                .tipo(TipoAlerta.SATELITES_INSUFICIENTES)
                .severidade(SeveridadeAlerta.ALTO)
                .mensagem(String.format("Satélites insuficientes em área aberta por %d minutos: %d satélites",
                        minutos, telemetria.getSatelites()))
                .latitude(telemetria.getLatitude())
                .longitude(telemetria.getLongitude())
                .velocidadeKmh(telemetria.getVelocidade())
                .odometroKm(telemetria.getOdometro())
                .dataHora(LocalDateTime.now())
                .lido(false)
                .resolvido(false)
                .build();
        alertaRepository.save(alerta);
        log.warn("⚠️ ALERTA: Satélites insuficientes - Veículo {}, Satélites {}, Área aberta",
                 telemetria.getVeiculoId(), telemetria.getSatelites());
        enviarAlertaWebSocket(alerta);
    }
}