package com.telemetria.domain.service; import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.Abastecimento;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.infrastructure.persistence.AbastecimentoRepository;
@Service public class AbastecimentoService {private final AbastecimentoRepository r; public AbastecimentoService(AbastecimentoRepository r){this.r=r;}
 @Transactional public Abastecimento registrar(Abastecimento a){if(a.getTenantId()==null||a.getVeiculoId()==null||a.getDataHora()==null||a.getLitros()==null||a.getLitros()<=0||a.getValorTotal()==null||a.getValorTotal()<0)throw new BusinessException(ErrorCode.VALIDATION_ERROR,"Dados do abastecimento inválidos"); if(a.getLitros()>1000)throw new BusinessException(ErrorCode.VALIDATION_ERROR,"Limite de 1.000 litros por abastecimento excedido"); avaliarFraude(a);return r.save(a);} public List<Abastecimento> importarCartao(List<Abastecimento> transacoes){return transacoes.stream().map(a->{a.setTipoOrigem("CARTAO_FROTA");return registrar(a);}).toList();}
 private void avaliarFraude(Abastecimento a){int score=0;if(Boolean.FALSE.equals(a.getPostoAutorizado()))score+=50;if(a.getLitrosSensor()!=null&&a.getLitros()>0&&Math.abs(a.getLitros()-a.getLitrosSensor())/a.getLitros()>.10)score+=30;a.setFraudeScore(score);a.setStatusConciliacao("PENDENTE");}
}
