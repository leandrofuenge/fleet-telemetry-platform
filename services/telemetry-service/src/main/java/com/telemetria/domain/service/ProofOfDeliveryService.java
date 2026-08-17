package com.telemetria.domain.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.api.dto.request.AtualizarPontoEntregaRequest;
import com.telemetria.domain.entity.PontoEntrega;
import com.telemetria.domain.enums.StatusPontoEntrega;
import com.telemetria.domain.enums.TipoPontoEntrega;
import com.telemetria.infrastructure.integration.storage.MinioStorageService;
import com.telemetria.infrastructure.persistence.PontoEntregaRepository;

/**
 * RF11 — Proof of Delivery Service
 * 
 * RN-ENT-001:
 * - Tipo ENTREGA: assinatura_path ou foto_entrega_path obrigatório ao marcar ENTREGUE
 * - Status FALHOU requer campo ocorrencia preenchido
 * - Arquivos no MinIO — nunca BLOB no banco
 */
@Service
public class ProofOfDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(ProofOfDeliveryService.class);

    private final PontoEntregaRepository pontoEntregaRepository;
    private final MinioStorageService minioStorageService;

    public ProofOfDeliveryService(
            PontoEntregaRepository pontoEntregaRepository,
            MinioStorageService minioStorageService) {
        this.pontoEntregaRepository = pontoEntregaRepository;
        this.minioStorageService = minioStorageService;
    }

    /**
     * RN-ENT-001: Atualiza status do ponto de entrega com validações
     * 
     * Regras:
     * 1. ENTREGUE: obrigatório assinatura_path OU foto_entrega_path (para tipo ENTREGA)
     * 2. FALHOU: obrigatório campo ocorrência preenchido
     * 3. Arquivos validados no MinIO
     */
    @Transactional
    public PontoEntrega atualizarStatus(AtualizarPontoEntregaRequest request) {
        log.info("📦 RF11: Atualizando ponto {} para status {}", 
                request.getPontoEntregaId(), request.getNovoStatus());

        // Buscar ponto de entrega
        PontoEntrega ponto = pontoEntregaRepository.findById(request.getPontoEntregaId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Ponto de entrega não encontrado: " + request.getPontoEntregaId()));

        StatusPontoEntrega novoStatus = request.getNovoStatus();

        // RN-ENT-001: Validações específicas por status
        switch (novoStatus) {
            case ENTREGUE:
                validarEntrega(ponto, request);
                atualizarDadosEntrega(ponto, request);
                break;

            case FALHOU:
                validarFalha(ponto, request);
                atualizarDadosFalha(ponto, request);
                break;

            case CHEGOU:
                atualizarDadosChegada(ponto, request);
                break;

            case PULADO:
                // Pular não requer validações especiais
                break;

            default:
                break;
        }

        // Atualizar status
        ponto.setStatus(novoStatus);
        ponto.setAtualizadoEm(LocalDateTime.now());

        PontoEntrega salvo = pontoEntregaRepository.save(ponto);
        log.info("✅ Ponto {} atualizado para status {}", salvo.getId(), salvo.getStatus());

        return salvo;
    }

    /**
     * RN-ENT-001: Valida regras para status ENTREGUE
     * - Para tipo ENTREGA: obrigatório assinatura OU foto
     * - Arquivos devem existir no MinIO
     */
    private void validarEntrega(PontoEntrega ponto, AtualizarPontoEntregaRequest request) {
        log.debug("🔍 Validando entrega para ponto {}", ponto.getId());

        // Só valida Proof of Delivery para tipo ENTREGA
        if (ponto.getTipo() != TipoPontoEntrega.ENTREGA) {
            log.debug("✅ Tipo {} não requer proof of delivery", ponto.getTipo());
            return;
        }

        // Verificar se tem assinatura OU foto
        String assinaturaPath = request.getAssinaturaPath();
        String fotoPath = request.getFotoEntregaPath();

        boolean temAssinatura = assinaturaPath != null && !assinaturaPath.trim().isEmpty();
        boolean temFoto = fotoPath != null && !fotoPath.trim().isEmpty();

        if (!temAssinatura && !temFoto) {
            log.error("❌ RN-ENT-001: Proof of Delivery obrigatório para ENTREGUE");
            throw new IllegalArgumentException(
                    "Proof of Delivery obrigatório: informe assinatura_path ou foto_entrega_path");
        }

        // Validar existência dos arquivos no MinIO
        if (temAssinatura) {
            validarArquivoMinIO(assinaturaPath, "assinatura");
        }
        if (temFoto) {
            validarArquivoMinIO(fotoPath, "foto de entrega");
        }

        log.debug("✅ Proof of Delivery validado para ponto {}", ponto.getId());
    }

    /**
     * RN-ENT-001: Valida regras para status FALHOU
     * - Obrigatório campo ocorrencia preenchido
     */
    private void validarFalha(PontoEntrega ponto, AtualizarPontoEntregaRequest request) {
        log.debug("🔍 Validando falha para ponto {}", ponto.getId());

        String ocorrencia = request.getOcorrencia();

        if (ocorrencia == null || ocorrencia.trim().isEmpty()) {
            log.error("❌ RN-ENT-001: Ocorrência obrigatória para status FALHOU");
            throw new IllegalArgumentException(
                    "Campo 'ocorrencia' é obrigatório ao marcar entrega como FALHOU");
        }

        if (ocorrencia.length() < 10) {
            log.error("❌ Ocorrência muito curta (mínimo 10 caracteres)");
            throw new IllegalArgumentException(
                    "Ocorrência deve ter no mínimo 10 caracteres");
        }

        log.debug("✅ Ocorrência validada para ponto {}", ponto.getId());
    }

    /**
     * Valida se arquivo existe no MinIO
     */
    private void validarArquivoMinIO(String objectName, String tipoArquivo) {
        if (!minioStorageService.arquivoExiste(objectName)) {
            log.error("❌ Arquivo de {} não encontrado no MinIO: {}", tipoArquivo, objectName);
            throw new IllegalArgumentException(
                    "Arquivo de " + tipoArquivo + " não encontrado: " + objectName);
        }
        log.debug("✅ Arquivo {} validado no MinIO: {}", tipoArquivo, objectName);
    }

    /**
     * Atualiza dados de entrega
     */
    private void atualizarDadosEntrega(PontoEntrega ponto, AtualizarPontoEntregaRequest request) {
        ponto.setDataEntrega(LocalDateTime.now());
        ponto.setAssinaturaPath(request.getAssinaturaPath());
        ponto.setFotoEntregaPath(request.getFotoEntregaPath());

        // Calcular tempo de permanência se houver data de chegada
        if (ponto.getDataChegada() != null && request.getTempoPermanenciaMin() == null) {
            int tempoMin = (int) java.time.Duration.between(
                    ponto.getDataChegada(), LocalDateTime.now()).toMinutes();
            ponto.setTempoPermanenciaMin(tempoMin);
        } else if (request.getTempoPermanenciaMin() != null) {
            ponto.setTempoPermanenciaMin(request.getTempoPermanenciaMin());
        }

        log.debug("📦 Dados de entrega atualizados para ponto {}", ponto.getId());
    }

    /**
     * Atualiza dados de falha
     */
    private void atualizarDadosFalha(PontoEntrega ponto, AtualizarPontoEntregaRequest request) {
        ponto.setOcorrencia(request.getOcorrencia());
        log.debug("⚠️ Dados de falha atualizados para ponto {}", ponto.getId());
    }

    /**
     * Atualiza dados de chegada
     */
    private void atualizarDadosChegada(PontoEntrega ponto, AtualizarPontoEntregaRequest request) {
        ponto.setDataChegada(LocalDateTime.now());

        if (request.getLatitudeChegada() != null) {
            // Validar se está dentro do raio permitido
            validarProximidade(ponto, request.getLatitudeChegada(), request.getLongitudeChegada());
        }

        log.debug("📍 Dados de chegada atualizados para ponto {}", ponto.getId());
    }

    /**
     * Valida se coordenada está dentro do raio permitido
     */
    private void validarProximidade(PontoEntrega ponto, Double latChegada, Double lonChegada) {
        if (ponto.getLatitude() == null || ponto.getLongitude() == null) {
            return; // Sem coordenadas de referência, não valida
        }

        double distancia = calcularDistanciaHaversine(
                ponto.getLatitude(), ponto.getLongitude(),
                latChegada, lonChegada
        );

        int raioMetros = ponto.getRaioMetros() != null ? ponto.getRaioMetros() : 50;

        if (distancia > raioMetros) {
            log.warn("⚠️ Motorista a {}m do ponto (raio permitido: {}m)", 
                    (int) distancia, raioMetros);
            // Não bloqueia, apenas loga (pode ser configurável)
        } else {
            log.debug("✅ Motorista dentro do raio permitido ({}m)", (int) distancia);
        }
    }

    /**
     * Calcula distância entre duas coordenadas (Haversine)
     */
    private double calcularDistanciaHaversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Raio da Terra em metros

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distância em metros
    }

    /**
     * Busca próximo ponto pendente da viagem
     */
    public Optional<PontoEntrega> buscarProximoPendente(Long viagemId) {
        return pontoEntregaRepository.findProximoPendente(viagemId);
    }

    /**
     * Lista todos os pontos de uma viagem
     */
    public List<PontoEntrega> listarPorViagem(Long viagemId) {
        return pontoEntregaRepository.findByViagemIdOrderByOrdemAsc(viagemId);
    }

    /**
     * Verifica compliance de Proof of Delivery para uma viagem
     * Retorna lista de entregas sem POD (non-compliance)
     */
    public List<PontoEntrega> verificarCompliance(Long viagemId) {
        return pontoEntregaRepository.findEntregasSemProofOfDelivery(viagemId);
    }

    /**
     * Verifica se todas as entregas da viagem estão concluídas
     */
    public boolean isViagemConcluida(Long viagemId) {
        long pendentes = pontoEntregaRepository.countByViagemIdAndStatus(viagemId, StatusPontoEntrega.PENDENTE);
        return pendentes == 0;
    }

    /**
     * Estatísticas da viagem
     */
    public EstatisticaEntrega calcularEstatisticas(Long viagemId) {
        long total = pontoEntregaRepository.countByViagemId(viagemId);
        long entregues = pontoEntregaRepository.countEntreguesByViagemId(viagemId);
        long falhos = pontoEntregaRepository.countFalhosByViagemId(viagemId);
        long semPod = pontoEntregaRepository.findEntregasSemProofOfDelivery(viagemId).size();

        return new EstatisticaEntrega(total, entregues, falhos, semPod);
    }

    /**
     * DTO interno para estatísticas
     */
    public record EstatisticaEntrega(long total, long entregues, long falhos, long semPod) {
        public double getTaxaSucesso() {
            return total > 0 ? (entregues * 100.0 / total) : 0.0;
        }
    }
}
