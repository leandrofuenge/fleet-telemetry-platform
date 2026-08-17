package com.telemetria.application.scheduler; import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.telemetria.domain.service.DispositivoIotService;
@Component public class DispositivoIotScheduler {private final DispositivoIotService s;public DispositivoIotScheduler(DispositivoIotService s){this.s=s;}@Scheduled(fixedDelay=60000)public void verificar(){s.validarCertificados();s.semHeartbeat(10);s.semHeartbeat(30);}}
