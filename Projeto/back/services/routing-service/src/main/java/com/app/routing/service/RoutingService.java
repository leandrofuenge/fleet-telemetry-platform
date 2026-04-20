package com.app.routing.service;

import com.app.routing.client.OsrmClient;
import com.app.routing.dto.RouteResponse;
import com.app.routing.enums.PerfilOsrm;
import com.app.routing.exception.OsrmIndisponivelException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * RN-ROT-001 — Serviço de cálculo de rotas.
 * Garante uso obrigatório do OSRM e perfil CAMINHAO por padrão.
 */
@Service
public class RoutingService {

    private static final Logger log = LoggerFactory.getLogger(RoutingService.class);

    private final OsrmClient osrmClient;
    private final ObjectMapper mapper;

    public RoutingService(OsrmClient osrmClient) {
        this.osrmClient = osrmClient;
        this.mapper = new ObjectMapper();
    }

    /**
     * Calcula a melhor rota usando o perfil CAMINHAO por padrão (RN-ROT-001).
     */
    public RouteResponse calcularMelhorRota(Double origemLat,
                                            Double origemLon,
                                            Double destinoLat,
                                            Double destinoLon) {
        return calcularMelhorRota(origemLat, origemLon, destinoLat, destinoLon, null);
    }

    /**
     * Calcula a melhor rota com perfil explícito.
     * Se perfil for nulo, usa CAMINHAO como padrão (RN-ROT-001).
     *
     * @throws OsrmIndisponivelException se o OSRM não puder ser alcançado — nunca usa linha reta
     */
    public RouteResponse calcularMelhorRota(Double origemLat,
                                            Double origemLon,
                                            Double destinoLat,
                                            Double destinoLon,
                                            PerfilOsrm perfil) {

        // RN-ROT-001: Usar perfil CAMINHAO por padrão
        PerfilOsrm perfilEfetivo = (perfil != null) ? perfil : PerfilOsrm.CAMINHAO;

        log.info("📍 [RN-ROT-001] Calculando rota: Origem [{}, {}] → Destino [{}, {}] | Perfil: {}",
                origemLat, origemLon, destinoLat, destinoLon, perfilEfetivo);

        // OsrmIndisponivelException é intencionalmente NÃO capturada aqui.
        // Ela deve propagar para o controller e retornar 503 ao cliente.
        // RN-ROT-001: "Se OSRM indisponível: retornar erro — nunca usar linha reta silenciosamente."
        String json = osrmClient.calcularRota(origemLat, origemLon, destinoLat, destinoLon, perfilEfetivo);

        try {
            JsonNode node = mapper.readTree(json);
            JsonNode route = node.get("routes").get(0);

            double distanciaMetros = route.get("distance").asDouble();
            double duracaoSegundos = route.get("duration").asDouble();

            double distanciaKm = distanciaMetros / 1000.0;
            double duracaoMin = duracaoSegundos / 60.0;

            log.info("✅ Rota calculada — {:.2f} km, {:.1f} min | Perfil: {}", distanciaKm, duracaoMin, perfilEfetivo);

            return new RouteResponse(distanciaKm, duracaoMin, route.get("geometry"));

        } catch (Exception e) {
            log.error("❌ Erro ao interpretar resposta do OSRM: {}", e.getMessage());
            throw new RuntimeException("Erro ao interpretar resposta do OSRM", e);
        }
    }
}



