package com.telemetria.domain.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.telemetria.domain.entity.PontoRota;
import com.telemetria.domain.entity.Rota;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.domain.exception.RotaDuplicateException;
import com.telemetria.domain.exception.RotaNotFoundException;
import com.telemetria.domain.exception.RotaValidationException;
import com.telemetria.infrastructure.persistence.RotaRepository;
import com.telemetria.util.DistanciaCalculator;

@Service
public class RotaService {

    private static final Logger log = LoggerFactory.getLogger(RotaService.class);

    @Autowired
    private RotaRepository rotaRepository;

    @Autowired
    private DistanciaCalculator distanciaCalculator;  // ← INJEÇÃO ADICIONADA
    
    // ================ MÉTODOS CRUD ================

    /**
     * Lista todas as rotas
     */
    public List<Rota> listar() {
        log.info("📋 Listando todas as rotas");
        log.debug("🔍 Consultando repositório...");
        
        List<Rota> rotas = rotaRepository.findAll();
        
        log.info("✅ Total de rotas encontradas: {}", rotas.size());
        log.debug("📊 IDs das rotas: {}", rotas.stream().map(Rota::getId).toList());
        
        return rotas;
    }

    /**
     * Busca uma rota por ID
     */
    public Rota buscarPorId(Long id) {
        log.info("🔍 Buscando rota por ID: {}", id);
        log.debug("🔍 Consultando repositório para rota ID {}", id);
        
        Rota rota = rotaRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("❌ Rota não encontrada com ID: {}", id);
                    return new RotaNotFoundException("Rota não encontrada com ID: " + id);
                });

        log.info("✅ Rota encontrada - Origem: {}, Destino: {}", rota.getOrigem(), rota.getDestino());
        log.debug("📝 Rota completa: {}", rota);
        
        return rota;
    }

    /**
     * Salva uma nova rota
     */
    public Rota salvar(Rota rota) {
        log.info("➕ Salvando nova rota - Nome: {}, Origem: {}, Destino: {}", 
                rota.getNome(), rota.getOrigem(), rota.getDestino());
        
        // Validações básicas
        if (rota.getNome() == null || rota.getNome().trim().isEmpty()) {
            log.error("❌ Nome da rota é obrigatório");
            throw new RotaValidationException("Nome da rota é obrigatório");
        }
        
        if (rota.getOrigem() == null || rota.getOrigem().trim().isEmpty()) {
            log.error("❌ Origem da rota é obrigatória");
            throw new RotaValidationException("Origem da rota é obrigatória");
        }

        if (rota.getDestino() == null || rota.getDestino().trim().isEmpty()) {
            log.error("❌ Destino da rota é obrigatório");
            throw new RotaValidationException("Destino da rota é obrigatório");
        }

        // RN-ROT-001: Rota não pode ser ativada sem cálculo OSRM.
        // distanciaPrevista e pontosRota são preenchidos pelo cálculo OSRM.
        if (Boolean.TRUE.equals(rota.getAtiva())) {
            validarCalculoOsrmRealizado(rota);
        }
        
        // Verifica duplicidade
        Long quantidadeComMesmoNome = rotaRepository.countByNome(rota.getNome());

        if (quantidadeComMesmoNome != null && quantidadeComMesmoNome > 0) {
            log.error("❌ Já existe uma rota com o nome: {}", rota.getNome());
            throw new RotaDuplicateException("Rota com nome '" + rota.getNome() + "' já existe");
        }
        
        Rota rotaSalva = rotaRepository.save(rota);
        log.info("✅ Rota salva com ID: {}", rotaSalva.getId());
        return rotaSalva;
    }

	/**
     * Atualiza uma rota existente
     */
    public Rota atualizar(Long id, Rota dados) {
        log.info("🔄 Atualizando rota ID: {}", id);
        log.debug("📝 Dados para atualização: {}", dados);
        
        Rota rota = rotaRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("❌ Rota não encontrada com ID: {} para atualização", id);
                    return new RotaNotFoundException("Rota não encontrada com ID: " + id);
                });

        log.debug("📝 Rota original: {}", rota);

        // Atualiza campos
        boolean modificado = false;
        
        if (dados.getNome() != null && !dados.getNome().trim().isEmpty() && !dados.getNome().equals(rota.getNome())) {
            log.debug("📝 Atualizando nome: '{}' → '{}'", rota.getNome(), dados.getNome());
            
            // Verifica duplicidade do novo nome (SOLUÇÃO 2)
            Long quantidadeComMesmoNome = rotaRepository.countByNomeAndIdNot(dados.getNome(), id);

            if (quantidadeComMesmoNome != null && quantidadeComMesmoNome > 0) {
                log.error("❌ Já existe outra rota com o nome: {}", dados.getNome());
                throw new RotaDuplicateException("Rota com nome '" + dados.getNome() + "' já existe");
            }
            
            rota.setNome(dados.getNome());
            modificado = true;
        }
        
        if (dados.getOrigem() != null && !dados.getOrigem().trim().isEmpty() && !dados.getOrigem().equals(rota.getOrigem())) {
            log.debug("📝 Atualizando origem: '{}' → '{}'", rota.getOrigem(), dados.getOrigem());
            rota.setOrigem(dados.getOrigem());
            modificado = true;
        }
        
        if (dados.getDestino() != null && !dados.getDestino().trim().isEmpty() && !dados.getDestino().equals(rota.getDestino())) {
            log.debug("📝 Atualizando destino: '{}' → '{}'", rota.getDestino(), dados.getDestino());
            rota.setDestino(dados.getDestino());
            modificado = true;
        }
        
        if (dados.getLatitudeOrigem() != null && !dados.getLatitudeOrigem().equals(rota.getLatitudeOrigem())) {
            rota.setLatitudeOrigem(dados.getLatitudeOrigem());
            modificado = true;
        }
        
        if (dados.getLongitudeOrigem() != null && !dados.getLongitudeOrigem().equals(rota.getLongitudeOrigem())) {
            rota.setLongitudeOrigem(dados.getLongitudeOrigem());
            modificado = true;
        }
        
        if (dados.getLatitudeDestino() != null && !dados.getLatitudeDestino().equals(rota.getLatitudeDestino())) {
            rota.setLatitudeDestino(dados.getLatitudeDestino());
            modificado = true;
        }
        
        if (dados.getLongitudeDestino() != null && !dados.getLongitudeDestino().equals(rota.getLongitudeDestino())) {
            rota.setLongitudeDestino(dados.getLongitudeDestino());
            modificado = true;
        }
        
        if (dados.getDistanciaPrevista() != null && !dados.getDistanciaPrevista().equals(rota.getDistanciaPrevista())) {
            log.debug("📝 Atualizando distância prevista: {} → {}", 
                     rota.getDistanciaPrevista(), dados.getDistanciaPrevista());
            rota.setDistanciaPrevista(dados.getDistanciaPrevista());
            modificado = true;
        }
        
        if (dados.getTempoPrevisto() != null && !dados.getTempoPrevisto().equals(rota.getTempoPrevisto())) {
            log.debug("📝 Atualizando tempo previsto: {} → {}", 
                     rota.getTempoPrevisto(), dados.getTempoPrevisto());
            rota.setTempoPrevisto(dados.getTempoPrevisto());
            modificado = true;
        }
        
        if (dados.getStatus() != null && !dados.getStatus().equals(rota.getStatus())) {
            log.debug("📝 Atualizando status: '{}' → '{}'", rota.getStatus(), dados.getStatus());
            rota.setStatus(dados.getStatus());
            modificado = true;
        }
        
        if (dados.getAtiva() != null && !dados.getAtiva().equals(rota.getAtiva())) {
            // RN-ROT-001: Se está sendo ativada, garantir que o cálculo OSRM já foi feito.
            if (Boolean.TRUE.equals(dados.getAtiva())) {
                // Validar contra os dados que serão salvos (mescla atual + novos valores)
                Rota rotaParaValidar = new Rota();
                rotaParaValidar.setDistanciaPrevista(
                        dados.getDistanciaPrevista() != null ? dados.getDistanciaPrevista() : rota.getDistanciaPrevista());
                rotaParaValidar.setPontosRota(
                        dados.getPontosRota() != null ? dados.getPontosRota() : rota.getPontosRota());
                validarCalculoOsrmRealizado(rotaParaValidar);
            }
            log.debug("📝 Atualizando ativa: {} → {}", rota.getAtiva(), dados.getAtiva());
            rota.setAtiva(dados.getAtiva());
            modificado = true;
        }

        if (!modificado) {
            log.debug("ℹ️ Nenhuma modificação detectada para rota {}", id);
        } else {
            log.debug("📝 Rota após alterações, pronta para salvar: {}", rota);
        }
        
        Rota rotaAtualizada = rotaRepository.save(rota);
        
        log.info("✅ Rota {} atualizada com sucesso", id);
        log.debug("📝 Rota após atualização: {}", rotaAtualizada);
        
        return rotaAtualizada;
    }

    /**
     * Deleta uma rota por ID
     */
    public void deletar(Long id) {
        log.info("🗑️ Deletando rota ID: {}", id);
        
        if (!rotaRepository.existsById(id)) {
            log.error("❌ Rota não encontrada com ID: {} para deleção", id);
            throw new RotaNotFoundException("Rota não encontrada com ID: " + id);
        }
        
        rotaRepository.deleteById(id);
        log.info("✅ Rota {} deletada com sucesso", id);
    }

    // ================ MÉTODOS DE BUSCA ================

    /**
     * Busca rotas por status
     */
    public List<Rota> buscarPorStatus(String status) {
        log.info("🔍 Buscando rotas com status: {}", status);
        
        List<Rota> rotas = rotaRepository.findByStatus(status);
        
        log.info("✅ Encontradas {} rotas com status {}", rotas.size(), status);
        log.debug("📊 IDs: {}", rotas.stream().map(Rota::getId).toList());
        
        return rotas;
    }

    /**
     * Busca rotas ativas
     */
    public List<Rota> buscarAtivas() {
        log.info("🔍 Buscando rotas ativas");
        
        List<Rota> rotas = rotaRepository.findByAtivaTrue();
        
        log.info("✅ Encontradas {} rotas ativas", rotas.size());
        log.debug("📊 IDs: {}", rotas.stream().map(Rota::getId).toList());
        
        return rotas;
    }

    /**
     * Busca rotas por veículo
     */
    public List<Rota> buscarPorVeiculo(Long veiculoId) {
        log.info("🔍 Buscando rotas para veículo ID: {}", veiculoId);
        
        List<Rota> rotas = rotaRepository.findByVeiculoId(veiculoId);
        
        log.info("✅ Encontradas {} rotas para veículo {}", rotas.size(), veiculoId);
        
        return rotas;
    }

    /**
     * Busca rotas por motorista
     */
    public List<Rota> buscarPorMotorista(Long motoristaId) {
        log.info("🔍 Buscando rotas para motorista ID: {}", motoristaId);
        
        List<Rota> rotas = rotaRepository.findByMotoristaId(motoristaId);
        
        log.info("✅ Encontradas {} rotas para motorista {}", rotas.size(), motoristaId);
        
        return rotas;
    }
    
    
    
    private void validarCalculoOsrmRealizado(Rota rota) {
        // Verifica se a rota existe
        if (rota == null) {
            log.warn("Rota nula para validação OSRM");
            return;
        }
        
        // 1. Validar se os pontos da rota foram calculados pelo OSRM
        List<PontoRota> pontosRota = rota.getPontosRota();
        if (pontosRota == null || pontosRota.isEmpty()) {
            log.warn("Rota {} não possui pontos calculados pelo OSRM", rota.getId());
            return;
        }
        
        // 2. Verificar se os pontos têm coordenadas de snap (lat_snap, lng_snap)
        boolean temSnap = pontosRota.stream()
                .anyMatch(p -> p.getLatSnap() != null && p.getLngSnap() != null);
        
        if (!temSnap) {
            log.warn("Rota {} não possui pontos com snap-to-road OSRM", rota.getId());
            // Opcional: marcar rota como inconsistente
            rota.setStatus("INCONSISTENTE");
        }
        
        // 3. Validar distância prevista (deve ser > 0)
        if (rota.getDistanciaPrevista() == null || rota.getDistanciaPrevista() <= 0) {
            log.error("Rota {} com distância prevista inválida: {} km", 
                      rota.getId(), rota.getDistanciaPrevista());
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                String.format("Distância da rota %s não foi calculada corretamente pelo OSRM", rota.getNome()));
        }
        
        // 4. Validar tempo previsto (deve ser > 0)
        if (rota.getTempoPrevisto() == null || rota.getTempoPrevisto() <= 0) {
            log.error("Rota {} com tempo previsto inválido: {} minutos", 
                      rota.getId(), rota.getTempoPrevisto());
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                String.format("Tempo da rota %s não foi calculado corretamente pelo OSRM", rota.getNome()));
        }
        
        // 5. Validar origem e destino
        if (rota.getLatitudeOrigem() == null || rota.getLongitudeOrigem() == null) {
            log.error("Rota {} sem coordenadas de origem válidas", rota.getId());
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Coordenadas de origem não definidas");
        }
        
        if (rota.getLatitudeDestino() == null || rota.getLongitudeDestino() == null) {
            log.error("Rota {} sem coordenadas de destino válidas", rota.getId());
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Coordenadas de destino não definidas");
        }
        
        // 6. Verificar consistência entre distância prevista e distância calculada
        double distanciaCalculada = calcularDistanciaTotalRota(pontosRota);
        double distanciaPrevista = rota.getDistanciaPrevista();
        double diferencaPercentual = Math.abs(distanciaCalculada - distanciaPrevista) / distanciaPrevista * 100;
        
        if (diferencaPercentual > 10) { // tolerância de 10%
            log.warn("Rota {} com inconsistência de distância: prevista={}km, calculada={}km, diferença={:.1f}%",
                     rota.getId(), distanciaPrevista, distanciaCalculada, diferencaPercentual);
        }
        
        log.info("✅ Rota {} validada com sucesso: distância={}km, tempo={}min, pontos={}",
                 rota.getId(), rota.getDistanciaPrevista(), rota.getTempoPrevisto(), pontosRota.size());
    }

    /**
     * Calcula a distância total da rota baseada nos pontos OSRM
     */
    private double calcularDistanciaTotalRota(List<PontoRota> pontosRota) {
        if (pontosRota == null || pontosRota.size() < 2) {
            return 0.0;
        }
        
        double distanciaTotal = 0.0;
        for (int i = 0; i < pontosRota.size() - 1; i++) {
            PontoRota p1 = pontosRota.get(i);
            PontoRota p2 = pontosRota.get(i + 1);
            
            double lat1 = p1.getLatSnap() != null ? p1.getLatSnap() : p1.getLatitude();
            double lon1 = p1.getLngSnap() != null ? p1.getLngSnap() : p1.getLongitude();
            double lat2 = p2.getLatSnap() != null ? p2.getLatSnap() : p2.getLatitude();
            double lon2 = p2.getLngSnap() != null ? p2.getLngSnap() : p2.getLongitude();
            
            distanciaTotal += distanciaCalculator.calcularDistancia(lat1, lon1, lat2, lon2);
        }
        return distanciaTotal;
    }
}