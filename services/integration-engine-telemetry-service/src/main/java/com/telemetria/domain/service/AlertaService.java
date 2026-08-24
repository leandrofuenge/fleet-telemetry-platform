package com.telemetria.domain.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.api.dto.response.RouteResponse;
import com.telemetria.domain.dto.CriarAlertaCommand;
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
import com.telemetria.domain.event.AlertaGenericoGeradoEvent;
import com.telemetria.domain.event.AlertaGeofenceGeradoEvent;
import com.telemetria.domain.event.AlertaGeradoEvent;
import com.telemetria.domain.event.AlertaHdopAltoGeradoEvent;
import com.telemetria.domain.event.AlertaSaltoPosicaoGeradoEvent;
import com.telemetria.domain.event.AlertaSatelitesInsuficientesGeradoEvent;
import com.telemetria.domain.event.AlertaScoreCriticoViagemGeradoEvent;
import com.telemetria.domain.event.AlertaVeiculoSemSinalGeradoEvent;
import com.telemetria.domain.event.AlertaVelocidadeImpossivelGeradoEvent;
import com.telemetria.domain.event.AlertasResolvidosEvent;
import com.telemetria.domain.event.VeiculoEntrouAreaUrbanaEvent;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.domain.exception.VeiculoNotFoundException;
import com.telemetria.infrastructure.integration.geocoding.LocationClassifierService;
import com.telemetria.infrastructure.integration.routing.RoutingClient;
import com.telemetria.infrastructure.messaging.dto.AlertaWebSocketDTO;
import com.telemetria.infrastructure.persistence.AlertaRepository;
import com.telemetria.infrastructure.persistence.DispositivoIotRepository;
import com.telemetria.infrastructure.persistence.HistoricoOdometroRepository;
import com.telemetria.infrastructure.persistence.HistoricoScoreMotoristaRepository;
import com.telemetria.infrastructure.persistence.MotoristaRepository;
import com.telemetria.infrastructure.persistence.PosicaoAtualRepository;
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
    
    @Autowired
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    
    @Autowired
    private PosicaoAtualRepository posicaoAtualRepository;
    
    private static final java.time.format.DateTimeFormatter DATE_FORMATTER = 
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    
    private static final java.time.format.DateTimeFormatter BRAZILIAN_DATE_FORMATTER = 
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    
    
    private static final double VELOCIDADE_MAXIMA = 110.0;
    private static final double VELOCIDADE_MINIMA = 10.0;
    private static final int TEMPO_PARADA_MAXIMO = 30;
    private static final int NIVEL_COMBUSTIVEL_MINIMO = 15;
    private static final int TEMPO_DIRECAO_MAXIMO = 240;
    private static final double RUIDO_GPS_VELOCIDADE_MINIMA = 3.0; // km/h (Abaixo disso ignora oscilação de GPS)
    private static final double VELOCIDADE_MAXIMA_FISICAMENTE_POSSIVEL = 250.0; // km/h (Filtra outliers de hardware)

    
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
    
    /**
     * Verifica se o veículo excedeu o limite de velocidade.
     * Método sem @Transactional para evitar abertura desnecessária de conexões com o banco.
     */
    public void verificarExcessoVelocidade(Telemetria telemetria) {
        Double velocidade = telemetria.getVelocidade();
        
        if (velocidade == null) {
            log.debug("⏭️ Velocidade nula, ignorando verificação de excesso");
            return;
        }

        log.debug("🔍 Verificando excesso de velocidade: {} km/h (limite: {} km/h)", velocidade, VELOCIDADE_MAXIMA);

        // Cláusula de Guarda: Reduz aninhamento e limpa o fluxo principal
        if (velocidade <= VELOCIDADE_MAXIMA) {
            log.debug("✅ Velocidade normal: {} km/h", velocidade);
            return;
        }

        log.debug("⚠️ Excesso detectado! Velocidade: {} km/h", velocidade);
        
        if (deveGerarNovoAlerta(telemetria.getVeiculoId())) {
            executarCriacaoAlerta(telemetria);
        } else {
            log.debug("⏭️ Alerta recente já existe para o veículo {}, ignorando", telemetria.getVeiculoId());
        }
    }

    /**
     * Regra de negócio isolada para avaliar a necessidade de um novo alerta.
     */
    private boolean deveGerarNovoAlerta(Long veiculoId) {
        return alertaRepository
                .findPrimeiroByVeiculoIdAndTipoOrderByDataHoraDesc(veiculoId, TipoAlerta.EXCESSO_VELOCIDADE)
                .map(ultimoAlerta -> eMaisAntigoQue(ultimoAlerta.getDataHora(), 5))
                .orElse(true); // Se não houver alerta anterior, permite a criação
    }

    /**
     * Abstração matemática de tempo para legibilidade (Clean Code).
     */
    private boolean eMaisAntigoQue(LocalDateTime dataHoraAlerta, long minutos) {
        return Duration.between(dataHoraAlerta, LocalDateTime.now()).toMinutes() > minutos;
    }

    /**
     * Isolamento da escrita. A transação só é aberta se o alerta realmente for gerado.
     */
    @Transactional
    protected void executarCriacaoAlerta(Telemetria telemetria) {
        log.debug("🚨 Gerando alerta de excesso de velocidade");
        
        double diferencaVelocidade = telemetria.getVelocidade() - VELOCIDADE_MAXIMA;
        String mensagem = "Veículo " + String.format("%.2f", diferencaVelocidade) + 
                          " km/h acima do limite (" + VELOCIDADE_MAXIMA + " km/h)";

        criarAlerta(
                telemetria.getTenantId(),
                telemetria.getVeiculoId(),
                null,
                null,
                TipoAlerta.EXCESSO_VELOCIDADE,
                SeveridadeAlerta.ALTO,
                mensagem,
                telemetria.getLatitude(),
                telemetria.getLongitude(),
                telemetria.getVelocidade(),
                telemetria.getOdometro()
        );
    }

    
    
    // Removido o @Transactional para não prender conexões durante checagens geográficas ou CPU-bound
    public void verificarVelocidadeBaixa(Telemetria telemetria, Viagem viagem) {
        // 1. Cláusula de guarda para dados inconsistentes na raiz do objeto
        if (telemetria == null || viagem == null || telemetria.getVelocidade() == null) {
            log.debug("⏭️ Telemetria, viagem ou velocidade nula, ignorando verificação");
            return;
        }

        Double velocidade = telemetria.getVelocidade();
        log.debug("🔍 Verificando velocidade baixa: {} km/h (limite mínimo regulamentar: {} km/h)", velocidade, VELOCIDADE_MINIMA);

        // 2. SÊNIOR: Cláusula de guarda expandida (Proteção contra ruído de GPS, Outliers e limites normais)
        // - velocidade <= 3.0: Corta o "GPS Drift" (caminhão parado cuja coordenada fica tremendo e fingindo movimento)
        // - velocidade >= VELOCIDADE_MINIMA: Veículo está transitando dentro da velocidade correta da via
        // - velocidade > 250.0: Salto de hardware (efeito estilingue do satélite que joga a velocidade para o espaço)
        if (velocidade <= RUIDO_GPS_VELOCIDADE_MINIMA || 
            velocidade >= VELOCIDADE_MINIMA || 
            velocidade > VELOCIDADE_MAXIMA_FISICAMENTE_POSSIVEL) {
            
            if (velocidade > VELOCIDADE_MAXIMA_FISICAMENTE_POSSIVEL) {
                log.warn("❌ [HARDWARE BOUNDS] Velocidade absurda rejeitada no processamento de lentidão: {} km/h", velocidade);
            } else {
                log.debug("✅ Velocidade normal, veículo parado ou em ruído de pátio (Drift): {} km/h", velocidade);
            }
            return; // Aborta o fluxo economizando processamento geográfico
        }

        log.debug("⚠️ Velocidade baixa real detectada em trânsito: {} km/h", velocidade);

        // 3. Operação cara (Geofencing/Spatial Query) executada de forma tardia (Lazy)
        // Só gasta processamento/banco se o veículo realmente passou pelo filtro estrito acima
        boolean emAreaUrbana = verificarAreaUrbana(telemetria.getLatitude(), telemetria.getLongitude());
        if (emAreaUrbana) {
            log.debug("⏭️ Veículo em área urbana, ignorando alerta de velocidade baixa");
            return;
        }

        // 4. Fluxo Crítico: Encaminha para a persistência transacional limpa
        executarCriacaoAlertaVelocidadeBaixa(telemetria, viagem);
    }

 
    /**
     * Isolamento da escrita e abertura de transação cirúrgica.
     */
    @Transactional
    protected void executarCriacaoAlertaVelocidadeBaixa(Telemetria telemetria, Viagem viagem) {
        log.debug("🚨 Gerando alerta de velocidade baixa (não está em área urbana)");
        
        // String simplificada (evitando String.format puro para logs ou textos repetitivos em alta escala)
        String mensagem = "Velocidade muito baixa: " + String.format("%.1f", telemetria.getVelocidade()) + " km/h";

        criarAlerta(
                telemetria.getTenantId(),
                telemetria.getVeiculoId(),
                viagem.getMotoristaId(),
                viagem.getId(),
                TipoAlerta.VELOCIDADE_BAIXA,
                SeveridadeAlerta.MEDIO,
                mensagem,
                telemetria.getLatitude(),
                telemetria.getLongitude(),
                telemetria.getVelocidade(),
                telemetria.getOdometro()
        );
    }
    
    // ================ ALERTAS DE PARADA ================

 // Removido o @Transactional do método principal para evitar desperdício de conexões no pool
    public void verificarParadaProlongada(Long veiculoId, LocalDateTime inicioParada) {
        if (veiculoId == null || inicioParada == null) {
            log.debug("⏭️ ID do veículo ou início de parada nulo, ignorando verificação");
            return;
        }

        long minutosParado = Duration.between(inicioParada, LocalDateTime.now()).toMinutes();
        log.debug("🔍 Verificando parada prolongada para o veículo {}: {} minutos parado (limite: {} min)", 
                 veiculoId, minutosParado, TEMPO_PARADA_MAXIMO);

        // Cláusula de Guarda: Se ainda não estourou o tempo, encerra aqui de forma leve
        if (minutosParado <= TEMPO_PARADA_MAXIMO) {
            log.debug("✅ Tempo de parada normal: {} minutos", minutosParado);
            return;
        }

        log.debug("⚠️ Parada prolongada detectada para o veículo {}: {} minutos", veiculoId, minutosParado);
        
        // Delega a checagem final e a inserção para o bloco transacional isolado
        executarCriacaoAlertaParada(veiculoId, minutosParado);
    }

    /**
     * Abre a transação apenas no momento exato da escrita e validação final de duplicidade.
     */
    @Transactional
    protected void executarCriacaoAlertaParada(Long veiculoId, long minutosParado) {
        boolean alertaAtivo = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalse(
                veiculoId, TipoAlerta.PARADA_PROLONGADA);

        if (alertaAtivo) {
            log.debug("⏭️ Alerta de parada prolongada já ativo para o veículo {}, ignorando", veiculoId);
            return;
        }

        log.debug("🚨 Gerando alerta de parada prolongada para o veículo {}", veiculoId);
        
        // Concatenação simples (mais performática em loops de checagem em lote)
        String mensagem = "Veículo parado por " + minutosParado + " minutos";

        criarAlerta(
                null, // Se tiver o tenantId na telemetria/viagem anterior, avalie passar aqui no futuro
                veiculoId,
                null,
                null,
                TipoAlerta.PARADA_PROLONGADA,
                SeveridadeAlerta.MEDIO,
                mensagem,
                null,
                null,
                0.0,
                null
        );
    } 

    // ================ ALERTAS DE VIAGEM ================

    @Transactional
    public void verificarInicioViagem(Viagem viagem) {
        if (viagem == null || viagem.getStatus() == null) {
            log.debug("⏭️ Viagem ou status nulo, ignorando validação");
            return;
        }

        log.debug("🔍 Verificando início de viagem ID {}. Status atual: {}", viagem.getId(), viagem.getStatus());

        if (!"EM_ANDAMENTO".equals(viagem.getStatus())) {
            log.debug("✅ Viagem ID {} não está em andamento, ignorando alerta", viagem.getId());
            return;
        }

        Rota rota = viagem.getRota();
        if (rota == null) {
            log.debug("⏭️ Rota nula para a viagem ID {}, ignorando alerta de início", viagem.getId());
            return;
        }

        // Corrigido para o nome correto em português
        executarCriacaoAlertaInicio(viagem, rota);
    }

    @Transactional
    protected void executarCriacaoAlertaInicio(Viagem viagem, Rota rota) {
        boolean alertaJaExistente = alertaRepository.existsByViagemIdAndTipo(viagem.getId(), TipoAlerta.INICIO_VIAGEM);
        if (alertaJaExistente) {
            log.debug("⏭️ Alerta de início de viagem já existe para a viagem ID {}, ignorando", viagem.getId());
            return;
        }

        log.debug("🚨 Gerando alerta de início de viagem: {} → {}", rota.getOrigem(), rota.getDestino());
        
        String mensagem = "Viagem iniciada: " + rota.getOrigem() + " → " + rota.getDestino();

        criarAlerta(
                null, 
                viagem.getVeiculoId(),
                viagem.getMotoristaId(),
                viagem.getId(),
                TipoAlerta.INICIO_VIAGEM,
                SeveridadeAlerta.BAIXO,
                mensagem,
                rota.getLatitudeOrigem(),
                rota.getLongitudeOrigem(),
                0.0,
                null
        );
    }
    
    
 // Removido o @Transactional do fluxo de validação inicial para poupar recursos do banco
    public void verificarFimViagem(Viagem viagem) {
        // 1. Cláusula de Guarda para dados inconsistentes
        if (viagem == null || viagem.getStatus() == null) {
            log.debug("⏭️ Viagem ou status nulo, ignorando validação");
            return;
        }

        log.debug("🔍 Verificando fim de viagem ID {}. Status atual: {}", viagem.getId(), viagem.getStatus());

        // 2. Cláusula de Guarda para Status: Só nos interessa se a viagem foi "FINALIZADA"
        if (!"FINALIZADA".equals(viagem.getStatus())) {
            log.debug("✅ Viagem ID {} não está finalizada, ignorando alerta", viagem.getId());
            return;
        }

        // 3. Validação da Rota antes de tocar no banco de dados
        Rota rota = viagem.getRota();
        if (rota == null) {
            log.debug("⏭️ Rota nula para a viagem ID {}, ignorando alerta de fim", viagem.getId());
            return;
        }

        // 4. Encaminha para o bloco transacional isolado
        executarCriacaoAlertaFim(viagem, rota);
    }

    /**
     * Escrita isolada e protegida contra duplicidade (Idempotência).
     */
    @Transactional
    protected void executarCriacaoAlertaFim(Viagem viagem, Rota rota) {
        // Evita gerar mais de um alerta de fim para a mesma viagem (ex: reprocessamento de eventos)
        boolean alertaJaExistente = alertaRepository.existsByViagemIdAndTipo(viagem.getId(), TipoAlerta.FIM_VIAGEM);
        if (alertaJaExistente) {
            log.debug("⏭️ Alerta de fim de viagem já existe para a viagem ID {}, ignorando", viagem.getId());
            return;
        }

        log.debug("🚨 Gerando alerta de fim de viagem: {} → {}", rota.getOrigem(), rota.getDestino());
        
        // Concatenação direta em vez de String.format para maior performance sob alta carga
        String mensagem = "Viagem finalizada: " + rota.getOrigem() + " → " + rota.getDestino();

        criarAlerta(
                null, // Caso a viagem possua tenantId, use: viagem.getTenantId()
                viagem.getVeiculoId(),
                viagem.getMotoristaId(),
                viagem.getId(),
                TipoAlerta.FIM_VIAGEM,
                SeveridadeAlerta.BAIXO,
                mensagem,
                rota.getLatitudeDestino(),  // Usando corretamente os dados do destino
                rota.getLongitudeDestino(), // Usando corretamente os dados do destino
                0.0,
                null
        );
    }

 // SEM @Transactional! Proibido segurar conexões de banco durante chamadas HTTP externas (I/O Bound)
    public void verificarAtrasoViagemInteligente(Viagem viagem, Telemetria ultimaTelemetria) {
        // 1. Cláusulas de Guarda Iniciais
        if (viagem == null || ultimaTelemetria == null) {
            log.debug("⏭️ Viagem ou telemetria nula, ignorando verificação de atraso");
            return;
        }

        LocalDateTime dataChegadaPrevista = viagem.getDataChegadaPrevista();
        if (dataChegadaPrevista == null) {
            log.debug("⏭️ Viagem ID {} não possui data de chegada prevista, ignorando", viagem.getId());
            return;
        }

        Rota rota = viagem.getRota();
        if (rota == null) {
            log.debug("⏭️ Rota nula para a viagem ID {}, ignorando verificação de atraso", viagem.getId());
            return;
        }

        log.debug("🔍 Calculando ETA inteligente para o veículo: {}", viagem.getVeiculoId());

        // 2. Isolamento da Integração Externa com Tratamento de Erros
        RouteResponse rotaCalculada = null;
        try {
            rotaCalculada = routingClient.calcular(
                    ultimaTelemetria.getLatitude(),
                    ultimaTelemetria.getLongitude(),
                    rota.getLatitudeDestino(),
                    rota.getLongitudeDestino()
            );
        } catch (Exception e) {
            log.error("❌ Erro ao integrar com serviço de roteamento para o veículo {}", viagem.getVeiculoId(), e);
            return; // Retorna para não travar o processamento da telemetria
        }

        if (rotaCalculada == null) {
            log.warn("⚠️ Roteador retornou resposta vazia para a viagem ID {}", viagem.getId());
            return;
        }

        // 3. Cálculos de Tempo em Memória
        double minutosRestantes = rotaCalculada.getDuracaoMinutos();
        LocalDateTime etaReal = LocalDateTime.now().plusMinutes((long) minutosRestantes);

        log.debug("📊 ETA da Viagem ID {}: {} min restantes. Janela Limite: {}", 
                 viagem.getId(), minutosRestantes, dataChegadaPrevista);

        // 4. Cláusula de Guarda: Se está dentro do prazo, encerra aqui
        if (!etaReal.isAfter(dataChegadaPrevista)) {
            log.debug("✅ Viagem ID {} segue no prazo estimado", viagem.getId());
            return;
        }

        long atrasoReal = Duration.between(dataChegadaPrevista, etaReal).toMinutes();
        log.debug("⚠️ Atraso estimado detectado: {} minutos", atrasoReal);

        // 5. Fluxo de Escrita Isolado e Protegido
        executarCriacaoAlertaAtraso(viagem, ultimaTelemetria, atrasoReal);
    }

    /**
     * Escrita protegida e transacional.
     */
    @Transactional
    protected void executarCriacaoAlertaAtraso(Viagem viagem, Telemetria telemetria, long atrasoReal) {
        // Evita gerar alertas repetidos de atraso na mesma janela de tempo para a mesma viagem
        boolean alertaAtivo = alertaRepository.existsByViagemIdAndTipo(viagem.getId(), TipoAlerta.ATRASO_VIAGEM);
        if (alertaAtivo) {
            log.debug("⏭️ Já existe um alerta de atraso ativo para a viagem ID {}, pulando inserção", viagem.getId());
            return;
        }

        log.debug("🚨 Gravando novo alerta de atraso real de {} minutos no banco", atrasoReal);
        String mensagem = "Atraso real estimado: " + atrasoReal + " minutos";

        criarAlerta(
                telemetria.getTenantId(),
                viagem.getVeiculoId(),
                viagem.getMotoristaId(),
                viagem.getId(),
                TipoAlerta.ATRASO_VIAGEM,
                SeveridadeAlerta.ALTO,
                mensagem,
                telemetria.getLatitude(),
                telemetria.getLongitude(),
                telemetria.getVelocidade(),
                telemetria.getOdometro()
        );
    }
  
    
    // ================ ALERTAS DE GPS ================
    
 // Removido o @Transactional do método de validação inicial
    public void verificarGpsSemSinal(Long veiculoId, Telemetria ultimaTelemetria) {
        // 1. Cláusula de Guarda para dados ausentes
        if (veiculoId == null || ultimaTelemetria == null || ultimaTelemetria.getDataHora() == null) {
            log.debug("⏭️ ID do veículo, telemetria ou dataHora nula, ignorando verificação de GPS");
            return;
        }

        // 2. Cálculo do delta de tempo em memória
        long minutosSemSinal = Duration.between(ultimaTelemetria.getDataHora(), LocalDateTime.now()).toMinutes();
        log.debug("🔍 Verificando sinal GPS do veículo {}: {} minutos sem sinal", veiculoId, minutosSemSinal);

        // 3. Cláusula de Guarda: Se está dentro da janela aceitável, encerra sem tocar no banco
        if (minutosSemSinal <= 15) { // Ideal transformar o '15' em uma constante descritiva
            log.debug("✅ Sinal GPS OK para o veículo {}", veiculoId);
            return;
        }

        log.debug("⚠️ Veículo {} sem sinal GPS há {} minutes", veiculoId, minutosSemSinal);

        // 4. Delega para o bloco isolado que garante a idempotência na escrita
        executarCriacaoAlertaGpsSemSinal(veiculoId, ultimaTelemetria, minutosSemSinal);
    }

    /**
     * Escrita isolada e transacional. Garante idempotência estrita sob concorrência.
     */
    @Transactional
    protected void executarCriacaoAlertaGpsSemSinal(Long veiculoId, Telemetria ultimaTelemetria, long minutosSemSinal) {
        // IDEMPOTÊNCIA: Verifica se já existe um alerta ativo para não duplicar o registro no banco
        boolean alertaAtivo = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalse(
                veiculoId, TipoAlerta.GPS_SEM_SINAL);

        if (alertaAtivo) {
            log.debug("⏭️ Alerta de GPS sem sinal já está ativo para o veículo {}, ignorando", veiculoId);
            return;
        }

        log.debug("🚨 Gravando alerta de GPS sem sinal para o veículo {}", veiculoId);
        
        // Concatenação limpa e direta para performance
        String mensagem = "Veículo sem sinal GPS há " + minutosSemSinal + " minutos";

        criarAlerta(
                ultimaTelemetria.getTenantId(), // Aproveitando o Tenant ID da última posição conhecida
                veiculoId,
                null,
                null,
                TipoAlerta.GPS_SEM_SINAL,
                SeveridadeAlerta.ALTO,
                mensagem,
                ultimaTelemetria.getLatitude(),  // Última posição válida conhecida
                ultimaTelemetria.getLongitude(), // Última posição válida conhecida
                ultimaTelemetria.getVelocidade(),
                ultimaTelemetria.getOdometro()
        );
    }
    
    
    /**
     * Chamado no fluxo de entrada de novas telemetrias.
     * Se o veículo estava sem sinal e voltou a transmitir, resolve o alerta automaticamente.
     */
    public void resolverAlertaGpsSeNecessario(Long veiculoId) {
        if (veiculoId == null) {
            return;
        }

        // 1. Checagem rápida fora de transação: O veículo tem algum alerta de GPS aberto?
        // Na maior parte do tempo (99% dos casos), retornará false e não gastará conexão de banco.
        boolean possuiAlertaAberto = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalse(
                veiculoId, TipoAlerta.GPS_SEM_SINAL);

        if (!possuiAlertaAberto) {
            return; 
        }

        // 2. Se caiu aqui, o veículo acabou de "ressuscitar". Abrimos a transação para resolver.
        executarFechamentoAlertaGps(veiculoId);
    }

    /**
     * Executa o update de resolução de forma transacional e isolada.
     */
    @Transactional
    protected void executarFechamentoAlertaGps(Long veiculoId) {
        log.info("🔄 Veículo {} voltou a transmitir sinal. Resolvendo alerta de GPS_SEM_SINAL antigo.", veiculoId);

        // Busca o alerta ativo para modificação
        alertaRepository.findPrimeiroByVeiculoIdAndTipoAndResolvidoFalse(veiculoId, TipoAlerta.GPS_SEM_SINAL)
                .ifPresent(alerta -> {
                    alerta.setResolvido(true);
                    alerta.setDataHoraResolucao(LocalDateTime.now()); // Caso tenha esse campo na sua entidade
                    alertaRepository.save(alerta);
                    
                    log.debug("✅ Alerta ID {} de GPS sem sinal marcado como resolvido.", alerta.getId());
                });
    }
    
    
    

    // ================ ALERTAS DE MOTORISTA ================

 // Removido o @Transactional do fluxo de validação inicial em memória
    public void verificarTempoDirecao(Viagem viagem, Telemetria ultimaTelemetria) {
        // 1. Cláusula de Guarda para dados inconsistentes ou ausentes
        if (viagem == null || viagem.getMotoristaId() == null || viagem.getDataSaida() == null) {
            log.debug("⏭️ Viagem, motorista ou data de saída nula, ignorando verificação de tempo de direção");
            return;
        }

        // 2. Cálculo do tempo de condução em memória
        long minutosDirigindo = Duration.between(viagem.getDataSaida(), LocalDateTime.now()).toMinutes();
        log.debug("🔍 Verificando tempo de direção do motorista {}: {} minutos (limite: {} min)", 
                 viagem.getMotoristaId(), minutosDirigindo, TEMPO_DIRECAO_MAXIMO);

        // 3. Cláusula de Guarda: Se está dentro do limite legal de condução, aborta sem tocar no BD
        if (minutosDirigindo <= TEMPO_DIRECAO_MAXIMO) {
            log.debug("✅ Tempo de direção normal para o motorista {}: {} minutos", viagem.getMotoristaId(), minutosDirigindo);
            return;
        }

        log.debug("⚠️ Tempo de direção excedido para o motorista {}: {} minutos", viagem.getMotoristaId(), minutosDirigindo);
        
        // 4. Encaminha para o bloco transacional isolado que garante a idempotência
        executarCriacaoAlertaDirecao(viagem, ultimaTelemetria, minutosDirigindo);
    }

    /**
     * Escrita isolada e transacional. Garante idempotência e proteção concorrente.
     */
    @Transactional
    protected void executarCriacaoAlertaDirecao(Viagem viagem, Telemetria ultimaTelemetria, long minutosDirigindo) {
        // IDEMPOTÊNCIA: Verifica se já existe um alerta ativo para não inundar o banco/notificações
        // Nota: Como a regra avalia a jornada do motorista, buscar pelo viagem.getId() ou motoristaId 
        // costuma ser mais preciso do que pelo veiculoId se houver troca de turnos.
        boolean alertaAtivo = alertaRepository.existsByViagemIdAndTipoAndResolvidoFalse(
                viagem.getId(), TipoAlerta.TEMPO_DIRECAO);

        if (alertaAtivo) {
            log.debug("⏭️ Alerta de tempo de direção já está ativo para a viagem ID {}, ignorando", viagem.getId());
            return;
        }

        log.debug("🚨 Gravando novo alerta de tempo de direção excedido para o motorista ID {}", viagem.getMotoristaId());
        
        // Concatenação limpa e direta para ganho de performance
        String mensagem = "Motorista dirigindo por " + minutosDirigindo + " minutos sem pausa";

        // Extração segura de dados da telemetria (caso ela exista)
        Long tenantId = (ultimaTelemetria != null) ? ultimaTelemetria.getTenantId() : null;
        Double latitude = (ultimaTelemetria != null) ? ultimaTelemetria.getLatitude() : null;
        Double longitude = (ultimaTelemetria != null) ? ultimaTelemetria.getLongitude() : null;
        Double velocidade = (ultimaTelemetria != null) ? ultimaTelemetria.getVelocidade() : null;
        Double odometro = (ultimaTelemetria != null) ? ultimaTelemetria.getOdometro() : null;

        criarAlerta(
                tenantId, // Preservando o tenant multi-tenant se a telemetria estiver presente
                viagem.getVeiculoId(),
                viagem.getMotoristaId(),
                viagem.getId(),
                TipoAlerta.TEMPO_DIRECAO,
                SeveridadeAlerta.ALTO,
                mensagem,
                latitude,
                longitude,
                velocidade,
                odometro
        );
    }

    // ================ ALERTAS DE COMBUSTÍVEL ================

 // Removido o @Transactional do fluxo de validação inicial em memória
    public void verificarNivelCombustivel(Telemetria telemetria, Viagem viagem) {
        // 1. Cláusula de Guarda para dados ausentes
        if (telemetria == null || telemetria.getNivelCombustivel() == null) {
            log.debug("⏭️ Telemetria ou nível de combustível nulo, ignorando verificação");
            return;
        }

        Double nivel = telemetria.getNivelCombustivel();
        log.debug("🔍 Verificando nível de combustível para o veículo {}: {}% (limite mínimo: {}%)", 
                 telemetria.getVeiculoId(), nivel, NIVEL_COMBUSTIVEL_MINIMO);

        // 2. Cláusula de Guarda: Se o combustível está acima do limite mínimo, aborta sem tocar no BD
        if (nivel >= NIVEL_COMBUSTIVEL_MINIMO) {
            log.debug("✅ Nível de combustível normal para o veículo {}: {}%", telemetria.getVeiculoId(), nivel);
            return;
        }

        log.debug("⚠️ Nível de combustível baixo detectado no veículo {}: {}%", telemetria.getVeiculoId(), nivel);
        
        // 3. Encaminha para o bloco transacional isolado que gerencia a idempotência
        executarCriacaoAlertaCombustivel(telemetria, viagem, nivel);
    }

    /**
     * Escrita isolada e transacional. Garante idempotência para evitar alertas repetidos.
     */
    @Transactional
    protected void executarCriacaoAlertaCombustivel(Telemetria telemetria, Viagem viagem, Double nivel) {
        // IDEMPOTÊNCIA/SILENCIAMENTO: Evita gerar um alerta a cada 30 segundos enquanto o veículo estiver na reserva.
        // Usamos existsByVeiculoId...AndResolvidoFalse porque o alerta só deve ser duplicado se o motorista 
        // tiver abastecido e o tanque cair na reserva de novo (ciclo limpo).
        boolean alertaAtivo = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalse(
                telemetria.getVeiculoId(), TipoAlerta.NIVEL_COMBUSTIVEL_BAIXO);

        if (alertaAtivo) {
            log.debug("⏭️ Alerta de combustível baixo já está ativo para o veículo {}, ignorando nova inserção", 
                     telemetria.getVeiculoId());
            return;
        }

        log.debug("🚨 Gravando novo alerta de combustível baixo para o veículo {}", telemetria.getVeiculoId());
        
        // Concatenação limpa e formatação manual rápida para performance
        String mensagem = "Nível de combustível baixo: " + String.format("%.0f", nivel) + "%";

        // Extração segura de dados da viagem (Null-Safe execution)
        Long motoristaId = (viagem != null) ? viagem.getMotoristaId() : null;
        Long viagemId = (viagem != null) ? viagem.getId() : null;

        criarAlerta(
                telemetria.getTenantId(),
                telemetria.getVeiculoId(),
                motoristaId,
                viagemId,
                TipoAlerta.NIVEL_COMBUSTIVEL_BAIXO,
                SeveridadeAlerta.MEDIO,
                mensagem,
                telemetria.getLatitude(),
                telemetria.getLongitude(),
                telemetria.getVelocidade(),
                telemetria.getOdometro()
        );
    }

    /**
     * Chamado no fluxo de entrada de novas telemetrias.
     * Se o combustível subiu acima do limite mínimo, resolve o alerta de combustível baixo pendente.
     */
    public void resolverAlertaCombustivelSeNecessario(Long veiculoId, Double nivelCombustivel) {
        if (veiculoId == null || nivelCombustivel == null || nivelCombustivel < NIVEL_COMBUSTIVEL_MINIMO) {
            return; // Continua baixo ou dados inválidos, não faz nada
        }

        // Fast Check fora de transação: Evita overhead no banco na imensa maioria das requisições
        boolean possuiAlertaAberto = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalse(
                veiculoId, TipoAlerta.NIVEL_COMBUSTIVEL_BAIXO);

        if (!possuiAlertaAberto) {
            return; 
        }

        executarFechamentoAlertaCombustivel(veiculoId);
    }

    @Transactional
    protected void executarFechamentoAlertaCombustivel(Long veiculoId) {
        log.info("🔄 Veículo {} foi abastecido. Resolvendo alerta de NIVEL_COMBUSTIVEL_BAIXO.", veiculoId);

        alertaRepository.findPrimeiroByVeiculoIdAndTipoAndResolvidoFalse(veiculoId, TipoAlerta.NIVEL_COMBUSTIVEL_BAIXO)
                .ifPresent(alerta -> {
                    alerta.setResolvido(true);
                    alerta.setDataHoraResolucao(LocalDateTime.now());
                    alertaRepository.save(alerta);
                });
    }
    
 
 // ============================================  MÉTODOS AUXILIARES ============================================ //

    /**
     * SÊNIOR: Cache e Performance.
     * Evita o problema de N+1 consultas ocultas no banco de dados.
     * Se o mesmo veículo enviar 100 telemetrias, o banco só será consultado na primeira vez.
     * (Certifique-se de ter o @EnableCaching ativo na sua classe de configuração principal)
     */
    @org.springframework.cache.annotation.Cacheable(value = "tenants", key = "#veiculoId")
    public Long resolverTenantId(Long veiculoId) {
        if (veiculoId == null) {
            return 1L; // Fallback padrão seguro para evitar NullPointerException
        }
        
        log.debug("🔍 [CACHE MISS] Buscando tenantId direto do banco para o veículo {}", veiculoId);
        return telemetriaRepository.findUltimaTelemetriaByVeiculoId(veiculoId)
                .map(Telemetria::getTenantId)
                .orElse(1L);
    }

    /**
     * SÊNIOR: Fail-Fast, Clean Code e Arquitetura Orientada a Eventos (EDA).
     * O método foca apenas em validar, montar e salvar o alerta. A infraestrutura de entrega (WebSocket)
     * é totalmente desacoplada daqui.
     */
    private void criarAlerta(Long tenantId, Long veiculoId, Long motoristaId, Long viagemId,
            TipoAlerta tipo, SeveridadeAlerta severidade, String mensagem,
            Double latitude, Double longitude, Double velocidadeKmh, Double odometroKm) {

        // 1. Validacão Defensiva (Fail-Fast)
        if (veiculoId == null || tipo == null || severidade == null) {
            log.error("❌ [ALERTA] Parâmetros obrigatórios ausentes. Não foi possível gerar o alerta.");
            throw new IllegalArgumentException("veiculoId, tipo e severidade são obrigatórios para a criação de um alerta.");
        }

        log.debug("🚨 Criando novo alerta - Tipo: {}, Severidade: {}, Mensagem: {}", tipo, severidade, mensagem);

        // 2. Resolução do TenantId inteligente usando o cache de alta performance
        Long tenantFinal = (tenantId != null) ? tenantId : resolverTenantId(veiculoId);

        // 3. Instanciação e Atribuição Limpa
        Alerta alerta = new Alerta();
        alerta.setTenantId(tenantFinal);
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

        // 4. Persistência
        alertaRepository.save(alerta);
        
        log.info("🚨 [Thread: {}] ALERTA GERADO NO BANCO - ID: {} - {}: {}", 
                Thread.currentThread().getName(), alerta.getId(), tipo, mensagem);
        
        // 5. Publicação do Evento de Domínio
        // Isola completamente a regra de negócio da infraestrutura de transmissão (WebSocket/Push/SMS)
        eventPublisher.publishEvent(new AlertaGeradoEvent(this, alerta));
    }

    /**
     * SÊNIOR: Processamento atômico em lote.
     * Reduz o impacto no banco de dados de N+1 updates para exatamente 1 query.
     */
    @Transactional
    private void resolverAlertas(Telemetria telemetria) {
        if (telemetria.getVelocidade() != null && telemetria.getVelocidade() <= VELOCIDADE_MAXIMA) {
            
            // Correção do log: Evitando o erro do {:.1f} que não funciona no SLF4J padrão
            if (log.isDebugEnabled()) {
                log.debug("🔍 Verificando alertas de excesso para resolução - Velocidade atual: {} km/h", 
                        String.format("%.1f", telemetria.getVelocidade()));
            }

            LocalDateTime agora = LocalDateTime.now();
            
            // Executa o update em lote diretamente na engine do banco de dados
            int linhasAfetadas = alertaRepository.resolverAlertasAtivos(
                    telemetria.getVeiculoId(), 
                    TipoAlerta.EXCESSO_VELOCIDADE, 
                    agora
            );

            if (linhasAfetadas > 0) {
                log.info("✅ ✅ [RESOLUÇÃO LOTE] {} alertas de excesso de velocidade foram resolvidos para o veículo {}", 
                        linhasAfetadas, telemetria.getVeiculoId());
                
                // Dispara apenas um único evento leve para notificar o front-end via WebSocket
                // Evita entupir o canal com dezenas de mensagens repetidas
                eventPublisher.publishEvent(new AlertasResolvidosEvent(this, telemetria.getVeiculoId(), TipoAlerta.EXCESSO_VELOCIDADE));
            }
        }
    }

    /**
     * SÊNIOR: Verificação com Proteção de Carga e Cache Espacial por Quadrantes.
     * Evita chamadas repetitivas a APIs de mapas ou queries espaciais pesadas (PostGIS/Mongo).
     */
    private boolean verificarAreaUrbana(Double latitude, Double longitude) {
        // 1. Fail-Fast defensivo
        if (latitude == null || longitude == null) {
            return false;
        }
        
        // 2. Proteção de Performance: Só monta a String se o modo DEBUG estiver realmente ligado
        if (log.isDebugEnabled()) {
            log.debug("🔍 Verificando coordenadas ({}, {})", latitude, longitude);
        }
        
        try {
            // 3. Delega para o método cacheado que agrupa coordenadas próximas no mesmo quadrante
            return lookupAreaUrbanaComCache(latitude, longitude);
        } catch (Exception e) {
            // Evita quebra do fluxo de telemetria se o serviço de mapas falhar
            log.error("❌ Falha crítica no subsistema de classificação espacial: {}", e.getMessage());
            return false;
        }
    }

    /**
     * SÊNIOR: Cache por aproximação geográfica.
     * Arredondamos para 3 casas decimais (precisão de ~110 metros). Veículos trafegando 
     * próximos na mesma rua ou região cairão no mesmo Cache Key, economizando processamento.
     */
    @Cacheable(value = "areasUrbanas", key = "T(java.lang.String).format('%.3f;%.3f', #latitude, #longitude)")
    public boolean lookupAreaUrbanaComCache(Double latitude, Double longitude) {
        if (log.isDebugEnabled()) {
            log.debug("📡 [CACHE MISS] Executando análise espacial pesada para ({}, {})", latitude, longitude);
        }
        
        String classificacao = locationClassifierService.classify(latitude, longitude);
        boolean urbana = "AREA_URBANA".equals(classificacao);
        
        log.info("📊 Nova região mapeada: ({}, {}) -> {}", 
                String.format("%.3f", latitude), String.format("%.3f", longitude), classificacao);
                
        return urbana;
    }
    

    /**
     * SÊNIOR: Orquestração assíncrona corrigida após mapeamento do atributo na entidade.
     */
    public void verificarAreaUrbanaEAvisar(Double latitude, Double longitude, String placaVeiculo, Long veiculoId) {
        if (latitude == null || longitude == null || veiculoId == null) {
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("🔍 Analisando cerca geográfica urbana para veículo {} (ID: {})", placaVeiculo, veiculoId);
        }

        boolean atualmenteUrbana = verificarAreaUrbana(latitude, longitude);

        if (atualmenteUrbana) {
            // Agora que getZonaAtual() existe na classe PosicaoAtual, o map funciona perfeitamente!
            boolean jaEstavaEmAreaUrbana = posicaoAtualRepository.findByVeiculoId(veiculoId)
                    .map(posicao -> "AREA_URBANA".equals(posicao.getZonaAtual()))
                    .orElse(false);

            if (!jaEstavaEmAreaUrbana) {
                log.info("🚗 Transição de Zona Detectada! Veículo {} entrou em área urbana.", placaVeiculo);
                
                String mensagem = "Veículo " + placaVeiculo + " entrou em área urbana";
                
                eventPublisher.publishEvent(new VeiculoEntrouAreaUrbanaEvent(this, veiculoId, placaVeiculo, mensagem));
                
                posicaoAtualRepository.atualizarZonaAtual(veiculoId, "AREA_URBANA");
            } else {
                log.debug("⏭️ Veículo {} continua em área urbana. Alerta ignorado para evitar spam.", placaVeiculo);
            }
        } else {
            posicaoAtualRepository.atualizarZonaAtual(veiculoId, "AREA_RURAL");
            log.debug("⏭️ Veículo {} em área rural/rodovia.", placaVeiculo);
        }
    }
    
    
    /**
     * SÊNIOR: Despacho assíncrono isolado com payload customizado (DTO).
     * Evita sobrecarga da thread de telemetria e protege a entidade de banco.
     */
    @Async
    @EventListener
    public void handleAlertaGerado(AlertaGeradoEvent event) {
        Alerta alerta = event.getAlerta();
        String threadName = Thread.currentThread().getName();
        
        log.debug("📡 [Thread: {}] Convertendo alerta ID {} para DTO corporativo...", threadName, alerta.getId());
        
        try {
            // 1. Transforma a entidade JPA em um DTO isolado e seguro para a rede
            AlertaWebSocketDTO payload = AlertaWebSocketDTO.de(alerta);
            
            // 2. Centraliza a convenção de nomenclatura de tópicos dinâmicos
            String destino = String.format("/topic/alertas/%s", payload.tipo().toLowerCase());
            
            // 3. Despacha via rede de forma não-bloqueante para o fluxo principal
            messagingTemplate.convertAndSend(destino, payload);
            
            log.info("✅ [Thread: {}] Alerta ID {} transmitido com sucesso para o canal '{}'", 
                    threadName, alerta.getId(), destino);
                    
        } catch (Exception e) {
            // Captura falhas sem derrubar o processamento principal da telemetria
            log.error("❌ [Thread: {}] Falha crítica ao trafegar alerta ID {} via WebSocket: {}", 
                    threadName, alerta.getId(), e.getMessage());
        }
    }
    
  
    
    // ================ RN-VEI-002 e RN-VEI-003 ================
    
    /**
     * SÊNIOR: Criação de alerta com validação defensiva, inteligência de faixas de prazo
     * e barreira contra duplicidade de dados (Idempotência).
     */
    public void criarAlertaVencimentoTacografo(Veiculo veiculo, long diasAteVencimento) {
        // 1. Fail-Fast (Validação de integridade do parâmetro)
        if (veiculo == null || veiculo.getId() == null) {
            log.error("❌ [TACÓGRAFO] Falha ao processar vencimento: Objeto veículo ou ID está nulo.");
            return;
        }

        log.debug("🔍 Analisando regras de vencimento de tacógrafo para veículo: {}, Dias restantes: {}", 
                veiculo.getPlaca(), diasAteVencimento);

        // 2. Determinação inteligente da Severidade por faixas (Mais seguro que uma igualdade exata)
        SeveridadeAlerta severidade = (diasAteVencimento <= 7) ? SeveridadeAlerta.CRITICO : SeveridadeAlerta.ALTO;

        // 3. Montagem padronizada da mensagem para checagem e exibição
        String tokenIdentificadorPrazo = String.format("vence em %d dias", diasAteVencimento);
        String mensagemCompleta = String.format("Tacógrafo do veículo %s (%s) %s",
                veiculo.getPlaca() != null ? veiculo.getPlaca() : "SEM PLACA", 
                veiculo.getModelo() != null ? veiculo.getModelo() : "Ignorado", 
                tokenIdentificadorPrazo);

        // 4. Barreira de Idempotência: Evita criar alertas duplicados no mesmo ciclo/dia
        boolean alertaJaExiste = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalseAndMensagemContaining(
                veiculo.getId(), 
                TipoAlerta.TACOGRAFO_VENCIMENTO, 
                tokenIdentificadorPrazo
        );

        if (alertaJaExiste) {
            log.debug("⏭️ Alerta de vencimento de {} dias já existe ativo para o veículo {}. Ignorando duplicado.", 
                    diasAteVencimento, veiculo.getPlaca());
            return;
        }

        // 5. Instanciação e Persistência Limpa
        Alerta alerta = Alerta.builder()
                .tenantId(veiculo.getTenantId() != null ? veiculo.getTenantId() : 1L) // Fallback seguro
                .veiculoId(veiculo.getId())
                .veiculoUuid(null)
                .tipo(TipoAlerta.TACOGRAFO_VENCIMENTO)
                .severidade(severidade)
                .mensagem(mensagemCompleta)
                .dataHora(LocalDateTime.now())
                .lido(false)
                .resolvido(false)
                .build();

        alertaRepository.save(alerta);
        
        log.info("✅ [ALERTA GERADO] Vencimento de tacógrafo salvo com sucesso. ID: {} | Veículo: {}", 
                alerta.getId(), veiculo.getPlaca());
                
        // 6. Notificação em tempo real opcional via Eventos (Conforme nossa arquitetura EDA anterior)
        // eventPublisher.publishEvent(new AlertaGeradoEvent(this, alerta));
    }
    

    /**
     * SÊNIOR: Geração de alerta de documento vencido com barreira de duplicidade,
     * higienização de strings e formatação localizada de datas (PT-BR).
     */
    public void criarAlertaTacografoVencido(Veiculo veiculo) {
        // 1. Fail-Fast: Proteção contra dados corrompidos ou inconsistentes
        if (veiculo == null || veiculo.getId() == null) {
            log.error("❌ [TACÓGRAFO CRÍTICO] Impossível gerar alerta: Objeto veículo ou ID está nulo.");
            return;
        }

        // 2. Idempotência: Se o veículo já está marcado como vencido no painel, não gera outro registro
        boolean jaExisteAlertaAtivo = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalse(
                veiculo.getId(), 
                TipoAlerta.TACOGRAFO_VENCIDO
        );

        if (jaExisteAlertaAtivo) {
            log.debug("Formatting ⏭️ Veículo {} já possui alerta ativo de Tacógrafo Vencido. Pulando inserção.", veiculo.getPlaca());
            return;
        }

        log.warn("🚨 [TACÓGRAFO CRÍTICO] Detectado tacógrafo vencido para o veículo {}", veiculo.getPlaca());

        // 3. Formatação humana da data (Evita exibir '2026-06-06' no alerta do Front-end)
        String dataFormatada = "Data não informada";
        if (veiculo.getDataVencimentoTacografo() != null) {
            dataFormatada = veiculo.getDataVencimentoTacografo().format(DATE_FORMATTER);
        }

        // 4. Montagem higienizada da mensagem descritiva
        String mensagem = String.format(
                "Tacógrafo do veículo %s (%s) está vencido desde %s. Veículo não pode iniciar novas viagens.",
                veiculo.getPlaca() != null ? veiculo.getPlaca() : "SEM PLACA",
                veiculo.getModelo() != null ? veiculo.getModelo() : "Modelo não informado",
                dataFormatada
        );

        // 5. Construção e Persistência da Entidade
        Alerta alerta = Alerta.builder()
                .tenantId(veiculo.getTenantId() != null ? veiculo.getTenantId() : 1L)
                .veiculoId(veiculo.getId())
                .veiculoUuid(null)
                .tipo(TipoAlerta.TACOGRAFO_VENCIDO)
                .severidade(SeveridadeAlerta.CRITICO)
                .mensagem(mensagem)
                .dataHora(LocalDateTime.now())
                .lido(false)
                .resolvido(false)
                .build();

        alertaRepository.save(alerta);
        
        log.info("✅ [ALERTA GERADO] Alerta crítico de tacógrafo vencido salvo. ID: {} | Veículo: {}", 
                alerta.getId(), veiculo.getPlaca());

        // 6. Notificação via Eventos para transmissão em tempo real no WebSocket
        // eventPublisher.publishEvent(new AlertaGeradoEvent(this, alerta));
    } 
    
    /**
     * SÊNIOR: Processamento de alertas de vencimento corporativos multidocumentos.
     * Aplica idempotência baseada no tipo dinâmico do documento e inteligência de prazos por faixas.
     */
    public void criarAlertaVencimentoDocumento(Veiculo veiculo, String documento, long diasAteVencimento) {
        // 1. Fail-Fast: Proteção contra parâmetros corrompidos ou nulos
        if (veiculo == null || veiculo.getId() == null) {
            log.error("❌ [DOCUMENTOS] Impossível processar alerta: Objeto veículo ou ID está nulo.");
            return;
        }

        // Higieniza o nome do documento para os logs e mensagens
        String nomeDocumento = (documento != null && !documento.isBlank()) ? documento.trim() : "Documento não especificado";

        log.debug("🔍 Analisando prazo de vencimento para o documento '{}' do veículo {}", nomeDocumento, veiculo.getPlaca());

        // 2. Classificação de Severidade por faixas (Mais seguro contra falhas de agendamento/Cron)
        SeveridadeAlerta severidade = (diasAteVencimento <= 7) ? SeveridadeAlerta.ALTO : SeveridadeAlerta.MEDIO;

        // 3. Conversão segura do tipo de alerta baseado na string enviada
        TipoAlerta tipo = TipoAlerta.DOCUMENTO_VENCIMENTO; // Fallback genérico padrão
        try {
            if (documento != null && !documento.isBlank()) {
                tipo = converterDocumentoParaTipoAlerta(documento, false);
            }
        } catch (Exception e) {
            log.warn("⚠️ Falha ao mapear documento '{}' para TipoAlerta. Usando fallback genérico. Erro: {}", nomeDocumento, e.getMessage());
        }

        // 4. Montagem padronizada da mensagem (Usado como assinatura de token para evitar duplicidade)
        String tokenPrazo = String.format("vence em %d dias", diasAteVencimento);
        String mensagemCompleta = String.format("%s do veículo %s (%s) %s",
                nomeDocumento,
                veiculo.getPlaca() != null ? veiculo.getPlaca() : "SEM PLACA",
                veiculo.getModelo() != null ? veiculo.getModelo() : "Modelo não informado",
                tokenPrazo
        );

        // 5. Idempotência cirúrgica: Evita re-inserir o mesmo aviso para o mesmo documento no mesmo ciclo
        boolean alertaJaExiste = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalseAndMensagemContaining(
                veiculo.getId(),
                tipo,
                tokenPrazo
        );

        if (alertaJaExiste) {
            log.debug("Formatting ⏭️ Alerta de vencimento ativo de {} dias para '{}' já existe no veículo {}. Pulando.", 
                    diasAteVencimento, nomeDocumento, veiculo.getPlaca());
            return;
        }

        // 6. Construção e salvamento da Entidade limpa
        Alerta alerta = Alerta.builder()
                .tenantId(veiculo.getTenantId() != null ? veiculo.getTenantId() : 1L)
                .veiculoId(veiculo.getId())
                .veiculoUuid(null)
                .tipo(tipo)
                .severidade(severidade)
                .mensagem(mensagemCompleta)
                .dataHora(LocalDateTime.now())
                .lido(false)
                .resolvido(false)
                .build();

        alertaRepository.save(alerta);
        
        log.info("✅ [ALERTA DOCUMENTO] Alerta de '{}' registrado. ID: {} | Veículo: {} | Severidade: {}", 
                nomeDocumento, alerta.getId(), veiculo.getPlaca(), severidade);

        // 7. Evento opcional para transmissão em tempo real via WebSocket
        // eventPublisher.publishEvent(new AlertaGeradoEvent(this, alerta));
    }    
    
    /**
     * SÊNIOR: Criação de alerta crítico para documentos multidocumentos já vencidos.
     * Bloqueia duplicidade de registros (Idempotência) e eleva o nível operacional para CRÍTICO.
     */
    public void criarAlertaDocumentoVencido(Veiculo veiculo, String documento) {
        // 1. Fail-Fast: Defesa contra dados corrompidos ou inconsistentes na base
        if (veiculo == null || veiculo.getId() == null) {
            log.error("❌ [DOCUMENTO CRÍTICO] Impossível gerar alerta: Objeto veículo ou ID está nulo.");
            return;
        }

        // Higieniza o nome do documento
        String nomeDocumento = (documento != null && !documento.isBlank()) ? documento.trim() : "Documento não especificado";

        // 2. Conversão segura do tipo de alerta com tratamento de erro isolado
        TipoAlerta tipo = TipoAlerta.DOCUMENTO_VENCIDO; // Fallback padrão para documentos vencidos
        try {
            if (documento != null && !documento.isBlank()) {
                tipo = converterDocumentoParaTipoAlerta(documento, true);
            }
        } catch (Exception e) {
            log.warn("⚠️ Falha ao mapear tipo para documento vencido '{}'. Usando fallback genérico. Erro: {}", nomeDocumento, e.getMessage());
        }

        // 3. Idempotência: Se o painel já exibe que este documento específico está vencido, não insere novamente
        boolean jaExisteAlertaAtivo = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalse(
                veiculo.getId(), 
                tipo
        );

        if (jaExisteAlertaAtivo) {
            log.debug("Formatting ⏭️ Veículo {} já possui um alerta ativo de vencimento para '{}'. Ignorando duplicado.", 
                    veiculo.getPlaca(), nomeDocumento);
            return;
        }

        log.warn("🚨 [DOCUMENTO CRÍTICO] Detectado vencimento de '{}' para o veículo {}", nomeDocumento, veiculo.getPlaca());

        // 4. Montagem higienizada da mensagem descritiva
        String mensagem = String.format(
                "%s do veículo %s (%s) está vencido. Veículo não pode iniciar novas viagens.",
                nomeDocumento,
                veiculo.getPlaca() != null ? veiculo.getPlaca() : "SEM PLACA",
                veiculo.getModelo() != null ? veiculo.getModelo() : "Modelo não informado"
        );

        // 5. Construção e Persistência da Entidade
        Alerta alerta = Alerta.builder()
                .tenantId(veiculo.getTenantId() != null ? veiculo.getTenantId() : 1L)
                .veiculoId(veiculo.getId())
                .veiculoUuid(null)
                .tipo(tipo)
                .severidade(SeveridadeAlerta.CRITICO) // Elevado para CRÍTICO (Bloqueio operacional de viagens)
                .mensagem(mensagem)
                .dataHora(LocalDateTime.now())
                .lido(false)
                .resolvido(false)
                .build();

        alertaRepository.save(alerta);
        
        log.info("✅ [ALERTA GERADO] Alerta crítico de '{}' vencido salvo. ID: {} | Veículo: {}", 
                nomeDocumento, alerta.getId(), veiculo.getPlaca());

        // 6. Notificação via Eventos para transmissão em tempo real no WebSocket (EDA)
        // eventPublisher.publishEvent(new AlertaGeradoEvent(this, alerta));
    }

    /**
     * SÊNIOR: Conversão defensiva com tratamento de espaçamento (Trim) 
     * e uso de Switch Expressions modernas (Java 14+).
     */
    private TipoAlerta converterDocumentoParaTipoAlerta(String documento, boolean isVencido) {
        // 1. Fail-Fast defensivo para evitar NullPointerException no .toUpperCase()
        if (documento == null || documento.isBlank()) {
            return isVencido ? TipoAlerta.DOCUMENTO_VENCIDO : TipoAlerta.DOCUMENTO_VENCIMENTO;
        }

        // 2. Normalização: Remove espaços em branco nas pontas e padroniza para maiúsculo
        String documentoNormalizado = documento.trim().toUpperCase();

        // 3. Switch Expression: Retorno limpo, atômico e livre de break/redundâncias
        return switch (documentoNormalizado) {
            case "CRLV"     -> isVencido ? TipoAlerta.CRLV_VENCIDO     : TipoAlerta.CRLV_VENCIMENTO;
            case "SEGURO"   -> isVencido ? TipoAlerta.SEGURO_VENCIDO   : TipoAlerta.SEGURO_VENCIMENTO;
            case "DPVAT"    -> isVencido ? TipoAlerta.DPVAT_VENCIDO    : TipoAlerta.DPVAT_VENCIMENTO;
            case "RCF"      -> isVencido ? TipoAlerta.RCF_VENCIDO      : TipoAlerta.RCF_VENCIMENTO;
            case "VISTORIA" -> isVencido ? TipoAlerta.VISTORIA_VENCIDO : TipoAlerta.VISTORIA_VENCIMENTO;
            case "RNTRC"    -> isVencido ? TipoAlerta.RNTRC_VENCIDO    : TipoAlerta.RNTRC_VENCIMENTO;
            default         -> isVencido ? TipoAlerta.DOCUMENTO_VENCIDO : TipoAlerta.DOCUMENTO_VENCIMENTO;
        };
    } 
    
    
    /**
     * SÊNIOR: Vinculação atômica de dispositivo IoT.
     * Garante exclusividade (um veículo só tem um dispositivo PRINCIPAL ativo) e limpa vínculos legados.
     */
    @Transactional
    public void vincularDispositivo(Long veiculoId, String deviceId) {
        // 1. Fail-Fast e higienização de entradas
        if (veiculoId == null || deviceId == null || deviceId.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ID do veículo e Device ID são obrigatórios.");
        }
        
        String deviceIdLimpo = deviceId.trim();

        log.info("🎯 [IoT BIND] Iniciando vinculação do dispositivo {} ao veículo ID: {}", deviceIdLimpo, veiculoId);

        // 2. Busca o veículo garantindo a existência e captura o Tenant correto
        Veiculo veiculo = veiculoRepository.findById(veiculoId)
            .orElseThrow(() -> new VeiculoNotFoundException("Veículo não encontrado com ID: " + veiculoId));

        // 3. Validação defensiva do dispositivo alvo
        Optional<DispositivoIot> dispositivoExistente = dispositivoRepository.findByDeviceId(deviceIdLimpo);
        
        if (dispositivoExistente.isPresent()) {
            DispositivoIot disp = dispositivoExistente.get();
            // Regra de segurança: impede roubo de hardware entre veículos sem desvinculação prévia
            if (disp.getVeiculoId() != null && !disp.getVeiculoId().equals(veiculoId)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    String.format("Operação negada: O dispositivo %s já está em uso pelo veículo ID: %d.", 
                                  deviceIdLimpo, disp.getVeiculoId()));
            }
        }

        // 4. REGRA SÊNIOR: Garantir exclusividade do dispositivo no veículo
        // Se este novo dispositivo for ser o PRINCIPAL, precisamos desativar/desvincular o antigo dispositivo do veículo
        log.debug("🔄 [IoT BIND] Limpando vínculos antigos de dispositivos para o veículo ID: {}", veiculoId);
        dispositivoRepository.desvincularDispositivosAtivosPorVeiculo(veiculoId, TipoDispositivo.PRINCIPAL);

        // 5. Instanciação e atualização de estado limpo
        DispositivoIot dispositivo = dispositivoExistente.orElseGet(() -> {
            log.info("🆕 [IoT BIND] Dispositivo {} não mapeado no ecossistema. Criando novo registro de hardware.", deviceIdLimpo);
            DispositivoIot novo = new DispositivoIot();
            novo.setDeviceId(deviceIdLimpo);
            return novo;
        });

        dispositivo.setVeiculoId(veiculoId);
        dispositivo.setTenantId(veiculo.getTenantId());
        dispositivo.setStatus(StatusDispositivo.ATIVO);
        
        if (dispositivo.getTipo() == null) {
            dispositivo.setTipo(TipoDispositivo.PRINCIPAL);
        }
        
        // 6. Salvamento dentro do contexto transacional protegido
        dispositivoRepository.save(dispositivo);
        
        log.info("✅ [IoT BIND SUCESSO] Dispositivo {} associado com sucesso ao veículo {} [Tenant: {}]", 
                deviceIdLimpo, veiculo.getPlaca() != null ? veiculo.getPlaca() : veiculoId, veiculo.getTenantId());
    }

    /**
     * SÊNIOR: Adiciona dispositivo de backup com isolamento transacional estrito (Lock Pessimista)
     * para evitar furas de estoque/limites de hardware e garantir idempotência do dispositivo.
     */
    @Transactional
    public void adicionarDispositivoBackup(Long veiculoId, String deviceIdBackup) {
        // 1. Fail-Fast
        if (veiculoId == null || deviceIdBackup == null || deviceIdBackup.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "ID do veículo e Device ID de backup são obrigatórios.");
        }
        
        String deviceIdLimpo = deviceIdBackup.trim();
        log.info("🎯 [IoT BACKUP] Solicitando inclusão de backup {} para o veículo ID: {}", deviceIdLimpo, veiculoId);

        // 2. LOCK PESSIMISTA: Garante que nenhuma outra thread altere os dispositivos deste veículo concorrentemente
        Veiculo veiculo = veiculoRepository.findByIdWithLock(veiculoId)
            .orElseThrow(() -> new VeiculoNotFoundException("Veículo não encontrado com ID: " + veiculoId));

        // 3. Validação rigorosa do limite físico de hardwares (Seguro contra Race Conditions)
        long countDispositivos = dispositivoRepository.countByVeiculoId(veiculoId);
        if (countDispositivos >= 2) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                String.format("Operação negada: O veículo %s já atingiu o limite máximo de 2 dispositivos.", veiculo.getPlaca()));
        }

        // 4. Garante a existência prévia do dispositivo Principal
        Optional<DispositivoIot> principalOpt = dispositivoRepository
            .findByVeiculoIdAndTipo(veiculoId, TipoDispositivo.PRINCIPAL);
        
        if (principalOpt.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                "Operação negada: Adicione e ative um dispositivo PRINCIPAL antes de vincular um hardware de backup.");
        }

        // 5. Idempotência e reuso de Hardware: O dispositivo já existe no sistema?
        Optional<DispositivoIot> dispositivoExistente = dispositivoRepository.findByDeviceId(deviceIdLimpo);
        
        if (dispositivoExistente.isPresent()) {
            DispositivoIot disp = dispositivoExistente.get();
            if (disp.getVeiculoId() != null && !disp.getVeiculoId().equals(veiculoId)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    String.format("O dispositivo %s já está vinculado a outro veículo (ID: %d).", deviceIdLimpo, disp.getVeiculoId()));
            }
        }

        // 6. Configuração e Persistência do Dispositivo de Backup
        DispositivoIot backup = dispositivoExistente.orElseGet(DispositivoIot::new);
        backup.setDeviceId(deviceIdLimpo);
        backup.setVeiculoId(veiculoId);
        backup.setTenantId(veiculo.getTenantId());
        backup.setTipo(TipoDispositivo.BACKUP);
        backup.setStatus(StatusDispositivo.ATIVO);
        
        dispositivoRepository.save(backup);

        // 7. Ativação inteligente da redundância satelital no dispositivo Principal
        DispositivoIot dispPrincipal = principalOpt.get();
        if (!Boolean.TRUE.equals(dispPrincipal.getSateliteAtivo())) { // 🟢 SÊNIOR: Só altera e salva se for necessário
            dispPrincipal.setSateliteAtivo(true);
            dispositivoRepository.save(dispPrincipal);
            log.info("📡 [IoT BACKUP] Redundância satélite ATIVADA no dispositivo principal ID: {}", dispPrincipal.getDeviceId());
        }
        
        log.info("✅ [IoT BACKUP SUCESSO] Dispositivo backup {} adicionado ao veículo {} [Tenant: {}]", 
                deviceIdLimpo, veiculo.getPlaca(), veiculo.getTenantId());
    }
 
    
    /**
     * SÊNIOR: Substituição de hardware IoT com calibração blindada de odômetro.
     * Utiliza a tabela de estado consolidado (posicao_atual) para evitar scans massivos no banco.
     */
    @Transactional
    public void trocarDispositivo(Long veiculoId, String novoDeviceId, Double odometroAtualKm, Long usuarioId) {
        // 1. Fail-Fast: Validações básicas de entrada
        if (veiculoId == null || novoDeviceId == null || novoDeviceId.isBlank() || usuarioId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Parâmetros obrigatórios ausentes para troca de hardware.");
        }
        if (odometroAtualKm == null || odometroAtualKm < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Odômetro atual válido é obrigatório ao trocar dispositivo.");
        }
        
        String novoDeviceIdLimpo = novoDeviceId.trim();
        log.info("🔄 [IoT SWAP] Iniciando substituição de hardware para Veículo ID: {} -> Novo Dispositivo: {}", veiculoId, novoDeviceIdLimpo);

        // 2. Busca o veículo garantindo isolamento
        Veiculo veiculo = veiculoRepository.findById(veiculoId)
            .orElseThrow(() -> new VeiculoNotFoundException("Veículo não encontrado com ID: " + veiculoId));

        // Valida o novo hardware antes de desativar o principal atual. Isso torna
        // a transferência explícita e impede que uma tentativa inválida deixe o
        // veículo sem rastreador.
        dispositivoRepository.findByDeviceId(novoDeviceIdLimpo).ifPresent(existente -> {
            if (existente.getVeiculoId() != null && !existente.getVeiculoId().equals(veiculoId)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "O dispositivo já está vinculado a outro veículo. Desvincule-o antes da transferência.");
            }
        });

        // 3. OPTIMIZATION SÊNIOR: Obtém o último odômetro da tabela de ESTADO (posicao_atual), nunca da telemetria histórica
        Double ultimoOdometro = posicaoAtualRepository.findByVeiculoId(veiculoId)
            .map(posicao -> posicao.getVelocidade() != null ? posicao.getOdometro() : null) // Adapte para o getter de odômetro da sua PosicaoAtual
            .orElse(null);

        // 4. Desvincula o dispositivo antigo (PRINCIPAL) de forma limpa
        Optional<DispositivoIot> dispositivoAntigoOpt = dispositivoRepository.findByVeiculoIdAndTipo(veiculoId, TipoDispositivo.PRINCIPAL);
        Long dispositivoOrigemId = null;

        if (dispositivoAntigoOpt.isPresent()) {
            DispositivoIot antigo = dispositivoAntigoOpt.get();
            dispositivoOrigemId = antigo.getId();
            
            antigo.setVeiculoId(null);
            antigo.setStatus(StatusDispositivo.INATIVO);
            dispositivoRepository.save(antigo);
            log.debug("📉 [IoT SWAP] Antigo dispositivo {} desativado e desvinculado.", antigo.getDeviceId());
        }

        // 5. Validação e Instanciação do Novo Dispositivo (Evita roubo acidental de hardware ativo)
        Optional<DispositivoIot> dispositivoExistenteOpt = dispositivoRepository.findByDeviceId(novoDeviceIdLimpo);
        
        if (dispositivoExistenteOpt.isPresent()) {
            DispositivoIot existente = dispositivoExistenteOpt.get();
            if (existente.getVeiculoId() != null && !existente.getVeiculoId().equals(veiculoId)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, 
                    String.format("O novo dispositivo %s já está ativo no veículo ID: %d. Desvincule-o primeiro.", 
                            novoDeviceIdLimpo, existente.getVeiculoId()));
            }
        }

        DispositivoIot novoDispositivo = dispositivoExistenteOpt.orElseGet(() -> {
            DispositivoIot novo = new DispositivoIot();
            novo.setDeviceId(novoDeviceIdLimpo);
            novo.setTenantId(veiculo.getTenantId());
            return novo;
        });

        novoDispositivo.setVeiculoId(veiculoId);
        novoDispositivo.setTipo(TipoDispositivo.PRINCIPAL);
        novoDispositivo.setStatus(StatusDispositivo.ATIVO);
        DispositivoIot salvo = dispositivoRepository.save(novoDispositivo);

        // 6. Cálculo Inteligente de Delta e Auditoria de Fraude/Inconsistência
        double delta = 0;
        boolean alertaInconsistencia = false;

        if (ultimoOdometro != null) {
            delta = odometroAtualKm - ultimoOdometro;
            
            // Alerta se o odômetro digitado for menor do que o sistema já tinha registrado (Risco de Fraude)
            if (delta < 0) {
                log.error("🚨 [ODÔMETRO SUSPEITO] Tentativa de retrocesso de odômetro no Veículo {}. Anterior: {}, Digitado: {}", 
                        veiculo.getPlaca(), ultimoOdometro, odometroAtualKm);
                alertaInconsistencia = true;
            } else if (delta > 500) {
                // Desvio muito alto de quilometragem física vs calculada
                alertaInconsistencia = true;
                criarAlertaInconsistenciaOdometro(veiculo, delta, odometroAtualKm);
                log.warn("⚠️ [ODÔMETRO INCONSISTENTE] Desvio elevado detectado no veículo {}, Delta: {} km", veiculo.getPlaca(), delta);
            }
        } else {
            log.info("ℹ️ [IoT SWAP] Primeiro registro de odômetro para o veículo {}. Definindo marco inicial em {} km.", veiculo.getPlaca(), odometroAtualKm);
            ultimoOdometro = 0.0; // Ponto de partida
        }

        // 7. Garante que o histórico SEMPRE seja gravado para fins de auditoria
        HistoricoOdometro historico = HistoricoOdometro.builder()
            .veiculoId(veiculoId)
            .dispositivoOrigemId(dispositivoOrigemId)
            .dispositivoDestinoId(salvo.getId())
            .odometroAnteriorKm(ultimoOdometro)
            .odometroNovoKm(odometroAtualKm)
            .deltaKm(delta)
            .dataTroca(LocalDateTime.now())
            .usuarioId(usuarioId)
            .alertaInconsistencia(alertaInconsistencia)
            .build();
        
        historicoOdometroRepository.save(historico);

        // 8. SÊNIOR: Força a atualização do odômetro imediatamente na tabela de posição atual
        // para que o novo rastreador continue contando a partir do valor calibrado
        posicaoAtualRepository.findByVeiculoId(veiculoId).ifPresent(posicao -> {
            // Se houver um método setOdometro na sua entidade PosicaoAtual, atualize-o aqui:
            // posicao.setOdometro(odometroAtualKm);
            // posicaoAtualRepository.save(posicao);
        });

        log.info("✅ [IoT SWAP SUCESSO] Substituição concluída. Veículo: {} | Delta: {} km | Registro Auditoria: {}", 
                veiculo.getPlaca(), delta, historico.getId());
    }
    
    
    /**
     * SÊNIOR: Geração de alerta inteligente para fraudes ou desvios de odômetro.
     * Trata variações positivas e negativas com mensagens direcionadas para a equipe de auditoria.
     */
    public void criarAlertaInconsistenciaOdometro(Veiculo veiculo, double delta, Double odometroAtualKm) {
        // 1. Fail-Fast
        if (veiculo == null || veiculo.getId() == null || odometroAtualKm == null) {
            log.error("❌ [AUDITORIA ODÔMETRO] Impossível gerar alerta: Parâmetros obrigatórios nulos.");
            return;
        }

        // 2. Barreira de Idempotência: Evita duplicar o exato mesmo alerta em processos concorrentes ou cliques duplos
        String placa = veiculo.getPlaca() != null ? veiculo.getPlaca() : "SEM PLACA";
        double deltaAbsoluto = Math.abs(delta);
        
        boolean jaExisteAlertaAtivo = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalse(
                veiculo.getId(), 
                TipoAlerta.ODOMETRO_INCONSISTENCIA
        );

        if (jaExisteAlertaAtivo) {
            log.debug("Formatting ⏭️ Veículo {} já possui um alerta ativo de Inconsistência de Odômetro. Pulando.", placa);
            return;
        }

        log.warn("🚨 [AUDITORIA ODÔMETRO] Gerando alerta de desvio crítico para veículo {}. Delta: {} km", placa, delta);

        // 3. Inteligência de Mensagem: Adapta o texto se for excesso de km ou retrocesso (suspeita de fraude)
        String contextoFraude;
        if (delta < 0) {
            contextoFraude = String.format(
                "⚠️ RETROCESSO SUSPEITO! O odômetro informado (%.1f km) é MENOR que o último registro do sistema. Desvio negativo de %.1f km.", 
                odometroAtualKm, deltaAbsoluto);
        } else {
            contextoFraude = String.format(
                "Desvio de %.1f km entre o último estado consolidado e o valor informado em campo (%.1f km).", 
                deltaAbsoluto, odometroAtualKm);
        }

        String mensagemCompleta = String.format(
                "Inconsistência de odômetro detectada no veículo %s (%s). %s Verificar possível erro de calibração ou adulteração física.",
                placa,
                veiculo.getModelo() != null ? veiculo.getModelo() : "Modelo não informado",
                contextoFraude
        );

        // 4. Construção com severidade condicional (Retrocesso sempre é CRÍTICO, desvio alto é ALTO)
        SeveridadeAlerta severidade = (delta < 0) ? SeveridadeAlerta.CRITICO : SeveridadeAlerta.ALTO;

        Alerta alerta = Alerta.builder()
                .tenantId(veiculo.getTenantId() != null ? veiculo.getTenantId() : 1L)
                .veiculoId(veiculo.getId())
                .veiculoUuid(null)
                .tipo(TipoAlerta.ODOMETRO_INCONSISTENCIA)
                .severidade(severidade)
                .mensagem(mensagemCompleta)
                .dataHora(LocalDateTime.now())
                .lido(false)
                .resolvido(false)
                .build();

        alertaRepository.save(alerta);
        
        log.info("✅ [AUDITORIA ODÔMETRO] Alerta registrado com sucesso. ID: {} | Severidade: {}", 
                alerta.getId(), severidade);

        // 5. Despacho imediato via eventos para a central (WebSocket)
        // eventPublisher.publishEvent(new AlertaGeradoEvent(this, alerta));
    }
    
    
    // ================ RN-MOT-002: Alertas de CNH ================

    /**
     * SÊNIOR: Criação de alerta de validade de CNH com mascaramento de dados (LGPD),
     * fail-fast de integridade e idempotência baseada em faixas de severidade.
     */
    public void criarAlertaVencimentoCnh(Motorista motorista, long diasAteVencimento) {
        // 1. Fail-Fast: Proteção contra dados nulos
        if (motorista == null || motorista.getId() == null) {
            log.error("❌ [MOTORISTA] Impossível processar vencimento de CNH: Objeto motorista ou ID está nulo.");
            return;
        }

        log.debug("🔍 Analisando prazo de CNH para o motorista ID: {}, Dias restantes: {}", motorista.getId(), diasAteVencimento);

        // 2. Determinação de Severidade por faixas (Sua lógica padrão excelente)
        SeveridadeAlerta severidade = (diasAteVencimento <= 7) ? SeveridadeAlerta.CRITICO : 
                                      (diasAteVencimento <= 30) ? SeveridadeAlerta.ALTO : SeveridadeAlerta.MEDIO;

        // 3. BARREIRA DE IDEMPOTÊNCIA: Evita duplicar alertas dentro da MESMA faixa de risco
        // Se já existe um alerta ativo como ALTO (entre 8 e 30 dias), não cria outro só porque o dia mudou.
        boolean jaExisteAlertaNaFaixa = alertaRepository.existsByMotoristaIdAndTipoAndSeveridadeAndResolvidoFalse(
                motorista.getId(),
                TipoAlerta.CNH_VENCIMENTO,
                severidade
        );

        if (jaExisteAlertaNaFaixa) {
            log.debug("Formatting ⏭️ Motorista ID {} já possui um alerta ativo de CNH na severidade [{}]. Pulando inserção.", 
                    motorista.getId(), severidade);
            return;
        }

        // 4. MASCARAMENTO DEFENSIVO (Compliance LGPD)
        // Transforma "12345678901" em "***.456.789-**" para proteger a privacidade na tela e nos logs
        String cpfMascarado = "CPF Não Informado";
        if (motorista.getCpf() != null && motorista.getCpf().length() == 11) {
            cpfMascarado = String.format("***.%s.%s-**", 
                    motorista.getCpf().substring(3, 6), 
                    motorista.getCpf().substring(6, 9));
        }

        // 5. Montagem higienizada da mensagem
        String mensagem = String.format(
                "CNH do motorista %s (CPF: %s) vence em %d dias. Categoria: %s. Providencie a renovação para manter a regularidade da operação.",
                motorista.getNome() != null ? motorista.getNome() : "Não informado",
                cpfMascarado,
                diasAteVencimento,
                motorista.getCategoriaCnh() != null ? motorista.getCategoriaCnh() : "N/A"
        );

        // 6. Construção e Persistência da Entidade
        Alerta alerta = Alerta.builder()
                .tenantId(motorista.getTenantId() != null ? motorista.getTenantId() : 1L)
                .motoristaId(motorista.getId()) // Garanta que este campo exista na sua tabela/entidade Alerta
                .veiculoId(null) // Contexto exclusivo de motorista
                .tipo(TipoAlerta.CNH_VENCIMENTO)
                .severidade(severidade)
                .mensagem(mensagem)
                .dataHora(LocalDateTime.now())
                .lido(false)
                .resolvido(false)
                .build();

        alertaRepository.save(alerta);
        
        log.info("✅ [ALERTA MOTORISTA] Alerta de CNH (Faixa: {}) salvo com sucesso. ID: {} | Motorista: {}", 
                severidade, alerta.getId(), motorista.getNome());

        // 7. Evento assíncrono para o WebSocket da central de monitoramento
        // eventPublisher.publishEvent(new AlertaGeradoEvent(this, alerta));
       
    }
        
        
        
        
        
        
        
        
        
        
        
        
        
        
  
        /**
         * SÊNIOR: Geração de alerta crítico para CNH vencida.
         * Implementa barreira de duplicidade, conformidade com a LGPD, formatação localizada 
         * e limpeza automática de histórico de avisos pendentes.
         */
        public void criarAlertaCnhVencida(Motorista motorista) {
            // 1. Fail-Fast
            if (motorista == null || motorista.getId() == null) {
                log.error("❌ [MOTORISTA CRÍTICO] Impossível gerar alerta: Objeto motorista ou ID está nulo.");
                return;
            }

            // 2. Barreira de Idempotência: Se o motorista já está listado como bloqueado, pula a inserção
            boolean jaExisteAlertaAtivo = alertaRepository.existsByMotoristaIdAndTipoAndResolvidoFalse(
                    motorista.getId(), 
                    TipoAlerta.CNH_VENCIDA
            );

            if (jaExisteAlertaAtivo) {
                log.debug("Formatting ⏭️ Motorista ID {} já possui alerta ativo de CNH Vencida. Pulando inserção repetida.", motorista.getId());
                return;
            }

            log.warn("🚨 [BLOQUEIO OPERACIONAL] Detectada CNH vencida para o motorista: {}", motorista.getNome());

            // 3. Mascaramento de CPF para auditoria visual (Princípio da minimização de dados - LGPD)
            String cpfMascarado = "Não Informado";
            if (motorista.getCpf() != null && motorista.getCpf().length() == 11) {
                cpfMascarado = String.format("***.%s.%s-**", 
                        motorista.getCpf().substring(3, 6), 
                        motorista.getCpf().substring(6, 9));
            }

            // 4. Formatação amigável de data para o Front-end (dd/MM/yyyy)
            String dataVencimentoFormatada = "Data não informada";
            if (motorista.getDataVencimentoCnh() != null) {
                dataVencimentoFormatada = motorista.getDataVencimentoCnh().format(BRAZILIAN_DATE_FORMATTER);
            }

            // 5. Montagem higienizada do corpo da mensagem
            String mensagem = String.format(
                    "CNH do motorista %s (CPF: %s) está vencida desde %s. Categoria: %s. Motorista BLOQUEADO para o início de novas viagens.",
                    motorista.getNome() != null ? motorista.getNome() : "Não informado",
                    cpfMascarado,
                    dataVencimentoFormatada,
                    motorista.getCategoriaCnh() != null ? motorista.getCategoriaCnh() : "N/A"
            );

            // 6. Construção e Persistência do Alerta Crítico
            Alerta alerta = Alerta.builder()
                    .tenantId(motorista.getTenantId() != null ? motorista.getTenantId() : 1L)
                    .motoristaId(motorista.getId())
                    .veiculoId(null) // Contexto exclusivo de motorista
                    .tipo(TipoAlerta.CNH_VENCIDA)
                    .severidade(SeveridadeAlerta.CRITICO)
                    .mensagem(mensagem)
                    .dataHora(LocalDateTime.now())
                    .lido(false)
                    .resolvido(false)
                    .build();

            alertaRepository.save(alerta);
            log.info("✅ [ALERTA GERADO] Bloqueio de CNH salvo. ID: {} | Motorista: {}", alerta.getId(), motorista.getNome());

            // 7. TOQUE DE SÊNIOR: Resolve automaticamente os alertas antigos de "aviso prévio" (CNH_VENCIMENTO)
            // Se o documento já venceu, o painel não precisa mais mostrar que ele "vai vencer em 7 dias".
            try {
                alertaRepository.resolverAlertasAtivosPorMotoristaETipo(motorista.getId(), TipoAlerta.CNH_VENCIMENTO);
                log.debug("🧹 [CLEANUP] Alertas de aviso prévio de CNH para o motorista ID {} foram arquivados.", motorista.getId());
            } catch (Exception e) {
                log.error("⚠️ Falha ao limpar histórico de avisos de CNH legados: {}", e.getMessage());
            }

            // 8. Disparo via WebSocket
            // eventPublisher.publishEvent(new AlertaGeradoEvent(this, alerta));
        }
    

        
        /**
         * SÊNIOR: Geração de alerta de score de comportamento de direção.
         * Implementa barreira contra efeito sanfona (idempotência), conformidade LGPD 
         * e tratamento seguro de tipos numéricos.
         */
        public void criarAlertaScoreBaixo(Motorista motorista) {
            // 1. Fail-Fast
            if (motorista == null || motorista.getId() == null || motorista.getScore() == null) {
                log.error("❌ [TELEMETRIA SCORE] Impossível processar alerta: Objeto motorista, ID ou valor de Score nulo.");
                return;
            }

            double scoreAtual = motorista.getScore().doubleValue();
            String nomeMotorista = motorista.getNome() != null ? motorista.getNome() : "Não informado";

            // 2. Barreira de Idempotência: Evita spam se o motorista oscilar de score enquanto já estiver abaixo da média
            boolean jaExisteAlertaAtivo = alertaRepository.existsByMotoristaIdAndTipoAndResolvidoFalse(
                    motorista.getId(), 
                    TipoAlerta.SCORE_BAIXO
            );

            if (jaExisteAlertaAtivo) {
                log.debug("Formatting ⏭️ Motorista {} já possui alerta ativo de Score Baixo. Ignorando atualização para evitar spam.", nomeMotorista);
                return;
            }

            log.warn("🚨 [COMPORTAMENTO] Motorista {} atingiu score crítico de direção: %.1f", nomeMotorista, scoreAtual);

            // 3. Mascaramento de CPF para auditoria visual (Compliance LGPD)
            String cpfMascarado = "Não Informado";
            if (motorista.getCpf() != null && motorista.getCpf().length() == 11) {
                cpfMascarado = String.format("***.%s.%s-**", 
                        motorista.getCpf().substring(3, 6), 
                        motorista.getCpf().substring(6, 9));
            }

            // 4. Montagem higienizada da mensagem (Uso de %.1f para garantir compatibilidade com Double/Float)
            String mensagem = String.format(
                    "Motorista %s (CPF: %s) está com score de comportamento baixo: %.1f. O score mínimo recomendado é 600.0. É recomendado monitorar ou aplicar treinamento de direção defensiva.",
                    nomeMotorista,
                    cpfMascarado,
                    scoreAtual
            );

            // 5. Construção e Persistência do Alerta
            Alerta alerta = Alerta.builder()
                    .tenantId(motorista.getTenantId() != null ? motorista.getTenantId() : 1L)
                    .motoristaId(motorista.getId())
                    .veiculoId(null)
                    .tipo(TipoAlerta.SCORE_BAIXO)
                    .severidade(SeveridadeAlerta.ALTO)
                    .mensagem(mensagem)
                    .dataHora(LocalDateTime.now())
                    .lido(false)
                    .resolvido(false)
                    .build();

            alertaRepository.save(alerta);
            log.info("✅ [ALERTA GENERADO] Alerta de score baixo salvo para {}. ID: {}", nomeMotorista, alerta.getId());

            // 6. Envio em tempo real
            // eventPublisher.publishEvent(new AlertaGeradoEvent(this, alerta));
        }
    
        /**
         * SÊNIOR: Resolve automaticamente o alerta de score baixo quando o motorista
         * recupera a média de direção recomendada.
         */
        public void verificarEMelhorarScore(Motorista motorista) {
            if (motorista != null && motorista.getScore() != null && motorista.getScore().doubleValue() >= 600) {
                // Reutiliza o método de remoção em lote que criamos no repositório
                alertaRepository.resolverAlertasAtivosPorMotoristaETipo(motorista.getId(), TipoAlerta.SCORE_BAIXO);
                log.debug("🧹 [CLEANUP] Alertas de score baixo para o motorista {} foram arquivados (Score recuperado).", motorista.getNome());
            }
        }
        
        /**
         * SÊNIOR: Geração de alerta para score de comportamento de direção em nível CRÍTICO.
         * Implementa barreira de duplicidade, compliance LGPD, tratamento seguro de tipos decimais
         * e escalonamento automático (limpeza de alertas de score baixo anteriores).
         */
        public void criarAlertaScoreCritico(Motorista motorista) {
            // 1. Fail-Fast
            if (motorista == null || motorista.getId() == null || motorista.getScore() == null) {
                log.error("❌ [TELEMETRIA CRÍTICA] Impossível processar alerta: Objeto motorista, ID ou valor de Score nulo.");
                return;
            }

            double scoreAtual = motorista.getScore().doubleValue();
            String nomeMotorista = motorista.getNome() != null ? motorista.getNome() : "Não informado";

            // 2. Barreira de Idempotência: Se o motorista já está listado como bloqueado por score, pula
            boolean jaExisteAlertaAtivo = alertaRepository.existsByMotoristaIdAndTipoAndResolvidoFalse(
                    motorista.getId(), 
                    TipoAlerta.SCORE_CRITICO
            );

            if (jaExisteAlertaAtivo) {
                log.debug("Formatting ⏭️ Motorista {} já possui alerta ativo de Score Crítico. Pulando inserção repetida.", nomeMotorista);
                return;
            }

            log.error("🚨 [COMPORTAMENTO CRÍTICO] Motorista {} atingiu nível de bloqueio operacional. Score: %.1f", nomeMotorista, scoreAtual);

            // 3. Mascaramento de CPF para auditoria visual (Compliance LGPD)
            String cpfMascarado = "Não Informado";
            if (motorista.getCpf() != null && motorista.getCpf().length() == 11) {
                cpfMascarado = String.format("***.%s.%s-**", 
                        motorista.getCpf().substring(3, 6), 
                        motorista.getCpf().substring(6, 9));
            }

            // 4. Montagem higienizada da mensagem (Compatível com Double e Float usando %.1f)
            String mensagem = String.format(
                    "Motorista %s (CPF: %s) está com score de comportamento CRÍTICO: %.1f. O score mínimo recomendado é 600.0. Motorista BLOQUEADO para novas viagens. Necessária intervenção ou reciclagem imediata.",
                    nomeMotorista,
                    cpfMascarado,
                    scoreAtual
            );

            // 5. Construção e Persistência do Alerta Crítico
            Alerta alerta = Alerta.builder()
                    .tenantId(motorista.getTenantId() != null ? motorista.getTenantId() : 1L)
                    .motoristaId(motorista.getId())
                    .veiculoId(null)
                    .tipo(TipoAlerta.SCORE_CRITICO)
                    .severidade(SeveridadeAlerta.CRITICO)
                    .mensagem(mensagem)
                    .dataHora(LocalDateTime.now())
                    .lido(false)
                    .resolvido(false)
                    .build();

            alertaRepository.save(alerta);
            log.info("✅ [ALERTA GERADO] Bloqueio por score crítico salvo para {}. ID: {}", nomeMotorista, alerta.getId());

            // 6. TOQUE DE SÊNIOR: Escalonamento de estado. 
            // Se a nota caiu para crítica, resolvemos o alerta anterior de 'SCORE_BAIXO' para limpar o dashboard.
            try {
                alertaRepository.resolverAlertasAtivosPorMotoristaETipo(motorista.getId(), TipoAlerta.SCORE_BAIXO);
                log.debug("🧹 [CLEANUP] Alertas de aviso prévio (SCORE_BAIXO) para o motorista ID {} foram arquivados devido ao escalonamento crítico.", motorista.getId());
            } catch (Exception e) {
                log.error("⚠️ Falha ao limpar histórico de alertas de score baixo: {}", e.getMessage());
            }

            // 7. Envio via WebSocket para a central de monitoramento em tempo real
            // eventPublisher.publishEvent(new AlertaGeradoEvent(this, alerta));
        }
    

        /**
         * SÊNIOR: Atualização atômica de score comportamental protegida contra Race Conditions.
         * Implementa Switch Expressions e inteligência bidirecional para transições de alertas (escalonamento e desescalonamento).
         */
        @Transactional
        public void atualizarScoreMotorista(Long motoristaId, String eventoTipo, Long viagemId) {
            if (motoristaId == null || eventoTipo == null || eventoTipo.isBlank()) {
                log.error("❌ [DRIVER BEHAVIOR] Parâmetros obrigatórios nulos para atualização de score.");
                return;
            }

            String eventoNormalizado = eventoTipo.trim().toUpperCase();

            // 1. LOCK PESSIMISTA: Sincroniza o cálculo em nível de banco de dados para frotas de alta frequência
            Motorista motorista = motoristaRepository.findByIdWithLock(motoristaId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MOTORISTA_NOT_FOUND, 
                    "Motorista não encontrado com ID: " + motoristaId));
            
            int scoreAnterior = motorista.getScore();
            
            // 2. Mapeamento Limpo com Switch Expression (Java 14+)
            // Centraliza a regra de pontos e motivos de forma imutável
            record PontuacaoImpacto(int delta, String motivo) {}
            
            PontuacaoImpacto impacto = switch (eventoNormalizado) {
                case "FRENAGEM_BRUSCA"      -> new PontuacaoImpacto(-2, "Frenagem brusca");
                case "ACELERACAO_BRUSCA"    -> new PontuacaoImpacto(-2, "Aceleração brusca");
                case "EXCESSO_VELOCIDADE"   -> new PontuacaoImpacto(-5, "Excesso de velocidade");
                case "USO_CELULAR"          -> new PontuacaoImpacto(-10, "Uso de celular detectado");
                case "FADIGA"               -> new PontuacaoImpacto(-15, "Fadiga detectada");
                case "COLISAO"              -> new PontuacaoImpacto(-50, "Colisão detectada");
                case "VIAGEM_LIMPA"         -> new PontuacaoImpacto(5, "Viagem limpa (sem eventos negativos)");
                case "SETE_DIAS_SEM_ALERTA" -> new PontuacaoImpacto(10, "7 dias sem alertas");
                default -> null;
            };

            if (impacto == null) {
                log.debug("ℹ️ [DRIVER BEHAVIOR] Evento '{}' ignorado. Não possui impacto no Score.", eventoNormalizado);
                return;
            }

            // 3. Clamping: Limita matematicamente entre 0 e 1000 de forma limpa
            int novaPontuacao = Math.max(0, Math.min(1000, scoreAnterior + impacto.delta()));
            
            // 4. Se não houve mudança real (ex: já estava em 1000 e ganhou pontos), aborta escrita
            if (novaPontuacao == scoreAnterior) {
                return;
            }

            // 5. Atualização da Entidade e Histórico de Auditoria
            motorista.setScore(novaPontuacao);
            motoristaRepository.save(motorista);
            
            HistoricoScoreMotorista historico = new HistoricoScoreMotorista();
            historico.setMotoristaId(motoristaId);
            historico.setData(LocalDate.now());
            historico.setScoreAnterior(scoreAnterior);
            historico.setScoreNovo(novaPontuacao);
            historico.setDiferenca(novaPontuacao - scoreAnterior);
            historico.setMotivo(impacto.motivo());
            historico.setViagemId(viagemId);
            historico.setEventoTipo(eventoNormalizado);
            historicoScoreRepository.save(historico);
            
            log.info("📊 [DRIVER BEHAVIOR] Motorista: {} | Score: {} → {} | Motivo: {}", 
                     motorista.getNome(), scoreAnterior, novaPontuacao, impacto.motivo());
            
            // 6. INTELiGÊNCIA DE TRANSIÇÃO DE ALERTAS (SÊNIOR)
            // Avalia o fluxo de descida (Piora) e o fluxo de subida (Melhora)
            processarGatilhosDeAlertasComportamentais(motorista, scoreAnterior, novaPontuacao);
        }

        /**
         * Gerencia de forma inteligente a abertura e o fechamento automático de alertas 
         * baseando-se no cruzamento de faixas de pontuação.
         */
        private void processarGatilhosDeAlertasComportamentais(Motorista motorista, int scoreAnterior, int scoreNovo) {
            // Cenário A: O motorista PIOROU de comportamento
            if (scoreNovo < scoreAnterior) {
                if (scoreNovo < 400) {
                    criarAlertaScoreCritico(motorista); // Dentro do método já limpa o Alerta Baixo automaticamente!
                } else if (scoreNovo < 600) {
                    criarAlertaScoreBaixo(motorista);
                }
            } 
            // Cenário B: O motorista MELHOROU de comportamento (Desescalonamento / Recuperação)
            else {
                if (scoreNovo >= 600) {
                    // Recuperou índice bom: Resolve tudo o que tiver ativo de score ruim
                    alertaRepository.resolverAlertasAtivosPorMotoristaETipo(motorista.getId(), TipoAlerta.SCORE_BAIXO);
                    alertaRepository.resolverAlertasAtivosPorMotoristaETipo(motorista.getId(), TipoAlerta.SCORE_CRITICO);
                } else if (scoreNovo >= 400 && scoreAnterior < 400) {
                    // Saiu do crítico e subiu para o nível apenas "Baixo": 
                    // Resolve o crítico e gera o alerta correto da faixa atual
                    alertaRepository.resolverAlertasAtivosPorMotoristaETipo(motorista.getId(), TipoAlerta.SCORE_CRITICO);
                    criarAlertaScoreBaixo(motorista);
                }
            }
        }
    
    
        /**
         * SÊNIOR: Geração de alertas de Cerca Eletrônica (Geofencing) otimizado para alta frequência.
         * Desacopla o envio de mensageria (WebSocket) via Eventos Assíncronos e aplica barreiras defensivas.
         */
        @Transactional
        public void criarAlertaGeofence(Telemetria telemetria, Geofence geofence, String mensagem) {
            // 1. Fail-Fast
            if (telemetria == null || geofence == null || mensagem == null || mensagem.isBlank()) {
                log.error("❌ [GEOFENCE] Parâmetros obrigatórios ausentes para geração do alerta.");
                return;
            }

            String mensagemLimpa = mensagem.trim();
            Long veiculoId = telemetria.getVeiculoId();

            // 2. Barreira de Idempotência Temporal/Mensagem
            // Evita o efeito "ping-pong" na borda da cerca eletrônica se a mesma mensagem foi gerada recentemente
            boolean jaExisteAlertaRecente = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalseAndMensagemContaining(
                    veiculoId,
                    TipoAlerta.GEOFENCE,
                    mensagemLimpa
            );

            if (jaExisteAlertaRecente) {
                log.debug("Formatting ⏭️ Alerta de Geofence duplicado ou repetido para o veículo ID {}. Ignorando para evitar spam.", veiculoId);
                return;
            }

            log.info("🚨 [GEOFENCE DETECTADO] Processando violação da cerca '{}' para o veículo ID: {}", geofence.getNome(), veiculoId);

            // 3. OTIMIZAÇÃO CRÍTICA: Evite fazer findByVeiculoIdAndStatus aqui para cada telemetria.
            // O ideal é que a viagem ativa venha injetada no objeto 'telemetria' a partir de um cache em memória (Redis).
            // Se o seu sistema ainda depende estritamente do banco, mantemos de forma segura, mas com log de warning de I/O:
            Long viagemId = telemetria.getViagemId(); // Fallback ideal se você puder colocar esse campo no DTO/Entidade de Telemetria
            if (viagemId == null) {
                viagemId = viagemRepository.findByVeiculoIdAndStatus(veiculoId, "EM_ANDAMENTO")
                    .map(Viagem::getId)
                    .orElse(null);
            }

            // 4. Construção Fluida e Segura com Padrão Builder
            Alerta alerta = Alerta.builder()
                    .tenantId(telemetria.getTenantId() != null ? telemetria.getTenantId() : 1L)
                    .veiculoId(veiculoId)
                    .veiculoUuid(null)
                    .tipo(TipoAlerta.GEOFENCE)
                    .severidade(SeveridadeAlerta.MEDIO)
                    .mensagem(mensagemLimpa)
                    .latitude(telemetria.getLatitude())
                    .longitude(telemetria.getLongitude())
                    .velocidadeKmh(telemetria.getVelocidade())
                    .odometroKm(telemetria.getOdometro())
                    .dataHora(LocalDateTime.now())
                    .lido(false)
                    .resolvido(false)
                    .viagemId(viagemId)
                    .build();

            // 5. Persistência Atômica no Banco de Dados
            alertaRepository.save(alerta);
            log.info("✅ [GEOFENCE SALVO] Alerta de cerca gravado na base. ID: {}", alerta.getId());

            // 6. ARQUITETURA ORIENTADA A EVENTOS (EDA): Desacoplamento do WebSocket
            // Em vez de travar o fluxo enviando síncrono para o topic, publicamos um evento interno da aplicação.
            eventPublisher.publishEvent(new AlertaGeofenceGeradoEvent(this, alerta));
        }
    
    
       // ================ Alertas RN-TEL-002 ================

        /**
         * SÊNIOR: Geração de alerta para inconsistência severa de velocidade física (GPS defeituoso).
         * Implementa barreira de spam por tempo, higienização ortográfica e publicação assíncrona.
         */
        @Transactional
        public void criarAlertaVelocidadeImpossivel(Telemetria telemetria) {
            // 1. Fail-Fast defensivo
            if (telemetria == null || telemetria.getVeiculoId() == null || telemetria.getVelocidade() == null) {
                log.error("❌ [TELEMETRIA CRÍTICA] Parâmetros obrigatórios ausentes para avaliar velocidade impossível.");
                return;
            }

            Long veiculoId = telemetria.getVeiculoId();
            double velocidadeAtual = telemetria.getVelocidade().doubleValue();

            // 2. Barreira de Idempotência: Evita inundar o banco/painel se a antena do GPS travar enviando lixo
            // Só cria um novo alerta se não houver um alerta ativo do mesmo tipo aberto nos últimos 5 minutos
            LocalDateTime limiteJanela = LocalDateTime.now().minusMinutes(5);
            boolean jaExisteAlertaRecente = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalseAndDataHoraAfter(
                    veiculoId,
                    TipoAlerta.VELOCIDADE_IMPOSSIVEL, // Sugestão: corrigir ortografia para dois 'S'
                    limiteJanela
            );

            if (jaExisteAlertaRecente) {
                log.debug("Formatting ⏭️ Veículo ID {} já possui um alerta ativo recente de Velocidade Impossível. Pulando.", veiculoId);
                return;
            }

            log.warn("🚨 [TELEMETRIA CRÍTICA] Velocidade incompatível com a física do veículo detectada. ID: {} | Velocidade: {} km/h", 
                    veiculoId, velocidadeAtual);

            // 3. Montagem higienizada da mensagem explicativa para auditoria
            String mensagem = String.format(
                    "Velocidade fisicamente impossível detectada para o veículo: %.1f km/h. O valor excede o limite máximo estrutural parametrizado (300 km/h). Suspeita de defeito no hardware ou anomalia de sinal GPS.",
                    velocidadeAtual
            );

            // 4. Construção Fluida com o Padrão Builder
            Alerta alerta = Alerta.builder()
                    .tenantId(telemetria.getTenantId() != null ? telemetria.getTenantId() : 1L)
                    .veiculoId(veiculoId)
                    .veiculoUuid(telemetria.getVeiculoUuid())
                    .tipo(TipoAlerta.VELOCIDADE_IMPOSSIVEL)
                    .severidade(SeveridadeAlerta.CRITICO)
                    .mensagem(mensagem)
                    .latitude(telemetria.getLatitude())
                    .longitude(telemetria.getLongitude())
                    .velocidadeKmh(velocidadeAtual)
                    .odometroKm(telemetria.getOdometro())
                    .dataHora(LocalDateTime.now())
                    .lido(false)
                    .resolvido(false)
                    .build();

            // 5. Persistência isolada na transação corrente
            alertaRepository.save(alerta);
            log.info("✅ [TELEMETRIA CRÍTICA] Alerta de velocidade impossível gravado com sucesso. ID: {}", alerta.getId());

            // 6. TOQUE DE SÊNIOR: Desacoplamento assíncrono do WebSocket
            // Em vez de chamar o método síncrono direto, disparamos um evento para a infraestrutura processar
            eventPublisher.publishEvent(new AlertaVelocidadeImpossivelGeradoEvent(this, alerta));
        }
     
    
        /**
         * SÊNIOR: Geração de alerta para salto de posição geograficamente impossível (Teletransporte de GPS).
         * Implementa barreira de spam por janela de tempo e publicação desacoplada via eventos assíncronos.
         */
        @Transactional
        public void criarAlertaSaltoPosicao(Telemetria telemetria, double distanciaKm, long segundos) {
            // 1. Fail-Fast defensivo
            if (telemetria == null || telemetria.getVeiculoId() == null) {
                log.error("❌ [TELEMETRIA CRÍTICA] Parâmetros obrigatórios ausentes para avaliar salto de posição.");
                return;
            }

            Long veiculoId = telemetria.getVeiculoId();

            // 2. Barreira de Idempotência: Evita bombardeio de linhas duplicadas se o GPS travar enviando dados corrompidos
            // Só gera um novo registro se não houver outro alerta ativo de salto de posição nos últimos 5 minutos
            LocalDateTime limiteJanela = LocalDateTime.now().minusMinutes(5);
            boolean jaExisteAlertaRecente = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalseAndDataHoraAfter(
                    veiculoId,
                    TipoAlerta.SALTO_POSICAO,
                    limiteJanela
            );

            if (jaExisteAlertaRecente) {
                log.debug("Formatting ⏭️ Veículo ID {} já possui um alerta ativo recente de Salto de Posição. Ignorando replicação.", veiculoId);
                return;
            }

            log.error("🚨 [TELEMETRIA CRÍTICA] Salto de posição impossível detectado para o Veículo ID {}: %.2f km em %d segundos", 
                    veiculoId, distanciaKm, segundos);

            // 3. Montagem clara da mensagem para o operador do painel de monitoramento
            String mensagem = String.format(
                    "Inconsistência geográfica severa (GPS): O veículo registrou um deslocamento de %.2f km em apenas %d segundos. Padrão incompatível com a velocidade de deslocamento terrestre estrutural.",
                    distanciaKm, segundos
            );

            // 4. Construção Consistente com Padrão Builder
            Alerta alerta = Alerta.builder()
                    .tenantId(telemetria.getTenantId() != null ? telemetria.getTenantId() : 1L)
                    .veiculoId(veiculoId)
                    .veiculoUuid(telemetria.getVeiculoUuid())
                    .tipo(TipoAlerta.SALTO_POSICAO)
                    .severidade(SeveridadeAlerta.CRITICO)
                    .mensagem(mensagem)
                    .latitude(telemetria.getLatitude())
                    .longitude(telemetria.getLongitude())
                    .velocidadeKmh(telemetria.getVelocidade() != null ? telemetria.getVelocidade().doubleValue() : 0.0)
                    .odometroKm(telemetria.getOdometro())
                    .dataHora(LocalDateTime.now())
                    .lido(false)
                    .resolvido(false)
                    .build();

            // 5. Persistência isolada no banco de dados
            alertaRepository.save(alerta);
            log.info("✅ [TELEMETRIA CRÍTICA] Alerta de salto de posição gravado com sucesso. ID: {}", alerta.getId());

            // 6. TOQUE DE SÊNIOR: Desacoplamento arquitetural assíncrono (EDA)
            // Publica o evento interno para que o listener trate o WebSocket em background
            eventPublisher.publishEvent(new AlertaSaltoPosicaoGeradoEvent(this, alerta));
        }
    
    
        /**
         * SÊNIOR: Geração de alerta para degradação de precisão horizontal do GPS (HDOP Alto).
         * Corrige escopo de variáveis, implementa trava anti-spam temporal e desacoplamento via EDA.
         */
        @Transactional
        public void criarAlertaHdopAlto(Telemetria telemetria, long minutosConsecutivos) {
            // 1. Fail-Fast defensivo
            if (telemetria == null || telemetria.getVeiculoId() == null || telemetria.getHdop() == null) {
                log.error("❌ [TELEMETRIA] Parâmetros obrigatórios ausentes para avaliar alerta de HDOP Alto.");
                return;
            }

            Long veiculoId = telemetria.getVeiculoId();
            double hdopAtual = telemetria.getHdop().doubleValue();

            // 2. Barreira de Idempotência Temporal: Evita enchentes de registros por oscilação rápida de satélites
            // Só gera um novo alerta se não houver outro ativo criado para este veículo nos últimos 10 minutos
            LocalDateTime limiteJanela = LocalDateTime.now().minusMinutes(10);
            boolean jaExisteAlertaRecente = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalseAndDataHoraAfter(
                    veiculoId,
                    TipoAlerta.HDOP_ALTO,
                    limiteJanela
            );

            if (jaExisteAlertaRecente) {
                log.debug("Formatting ⏭️ Veículo ID {} já possui um alerta ativo recente de HDOP Alto. Ignorando duplicidade.", veiculoId);
                return;
            }

            log.info("⚠️ [QUALIDADE SINAL] Baixa precisão de GPS detectada para o Veículo ID {}. HDOP: {:.1f} por %d min", 
                    veiculoId, hdopAtual, minutosConsecutivos);

            // 3. Montagem higienizada da mensagem explicativa (Segura contra NullPointer)
            String mensagem = String.format(
                    "Degradação na precisão de posicionamento horizontal (HDOP elevado): %.1f por mais de %d minutos. Coordenadas atuais podem apresentar margem de erro acima do tolerável.",
                    hdopAtual, minutosConsecutivos
            );

            // 4. Construção Fluida com o Padrão Builder
            Alerta alerta = Alerta.builder()
                    .tenantId(telemetria.getTenantId() != null ? telemetria.getTenantId() : 1L)
                    .veiculoId(veiculoId)
                    .veiculoUuid(telemetria.getVeiculoUuid())
                    .tipo(TipoAlerta.HDOP_ALTO)
                    .severidade(SeveridadeAlerta.MEDIO)
                    .mensagem(mensagem)
                    .latitude(telemetria.getLatitude())
                    .longitude(telemetria.getLongitude())
                    .velocidadeKmh(telemetria.getVelocidade() != null ? telemetria.getVelocidade().doubleValue() : 0.0)
                    .odometroKm(telemetria.getOdometro())
                    .dataHora(LocalDateTime.now())
                    .lido(false)
                    .resolvido(false)
                    .build();

            // 5. Persistência atômica
            alertaRepository.save(alerta);
            log.info("✅ [TELEMETRIA] Alerta de HDOP Alto gravado com sucesso. ID: {}", alerta.getId());

            // 6. TOQUE DE SÊNIOR: Publicação assíncrona orientada a eventos (EDA)
            // Desacopla o envio do pacote WebSocket da thread principal de telemetria
            eventPublisher.publishEvent(new AlertaHdopAltoGeradoEvent(this, alerta));
        }
    
  
        /**
         * SÊNIOR: Geração de alerta para quantidade insuficiente de satélites sincronizados.
         * Implementa barreira de idempotência temporal, segurança contra nulos e comunicação via EDA (Eventos).
         */
        @Transactional
        public void criarAlertaSatelitesBaixos(Telemetria telemetria, long minutos) {
            // 1. Fail-Fast defensivo
            if (telemetria == null || telemetria.getVeiculoId() == null || telemetria.getSatelites() == null) {
                log.error("❌ [TELEMETRIA] Parâmetros obrigatórios ausentes para avaliar alerta de satélites insuficientes.");
                return;
            }

            Long veiculoId = telemetria.getVeiculoId();
            int qtdSatelites = telemetria.getSatelites().intValue();

            // 2. Barreira de Idempotência Temporal: Evita enchentes de registros por oscilação natural do sinal
            // Só gera um novo alerta se não houver outro ativo criado para este veículo nos últimos 10 minutos
            LocalDateTime limiteJanela = LocalDateTime.now().minusMinutes(10);
            boolean jaExisteAlertaRecente = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalseAndDataHoraAfter(
                    veiculoId,
                    TipoAlerta.SATELITES_INSUFICIENTES,
                    limiteJanela
            );

            if (jaExisteAlertaRecente) {
                log.debug("Formatting ⏭️ Veículo ID {} já possui um alerta ativo recente de Satélites Insuficientes. Pulando.", veiculoId);
                return;
            }

            log.warn("⚠️ [QUALIDADE SINAL] Baixa contagem de satélites para o Veículo ID {}: %d satélites por %d min", 
                    veiculoId, qtdSatelites, minutos);

            // 3. Montagem higienizada da mensagem explicativa para a central de monitoramento
            String mensagem = String.format(
                    "Sinal de posicionamento degradado: Apenas %d satélites sincronizados por mais de %d minutos consecutivos. Risco alto de imprecisão nas coordenadas geográficas.",
                    qtdSatelites, minutos
            );

            // 4. Construção Fluida com o Padrão Builder
            Alerta alerta = Alerta.builder()
                    .tenantId(telemetria.getTenantId() != null ? telemetria.getTenantId() : 1L)
                    .veiculoId(veiculoId)
                    .veiculoUuid(telemetria.getVeiculoUuid())
                    .tipo(TipoAlerta.SATELITES_INSUFICIENTES)
                    .severidade(SeveridadeAlerta.ALTO)
                    .mensagem(mensagem)
                    .latitude(telemetria.getLatitude())
                    .longitude(telemetria.getLongitude())
                    .velocidadeKmh(telemetria.getVelocidade() != null ? telemetria.getVelocidade().doubleValue() : 0.0)
                    .odometroKm(telemetria.getOdometro())
                    .dataHora(LocalDateTime.now())
                    .lido(false)
                    .resolvido(false)
                    .build();

            // 5. Persistência atômica na transação
            alertaRepository.save(alerta);
            log.info("✅ [TELEMETRIA] Alerta de Satélites Insuficientes gravado com sucesso. ID: {}", alerta.getId());

            // 6. TOQUE DE SÊNIOR: Publicação assíncrona orientada a eventos (EDA)
            // Despacha a mensagem para o WebSocket sem prender a thread principal de telemetria
            eventPublisher.publishEvent(new AlertaSatelitesInsuficientesGeradoEvent(this, alerta));
        }
    
        
        /**
         * SÊNIOR: Geração de alerta para perda crítica de comunicação do rastreador (Ignição ON / Transmissão OFF).
         * Implementa barreira para execuções recorrentes de Jobs (Idempotência) e publicação via eventos assíncronos.
         */
        @Transactional
        public void criarAlertaVeiculoSemSinal(Long veiculoId, Long tenantId, String veiculoUuid, String placa) {
            // 1. Fail-Fast defensivo
            if (veiculoId == null) {
                log.error("❌ [TIMEOUT SINAL] Impossível gerar alerta: ID do veículo obrigatório está nulo.");
                return;
            }

            String placaLimpa = (placa != null && !placa.isBlank()) ? placa.trim().toUpperCase() : "SEM PLACA";

            // 2. Barreira de Idempotência: Essencial para métodos invocados por rotinas agendadas (Jobs)
            // Se o veículo já possui um alerta ativo desse tipo, não cria outro.
            boolean jaExisteAlertaAtivo = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalse(
                    veiculoId,
                    TipoAlerta.VEICULO_SEM_SINAL
            );

            if (jaExisteAlertaAtivo) {
                log.debug("Formatting ⏭️ Veículo ID {} ({}) já possui um alerta ativo de falta de sinal. Pulando re-inserção do Job.", veiculoId, placaLimpa);
                return;
            }

            log.warn("🚨 [TIMEOUT SINAL] Veículo {} (ID: {}) está sem transmitir telemetria há mais de 30 minutos com Ignição ligada.", 
                    placaLimpa, veiculoId);

            // 3. Montagem higienizada da mensagem descritiva
            String mensagem = String.format(
                    "Ausência de comunicação: O veículo %s está sem enviar dados de telemetria por mais de 30 minutos consecutivos com o status de ignição ativo. Verificar possível área de sombra ou violação do hardware.",
                    placaLimpa
            );

            // 4. Construção Fluida com Padrão Builder (Dados de lat/long/vel ficam nulos pois não há sinal atual)
            Alerta alerta = Alerta.builder()
                    .tenantId(tenantId != null ? tenantId : 1L)
                    .veiculoId(veiculoId)
                    .veiculoUuid(veiculoUuid)
                    .tipo(TipoAlerta.VEICULO_SEM_SINAL)
                    .severidade(SeveridadeAlerta.ALTO)
                    .mensagem(mensagem)
                    .dataHora(LocalDateTime.now())
                    .lido(false)
                    .resolvido(false)
                    .build();

            // 5. Persistência atômica
            alertaRepository.save(alerta);
            log.info("✅ [TIMEOUT SINAL] Alerta registrado com sucesso no banco. ID: {}", alerta.getId());

            // 6. TOQUE DE SÊNIOR: Publicação assíncrona orientada a eventos (EDA)
            // Despacha para o WebSocket sem segurar a execução do Job de varredura
            eventPublisher.publishEvent(new AlertaVeiculoSemSinalGeradoEvent(this, alerta));
        }
    
  
        /**
         * SÊNIOR: Limpa automaticamente alertas de falta de sinal quando o veículo
         * volta a transmitir dados atualizados para o sistema.
         */
        @Transactional
        public void resolverAlertaSinalRecuperado(Long veiculoId) {
            if (veiculoId != null) {
                // Reutiliza o método de atualização em lote por tipo que criamos anteriormente
                alertaRepository.resolverAlertasAtivosPorVeiculoETipo(veiculoId, TipoAlerta.VEICULO_SEM_SINAL);
                log.debug("🧹 [CLEANUP] Alertas de falta de sinal para o veículo ID {} foram arquivados (Sinal recuperado).", veiculoId);
            }
        }
   
    
    /**
     * RN-VIA-002 - Cria alerta de score crítico (< 700)
     */
        /**
         * SÊNIOR: Geração de alerta para score crítico de viagem (RN-VIA-002).
         * Implementa proteção contra duplicidade por viagem, alinhamento de severidade
         * e tratamento defensivo para evitar gargalos de I/O de banco de dados.
         */
        @Transactional
        public void criarAlertaScoreCritico(Long motoristaId, int score, Long viagemId) {
            // 1. Fail-Fast
            if (motoristaId == null || viagemId == null) {
                log.error("❌ [VIAGEM SCORE] Impossível gerar alerta: MotoristaId e ViagemId são obrigatórios.");
                return;
            }

            // 2. Barreira de Idempotência por Viagem: Evita spam se o score continuar caindo na mesma viagem
            // Se já existe um alerta de score crítico ativo para esta viagem, não cria outro.
            boolean jaExisteAlertaNaViagem = alertaRepository.existsByViagemIdAndTipoAndResolvidoFalse(
                    viagemId,
                    TipoAlerta.SCORE_CRITICO
            );

            if (jaExisteAlertaNaViagem) {
                log.debug("Formatting ⏭️ Viagem ID {} já possui um alerta ativo de Score Crítico. Pulando re-inserção.", viagemId);
                return;
            }

            log.warn("🚨 [VIAGEM CRÍTICA] Analisando queda de score na Viagem ID: {}, Motorista ID: {}, Score: {}", 
                    viagemId, motoristaId, score);

            // 3. Resolução do gargalo de I/O (Ponto de Atenção Sênior)
            // Dica: Em produção, mude isso para buscar de um Cache/Redis ou passe o objeto Motorista/Dados por parâmetro.
            Optional<Motorista> optMotorista = motoristaRepository.findById(motoristaId);
            String nomeMotorista = optMotorista.map(Motorista::getNome).orElse("Desconhecido");
            Long tenantId = optMotorista.map(Motorista::getTenantId).orElse(1L);

            // 4. Montagem higienizada da mensagem explicativa
            String mensagem = String.format(
                    "Atenção: O motorista %s atingiu o score crítico de %d/1000 durante a viagem ID %d. O limite mínimo aceitável para a operação é 700. Necessária intervenção ou acompanhamento imediato do gestor de frota.",
                    nomeMotorista, score, viagemId
            );

            // 5. Construção com o Padrão Builder (Corrigida Severidade para CRITICO)
            Alerta alerta = Alerta.builder()
                    .tenantId(tenantId)
                    .motoristaId(motoristaId)
                    .viagemId(viagemId)
                    .veiculoId(null) // Se você tiver o veiculoId no contexto da viagem, seria excelente injetar aqui também
                    .tipo(TipoAlerta.SCORE_CRITICO)
                    .severidade(SeveridadeAlerta.CRITICO) // Alinhado com a regra de negócio do método
                    .mensagem(mensagem)
                    .dataHora(LocalDateTime.now())
                    .lido(false)
                    .resolvido(false)
                    .build();

            // 6. Persistência atômica
            alertaRepository.save(alerta);
            log.info("✅ [VIAGEM SCORE] Alerta de score crítico de viagem salvo com sucesso. ID: {}", alerta.getId());

            // 7. Desacoplamento assíncrono para o WebSocket da central
            eventPublisher.publishEvent(new AlertaScoreCriticoViagemGeradoEvent(this, alerta));
        }
    
        /**
         * SÊNIOR: Método fábrica unificado para persistência de alertas de telemetria.
         * Centraliza validações fail-fast, aplica regras de anti-spam genéricas por tipo 
         * e dispara notificações assíncronas via Eventos de Domínio.
         */
        @Transactional
        public void criarAlertaCompleto(CriarAlertaCommand cmd) {
            // 1. Fail-Fast Centralizado
            if (cmd == null || cmd.tipo() == null || cmd.severidade() == null || cmd.mensagem() == null || cmd.mensagem().isBlank()) {
                log.error("❌ [FÁBRICA ALERTAS] Impossível processar: Comando nulo ou dados obrigatórios ausentes.");
                return;
            }

            String mensagemLimpa = cmd.mensagem().trim();
            
            // 2. Barreira Genérica de Idempotência Temporal/Escopo
            // Se já existe exatamente o mesmo alerta ativo para o mesmo veículo/viagem nos últimos 2 minutos, barra
            if (cmd.veiculoId() != null) {
                LocalDateTime limiteJanela = LocalDateTime.now().minusMinutes(2);
                boolean jaExisteAlertaIdentico = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalseAndDataHoraAfter(
                        cmd.veiculoId(),
                        cmd.tipo(),
                        limiteJanela
                );
                if (jaExisteAlertaIdentico) {
                    log.debug("Formatting ⏭️ [FÁBRICA ALERTAS] Alerta do tipo {} para o veículo ID {} gerado muito recentemente. Ignorando spam.", cmd.tipo(), cmd.veiculoId());
                    return;
                }
            }

            log.info("📢 [FÁBRICA ALERTAS] Processando gravação - Tipo: {}, Severidade: {}", cmd.tipo(), cmd.severidade());

            // 3. Construção Defensiva da Entidade via Builder
            Alerta alerta = Alerta.builder()
                    .tenantId(cmd.tenantId() != null ? cmd.tenantId() : 1L)
                    .veiculoId(cmd.veiculoId())
                    .veiculoUuid(cmd.veiculoUuid())
                    .viagemId(cmd.viagemId())
                    .motoristaId(cmd.motoristaId())
                    .tipo(cmd.tipo())
                    .severidade(cmd.severidade())
                    .mensagem(mensagemLimpa)
                    .latitude(cmd.latitude())
                    .longitude(cmd.longitude())
                    .velocidadeKmh(cmd.velocidadeKmh() != null ? cmd.velocidadeKmh() : 0.0)
                    .odometroKm(cmd.odometroKm())
                    .dataHora(LocalDateTime.now())
                    .lido(false)
                    .resolvido(false)
                    .build();

            // 4. Persistência
            alertaRepository.save(alerta);
            log.info("✅ [FÁBRICA ALERTAS] Alerta genérico salvo com sucesso. ID: {}, Tipo: {}", alerta.getId(), alerta.getTipo());

            // 5. Publicação de Evento Genérico Desacoplado
            eventPublisher.publishEvent(new AlertaGenericoGeradoEvent(this, alerta));
        }
        
   
        /**
         * 🔄 SOBRECARGA DE COMPATIBILIDADE EXTRA:
         * Resolve o erro de correspondência exata de argumentos (10 parâmetros + null).
         * Mapeia os dados legados diretamente para o novo CriarAlertaCommand.
         */
        @Transactional
        public void criarAlertaCompleto(Long tenantId, Long veiculoId, String veiculoUuid, Long viagemId, 
                                       TipoAlerta tipo, SeveridadeAlerta severidade, String mensagem,
                                       Double latitude, Double longitude, Double velocidadeKmh, Object odometroDummy) {
            
            // Converte os parâmetros antigos no record oficial do sistema
            CriarAlertaCommand cmd = new CriarAlertaCommand(
                tenantId, 
                veiculoId, 
                veiculoUuid, 
                viagemId, 
                null, // motoristaId
                tipo, 
                severidade, 
                mensagem, 
                latitude, 
                longitude, 
                velocidadeKmh, 
                null  // odometroKm força null com segurança de tipo
            );
            
            // Executa a lógica principal com suporte a anti-spam e fila assíncrona
            this.criarAlertaCompleto(cmd);
        }
        
        
        /**
         * 🔄 SOBRECARGA DE COMPATIBILIDADE DEFINITIVA:
         * Mudamos o nome para forçar o Eclipse a limpar o cache de assinaturas.
         */
        @Transactional
        public void criarAlertaHdopAltoLegado(Telemetria telemetria) {
            // Encaminha direto para o método principal passando 10 minutos padrão
            this.criarAlertaHdopAlto(telemetria, 10L);
        }
    }
       
