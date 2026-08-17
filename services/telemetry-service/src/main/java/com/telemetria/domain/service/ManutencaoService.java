package com.telemetria.domain.service; import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.Manutencao;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.infrastructure.persistence.ManutencaoRepository;
@Service public class ManutencaoService { private final ManutencaoRepository r; public ManutencaoService(ManutencaoRepository r){this.r=r;}
 @Transactional public Manutencao salvar(Manutencao m){ if(m.getVeiculo()==null||m.getTipo()==null) throw new BusinessException(ErrorCode.VALIDATION_ERROR,"Veículo e tipo de manutenção são obrigatórios"); double total=(m.getCustoPecas()==null?0:m.getCustoPecas())+(m.getCustoMaoObra()==null?0:m.getCustoMaoObra()); m.setCusto(total); if("CORRETIVA".equalsIgnoreCase(m.getTipo())&&total>500&&(m.getNotaFiscalPath()==null||m.getNotaFiscalPath().isBlank())) throw new BusinessException(ErrorCode.VALIDATION_ERROR,"Nota fiscal é obrigatória para manutenção corretiva acima de R$ 500,00"); return r.save(m); }
 public boolean bloqueiaViagem(Long veiculoId){return !r.findByVeiculoIdAndStatus(veiculoId,"AGENDADA").isEmpty() && r.findByVeiculoIdAndStatus(veiculoId,"AGENDADA").stream().anyMatch(m->m.getDataAgendada()!=null&&m.getDataAgendada().isBefore(LocalDate.now().minusDays(7)));}
 @Transactional public Manutencao registrarPredicao(Manutencao m,double anomaly,int rul,double prob){m.setTipo("PREDITIVA");m.setAnomalyScore(anomaly);m.setRulDiasEstimado(rul);m.setProbabilidadeFalha(prob);m.setStatus(anomaly>=85||prob>=.80?"CRITICO":"ATENCAO");if(rul<=7)m.setDataAgendada(LocalDate.now().plusDays(Math.max(0,rul)));return salvar(m);}
}
