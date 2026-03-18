package com.app.telemetria.domain.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.telemetria.api.dto.response.RouteResponse;
import com.app.telemetria.domain.entity.Alerta;
import com.app.telemetria.domain.entity.Rota;
import com.app.telemetria.domain.entity.Telemetria;
import com.app.telemetria.domain.entity.Viagem;
import com.app.telemetria.domain.enums.SeveridadeAlerta;
import com.app.telemetria.domain.enums.TipoAlerta;
import com.app.telemetria.infrastructure.integration.geocoding.LocationClassifierService;
import com.app.telemetria.infrastructure.integration.routing.RoutingClient;
import com.app.telemetria.infrastructure.persistence.AlertaRepository;
import com.app.telemetria.infrastructure.persistence.TelemetriaRepository;
import com.app.telemetria.infrastructure.persistence.ViagemRepository;

@Service
public class AlertaService {

    private static final Logger log = LoggerFactory.getLogger(AlertaService.class);
    
    private final AlertaRepository alertaRepository;
    private final ViagemRepository viagemRepository;
    private final TelemetriaRepository telemetriaRepository;
    private final LocationClassifierService locationClassifierService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RoutingClient routingClient;

    private static final double VELOCIDADE_MAXIMA = 110.0;
    private static final double VELOCIDADE_MINIMA = 10.0;
    private static final int TEMPO_PARADA_MAXIMO = 30;
    private static final int NIVEL_COMBUSTIVEL_MINIMO = 15;
    private static final int TEMPO_DIRECAO_MAXIMO = 240;

    public AlertaService(
            AlertaRepository alertaRepository,
            ViagemRepository viagemRepository,
            TelemetriaRepository telemetriaRepository,
            LocationClassifierService locationClassifierService,
            SimpMessagingTemplate messagingTemplate,
            RoutingClient routingClient) {
        this.alertaRepository = alertaRepository;
        this.viagemRepository = viagemRepository;
        this.telemetriaRepository = telemetriaRepository;
        this.locationClassifierService = locationClassifierService;
        this.messagingTemplate = messagingTemplate;
        this.routingClient = routingClient;
        
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
            
            // CORREÇÃO: Converter para DTOs em vez de entidades
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
                        
                        // Tenta carregar dados adicionais de forma segura
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
            
            // Retorna um dashboard vazio em caso de erro
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
        if (telemetria.getVelocidade() == null || viagem == null) {
            log.debug("⏭️ Velocidade nula ou viagem nula, ignorando verificação");
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

    @Async("alertaTaskExecutor")
    @Transactional
    public CompletableFuture<String> processarTelemetria(Telemetria telemetria) {
        if (telemetria == null || telemetria.getVeiculoId() == null) {
            log.warn("⚠️ Telemetria inválida recebida para processamento");
            return CompletableFuture.completedFuture("Telemetria inválida");
        }

        long inicio = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        
        log.info("🔄 [Thread: {}] Iniciando processamento assíncrono de alertas para veículo {}", 
                threadName, telemetria.getVeiculoId());

        try {
            log.debug("📊 Dados da telemetria - Vel: {} km/h, Lat: {}, Long: {}, Nível: {}%", 
                     telemetria.getVelocidade(), 
                     telemetria.getLatitude(), 
                     telemetria.getLongitude(),
                     telemetria.getNivelCombustivel());

            // Busca viagem ativa do veículo (status "EM_ANDAMENTO")
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

    @Async("alertaTaskExecutor")
    public CompletableFuture<List<String>> processarMultiplasTelemetrias(List<Telemetria> telemetrias) {
        log.info("🔄 Processando lote de {} telemetrias", telemetrias.size());
        
        return CompletableFuture.supplyAsync(() -> {
            return telemetrias.stream()
                    .map(t -> {
                        try {
                            processarTelemetria(t).join();
                            return "Sucesso: " + t.getId();
                        } catch (Exception e) {
                            log.error("❌ Falha ao processar telemetria {}: {}", t.getId(), e.getMessage());
                            return "Erro: " + t.getId() + " - " + e.getMessage();
                        }
                    })
                    .toList();
        });
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
        
        // Enviar via WebSocket
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
                    
                    // Notificar via WebSocket que o alerta foi resolvido
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
            messagingTemplate.convertAndSend("/topic/alertas", alerta);
            messagingTemplate.convertAndSend("/topic/alertas/" + alerta.getTipo(), alerta);
            log.debug("✅ Alerta {} enviado via WebSocket", alerta.getId());
        } catch (Exception e) {
            log.error("❌ Erro ao enviar alerta via WebSocket: {}", e.getMessage());
        }
    }
}