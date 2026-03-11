package com.app.telemetria.service;

import com.app.telemetria.entity.*;
import com.app.telemetria.enums.TipoAlerta;
import com.app.telemetria.enums.SeveridadeAlerta;
// Import StatusViagem removido, usaremos strings
import com.app.telemetria.repository.AlertaRepository;
import com.app.telemetria.repository.VeiculoRepository;
import com.app.telemetria.repository.ViagemRepository;
import com.app.telemetria.client.RoutingClient;
import com.app.telemetria.dto.RouteResponse;

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
    private final ViagemRepository viagemRepository;
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
            VeiculoRepository veiculoRepository,
            ViagemRepository viagemRepository,
            LocationClassifierService locationClassifierService,
            SimpMessagingTemplate messagingTemplate,
            RoutingClient routingClient) {
        this.alertaRepository = alertaRepository;
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
        return alertaRepository.findByVeiculoIdOrderByDataHoraDesc(veiculoId);
    }

    @Transactional(readOnly = true)
    public List<Alerta> listarPorMotorista(Long motoristaId) {
        return alertaRepository.findByMotoristaIdOrderByDataHoraDesc(motoristaId);
    }

    @Transactional(readOnly = true)
    public List<Alerta> listarPorViagem(Long viagemId) {
        return alertaRepository.findByViagemIdOrderByDataHoraDesc(viagemId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        List<Alerta> alertasAtivos = alertaRepository.findByResolvidoFalseOrderByDataHoraDesc();
        dashboard.put("totalAtivos", alertasAtivos.size());

        long altaGravidade = alertasAtivos.stream()
                .filter(a -> SeveridadeAlerta.ALTA.equals(a.getSeveridade()))
                .count();
        long mediaGravidade = alertasAtivos.stream()
                .filter(a -> SeveridadeAlerta.MEDIA.equals(a.getSeveridade()))
                .count();
        long baixaGravidade = alertasAtivos.stream()
                .filter(a -> SeveridadeAlerta.BAIXA.equals(a.getSeveridade()))
                .count();

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
            }
        }
        dashboard.put("alertasPorTipo", alertasPorTipo);
        dashboard.put("ultimosAlertas", alertasAtivos.stream().limit(10).toList());

        return dashboard;
    }

    @Transactional
    public Alerta marcarComoLido(Long id) {
        Alerta alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alerta não encontrado"));
        alerta.setLido(true);
        alerta.setDataHoraLeitura(LocalDateTime.now());
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
        if (telemetria.getVelocidade() == null)
            return;

        if (telemetria.getVelocidade() > VELOCIDADE_MAXIMA) {
            Optional<Alerta> alertaRecente = alertaRepository
                    .findPrimeiroByVeiculoIdAndTipoOrderByDataHoraDesc(
                            telemetria.getVeiculoId(), TipoAlerta.EXCESSO_VELOCIDADE);

            if (alertaRecente.isEmpty() ||
                    Duration.between(alertaRecente.get().getDataHora(), LocalDateTime.now()).toMinutes() > 5) {

                criarAlerta(
                        telemetria.getVeiculoId(),
                        null,
                        null,
                        TipoAlerta.EXCESSO_VELOCIDADE,
                        SeveridadeAlerta.ALTA,
                        String.format("Veículo %.2f km/h acima do limite (%.0f km/h)",
                                telemetria.getVelocidade() - VELOCIDADE_MAXIMA, VELOCIDADE_MAXIMA),
                        telemetria.getLatitude(),
                        telemetria.getLongitude(),
                        telemetria.getVelocidade(),
                        telemetria.getOdometro());
            }
        }
    }

    @Transactional
    public void verificarVelocidadeBaixa(Telemetria telemetria, Viagem viagem) {
        if (telemetria.getVelocidade() == null || viagem == null)
            return;

        if (telemetria.getVelocidade() < VELOCIDADE_MINIMA && telemetria.getVelocidade() > 0) {
            boolean emAreaUrbana = verificarAreaUrbana(telemetria.getLatitude(), telemetria.getLongitude());

            if (!emAreaUrbana) {
                criarAlerta(
                        telemetria.getVeiculoId(),
                        viagem.getMotoristaId(),
                        viagem.getId(),
                        TipoAlerta.VELOCIDADE_BAIXA,
                        SeveridadeAlerta.MEDIA,
                        String.format("Velocidade muito baixa: %.1f km/h", telemetria.getVelocidade()),
                        telemetria.getLatitude(),
                        telemetria.getLongitude(),
                        telemetria.getVelocidade(),
                        telemetria.getOdometro());
            }
        }
    }

    // ================ ALERTAS DE PARADA ================

    @Transactional
    public void verificarParadaProlongada(Long veiculoId, LocalDateTime inicioParada) {
        if (inicioParada == null)
            return;

        long minutosParado = Duration.between(inicioParada, LocalDateTime.now()).toMinutes();

        if (minutosParado > TEMPO_PARADA_MAXIMO) {
            boolean alertaAtivo = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalse(
                    veiculoId, TipoAlerta.PARADA_PROLONGADA);

            if (!alertaAtivo) {
                criarAlerta(
                        veiculoId,
                        null,
                        null,
                        TipoAlerta.PARADA_PROLONGADA,
                        SeveridadeAlerta.MEDIA,
                        String.format("Veículo parado por %d minutos", minutosParado),
                        null,
                        null,
                        0.0,
                        null);
            }
        }
    }

    // ================ ALERTAS DE VIAGEM ================

    @Transactional
    public void verificarInicioViagem(Viagem viagem) {
        if (viagem == null || viagem.getStatus() == null)
            return;

        if ("EM_ANDAMENTO".equals(viagem.getStatus())) {
            Rota rota = viagem.getRota();
            if (rota != null) {
                criarAlerta(
                        viagem.getVeiculoId(),
                        viagem.getMotoristaId(),
                        viagem.getId(),
                        TipoAlerta.INICIO_VIAGEM,
                        SeveridadeAlerta.BAIXA,
                        String.format("Viagem iniciada: %s → %s",
                                rota.getOrigem(),
                                rota.getDestino()),
                        rota.getLatitudeOrigem(),
                        rota.getLongitudeOrigem(),
                        0.0,
                        null);
            }
        }
    }

    @Transactional
    public void verificarFimViagem(Viagem viagem) {
        if (viagem == null || viagem.getStatus() == null)
            return;

        if ("FINALIZADA".equals(viagem.getStatus())) {
            Rota rota = viagem.getRota();
            if (rota != null) {
                criarAlerta(
                        viagem.getVeiculoId(),
                        viagem.getMotoristaId(),
                        viagem.getId(),
                        TipoAlerta.FIM_VIAGEM,
                        SeveridadeAlerta.BAIXA,
                        String.format("Viagem finalizada: %s → %s",
                                rota.getOrigem(),
                                rota.getDestino()),
                        rota.getLatitudeDestino(),
                        rota.getLongitudeDestino(),
                        0.0,
                        null);
            }
        }
    }

    @Transactional
    public void verificarAtrasoViagemInteligente(Viagem viagem, Telemetria ultimaTelemetria) {
        if (viagem == null || ultimaTelemetria == null)
            return;

        Rota rota = viagem.getRota();
        if (rota == null)
            return;

        RouteResponse rotaCalculada = routingClient.calcular(
                ultimaTelemetria.getLatitude(),
                ultimaTelemetria.getLongitude(),
                rota.getLatitudeDestino(),
                rota.getLongitudeDestino());

        if (rotaCalculada == null)
            return;

        double minutosRestantes = rotaCalculada.getDuracaoMinutos();
        LocalDateTime etaReal = LocalDateTime.now().plusMinutes((long) minutosRestantes);

        LocalDateTime dataChegadaPrevista = viagem.getDataChegadaPrevista();
        if (dataChegadaPrevista != null && etaReal.isAfter(dataChegadaPrevista)) {
            long atrasoReal = Duration.between(dataChegadaPrevista, etaReal).toMinutes();

            criarAlerta(
                    viagem.getVeiculoId(),
                    viagem.getMotoristaId(),
                    viagem.getId(),
                    TipoAlerta.ATRASO_VIAGEM,
                    SeveridadeAlerta.ALTA,
                    "Atraso real estimado: " + atrasoReal + " minutos",
                    ultimaTelemetria.getLatitude(),
                    ultimaTelemetria.getLongitude(),
                    ultimaTelemetria.getVelocidade(),
                    ultimaTelemetria.getOdometro());
        }
    }

    // ================ ALERTAS DE GPS ================

    @Transactional
    public void verificarGpsSemSinal(Long veiculoId, Telemetria ultimaTelemetria) {
        if (ultimaTelemetria == null)
            return;

        LocalDateTime agora = LocalDateTime.now();
        long minutosSemSinal = Duration.between(ultimaTelemetria.getDataHora(), agora).toMinutes();

        if (minutosSemSinal > 15) {
            boolean alertaAtivo = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalse(
                    veiculoId, TipoAlerta.GPS_SEM_SINAL);

            if (!alertaAtivo) {
                criarAlerta(
                        veiculoId,
                        null,
                        null,
                        TipoAlerta.GPS_SEM_SINAL,
                        SeveridadeAlerta.ALTA,
                        String.format("Veículo sem sinal GPS há %d minutos", minutosSemSinal),
                        ultimaTelemetria.getLatitude(),
                        ultimaTelemetria.getLongitude(),
                        ultimaTelemetria.getVelocidade(),
                        ultimaTelemetria.getOdometro());
            }
        }
    }

    // ================ ALERTAS DE MOTORISTA ================

    @Transactional
    public void verificarTempoDirecao(Viagem viagem, Telemetria ultimaTelemetria) {
        if (viagem == null || viagem.getMotoristaId() == null)
            return;

        LocalDateTime dataSaida = viagem.getDataSaida();
        if (dataSaida != null) {
            long minutosDirigindo = Duration.between(dataSaida, LocalDateTime.now()).toMinutes();

            if (minutosDirigindo > TEMPO_DIRECAO_MAXIMO) {
                boolean alertaAtivo = alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalse(
                        viagem.getVeiculoId(), TipoAlerta.TEMPO_DIRECAO);

                if (!alertaAtivo) {
                    criarAlerta(
                            viagem.getVeiculoId(),
                            viagem.getMotoristaId(),
                            viagem.getId(),
                            TipoAlerta.TEMPO_DIRECAO,
                            SeveridadeAlerta.ALTA,
                            String.format("Motorista dirigindo por %d minutos sem pausa", minutosDirigindo),
                            ultimaTelemetria != null ? ultimaTelemetria.getLatitude() : null,
                            ultimaTelemetria != null ? ultimaTelemetria.getLongitude() : null,
                            ultimaTelemetria != null ? ultimaTelemetria.getVelocidade() : null,
                            ultimaTelemetria != null ? ultimaTelemetria.getOdometro() : null);
                }
            }
        }
    }

    // ================ ALERTAS DE COMBUSTÍVEL ================

    @Transactional
    public void verificarNivelCombustivel(Telemetria telemetria, Viagem viagem) {
        if (telemetria.getNivelCombustivel() == null)
            return;

        if (telemetria.getNivelCombustivel() < NIVEL_COMBUSTIVEL_MINIMO) {
            criarAlerta(
                    telemetria.getVeiculoId(),
                    viagem != null ? viagem.getMotoristaId() : null,
                    viagem != null ? viagem.getId() : null,
                    TipoAlerta.NIVEL_COMBUSTIVEL_BAIXO,
                    SeveridadeAlerta.MEDIA,
                    String.format("Nível de combustível baixo: %.0f%%", telemetria.getNivelCombustivel()),
                    telemetria.getLatitude(),
                    telemetria.getLongitude(),
                    telemetria.getVelocidade(),
                    telemetria.getOdometro());
        }
    }

    // ================ MÉTODO PRINCIPAL ================

    @Async("alertaTaskExecutor")
    @Transactional
    public CompletableFuture<String> processarTelemetria(Telemetria telemetria) {
        if (telemetria == null || telemetria.getVeiculoId() == null) {
            return CompletableFuture.completedFuture("Telemetria inválida");
        }

        long inicio = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        System.out.println("🔄 [Thread: " + threadName + "] Iniciando processamento assíncrono de alertas");

        try {
            // Busca viagem ativa do veículo (status "EM_ANDAMENTO")
            Viagem viagemAtiva = viagemRepository.findByVeiculoIdAndStatus(
                    telemetria.getVeiculoId(), "EM_ANDAMENTO").orElse(null);

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
            System.out.println("✅ [Thread: " + threadName + "] Alertas processados em " + (fim - inicio) + "ms");

            return CompletableFuture.completedFuture("Alertas processados com sucesso");

        } catch (Exception e) {
            System.err.println("❌ [Thread: " + threadName + "] Erro no processamento: " + e.getMessage());
            e.printStackTrace();
            return CompletableFuture.failedFuture(e);
        }
    }

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

    private void criarAlerta(Long veiculoId, Long motoristaId, Long viagemId,
            TipoAlerta tipo, SeveridadeAlerta severidade, String mensagem,
            Double latitude, Double longitude, Double velocidadeKmh, Double odometroKm) {

        Alerta alerta = new Alerta();
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
        System.out.println("🚨 [Thread: " + threadName + "] ALERTA GERADO: " + mensagem);
    }

    private void resolverAlertas(Telemetria telemetria) {
        if (telemetria.getVelocidade() != null && telemetria.getVelocidade() <= VELOCIDADE_MAXIMA) {
            List<Alerta> alertasExcesso = alertaRepository
                    .findByVeiculoIdAndTipoAndResolvidoFalseOrderByDataHoraDesc(
                            telemetria.getVeiculoId(), TipoAlerta.EXCESSO_VELOCIDADE);

            for (Alerta alerta : alertasExcesso) {
                alerta.setResolvido(true);
                alerta.setDataHoraResolucao(LocalDateTime.now());
                alertaRepository.save(alerta);
            }
        }
    }

    private boolean verificarAreaUrbana(Double latitude, Double longitude) {
        if (latitude == null || longitude == null)
            return false;
        try {
            String classificacao = locationClassifierService.classify(latitude, longitude);
            return "AREA_URBANA".equals(classificacao);
        } catch (Exception e) {
            System.err.println("Erro ao verificar area urbana: " + e.getMessage());
            return false;
        }
    }

    @Transactional
    public void verificarAreaUrbanaEAvisar(Double latitude, Double longitude, String placaVeiculo) {
        boolean urbana = verificarAreaUrbana(latitude, longitude);
        if (urbana) {
            String mensagem = "Veiculo " + placaVeiculo + " entrou em area urbana";
            messagingTemplate.convertAndSend("/topic/alertas", mensagem);
            System.out.println("WebSocket enviado:" + mensagem);
        }
    }
}