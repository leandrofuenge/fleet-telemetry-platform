package com.telemetria.api.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.api.dto.request.AtualizarPontoEntregaRequest;
import com.telemetria.domain.entity.PontoEntrega;
import com.telemetria.domain.service.ProofOfDeliveryService;
import com.telemetria.infrastructure.persistence.PontoEntregaRepository;

import jakarta.validation.Valid;

/**
 * RF11 — REST Controller para Pontos de Entrega e Proof of Delivery
 */
@RestController
@RequestMapping("/api/v1/pontos-entrega")
public class PontoEntregaController {

    private static final Logger log = LoggerFactory.getLogger(PontoEntregaController.class);

    private final ProofOfDeliveryService proofOfDeliveryService;
    private final PontoEntregaRepository pontoEntregaRepository;

    public PontoEntregaController(
            ProofOfDeliveryService proofOfDeliveryService,
            PontoEntregaRepository pontoEntregaRepository) {
        this.proofOfDeliveryService = proofOfDeliveryService;
        this.pontoEntregaRepository = pontoEntregaRepository;
    }

    /**
     * RF11: Lista todos os pontos de uma viagem
     */
    @GetMapping("/viagem/{viagemId}")
    public ResponseEntity<List<PontoEntregaResponse>> listarPorViagem(@PathVariable Long viagemId) {
        log.info("📦 Listando pontos de entrega da viagem {}", viagemId);
        
        List<PontoEntrega> pontos = proofOfDeliveryService.listarPorViagem(viagemId);
        List<PontoEntregaResponse> response = pontos.stream()
                .map(this::toResponse)
                .toList();
        
        return ResponseEntity.ok(response);
    }

    /**
     * RF11 RN-ENT-001: Atualiza status do ponto de entrega
     * - ENTREGUE: obrigatório assinatura_path ou foto_entrega_path
     * - FALHOU: obrigatório ocorrência
     */
    @PostMapping("/atualizar-status")
    public ResponseEntity<PontoEntregaResponse> atualizarStatus(
            @Valid @RequestBody AtualizarPontoEntregaRequest request) {
        
        log.info("🔄 Atualizando status do ponto {} para {}", 
                request.getPontoEntregaId(), request.getNovoStatus());
        
        PontoEntrega atualizado = proofOfDeliveryService.atualizarStatus(request);
        
        return ResponseEntity.ok(toResponse(atualizado));
    }

    /**
     * RF11: Busca próximo ponto pendente da viagem
     */
    @GetMapping("/viagem/{viagemId}/proximo-pendente")
    public ResponseEntity<PontoEntregaResponse> buscarProximoPendente(@PathVariable Long viagemId) {
        log.info("🔍 Buscando próximo ponto pendente da viagem {}", viagemId);
        
        return proofOfDeliveryService.buscarProximoPendente(viagemId)
                .map(p -> ResponseEntity.ok(toResponse(p)))
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * RF11: Verifica compliance de Proof of Delivery
     * Retorna entregas sem POD (non-compliance)
     */
    @GetMapping("/viagem/{viagemId}/compliance")
    public ResponseEntity<ComplianceResponse> verificarCompliance(@PathVariable Long viagemId) {
        log.info("🔍 Verificando compliance de POD para viagem {}", viagemId);
        
        List<PontoEntrega> semPod = proofOfDeliveryService.verificarCompliance(viagemId);
        boolean complianceOk = semPod.isEmpty();
        
        ComplianceResponse response = new ComplianceResponse(
                complianceOk,
                semPod.size(),
                semPod.stream().map(this::toResponse).toList()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * RF11: Estatísticas de entrega da viagem
     */
    @GetMapping("/viagem/{viagemId}/estatisticas")
    public ResponseEntity<EstatisticasResponse> obterEstatisticas(@PathVariable Long viagemId) {
        log.info("📊 Obtendo estatísticas da viagem {}", viagemId);
        
        ProofOfDeliveryService.EstatisticaEntrega stats = 
                proofOfDeliveryService.calcularEstatisticas(viagemId);
        
        EstatisticasResponse response = new EstatisticasResponse(
                stats.total(),
                stats.entregues(),
                stats.falhos(),
                stats.semPod(),
                stats.getTaxaSucesso()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * RF11: Verifica se viagem está concluída
     */
    @GetMapping("/viagem/{viagemId}/concluida")
    public ResponseEntity<Boolean> isViagemConcluida(@PathVariable Long viagemId) {
        boolean concluida = proofOfDeliveryService.isViagemConcluida(viagemId);
        return ResponseEntity.ok(concluida);
    }

    /**
     * Converte entidade para DTO de resposta
     */
    private PontoEntregaResponse toResponse(PontoEntrega p) {
        return new PontoEntregaResponse(
                p.getId(),
                p.getViagemId(),
                p.getOrdem(),
                p.getTipo(),
                p.getStatus(),
                p.getNomeDestinatario(),
                p.getEndereco(),
                p.getLatitude(),
                p.getLongitude(),
                p.getRaioMetros(),
                p.getAssinaturaPath(),
                p.getFotoEntregaPath(),
                p.getOcorrencia(),
                p.getDataChegada(),
                p.getDataEntrega(),
                p.getTempoPermanenciaMin(),
                p.isEntrega(), // helper method
                p.isEntregue() // helper method
        );
    }

    // ================================
    // DTOs de Resposta
    // ================================

    public record PontoEntregaResponse(
            Long id,
            Long viagemId,
            Integer ordem,
            com.telemetria.domain.enums.TipoPontoEntrega tipo,
            com.telemetria.domain.enums.StatusPontoEntrega status,
            String nomeDestinatario,
            String endereco,
            Double latitude,
            Double longitude,
            Integer raioMetros,
            String assinaturaPath,
            String fotoEntregaPath,
            String ocorrencia,
            java.time.LocalDateTime dataChegada,
            java.time.LocalDateTime dataEntrega,
            Integer tempoPermanenciaMin,
            boolean isEntrega,
            boolean isEntregue
    ) {}

    public record ComplianceResponse(
            boolean complianceOk,
            int quantidadeSemPod,
            List<PontoEntregaResponse> pontosSemPod
    ) {}

    public record EstatisticasResponse(
            long total,
            long entregues,
            long falhos,
            long semPod,
            double taxaSucesso
    ) {}
}
