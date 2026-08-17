package com.telemetria.api.controller; import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.telemetria.domain.entity.Abastecimento;
import com.telemetria.domain.service.AbastecimentoService;
@RestController @RequestMapping("/api/v1/abastecimentos") public class AbastecimentoController {private final AbastecimentoService s;public AbastecimentoController(AbastecimentoService s){this.s=s;}@PostMapping public ResponseEntity<Abastecimento> salvar(@RequestBody Abastecimento a){return ResponseEntity.status(HttpStatus.CREATED).body(s.registrar(a));}@PostMapping("/cartao-frota/importar") public List<Abastecimento> importar(@RequestBody List<Abastecimento> a){return s.importarCartao(a);}}
