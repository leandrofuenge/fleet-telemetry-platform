package com.app.telemetria.service;

import com.app.telemetria.entity.*;
import com.app.telemetria.enums.TipoAlerta;
import com.app.telemetria.enums.GravidadeAlerta;
import com.app.telemetria.repository.AlertaRepository;
import com.app.telemetria.repository.VeiculoRepository;
import com.app.telemetria.repository.ViagemRepository;
import com.app.telemetria.client.RoutingClient;
import com.app.telemetria.dto.RouteResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class AlertaService {
    
    private final AlertaRepository alertaRepository;
    private final VeiculoRepository veiculoRepository;
    private final ViagemRepository viagemRepository;
    private final LocationClassifierService locationClassifierService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RoutingClient routingClient;
    
    // Configurações
    private static final double VELOCIDADE_MAXIMA = 110.0; // km/h
    private static final double VELOCIDADE_MINIMA = 10.0; // km/h
    private static final int TEMPO_PARADA_MAXIMO = 30; // minutos
    private static final int NIVEL_COMBUSTIVEL_MINIMO = 15; // percentual
    private static final int TEMPO_DIRECAO_MAXIMO = 240; // minutos (4 horas)
    
    public AlertaService(
            AlertaRepository alertaRepository,
            VeiculoRepository veiculoRepository,
            ViagemRepository viagemRepository,
            LocationClassifierService locationClassifierService,
            SimpMessagingTemplate messagingTemplate,
            RoutingClient routingClient) {
    	
        this.alertaRepository = alertaRepository;
        this.veiculoRepository = veiculoRepository;
        this.viagemRepository = viagemRepository;
        this.locationClassifierService = locationClassifierService;
        this.messagingTemplate = messagingTemplate;
        this.routingClient = routingClient;
    }
    
    // ================ MÉTODOS PARA O CONTROLLER ================
    
    @Transactional(readOnly = true)
    public Page<Alerta> listarTodos(Pageable pageable) {
        return alertaRepository.findAll(pageable);
    }
    
    @Transactional(readOnly = true)
    public List<Alerta> listarAtivos() {
        return alertaRepository.findByResolvidoFalseOrderByDataHoraDesc();
    }
    
    @Transactional(readOnly = true)
    public List<Alerta> listarPorVeiculo(Long veiculoId) {
        Veiculo veiculo = veiculoRepository.findById(veiculoId)
            .orElseThrow(() -> new RuntimeException("Veículo não encontrado"));
        return alertaRepository.findByVeiculoOrderByDataHoraDesc(veiculo);
    }
    
    @Transactional(readOnly = true)
    public List<Alerta> listarPorMotorista(Long motoristaId) {
        return alertaRepository.findByMotoristaIdOrderByDataHoraDesc(motoristaId);
    }
    
    @Transactional(readOnly = true)
    public List<Alerta> listarPorViagem(Long viagemId) {
        Viagem viagem = viagemRepository.findById(viagemId)
            .orElseThrow(() -> new RuntimeException("Viagem não encontrada"));
        return alertaRepository.findByViagemOrderByDataHoraDesc(viagem);
    }
    
    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Total de alertas ativos
        List<Alerta> alertasAtivos = alertaRepository.findByResolvidoFalseOrderByDataHoraDesc();
        dashboard.put("totalAtivos", alertasAtivos.size());
        
        // Alertas por gravidade
        long altaGravidade = alertasAtivos.stream()
            .filter(a -> GravidadeAlerta.ALTA.name().equals(a.getGravidade()))
            .count();
        long mediaGravidade = alertasAtivos.stream()
            .filter(a -> GravidadeAlerta.MEDIA.name().equals(a.getGravidade()))
            .count();
        long baixaGravidade = alertasAtivos.stream()
            .filter(a -> GravidadeAlerta.BAIXA.name().equals(a.getGravidade()))
            .count();
        
        dashboard.put("altaGravidade", altaGravidade);
        dashboard.put("mediaGravidade", mediaGravidade);
        dashboard.put("baixaGravidade", baixaGravidade);
        
        // Alertas por tipo
        Map<String, Long> alertasPorTipo = new HashMap<>();
        for (TipoAlerta tipo : TipoAlerta.values()) {
            long count = alertasAtivos.stream()
                .filter(a -> tipo.name().equals(a.getTipo()))
                .count();
            if (count > 0) {
                alertasPorTipo.put(tipo.name(), count);
            }
        }
        dashboard.put("alertasPorTipo", alertasPorTipo);
        
        // Últimos 10 alertas
        dashboard.put("ultimosAlertas", alertasAtivos.stream().limit(10).toList());
        
        return dashboard;
    }
    
    @Transactional
    public Alerta marcarComoLido(Long id) {
        Alerta alerta = alertaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Alerta não encontrado"));
        alerta.setLido(true);
        return alertaRepository.save(alerta);
    }
    
    @Transactional
    public Alerta resolverAlerta(Long id) {
        Alerta alerta = alertaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Alerta não encontrado"));
        alerta.setResolvido(true);
        alerta.setDataHoraResolucao(LocalDateTime.now());
        return alertaRepository.save(alerta);
    }
    
    @Transactional(readOnly = true)
    public List<Alerta> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return alertaRepository.findByDataHoraBetweenOrderByDataHoraDesc(inicio, fim);
    }
    
    // ================ ALERTAS DE VELOCIDADE ================
    
    @Transactional
    public void verificarExcessoVelocidade(Telemetria telemetria) {
        if (telemetria.getVelocidade() == null) return;
        
        if (telemetria.getVelocidade() > VELOCIDADE_MAXIMA) {
            // Verifica se já existe um alerta recente para este veículo
            Optional<Alerta> alertaRecente = alertaRepository
                .findPrimeiroByVeiculoAndTipoOrderByDataHoraDesc(
                    telemetria.getVeiculo(), 
                    TipoAlerta.EXCESSO_VELOCIDADE.name()
                );
            
            if (alertaRecente.isEmpty() || 
                Duration.between(alertaRecente.get().getDataHora(), LocalDateTime.now()).toMinutes() > 5) {
                
                criarAlerta(
                    telemetria.getVeiculo(),
                    null,
                    null,
                    TipoAlerta.EXCESSO_VELOCIDADE.name(),
                    GravidadeAlerta.ALTA.name(),
                    String.format("Veículo %.2f km/h acima do limite (%.0f km/h)",
                        telemetria.getVelocidade() - VELOCIDADE_MAXIMA, VELOCIDADE_MAXIMA),
                    telemetria.getLatitude(),
                    telemetria.getLongitude(),
                    telemetria.getVelocidade(),
                    telemetria.getOdometro()
                );
            }
        }
    }
    
    @Transactional
    public void verificarVelocidadeBaixa(Telemetria telemetria, Viagem viagem) {
        if (telemetria.getVelocidade() == null || viagem == null) return;
        
        if (telemetria.getVelocidade() < VELOCIDADE_MINIMA && telemetria.getVelocidade() > 0) {
            // Verifica se está em trecho urbano (lógica simplificada)
            boolean emAreaUrbana = verificarAreaUrbana(telemetria.getLatitude(), telemetria.getLongitude());
            
            if (!emAreaUrbana) {
                criarAlerta(
                    telemetria.getVeiculo(),
                    viagem.getMotorista(),
                    viagem,
                    TipoAlerta.VELOCIDADE_BAIXA.name(),
                    GravidadeAlerta.MEDIA.name(),
                    String.format("Velocidade muito baixa: %.1f km/h", telemetria.getVelocidade()),
                    telemetria.getLatitude(),
                    telemetria.getLongitude(),
                    telemetria.getVelocidade(),
                    telemetria.getOdometro()
                );
            }
        }
    }
    
    // ================ ALERTAS DE PARADA ================
    
    @Transactional
    public void verificarParadaProlongada(Veiculo veiculo, LocalDateTime inicioParada) {
        if (inicioParada == null) return;
        
        long minutosParado = Duration.between(inicioParada, LocalDateTime.now()).toMinutes();
        
        if (minutosParado > TEMPO_PARADA_MAXIMO) {
            // Verifica se já existe alerta ativo para esta parada
            boolean alertaAtivo = alertaRepository.existsByVeiculoAndTipoAndResolvidoFalse(
                veiculo, TipoAlerta.PARADA_PROLONGADA.name());
            
            if (!alertaAtivo) {
                criarAlerta(
                    veiculo,
                    null,
                    null,
                    TipoAlerta.PARADA_PROLONGADA.name(),
                    GravidadeAlerta.MEDIA.name(),
                    String.format("Veículo parado por %d minutos", minutosParado),
                    null,
                    null,
                    0.0,
                    null
                );
            }
        }
    }
    
    // ================ ALERTAS DE VIAGEM ================
    
    @Transactional
    public void verificarInicioViagem(Viagem viagem) {
        if (viagem == null || viagem.getStatus() == null) return;
        
        if ("EM_ANDAMENTO".equals(viagem.getStatus())) {
            criarAlerta(
                viagem.getVeiculo(),
                viagem.getMotorista(),
                viagem,
                TipoAlerta.INICIO_VIAGEM.name(),
                GravidadeAlerta.BAIXA.name(),
                String.format("Viagem iniciada: %s → %s", 
                    viagem.getRota().getOrigem(), 
                    viagem.getRota().getDestino()),
                viagem.getRota().getLatitudeOrigem(),
                viagem.getRota().getLongitudeOrigem(),
                0.0,
                null
            );
        }
    }
    
    @Transactional
    public void verificarFimViagem(Viagem viagem) {
        if (viagem == null || viagem.getStatus() == null) return;
        
        if ("FINALIZADA".equals(viagem.getStatus())) {
            criarAlerta(
                viagem.getVeiculo(),
                viagem.getMotorista(),
                viagem,
                TipoAlerta.FIM_VIAGEM.name(),
                GravidadeAlerta.BAIXA.name(),
                String.format("Viagem finalizada: %s → %s", 
                    viagem.getRota().getOrigem(), 
                    viagem.getRota().getDestino()),
                viagem.getRota().getLatitudeDestino(),
                viagem.getRota().getLongitudeDestino(),
                0.0,
                null
            );
        }
    }
    
    @Transactional
    public void verificarAtrasoViagemInteligente(Viagem viagem, Telemetria ultimaTelemetria) {

        if (viagem == null || ultimaTelemetria == null) return;

        RouteResponse rota = routingClient.calcular(
                ultimaTelemetria.getLatitude(),
                ultimaTelemetria.getLongitude(),
                viagem.getRota().getLatitudeDestino(),
                viagem.getRota().getLongitudeDestino()
        );

        if (rota == null) return;

        double minutosRestantes = rota.getDuracaoMinutos();

        LocalDateTime etaReal = LocalDateTime.now()
                .plusMinutes((long) minutosRestantes);

        if (etaReal.isAfter(viagem.getDataChegadaPrevista())) {

            long atrasoReal =
                    Duration.between(viagem.getDataChegadaPrevista(), etaReal)
                            .toMinutes();

            criarAlerta(
                    viagem.getVeiculo(),
                    viagem.getMotorista(),
                    viagem,
                    TipoAlerta.ATRASO_VIAGEM.name(),
                    GravidadeAlerta.ALTA.name(),
                    "Atraso real estimado: " + atrasoReal + " minutos",
                    ultimaTelemetria.getLatitude(),
                    ultimaTelemetria.getLongitude(),
                    ultimaTelemetria.getVelocidade(),
                    ultimaTelemetria.getOdometro()
            );
        }
    }
    
    // ================ ALERTAS DE GPS ================
    
    @Transactional
    public void verificarGpsSemSinal(Veiculo veiculo, Telemetria ultimaTelemetria) {
        if (ultimaTelemetria == null) return;
        
        LocalDateTime agora = LocalDateTime.now();
        long minutosSemSinal = Duration.between(ultimaTelemetria.getDataHora(), agora).toMinutes();
        
        if (minutosSemSinal > 15) { // 15 minutos sem sinal
            boolean alertaAtivo = alertaRepository.existsByVeiculoAndTipoAndResolvidoFalse(
                veiculo, TipoAlerta.GPS_SEM_SINAL.name());
            
            if (!alertaAtivo) {
                criarAlerta(
                    veiculo,
                    null,
                    null,
                    TipoAlerta.GPS_SEM_SINAL.name(),
                    GravidadeAlerta.ALTA.name(),
                    String.format("Veículo sem sinal GPS há %d minutos", minutosSemSinal),
                    ultimaTelemetria.getLatitude(),
                    ultimaTelemetria.getLongitude(),
                    ultimaTelemetria.getVelocidade(),
                    ultimaTelemetria.getOdometro()
                );
            }
        }
    }
    
    // ================ ALERTAS DE MOTORISTA ================
    
    @Transactional
    public void verificarTempoDirecao(Viagem viagem, Telemetria ultimaTelemetria) {
        if (viagem == null || viagem.getMotorista() == null) return;
        
        if (viagem.getDataInicio() != null) {
            long minutosDirigindo = Duration.between(viagem.getDataInicio(), LocalDateTime.now()).toMinutes();
            
            if (minutosDirigindo > TEMPO_DIRECAO_MAXIMO) {
                boolean alertaAtivo = alertaRepository.existsByVeiculoAndTipoAndResolvidoFalse(
                    viagem.getVeiculo(), TipoAlerta.TEMPO_DIRECAO.name());
                
                if (!alertaAtivo) {
                    criarAlerta(
                        viagem.getVeiculo(),
                        viagem.getMotorista(),
                        viagem,
                        TipoAlerta.TEMPO_DIRECAO.name(),
                        GravidadeAlerta.ALTA.name(),
                        String.format("Motorista dirigindo por %d minutos sem pausa", minutosDirigindo),
                        ultimaTelemetria != null ? ultimaTelemetria.getLatitude() : null,
                        ultimaTelemetria != null ? ultimaTelemetria.getLongitude() : null,
                        ultimaTelemetria != null ? ultimaTelemetria.getVelocidade() : null,
                        ultimaTelemetria != null ? ultimaTelemetria.getOdometro() : null
                    );
                }
            }
        }
    }
    
    // ================ ALERTAS DE COMBUSTÍVEL ================
    
    @Transactional
    public void verificarNivelCombustivel(Telemetria telemetria, Viagem viagem) {
        if (telemetria.getNivelCombustivel() == null) return;
        
        if (telemetria.getNivelCombustivel() < NIVEL_COMBUSTIVEL_MINIMO) {
            criarAlerta(
                telemetria.getVeiculo(),
                viagem != null ? viagem.getMotorista() : null,
                viagem,
                TipoAlerta.NIVEL_COMBUSTIVEL_BAIXO.name(),
                GravidadeAlerta.MEDIA.name(),
                String.format("Nível de combustível baixo: %.0f%%", telemetria.getNivelCombustivel()),
                telemetria.getLatitude(),
                telemetria.getLongitude(),
                telemetria.getVelocidade(),
                telemetria.getOdometro()
            );
        }
    }
    
    // ================ MÉTODO PRINCIPAL (AGORA ASSÍNCRONO) ================
    
    /**
     * Método principal agora é assíncrono e retorna CompletableFuture
     */
    @Async("alertaTaskExecutor")
    @Transactional
    public CompletableFuture<String> processarTelemetria(Telemetria telemetria) {
        if (telemetria == null || telemetria.getVeiculo() == null) {
            return CompletableFuture.completedFuture("Telemetria inválida");
        }
        
        long inicio = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        System.out.println("🔄 [Thread: " + threadName + "] Iniciando processamento assíncrono de alertas");
        
        try {
            // Buscar viagem ativa do veículo
            Viagem viagemAtiva = viagemRepository.findByVeiculoAndStatus(
                telemetria.getVeiculo(), "EM_ANDAMENTO").orElse(null);
            
            // Verificar todos os tipos de alerta (tudo continua igual)
            verificarExcessoVelocidade(telemetria);
            verificarVelocidadeBaixa(telemetria, viagemAtiva);
            verificarNivelCombustivel(telemetria, viagemAtiva);
            
            // Verificações adicionais que podem ser feitas assincronamente
            verificarGpsSemSinal(telemetria.getVeiculo(), telemetria);
            
            if (viagemAtiva != null) {
                verificarTempoDirecao(viagemAtiva, telemetria);
                verificarAtrasoViagemInteligente(viagemAtiva, telemetria);
            }
            
            // Atualizar status de alertas resolvidos
            resolverAlertas(telemetria);
            
            long fim = System.currentTimeMillis();
            System.out.println("✅ [Thread: " + threadName + "] Alertas processados em " + (fim - inicio) + "ms");
            
            return CompletableFuture.completedFuture("Alertas processados com sucesso");
            
        } catch (Exception e) {
            System.err.println("❌ [Thread: " + threadName + "] Erro no processamento: " + e.getMessage());
            e.printStackTrace();
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Versão que processa múltiplas telemetrias em paralelo
     */
    @Async("alertaTaskExecutor")
    public CompletableFuture<List<String>> processarMultiplasTelemetrias(List<Telemetria> telemetrias) {
        return CompletableFuture.supplyAsync(() -> {
            return telemetrias.stream()
                .map(t -> {
                    try {
                        processarTelemetria(t).join();
                        return "Sucesso: " + t.getId();
                    } catch (Exception e) {
                        return "Erro: " + t.getId() + " - " + e.getMessage();
                    }
                })
                .toList();
        });
    }
    
    // ================ MÉTODOS AUXILIARES ================
    
    private void criarAlerta(Veiculo veiculo, Motorista motorista, Viagem viagem,
                             String tipo, String gravidade, String mensagem,
                             Double latitude, Double longitude, Double velocidade, Double odometro) {
        
        Alerta alerta = new Alerta();
        alerta.setVeiculo(veiculo);
        alerta.setMotorista(motorista);
        alerta.setViagem(viagem);
        alerta.setTipo(tipo);
        alerta.setGravidade(gravidade);
        alerta.setMensagem(mensagem);
        alerta.setLatitude(latitude);
        alerta.setLongitude(longitude);
        alerta.setVelocidade(velocidade);
        alerta.setOdometro(odometro);
        alerta.setDataHora(LocalDateTime.now());
        alerta.setLido(false);
        alerta.setResolvido(false);
        
        alertaRepository.save(alerta);
        
        String threadName = Thread.currentThread().getName();
        System.out.println("🚨 [Thread: " + threadName + "] ALERTA GERADO: " + mensagem);
    }
    
    private void resolverAlertas(Telemetria telemetria) {
        if (telemetria.getVelocidade() != null && telemetria.getVelocidade() <= VELOCIDADE_MAXIMA) {
            List<Alerta> alertasExcesso = alertaRepository
                .findByVeiculoAndTipoAndResolvidoFalseOrderByDataHoraDesc(
                    telemetria.getVeiculo(), TipoAlerta.EXCESSO_VELOCIDADE.name());
            
            for (Alerta alerta : alertasExcesso) {
                alerta.setResolvido(true);
                alerta.setDataHoraResolucao(LocalDateTime.now());
                alertaRepository.save(alerta);
            }
        }
    }
    
    private boolean verificarAreaUrbana(Double latitude, Double longitude) {
        
    	if (latitude  == null || longitude == null) return false;
    
    	 try {
             String classificacao =
                     locationClassifierService.classify(latitude, longitude);
             
             return "AREA_URBANA".equals(classificacao);
    		    		
    	} catch (Exception e) {
    		System.err.println("Erro ao verificar area urbana: " + e.getMessage());
    		return false;
    	}
    }
    
    @Transactional
    public void verificarAreaUrbanaEAvisar(
    		Double latitude,
    		Double longitude,
    		String placaVeiculo) {
    	
    	boolean urbana = verificarAreaUrbana(latitude, longitude);
    	
    	if (urbana) {
    		
    		String mensagem = "Veiculo " + placaVeiculo + " entrou em area urbana";
    		
    		// salvar no banco 
    		
    	  messagingTemplate.convertAndSend(
    			  "/topic/alertas",
    			  mensagem
    	  );
    		
    	  System.out.println("WebSocket enviado:" + mensagem);
    	}
    }
}