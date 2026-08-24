package com.telemetria.api.controller; import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.domain.entity.Manutencao;
import com.telemetria.domain.service.ManutencaoService;
@RestController @RequestMapping("/api/v1/manutencoes") public class ManutencaoController {private final ManutencaoService s;public ManutencaoController(ManutencaoService s){this.s=s;}@PostMapping public ResponseEntity<Manutencao> salvar(@RequestBody Manutencao m){return ResponseEntity.status(HttpStatus.CREATED).body(s.salvar(m));}@PostMapping("/predicao") public Manutencao predicao(@RequestBody Manutencao m,@RequestParam double anomalyScore,@RequestParam int rulDias,@RequestParam double probabilidade){return s.registrarPredicao(m,anomalyScore,rulDias,probabilidade);}}
