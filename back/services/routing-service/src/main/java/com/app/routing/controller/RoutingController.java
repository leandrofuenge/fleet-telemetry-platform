package com.app.routing.controller;

import com.app.routing.dto.RouteResponse;
import com.app.routing.service.RoutingService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/routing")
public class RoutingController {

    private final RoutingService routingService;

    public RoutingController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @GetMapping("/calcular")
    public RouteResponse calcular(@RequestParam Double origemLat,
                                  @RequestParam Double origemLon,
                                  @RequestParam Double destinoLat,
                                  @RequestParam Double destinoLon) {

        return routingService.calcularMelhorRota(
                origemLat,
                origemLon,
                destinoLat,
                destinoLon
        );
    }
}