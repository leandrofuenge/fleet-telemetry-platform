package com.telemetria.domain.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.Cte;
import com.telemetria.domain.entity.Mdfe;
import com.telemetria.domain.entity.Viagem;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.infrastructure.persistence.CiotRepository;
import com.telemetria.infrastructure.persistence.CteRepository;
import com.telemetria.infrastructure.persistence.MdfeRepository;
import com.telemetria.infrastructure.persistence.RntrcConsultaRepository;

@Service
public class FiscalService {
    private final MdfeRepository mdfeRepository;
    private final CteRepository cteRepository;
    @SuppressWarnings("unused")
    private final CiotRepository ciotRepository;
    private final RntrcConsultaRepository rntrcRepository;

    public FiscalService(
            MdfeRepository mdfeRepository,
            CteRepository cteRepository,
            CiotRepository ciotRepository,
            RntrcConsultaRepository rntrcRepository) {
        this.mdfeRepository = mdfeRepository;
        this.cteRepository = cteRepository;
        this.ciotRepository = ciotRepository;
        this.rntrcRepository = rntrcRepository;
    }

    @Transactional
    public Mdfe salvarMdfe(Mdfe mdfe) {
        if (mdfe.getTenantId() == null || mdfe.getViagemId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Tenant e viagem são obrigatórios no MDF-e");
        }
        if (mdfe.getChaveMdfe() != null && !chaveValida(mdfe.getChaveMdfe())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Chave MDF-e deve possuir 44 dígitos");
        }
        return mdfeRepository.save(mdfe);
    }

    @Transactional
    public Cte salvarCte(Cte cte) {
        if (!chaveValida(cte.getChaveCte())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Chave CT-e deve possuir 44 dígitos");
        }
        return cteRepository.save(cte);
    }

    public void validarInicio(Viagem viagem) {
        boolean exigeMdfe = viagem.getRota() != null
                && viagem.getRota().getOrigem() != null
                && viagem.getRota().getDestino() != null
                && !viagem.getRota().getOrigem().equalsIgnoreCase(viagem.getRota().getDestino());

        if (exigeMdfe && mdfeRepository.findByViagemIdAndStatus(viagem.getId(), "AUTORIZADO").isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "MDF-e obrigatório para esta rota");
        }
        if (viagem.getCarga() != null
                && viagem.getCarga().getCteChave() == null
                && viagem.getCarga().getNfeChave() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Carga deve possuir CT-e ou NF-e vinculado");
        }
        if (viagem.getCarga() != null
                && viagem.getCarga().getCteChave() == null
                && viagem.getCarga().getPeso() != null
                && viagem.getCarga().getPeso() > 5000) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "Carga acima de R$ 5.000,00 sem CT-e não pode iniciar viagem");
        }
    }

    @Transactional
    public void encerrarMdfe(Long viagemId) {
        mdfeRepository.findByViagemIdAndStatus(viagemId, "AUTORIZADO").ifPresent(mdfe -> {
            mdfe.setStatus("ENCERRADO");
            mdfe.setDataEncerramento(LocalDateTime.now());
            mdfeRepository.save(mdfe);
        });
    }

    public boolean rntrcRegular(String rntrc) {
        Optional<com.telemetria.domain.entity.RntrcConsulta> consulta =
                rntrcRepository.findTopByRntrcOrderByDataConsultaDesc(rntrc);
        if (consulta.isEmpty() || !consulta.get().getExpiraEm().isAfter(LocalDateTime.now())) {
            return true;
        }
        return "REGULAR".equals(consulta.get().getSituacao());
    }

    private boolean chaveValida(String chave) {
        return chave != null && chave.matches("\\d{44}");
    }
}
