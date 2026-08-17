package com.telemetria.domain.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.DesvioRota;
import com.telemetria.domain.entity.Motorista;
import com.telemetria.domain.entity.ScoreViagem;
import com.telemetria.domain.entity.Telemetria;
import com.telemetria.domain.entity.Viagem;
import com.telemetria.infrastructure.persistence.DesvioRotaRepository;
import com.telemetria.infrastructure.persistence.MotoristaRepository;
import com.telemetria.infrastructure.persistence.ScoreViagemRepository;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;
import com.telemetria.infrastructure.persistence.ViagemRepository;

/**
 * RN-VIA-002 - Score da Viagem (0-1000)
 * 
 * Penalidades:
 * - Frenagem brusca: -5 pontos
 * - Excesso de velocidade: -10 pontos
 * - Desvio não justificado: -20 pontos
 * - Uso de celular DMS: -25 pontos
 * - Entrega fora da janela: -15 pontos
 * 
 * Score < 700: notificar gestor
 */
@Service
public class ScoreViagemService {

    private static final Logger log = LoggerFactory.getLogger(ScoreViagemService.class);

    // RN-VIA-002: Penalidades
    private static final int PENALIDADE_FRENAGEM_BRUSCA = 5;
    private static final int PENALIDADE_EXCESSO_VELOCIDADE = 10;
    private static final int PENALIDADE_DESVIO_NAO_JUSTIFICADO = 20;
    private static final int PENALIDADE_USO_CELULAR = 25;
    private static final int PENALIDADE_ENTREGA_FORA_JANELA = 15;

    private static final int SCORE_INICIAL = 1000;
    private static final int SCORE_MINIMO_NOTIFICACAO = 700;

    @Autowired
    private ScoreViagemRepository scoreViagemRepository;

    @Autowired
    private ViagemRepository viagemRepository;

    @Autowired
    private TelemetriaRepository telemetriaRepository;

    @Autowired
    private DesvioRotaRepository desvioRotaRepository;

    @Autowired
    private MotoristaRepository motoristaRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private AlertaService alertaService;

    /**
     * Calcula o score final de uma viagem ao finalizá-la
     */
    @Transactional
    public ScoreViagem calcularScoreViagem(Long viagemId) {
        log.info("📊 [RN-VIA-002] Calculando score para viagem ID: {}", viagemId);

        Viagem viagem = viagemRepository.findById(viagemId)
                .orElseThrow(() -> new RuntimeException("Viagem não encontrada: " + viagemId));

        // Verificar se já existe score calculado
        if (scoreViagemRepository.findByViagemId(viagemId).isPresent()) {
            log.warn("⚠️ Score já calculado para viagem {}", viagemId);
            return scoreViagemRepository.findByViagemId(viagemId).get();
        }

        // Coletar dados da viagem
        List<Telemetria> telemetrias = telemetriaRepository.findByViagemIdOrderByDataHoraAsc(viagemId);
        List<DesvioRota> desvios = desvioRotaRepository.findByViagemId(viagemId);

        // Inicializar builder do score
        ScoreViagem.Builder builder = ScoreViagem.builder()
                .tenantId(viagem.getTenantId())
                .viagemId(viagemId)
                .motoristaId(viagem.getMotoristaId())
                .veiculoId(viagem.getVeiculoId())
                .scoreInicial(SCORE_INICIAL);

        // Contadores e penalidades
        int qtdFrenagemBrusca = 0;
        int qtdExcessoVelocidade = 0;
        int qtdDesvioNaoJustificado = 0;
        int qtdUsoCelular = 0;
        int qtdEntregaForaJanela = 0;

        int penalidadeFrenagem = 0;
        int penalidadeVelocidade = 0;
        int penalidadeDesvio = 0;
        int penalidadeCelular = 0;
        int penalidadeEntrega = 0;

        // 1. Verificar frenagens bruscas nas telemetrias
        for (int i = 1; i < telemetrias.size(); i++) {
            Telemetria anterior = telemetrias.get(i - 1);
            Telemetria atual = telemetrias.get(i);

            if (isFrenagemBrusca(anterior, atual)) {
                qtdFrenagemBrusca++;
                penalidadeFrenagem += PENALIDADE_FRENAGEM_BRUSCA;
                log.debug("🚨 Frenagem brusca detectada na telemetria {}", atual.getId());
            }

            if (isExcessoVelocidade(atual)) {
                qtdExcessoVelocidade++;
                penalidadeVelocidade += PENALIDADE_EXCESSO_VELOCIDADE;
                log.debug("🚨 Excesso de velocidade detectado: {} km/h", atual.getVelocidade());
            }
        }

        // 2. Verificar desvios não justificados
        for (DesvioRota desvio : desvios) {
            if (desvio.getResolvido() != null && !desvio.getResolvido()) {
                // Desvio ainda ativo ou não justificado
                qtdDesvioNaoJustificado++;
                penalidadeDesvio += PENALIDADE_DESVIO_NAO_JUSTIFICADO;
                log.debug("🚨 Desvio não justificado detectado: ID {}", desvio.getId());
            } else if (desvio.getMotivo() == null || desvio.getMotivo().isEmpty()) {
                // Desvio resolvido mas sem justificativa
                qtdDesvioNaoJustificado++;
                penalidadeDesvio += PENALIDADE_DESVIO_NAO_JUSTIFICADO;
                log.debug("🚨 Desvio sem justificativa: ID {}", desvio.getId());
            }
        }

        // 3. Verificar uso de celular (DMS - Driver Monitoring System)
        // Assumindo que há um campo na telemetria indicando uso de celular
        for (Telemetria telemetria : telemetrias) {
            if (telemetria.getUsoCelular() != null && telemetria.getUsoCelular()) {
                qtdUsoCelular++;
                penalidadeCelular += PENALIDADE_USO_CELULAR;
                log.debug("📱 Uso de celular detectado na telemetria {}", telemetria.getId());
            }
        }

        // 4. Verificar entregas fora da janela
        if (viagem.getDataChegadaReal() != null && viagem.getDataChegadaPrevista() != null) {
            if (viagem.getDataChegadaReal().isAfter(viagem.getDataChegadaPrevista())) {
                qtdEntregaForaJanela++;
                penalidadeEntrega += PENALIDADE_ENTREGA_FORA_JANELA;
                log.debug("⏰ Entrega fora da janela: prevista={}, real={}",
                        viagem.getDataChegadaPrevista(), viagem.getDataChegadaReal());
            }
        }

        // Construir score
        ScoreViagem score = builder
                .quantidadeFrenagemBrusca(qtdFrenagemBrusca)
                .quantidadeExcessoVelocidade(qtdExcessoVelocidade)
                .quantidadeDesvioNaoJustificado(qtdDesvioNaoJustificado)
                .quantidadeUsoCelular(qtdUsoCelular)
                .quantidadeEntregaForaJanela(qtdEntregaForaJanela)
                .penalidadeFrenagemBrusca(penalidadeFrenagem)
                .penalidadeExcessoVelocidade(penalidadeVelocidade)
                .penalidadeDesvioNaoJustificado(penalidadeDesvio)
                .penalidadeUsoCelular(penalidadeCelular)
                .penalidadeEntregaForaJanela(penalidadeEntrega)
                .observacoes(montarObservacoes(qtdFrenagemBrusca, qtdExcessoVelocidade,
                        qtdDesvioNaoJustificado, qtdUsoCelular, qtdEntregaForaJanela))
                .build();

        // Salvar score
        ScoreViagem scoreSalvo = scoreViagemRepository.save(score);
        log.info("✅ Score calculado para viagem {}: {}/{}",
                viagemId, scoreSalvo.getScoreFinal(), SCORE_INICIAL);

        // Atualizar score do motorista
        atualizarScoreMotorista(viagem.getMotoristaId(), scoreSalvo.getScoreFinal());

        // RN-VIA-002: Notificar gestor se score < 700
        if (scoreSalvo.isScoreCritico() && !scoreSalvo.getNotificacaoGestorEnviada()) {
            notificarGestor(scoreSalvo, viagem);
            scoreSalvo.setNotificacaoGestorEnviada(true);
            scoreViagemRepository.save(scoreSalvo);
        }

        return scoreSalvo;
    }

    /**
     * Detecta frenagem brusca baseado na variação de velocidade
     */
    private boolean isFrenagemBrusca(Telemetria anterior, Telemetria atual) {
        if (anterior.getVelocidade() == null || atual.getVelocidade() == null) {
            return false;
        }
        // Frenagem brusca: redução de velocidade > 15 km/h em menos de 1 segundo
        double reducao = anterior.getVelocidade() - atual.getVelocidade();
        return reducao > 15.0;
    }

    /**
     * Detecta excesso de velocidade (acima de 110 km/h)
     */
    private boolean isExcessoVelocidade(Telemetria telemetria) {
        if (telemetria.getVelocidade() == null) {
            return false;
        }
        return telemetria.getVelocidade() > 110.0;
    }

    /**
     * Monta observações detalhadas do score
     */
    private String montarObservacoes(int frenagens, int excessos, int desvios, int celular, int entregas) {
        StringBuilder obs = new StringBuilder();
        
        if (frenagens > 0) {
            obs.append(String.format("Frenagens bruscas: %d (-%d pts); ", 
                    frenagens, frenagens * PENALIDADE_FRENAGEM_BRUSCA));
        }
        if (excessos > 0) {
            obs.append(String.format("Excessos de velocidade: %d (-%d pts); ", 
                    excessos, excessos * PENALIDADE_EXCESSO_VELOCIDADE));
        }
        if (desvios > 0) {
            obs.append(String.format("Desvios não justificados: %d (-%d pts); ", 
                    desvios, desvios * PENALIDADE_DESVIO_NAO_JUSTIFICADO));
        }
        if (celular > 0) {
            obs.append(String.format("Uso de celular: %d (-%d pts); ", 
                    celular, celular * PENALIDADE_USO_CELULAR));
        }
        if (entregas > 0) {
            obs.append(String.format("Entregas fora da janela: %d (-%d pts); ", 
                    entregas, entregas * PENALIDADE_ENTREGA_FORA_JANELA));
        }
        
        if (obs.length() == 0) {
            obs.append("Viagem sem ocorrências. Score máximo mantido.");
        }
        
        return obs.toString();
    }

    /**
     * Atualiza o score acumulado do motorista
     */
    private void atualizarScoreMotorista(Long motoristaId, int scoreViagem) {
        Optional<Motorista> optMotorista = motoristaRepository.findById(motoristaId);
        if (optMotorista.isPresent()) {
            Motorista motorista = optMotorista.get();
            
            // Calcular média dos últimos scores
            Double mediaScores = scoreViagemRepository.calcularMediaScoreMotorista(motoristaId);
            if (mediaScores != null) {
                int novoScore = (int) Math.round(mediaScores);
                motorista.setScore(novoScore);
                motoristaRepository.save(motorista);
                log.info("📊 Score do motorista {} atualizado para {}", motorista.getNome(), novoScore);
            }
        }
    }

    /**
     * Notifica gestor sobre score crítico (< 700)
     */
    private void notificarGestor(ScoreViagem score, Viagem viagem) {
        try {
            String mensagem = String.format(
                    "⚠️ [RN-VIA-002] ALERTA DE SCORE CRÍTICO!\n" +
                    "Motorista ID: %d\n" +
                    "Viagem ID: %d\n" +
                    "Score: %d/1000\n" +
                    "Classificação: %s\n" +
                    "Penalidades:\n" +
                    "  - Frenagem brusca: %d ocorrências (-%d pts)\n" +
                    "  - Excesso velocidade: %d ocorrências (-%d pts)\n" +
                    "  - Desvios não justificados: %d (-%d pts)\n" +
                    "  - Uso de celular: %d (-%d pts)\n" +
                    "  - Entrega fora janela: %d (-%d pts)",
                    score.getMotoristaId(),
                    score.getViagemId(),
                    score.getScoreFinal(),
                    score.getScoreClassificacao(),
                    score.getQuantidadeFrenagemBrusca(),
                    score.getPenalidadeFrenagemBrusca(),
                    score.getQuantidadeExcessoVelocidade(),
                    score.getPenalidadeExcessoVelocidade(),
                    score.getQuantidadeDesvioNaoJustificado(),
                    score.getPenalidadeDesvioNaoJustificado(),
                    score.getQuantidadeUsoCelular(),
                    score.getPenalidadeUsoCelular(),
                    score.getQuantidadeEntregaForaJanela(),
                    score.getPenalidadeEntregaForaJanela()
            );

            log.warn(mensagem);

            // Enviar via WebSocket
            messagingTemplate.convertAndSend("/topic/score-critico", score);
            
            // Criar alerta no sistema
            alertaService.criarAlertaScoreCritico(
                score.getMotoristaId(), 
                score.getScoreFinal(),
                score.getViagemId()
            );
            
        } catch (Exception e) {
            log.error("❌ Erro ao notificar gestor: {}", e.getMessage());
        }
    }

    /**
     * Busca score por viagem
     */
    public ScoreViagem buscarScorePorViagem(Long viagemId) {
        return scoreViagemRepository.findByViagemId(viagemId).orElse(null);
    }

    /**
     * Lista scores por motorista
     */
    public List<ScoreViagem> listarScoresPorMotorista(Long motoristaId) {
        return scoreViagemRepository.findByMotoristaIdOrderByDataCalculoDesc(motoristaId);
    }

    /**
     * Lista scores críticos não notificados
     */
    public List<ScoreViagem> listarScoresCriticosNaoNotificados() {
        return scoreViagemRepository.findScoresCriticosNaoNotificados();
    }
}