package com.telemetria.domain.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.DispositivoIot;
import com.telemetria.domain.entity.PareamentoDispositivo;
import com.telemetria.domain.enums.StatusDispositivo;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.infrastructure.persistence.DispositivoIotRepository;
import com.telemetria.infrastructure.persistence.PareamentoDispositivoRepository;
import com.telemetria.util.Base32CodeGenerator;

@Service
public class PareamentoDispositivoService {
    private static final int TAMANHO_CODIGO = 16;
    private static final int EXPIRACAO_PADRAO_MINUTOS = 10;
    private static final int EXPIRACAO_MAXIMA_MINUTOS = 60;

    private final PareamentoDispositivoRepository pareamentos;
    private final DispositivoIotRepository dispositivos;

    public PareamentoDispositivoService(
            PareamentoDispositivoRepository pareamentos,
            DispositivoIotRepository dispositivos) {
        this.pareamentos = pareamentos;
        this.dispositivos = dispositivos;
    }

    @Transactional
    public CodigoPareamento criar(Long tenantId, Long veiculoId, Integer expiraEmMinutos) {
        if (tenantId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "O tenant é obrigatório para o pareamento.");
        }
        int minutos = expiraEmMinutos == null ? EXPIRACAO_PADRAO_MINUTOS : expiraEmMinutos;
        if (minutos < 1 || minutos > EXPIRACAO_MAXIMA_MINUTOS) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "A expiração deve estar entre 1 e 60 minutos.");
        }

        String codigo = gerarCodigoUnico();
        PareamentoDispositivo pareamento = new PareamentoDispositivo();
        pareamento.setCodigoHash(hash(codigo));
        pareamento.setTenantId(tenantId);
        pareamento.setVeiculoId(veiculoId);
        pareamento.setExpiraEm(LocalDateTime.now().plusMinutes(minutos));
        pareamentos.save(pareamento);
        return new CodigoPareamento(Base32CodeGenerator.formatarParaExibicao(codigo), pareamento.getExpiraEm());
    }

    @Transactional
    public DispositivoIot consumir(String deviceId, String codigo) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "O deviceId é obrigatório.");
        }
        String codigoNormalizado = Base32CodeGenerator.normalizar(codigo);
        if (!codigoNormalizado.matches("[A-Z2-7]{" + TAMANHO_CODIGO + "}")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Código de pareamento Base32 inválido.");
        }

        PareamentoDispositivo pareamento = pareamentos.findByCodigoHash(hash(codigoNormalizado))
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN, "Código de pareamento inválido."));
        if (pareamento.getConsumidoEm() != null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Código de pareamento já foi utilizado.");
        }
        if (!pareamento.getExpiraEm().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Código de pareamento expirado.");
        }

        DispositivoIot dispositivo = dispositivos.findByDeviceId(deviceId.trim()).orElseGet(DispositivoIot::new);
        if (dispositivo.getId() != null && dispositivo.getStatus() == StatusDispositivo.ATIVO) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Dispositivo ativo não pode ser pareado novamente.");
        }
        if (dispositivo.getTenantId() != null && !pareamento.getTenantId().equals(dispositivo.getTenantId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Dispositivo pertence a outro tenant.");
        }

        dispositivo.setDeviceId(deviceId.trim());
        dispositivo.setTenantId(pareamento.getTenantId());
        dispositivo.setVeiculoId(pareamento.getVeiculoId());
        dispositivo.setStatus(StatusDispositivo.PENDENTE);
        dispositivo = dispositivos.save(dispositivo);

        pareamento.setConsumidoEm(LocalDateTime.now());
        pareamento.setDeviceIdConsumidor(dispositivo.getDeviceId());
        pareamentos.save(pareamento);
        return dispositivo;
    }

    private String gerarCodigoUnico() {
        for (int tentativa = 0; tentativa < 5; tentativa++) {
            String codigo = Base32CodeGenerator.gerar(TAMANHO_CODIGO);
            if (!pareamentos.existsByCodigoHash(hash(codigo))) return codigo;
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Não foi possível gerar código de pareamento único.");
    }

    private String hash(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            StringBuilder resultado = new StringBuilder(64);
            for (byte value : digest) resultado.append(String.format("%02x", value));
            return resultado.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível proteger o código de pareamento.", exception);
        }
    }

    public record CodigoPareamento(String codigo, LocalDateTime expiraEm) {
    }
}
'