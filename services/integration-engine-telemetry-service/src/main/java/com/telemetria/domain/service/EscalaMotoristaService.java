package com.telemetria.domain.service;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.EscalaMotorista;
import com.telemetria.domain.entity.Motorista;
import com.telemetria.domain.entity.Veiculo;
import com.telemetria.domain.enums.StatusEscala;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.infrastructure.persistence.EscalaMotoristaRepository;
import com.telemetria.infrastructure.persistence.MotoristaRepository;
import com.telemetria.infrastructure.persistence.VeiculoRepository;
@Service public class EscalaMotoristaService {
 private final EscalaMotoristaRepository escalas; private final JornadaService jornadas; private final MotoristaRepository motoristas; private final VeiculoRepository veiculos;
 public EscalaMotoristaService(EscalaMotoristaRepository e,JornadaService j,MotoristaRepository m,VeiculoRepository v){escalas=e;jornadas=j;motoristas=m;veiculos=v;}
 @Transactional public EscalaMotorista criar(EscalaMotorista e){ if(e.getTenantId()==null||e.getMotoristaId()==null||e.getVeiculoId()==null||e.getDataInicioTurno()==null||e.getDataFimTurno()==null||!e.getDataFimTurno().isAfter(e.getDataInicioTurno())) throw new BusinessException(ErrorCode.VALIDATION_ERROR,"Dados do turno inválidos"); Motorista m=motoristas.findById(e.getMotoristaId()).orElseThrow(()->new BusinessException(ErrorCode.MOTORISTA_NOT_FOUND)); Veiculo v=veiculos.findById(e.getVeiculoId()).orElseThrow(()->new BusinessException(ErrorCode.VEICULO_NOT_FOUND)); if(!e.getTenantId().equals(m.getTenantId())||!e.getTenantId().equals(v.getTenantId())) throw new BusinessException(ErrorCode.FORBIDDEN,"Motorista e veículo devem pertencer ao tenant da escala"); if(!escalas.conflitosMotorista(e.getMotoristaId(),e.getDataInicioTurno(),e.getDataFimTurno()).isEmpty()) throw new BusinessException(ErrorCode.VALIDATION_ERROR,"Motorista já possui turno no período informado"); if(!jornadas.respeitouRepouso(e.getMotoristaId(),e.getDataInicioTurno())) throw new BusinessException(ErrorCode.VALIDATION_ERROR,"Motorista precisa cumprir 11 horas de repouso antes do turno"); return escalas.save(e); }
 @Transactional public EscalaMotorista confirmar(Long id){EscalaMotorista e=escalas.findById(id).orElseThrow(()->new BusinessException(ErrorCode.VALIDATION_ERROR,"Escala não encontrada")); e.setConfirmadoMotorista(true);e.setStatus(StatusEscala.CONFIRMADA);return escalas.save(e);}
 public List<EscalaMotorista> listar(){return escalas.findAll();}
}
