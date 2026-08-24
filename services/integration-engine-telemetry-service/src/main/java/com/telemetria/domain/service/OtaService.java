package com.telemetria.domain.service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.DispositivoIot;
import com.telemetria.domain.entity.OtaJob;
import com.telemetria.domain.enums.StatusDispositivo;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.infrastructure.persistence.DispositivoIotRepository;
import com.telemetria.infrastructure.persistence.OtaJobRepository;
@Service public class OtaService {private final OtaJobRepository jobs;private final DispositivoIotRepository dispositivos;public OtaService(OtaJobRepository j,DispositivoIotRepository d){jobs=j;dispositivos=d;}
 @Transactional public OtaDispatch criar(Long tenantId,String deviceId,String versao,String sha256,String assinatura){DispositivoIot d=dispositivos.findByDeviceId(deviceId).orElseThrow(()->new BusinessException(ErrorCode.VALIDATION_ERROR,"Dispositivo inexistente"));if(!tenantId.equals(d.getTenantId())||d.getStatus()!=StatusDispositivo.ATIVO)throw new BusinessException(ErrorCode.FORBIDDEN,"OTA exige dispositivo ativo do tenant");if(sha256==null||!sha256.matches("[a-fA-F0-9]{64}")||assinatura==null||assinatura.isBlank())throw new BusinessException(ErrorCode.VALIDATION_ERROR,"Firmware precisa de SHA-256 e assinatura HSM");String token=UUID.randomUUID().toString();OtaJob j=new OtaJob();j.setTenantId(tenantId);j.setDeviceId(deviceId);j.setVersaoAlvo(versao);j.setSha256(sha256.toLowerCase());j.setAssinaturaHsm(assinatura);j.setTokenHash(hash(token));d.setVersaoAlvo(versao);dispositivos.save(d);jobs.save(j);return new OtaDispatch(j.getId(),token,j.getVersaoAlvo(),j.getSha256(),j.getAssinaturaHsm(),j.getFase());}
 @Transactional public void confirmar(Long id,String token,String sha256Instalado,boolean sucesso){OtaJob j=jobs.findById(id).orElseThrow(()->new BusinessException(ErrorCode.VALIDATION_ERROR,"Job OTA não encontrado"));if(Boolean.TRUE.equals(j.getTokenConsumido())||!hash(token).equals(j.getTokenHash()))throw new BusinessException(ErrorCode.FORBIDDEN,"Token OTA inválido ou já utilizado");j.setTokenConsumido(true);j.setAtualizadoEm(LocalDateTime.now());if(!sucesso||!j.getSha256().equalsIgnoreCase(sha256Instalado)){j.setStatus("FALHOU");j.setErro("Falha de instalação ou checksum divergente");j.setFase("ROLLBACK");}else{j.setStatus("CONCLUIDO");dispositivos.findByDeviceId(j.getDeviceId()).ifPresent(d->{d.setVersaoFirmware(j.getVersaoAlvo());d.setVersaoAlvo(null);dispositivos.save(d);});}jobs.save(j);}
 @Transactional public OtaJob promover(Long id){OtaJob j=jobs.findById(id).orElseThrow(()->new BusinessException(ErrorCode.VALIDATION_ERROR,"Job OTA não encontrado"));if("FALHOU".equals(j.getStatus()))throw new BusinessException(ErrorCode.VALIDATION_ERROR,"Job com falha está em rollback");j.setFase(switch(j.getFase()){case "CANARY_1PCT"->"CANARY_5PCT";case "CANARY_5PCT"->"ROLLOUT_20PCT";case "ROLLOUT_20PCT"->"FULL";default->j.getFase();});j.setAtualizadoEm(LocalDateTime.now());return jobs.save(j);}
 private String hash(String v){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format("%02x",x));return s.toString();}catch(Exception e){throw new IllegalStateException(e);}} public record OtaDispatch(Long jobId,String token,String versao,String sha256,String assinatura,String fase){}
}
