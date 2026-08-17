package com.telemetria.domain.service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.Jornada;
import com.telemetria.domain.enums.OrigemDado;
import com.telemetria.domain.enums.StatusJornada;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.infrastructure.persistence.JornadaRepository;

/** Controle da jornada legal: direção, pausas e encerramento por troca de turno. */
@Service public class JornadaService {
 private final JornadaRepository repository; private final Map<Long,LocalDateTime> ultimoEvento=new ConcurrentHashMap<>(); private final Map<Long,LocalDateTime> pausaInicio=new ConcurrentHashMap<>();
 public JornadaService(JornadaRepository repository){this.repository=repository;}
 @Transactional public Jornada iniciar(Long tenantId,Long motoristaId,Long veiculoId,Long viagemId,OrigemDado origem){
   return repository.findTopByMotoristaIdAndStatusOrderByDataInicioDesc(motoristaId,StatusJornada.ABERTA).orElseGet(()->repository.save(Jornada.builder().tenantId(tenantId).motoristaId(motoristaId).veiculoId(veiculoId).viagemId(viagemId).dataInicio(LocalDateTime.now()).origemDado(origem).build())); }
 @Transactional public void registrarDirecao(Long tenantId,Long motoristaId,Long veiculoId,Long viagemId,boolean dirigindo,LocalDateTime instante){
   Jornada jornada=iniciar(tenantId,motoristaId,veiculoId,viagemId,OrigemDado.TELEMETRIA); LocalDateTime anterior=ultimoEvento.put(motoristaId,instante); if(anterior==null) return;
   long segundos=Math.max(0,Duration.between(anterior,instante).getSeconds()); if(segundos>900) return; // evento atrasado não infla a jornada
   if(dirigindo){ LocalDateTime inicioPausa=pausaInicio.remove(motoristaId); if(inicioPausa!=null && Duration.between(inicioPausa,instante).toMinutes()>=30) jornada.setHorasDisponivel(0.0); jornada.setHorasDirecao(jornada.getHorasDirecao()+segundos/3600.0); jornada.setHorasDisponivel(jornada.getHorasDisponivel()+segundos/3600.0); validarLimites(jornada); }
   else pausaInicio.putIfAbsent(motoristaId,instante); repository.save(jornada);
 }
 @Transactional public Jornada fechar(Long motoristaId){ Jornada j=repository.findTopByMotoristaIdAndStatusOrderByDataInicioDesc(motoristaId,StatusJornada.ABERTA).orElseThrow(()->new BusinessException(ErrorCode.VALIDATION_ERROR,"Não há jornada aberta")); j.setDataFim(LocalDateTime.now()); j.setStatus(j.getIrregular()?StatusJornada.IRREGULAR:StatusJornada.FECHADA); return repository.save(j); }
 public boolean respeitouRepouso(Long motoristaId,LocalDateTime inicio){ return repository.findTopByMotoristaIdOrderByDataFimDesc(motoristaId).map(j->j.getDataFim()!=null && Duration.between(j.getDataFim(),inicio).toHours()>=11).orElse(true); }
 private void validarLimites(Jornada j){ if(j.getHorasDirecao()>=9.5) j.setAlertaLimite30min(true); if(j.getHorasDirecao()>10){j.setIrregular(true);j.setStatus(StatusJornada.IRREGULAR);j.setMotivoIrregularidade("Limite legal de 10 horas de direção excedido");} if(j.getHorasDisponivel()>4 && !pausaInicio.containsKey(j.getMotoristaId())) j.setMotivoIrregularidade("Pausa obrigatória de 30 minutos pendente"); }
}
