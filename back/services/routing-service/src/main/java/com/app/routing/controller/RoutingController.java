package com.app.routing.controller;

import com.app.routing.dto.RouteResponse;
import com.app.routing.enums.PerfilOsrm;
import com.app.routing.exception.OsrmIndisponivelException;
import com.app.routing.service.RoutingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/routing")
public class RoutingController {

    private final RoutingService routingService;

    public RoutingController(RoutingService routingService) {
        this.routingService = routingService;
    }

    /**
     * RN-ROT-001 — Calcula rota obrigatoriamente via OSRM.
     * O perfil padrão é CAMINHAO. Retorna HTTP 503 se OSRM indisponível.
     *
     * @param perfil  Perfil OSRM (CAMINHAO ou CARRO). Padrão: CAMINHAO.
     */
    @GetMapping("/calcular")
    public ResponseEntity<?> calcular(@RequestParam Double origemLat,
                                      @RequestParam Double origemLon,
                                      @RequestParam Double destinoLat,
                                      @RequestParam Double destinoLon,
                                      @RequestParam(required = false) PerfilOsrm perfil) {
        try {
            RouteResponse response = routingService.calcularMelhorRota(
                    origemLat, origemLon, destinoLat, destinoLon, perfil);
            return ResponseEntity.ok(response);

        } catch (OsrmIndisponivelException e) {
            // RN-ROT-001: Se OSRM indisponível, retornar erro — nunca silenciar.
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of(
                            "erro", "OSRM indisponível",
                            "mensagem", e.getMessage(),
                            "regra", "RN-ROT-001"
                    ));
        }
    }
}