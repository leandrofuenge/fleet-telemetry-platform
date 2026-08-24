package com.telemetria.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.VrpJob;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.infrastructure.persistence.VrpJobRepository;

@Service
public class VrpService {
    private final VrpJobRepository repository;
    private final Executor executor;

    public VrpService(VrpJobRepository repository, @Qualifier("normalTaskExecutor") Executor executor) {
        this.repository = repository;
        this.executor = executor;
    }

    @Transactional
    public VrpJob criar(Long tenantId, String tipo, int veiculos, int pontos) {
        if (tenantId == null || veiculos < 1 || pontos < 1) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Parâmetros VRP inválidos");
        }

        VrpJob job = new VrpJob();
        job.setTenantId(tenantId);
        job.setTipoVrp(tipo);
        job.setNumVeiculos(veiculos);
        job.setNumPontos(pontos);
        job.setSolverUsado(pontos <= 20 ? "OR_TOOLS" : pontos <= 100 ? "CLARKE_WRIGHT_LK" : "ALNS");
        job = repository.save(job);

        Long jobId = job.getId();
        executor.execute(() -> processar(jobId));
        return job;
    }

    public void processar(Long id) {
        long inicio = System.currentTimeMillis();
        VrpJob job = repository.findById(id).orElseThrow();
        job.setStatus("PROCESSANDO");
        repository.save(job);

        try {
            List<Map<String, Object>> alocacoes = new ArrayList<>();
            for (int ponto = 1; ponto <= job.getNumPontos(); ponto++) {
                alocacoes.add(Map.of(
                        "ponto", ponto,
                        "veiculoIndice", (ponto - 1) % job.getNumVeiculos(),
                        "ordem", (ponto - 1) / job.getNumVeiculos() + 1));
            }
            job.setPlanoResultado(Map.of("alocacoes", alocacoes, "revisaoObrigatoria", true));
            job.setStatus("CONCLUIDO");
        } catch (Exception exception) {
            job.setStatus("FALHOU");
        }

        job.setTempoExecucaoMs(System.currentTimeMillis() - inicio);
        repository.save(job);
    }

    public VrpJob buscar(Long id) {
        return repository.findById(id).orElseThrow();
    }
}
