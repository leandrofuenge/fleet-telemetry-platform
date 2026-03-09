package com.app.telemetria.controller;

import com.app.telemetria.entity.DesvioRota;
import com.app.telemetria.repository.DesvioRotaRepository;
import com.app.telemetria.service.RotaService;
import com.app.telemetria.service.VeiculoService;
import com.app.telemetria.exception.ErrorCode;
import com.app.telemetria.exception.BusinessException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/desvios")
public class DesvioRotaController {
    
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
    }
    
    @GetMapping("/rota/{rotaId}")
    public List<DesvioRota> listarDesviosPorRota(@PathVariable Long rotaId) {
        try {
            rotaService.buscarPorId(rotaId);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.ROTA_NOT_FOUND, rotaId.toString());
        }
        
        List<DesvioRota> desvios = desvioRotaRepository.findByRotaIdOrderByDataHoraDesvioDesc(rotaId);
        
        if (desvios.isEmpty()) {
            throw new BusinessException(ErrorCode.DESVIO_NOT_FOUND, 
                "Nenhum desvio encontrado para a rota " + rotaId);
        }
        
        return desvios;
    }
    
    @GetMapping("/ativos")
    public List<DesvioRota> listarDesviosAtivos() {
        List<DesvioRota> desvios = desvioRotaRepository.findByResolvidoFalse();
        
        if (desvios.isEmpty()) {
            throw new BusinessException(ErrorCode.DESVIO_NOT_FOUND, 
                "Nenhum desvio ativo encontrado");
        }
        
        return desvios;
    }
    
    @GetMapping("/veiculo/{veiculoId}")
    public List<DesvioRota> listarDesviosPorVeiculo(@PathVariable Long veiculoId) {
        try {
            veiculoService.buscarPorId(veiculoId);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VEICULO_NOT_FOUND, veiculoId.toString());
        }
        
        List<DesvioRota> desvios = desvioRotaRepository.findByVeiculoIdOrderByDataHoraDesvioDesc(veiculoId);
        
        if (desvios.isEmpty()) {
            throw new BusinessException(ErrorCode.DESVIO_NOT_FOUND, 
                "Nenhum desvio encontrado para o veículo " + veiculoId);
        }
        
        return desvios;
    }
    

    @GetMapping("/{id}")
    public DesvioRota buscarDesvioPorId(@PathVariable Long id) {
        return desvioRotaRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.DESVIO_NOT_FOUND, id.toString()));
    }
    

    @PutMapping("/{id}/resolver")
    public DesvioRota resolverDesvio(@PathVariable Long id) {
        DesvioRota desvio = desvioRotaRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.DESVIO_NOT_FOUND, id.toString()));
        
        desvio.setResolvido(true);
        desvio.setDataHoraRetorno(java.time.LocalDateTime.now());
        
        return desvioRotaRepository.save(desvio);
    }
}