package com.app.telemetria.api.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.telemetria.domain.entity.DesvioRota;
//ADICIONE estes (pacote correto)
import com.app.telemetria.domain.exception.BusinessException;
import com.app.telemetria.domain.exception.ErrorCode;
import com.app.telemetria.domain.service.RotaService;
import com.app.telemetria.domain.service.VeiculoService;
import com.app.telemetria.infrastructure.persistence.DesvioRotaRepository;


@RestController
@RequestMapping("/api/v1/desvios")
public class DesvioRotaController {
    
    private static final Logger log = LoggerFactory.getLogger(DesvioRotaController.class);
    
    private final DesvioRotaRepository desvioRotaRepository;
    private final RotaService rotaService;
    private final VeiculoService veiculoService;
    
    public DesvioRotaController(
            DesvioRotaRepository desvioRotaRepository,
            RotaService rotaService,
            VeiculoService veiculoService) {
        this.desvioRotaRepository = desvioRotaRepository;
        this.rotaService = rotaService;
        this.veiculoService = veiculoService;
        log.info("✅ DesvioRotaController inicializado");
    }
    
    @GetMapping("/rota/{rotaId}")
    public List<DesvioRota> listarDesviosPorRota(@PathVariable Long rotaId) {
        log.info("🛣️ Listando desvios para rota ID: {}", rotaId);
        log.debug("🔍 Iniciando busca de desvios para rota {}", rotaId);
        
        try {
            log.debug("✅ Validando existência da rota {}", rotaId);
            rotaService.buscarPorId(rotaId);
            log.debug("✅ Rota {} encontrada", rotaId);
        } catch (Exception e) {
            log.error("❌ Rota {} não encontrada: {}", rotaId, e.getMessage());
            throw new BusinessException(ErrorCode.ROTA_NOT_FOUND, rotaId.toString());
        }
        
        log.debug("📊 Consultando desvios no repositório para rota {}", rotaId);
        List<DesvioRota> desvios = desvioRotaRepository.findByRotaIdOrderByDataHoraDesvioDesc(rotaId);
        
        log.debug("📊 Total de desvios encontrados: {}", desvios.size());
        
        if (desvios.isEmpty()) {
            log.warn("⚠️ Nenhum desvio encontrado para rota {}", rotaId);
            throw new BusinessException(ErrorCode.DESVIO_NOT_FOUND, 
                "Nenhum desvio encontrado para a rota " + rotaId);
        }
        
        log.info("✅ Retornando {} desvios para rota {}", desvios.size(), rotaId);
        log.trace("📝 Desvios: {}", desvios);
        return desvios;
    }
    
    @GetMapping("/ativos")
    public List<DesvioRota> listarDesviosAtivos() {
        log.info("🔴 Listando desvios ativos (não resolvidos)");
        log.debug("🔍 Consultando desvios ativos no repositório");
        
        List<DesvioRota> desvios = desvioRotaRepository.findByResolvidoFalse();
        
        log.debug("📊 Total de desvios ativos encontrados: {}", desvios.size());
        
        if (desvios.isEmpty()) {
            log.warn("⚠️ Nenhum desvio ativo encontrado");
            throw new BusinessException(ErrorCode.DESVIO_NOT_FOUND, 
                "Nenhum desvio ativo encontrado");
        }
        
        log.info("✅ Retornando {} desvios ativos", desvios.size());
        log.trace("📝 Desvios ativos: {}", desvios);
        return desvios;
    }
    
    @GetMapping("/veiculo/{veiculoId}")
    public List<DesvioRota> listarDesviosPorVeiculo(@PathVariable Long veiculoId) {
        log.info("🚛 Listando desvios para veículo ID: {}", veiculoId);
        log.debug("🔍 Iniciando busca de desvios para veículo {}", veiculoId);
        
        try {
            log.debug("✅ Validando existência do veículo {}", veiculoId);
            veiculoService.buscarPorId(veiculoId);
            log.debug("✅ Veículo {} encontrado", veiculoId);
        } catch (Exception e) {
            log.error("❌ Veículo {} não encontrado: {}", veiculoId, e.getMessage());
            throw new BusinessException(ErrorCode.VEICULO_NOT_FOUND, veiculoId.toString());
        }
        
        log.debug("📊 Consultando desvios no repositório para veículo {}", veiculoId);
        List<DesvioRota> desvios = desvioRotaRepository.findByVeiculoIdOrderByDataHoraDesvioDesc(veiculoId);
        
        log.debug("📊 Total de desvios encontrados para veículo {}: {}", veiculoId, desvios.size());
        
        if (desvios.isEmpty()) {
            log.warn("⚠️ Nenhum desvio encontrado para veículo {}", veiculoId);
            throw new BusinessException(ErrorCode.DESVIO_NOT_FOUND, 
                "Nenhum desvio encontrado para o veículo " + veiculoId);
        }
        
        log.info("✅ Retornando {} desvios para veículo {}", desvios.size(), veiculoId);
        log.trace("📝 Desvios: {}", desvios);
        return desvios;
    }
    
    @GetMapping("/{id}")
    public DesvioRota buscarDesvioPorId(@PathVariable Long id) {
        log.info("🔍 Buscando desvio por ID: {}", id);
        log.debug("🔍 Consultando desvio {} no repositório", id);
        
        DesvioRota desvio = desvioRotaRepository.findById(id)
            .orElseThrow(() -> {
                log.error("❌ Desvio {} não encontrado", id);
                return new BusinessException(ErrorCode.DESVIO_NOT_FOUND, id.toString());
            });
        
        log.info("✅ Desvio {} encontrado", id);
        log.debug("📝 Detalhes do desvio - Rota: {}, Veículo: {}, Data: {}, Resolvido: {}", 
                 desvio.getRotaId(), desvio.getVeiculoId(), 
                 desvio.getDataHoraDesvio(), desvio.isResolvido());
        log.trace("📝 Desvio completo: {}", desvio);
        
        return desvio;
    }
    
    @PutMapping("/{id}/resolver")
    public DesvioRota resolverDesvio(@PathVariable Long id) {
        log.info("✅ Resolvendo desvio ID: {}", id);
        log.debug("🔍 Buscando desvio {} para resolução", id);
        
        DesvioRota desvio = desvioRotaRepository.findById(id)
            .orElseThrow(() -> {
                log.error("❌ Desvio {} não encontrado para resolução", id);
                return new BusinessException(ErrorCode.DESVIO_NOT_FOUND, id.toString());
            });
        
        log.debug("📝 Desvio encontrado - Rota: {}, Veículo: {}, Data desvio: {}, Resolvido atual: {}", 
                 desvio.getRotaId(), desvio.getVeiculoId(), 
                 desvio.getDataHoraDesvio(), desvio.isResolvido());
        
        if (desvio.isResolvido()) {
            log.warn("⚠️ Desvio {} já estava resolvido", id);
        }
        
        desvio.setResolvido(true);
        desvio.setDataHoraRetorno(java.time.LocalDateTime.now());
        
        log.debug("💾 Salvando desvio {} com resolução em {}", id, desvio.getDataHoraRetorno());
        DesvioRota desvioSalvo = desvioRotaRepository.save(desvio);
        
        log.info("✅ Desvio {} resolvido com sucesso", id);
        log.debug("📝 Desvio após resolução - Data retorno: {}, Resolvido: {}", 
                 desvioSalvo.getDataHoraRetorno(), desvioSalvo.isResolvido());
        
        return desvioSalvo;
    }
}