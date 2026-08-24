package com.telemetria.api.controller; import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.domain.entity.DispositivoIot;
import com.telemetria.domain.service.DispositivoIotService;
import com.telemetria.domain.service.OtaService;
@RestController @RequestMapping("/api/v1/dispositivos") public class DispositivoIotLifecycleController {private final DispositivoIotService s;private final OtaService ota;public DispositivoIotLifecycleController(DispositivoIotService s,OtaService o){this.s=s;ota=o;}@PostMapping("/provisionar")public DispositivoIot provisionar(@RequestParam String deviceId,@RequestParam Long tenantId){return s.provisionar(deviceId,tenantId);}@PostMapping("/{id}/aprovar")public DispositivoIot aprovar(@PathVariable Long id){return s.aprovar(id);}@PostMapping("/{deviceId}/heartbeat")public DispositivoIot heartbeat(@PathVariable String deviceId,@RequestParam boolean ignicao,@RequestParam(required=false) Double rssi){return s.heartbeat(deviceId,ignicao,rssi);}@PostMapping("/{deviceId}/ota")public OtaService.OtaDispatch ota(@PathVariable String deviceId,@RequestParam Long tenantId,@RequestParam String versao,@RequestParam String sha256,@RequestParam String assinatura){return ota.criar(tenantId,deviceId,versao,sha256,assinatura);}@PostMapping("/ota/{jobId}/confirmar")public void confirmar(@PathVariable Long jobId,@RequestParam String token,@RequestParam String sha256,@RequestParam boolean sucesso){ota.confirmar(jobId,token,sha256,sucesso);}@PostMapping("/ota/{jobId}/promover")public com.telemetria.domain.entity.OtaJob promover(@PathVariable Long jobId){return ota.promover(jobId);}}
