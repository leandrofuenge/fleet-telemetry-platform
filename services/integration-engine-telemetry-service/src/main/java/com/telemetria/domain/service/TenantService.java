package com.telemetria.domain.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.Tenant;
import com.telemetria.domain.enums.StatusTenant;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.infrastructure.persistence.TenantRepository;
import com.telemetria.infrastructure.persistence.VeiculoRepository;

@Service
public class TenantService {
    private final TenantRepository tenantRepository;
    private final VeiculoRepository veiculoRepository;

    public TenantService(TenantRepository tenantRepository, VeiculoRepository veiculoRepository) {
        this.tenantRepository = tenantRepository;
        this.veiculoRepository = veiculoRepository;
    }

    @Transactional
    public Tenant criar(Tenant tenant) {
        String cnpj = DocumentoValidator.somenteDigitos(tenant.getCnpj());
        if (!DocumentoValidator.cnpjValido(cnpj)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "CNPJ inválido. Verifique os dígitos verificadores.");
        }
        tenant.setCnpj(cnpj);
        tenant.setStatus(StatusTenant.TRIAL);
        java.time.LocalDate hoje = java.time.LocalDate.now();
        tenant.setTrialInicio(hoje);
        tenant.setTrialExpiraEm(hoje.plusDays(14));
        if (tenant.getPlano() == null) {
            tenant.setPlano(com.telemetria.domain.enums.PlanoTenant.STARTER);
        }
        tenantRepository.findByCnpj(cnpj).ifPresent(existente -> {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "CNPJ já cadastrado. Transportadora: " + existente.getNomeRazaoSocial());
        });
        if (tenant.getNomeRazaoSocial() == null || tenant.getNomeRazaoSocial().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Nome ou razão social é obrigatório.");
        }
        return tenantRepository.save(tenant);
    }

    @Transactional(readOnly = true)
    public Tenant buscarObrigatorio(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_ERROR, "Tenant não encontrado."));
    }

    @Transactional
    public void validarAcesso(Long tenantId) {
        Tenant tenant = buscarObrigatorio(tenantId);
        tenant.expirarTrialSeNecessario();
        if (tenant.getStatus() == StatusTenant.BLOQUEADO) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "Tenant bloqueado.");
        }
        if (!tenant.estaAtivoParaOperacao()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "Tenant inativo ou com trial expirado.");
        }
        tenantRepository.save(tenant);
    }

    @Transactional
    public void validarLimiteDeVeiculos(Long tenantId) {
        validarAcesso(tenantId);
        Tenant tenant = buscarObrigatorio(tenantId);
        long cadastrados = veiculoRepository.countByTenantId(tenantId);
        if (cadastrados >= tenant.getPlano().getMaxVeiculos()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Limite de veículos do plano " + tenant.getPlano() + " atingido.");
        }
    }

    @Transactional
    public int expirarTrials() {
        List<Tenant> emTrial = tenantRepository.findByStatus(StatusTenant.TRIAL);
        int expirados = 0;
        for (Tenant tenant : emTrial) {
            StatusTenant antes = tenant.getStatus();
            tenant.expirarTrialSeNecessario();
            if (antes != tenant.getStatus()) expirados++;
        }
        tenantRepository.saveAll(emTrial);
        return expirados;
    }
}
