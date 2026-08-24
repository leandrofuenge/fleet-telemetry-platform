package com.telemetria.domain.service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.DispositivoIot;
import com.telemetria.domain.enums.StatusDispositivo;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.infrastructure.persistence.DispositivoIotRepository;
@Service public class DispositivoIotService {private final DispositivoIotRepository r;public DispositivoIotService(DispositivoIotRepository r){this.r=r;}
 @Transactional public DispositivoIot provisionar(String deviceId,Long tenantId){if(deviceId==null||deviceId.isBlank()||tenantId==null)throw new BusinessException(ErrorCode.VALIDATION_ERROR,"deviceId e tenant são obrigatórios");return r.findByDeviceId(deviceId).orElseGet(()->r.save(new DispositivoIot(deviceId,tenantId,null)));}
 @Transactional public DispositivoIot aprovar(Long id){DispositivoIot d=buscar(id);if(d.getStatus()!=StatusDispositivo.PENDENTE)throw new BusinessException(ErrorCode.VALIDATION_ERROR,"Somente dispositivo pendente pode ser aprovado");d.setStatus(StatusDispositivo.ATIVO);return r.save(d);}
 @Transactional public DispositivoIot heartbeat(String deviceId,boolean ignicao,Double rssi){DispositivoIot d=r.findByDeviceId(deviceId).orElseThrow(()->new BusinessException(ErrorCode.VALIDATION_ERROR,"Dispositivo não provisionado"));if(d.getStatus()==StatusDispositivo.REVOGADO)throw new BusinessException(ErrorCode.FORBIDDEN,"Certificado revogado; mensagem rejeitada");if(d.getStatus()!=StatusDispositivo.ATIVO)throw new BusinessException(ErrorCode.FORBIDDEN,"Dispositivo não está ativo");d.setUltimoHeartbeat(LocalDateTime.now());d.setUltimaConexao(LocalDateTime.now());d.setRssi(rssi);return r.save(d);}
 @Transactional public void validarCertificados(){for(DispositivoIot d:r.findAll()){if(d.getCertificadoExpira()!=null&&d.getCertificadoExpira().isBefore(LocalDate.now())){d.setStatus(StatusDispositivo.INATIVO);r.save(d);}}}
 public List<DispositivoIot> semHeartbeat(int minutos){LocalDateTime limite=LocalDateTime.now().minusMinutes(minutos);return r.findAll().stream().filter(d->d.getStatus()==StatusDispositivo.ATIVO&&(d.getUltimoHeartbeat()==null||d.getUltimoHeartbeat().isBefore(limite))).toList();} private DispositivoIot buscar(Long id){return r.findById(id).orElseThrow(()->new BusinessException(ErrorCode.VALIDATION_ERROR,"Dispositivo não encontrado"));}}
