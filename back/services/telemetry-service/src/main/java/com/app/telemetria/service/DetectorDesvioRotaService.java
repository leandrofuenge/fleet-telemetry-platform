package com.app.telemetria.service;

import com.app.telemetria.entity.*;
import com.app.telemetria.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DetectorDesvioRotaService {

    private final RotaRepository rotaRepository;
    private final TelemetriaRepository telemetriaRepository;
    private final DesvioRotaRepository desvioRotaRepository;
    // Constantes
    private static final double TOLERANCIA_DESVIO = 50.0; // 50 metros

    public DetectorDesvioRotaService(
            RotaRepository rotaRepository,
            TelemetriaRepository telemetriaRepository,
            DesvioRotaRepository desvioRotaRepository,
            GeocodingService geocodingService) {
        this.rotaRepository = rotaRepository;
        this.telemetriaRepository = telemetriaRepository;
        this.desvioRotaRepository = desvioRotaRepository;
    }

    @Transactional
    public void verificarDesviosAtivos() {
        // Busca rotas em andamento (status "EM_ANDAMENTO")
        List<Rota> rotasAtivas = rotaRepository.findByStatus("EM_ANDAMENTO");

        for (Rota rota : rotasAtivas) {
            verificarDesvioParaRota(rota);
        }
    }

    private void verificarDesvioParaRota(Rota rota) {
        // Busca última telemetria do veículo associado à rota
        // (presume-se que o repositório tenha método por veiculoId)
        Optional<Telemetria> optTelemetria = telemetriaRepository
                .findUltimaTelemetriaByVeiculoId(rota.getVeiculo().getId());

        if (optTelemetria.isEmpty())
            return;

        Telemetria ultimaTelemetria = optTelemetria.get();

        // Calcula distância até a rota planejada (em metros)
        double distanciaAteRota = calcularDistanciaAteRota(
                ultimaTelemetria.getLatitude(),
                ultimaTelemetria.getLongitude(),
                rota);

        // Verifica se está em desvio
        if (distanciaAteRota > TOLERANCIA_DESVIO) {
            registrarDesvio(rota, ultimaTelemetria, distanciaAteRota);
        } else {
            verificarRetornoRota(rota, ultimaTelemetria);
        }
    }

    private double calcularDistanciaAteRota(double lat, double lng, Rota rota) {
        double distanciaMinima = Double.MAX_VALUE;

        // Obtém a lista de pontos da rota (já disponível na entidade)
        List<PontoRota> pontos = rota.getPontosRota();
        if (pontos == null || pontos.size() < 2) {
            return Double.MAX_VALUE; // não é possível calcular
        }

        for (int i = 0; i < pontos.size() - 1; i++) {
            PontoRota p1 = pontos.get(i);
            PontoRota p2 = pontos.get(i + 1);

            double distancia = distanciaPontoParaSegmento(
                    lat, lng,
                    p1.getLatitude(), p1.getLongitude(),
                    p2.getLatitude(), p2.getLongitude());

            distanciaMinima = Math.min(distanciaMinima, distancia);
        }

        return distanciaMinima;
    }

    private double distanciaPontoParaSegmento(
            double px, double py,
            double x1, double y1,
            double x2, double y2) {

        // Algoritmo para distância de ponto a segmento de reta
        double A = px - x1;
        double B = py - y1;
        double C = x2 - x1;
        double D = y2 - y1;

        double dot = A * C + B * D;
        double len_sq = C * C + D * D;
        double param = len_sq != 0 ? dot / len_sq : -1;

        double xx, yy;

        if (param < 0) {
            xx = x1;
            yy = y1;
        } else if (param > 1) {
            xx = x2;
            yy = y2;
        } else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }

        double dx = px - xx;
        double dy = py - yy;

        // Converte graus para metros (aproximação: 1 grau ~ 111320 m no equador)
        return Math.sqrt(dx * dx + dy * dy) * 111320;
    }

    private void registrarDesvio(Rota rota, Telemetria telemetria, double distancia) {
        // Verifica se já existe um desvio ativo para esta rota (não resolvido)
        Optional<DesvioRota> desvioAtivo = desvioRotaRepository
                .findByRotaIdAndResolvidoFalse(rota.getId());

        if (desvioAtivo.isEmpty()) {
            // Cria novo registro de desvio usando os IDs
            DesvioRota desvio = DesvioRota.builder()
                    .rotaId(rota.getId())
                    .veiculoId(telemetria.getVeiculoId())
                    .veiculoUuid(telemetria.getVeiculoUuid())
                    .viagemId(buscarViagemAtiva(telemetria.getVeiculoId())) // ou null
                    .latitudeDesvio(telemetria.getLatitude())
                    .longitudeDesvio(telemetria.getLongitude())
                    .velocidadeKmh(telemetria.getVelocidade())
                    .distanciaMetros(distancia)
                    .dataHoraDesvio(LocalDateTime.now())
                    .alertaEnviado(false)
                    .resolvido(false)
                    .kmExtras(0.0)
                    .build();

            // Se houver uma viagem ativa, podemos associar
            desvio.setViagemId(buscarViagemAtiva(telemetria.getVeiculoId()));

            desvioRotaRepository.save(desvio);

            // Dispara notificação
            notificarDesvio(desvio, rota);
        }
    }

    private Long buscarViagemAtiva(Long veiculoId) {
        // Lógica para buscar ID da viagem em andamento do veículo
        // Exemplo: viagemRepository.findByVeiculoIdAndStatus(veiculoId, "EM_ANDAMENTO")
        // Retorna null se não houver
        return null; // substituir pela implementação real
    }

    private void verificarRetornoRota(Rota rota, Telemetria telemetria) {
        // Verifica se havia um desvio ativo
        Optional<DesvioRota> desvioAtivo = desvioRotaRepository
                .findByRotaIdAndResolvidoFalse(rota.getId());

        if (desvioAtivo.isPresent()) {
            DesvioRota desvio = desvioAtivo.get();
            desvio.setResolvido(true);
            desvio.setDataHoraRetorno(LocalDateTime.now());

            // Calcular km extras se necessário (pode usar distanciaMetros)
            // desvio.setKmExtras(...);

            desvioRotaRepository.save(desvio);

            // Notifica retorno à rota
            notificarRetorno(rota);
        }
    }

    private void notificarDesvio(DesvioRota desvio, Rota rota) {
        String mensagem = String.format(
                "🚨 DESVIO DE ROTA DETECTADO!\n" +
                        "Rota: %s\n" +
                        "Veículo: %s\n" +
                        "Distância: %.2f metros\n" +
                        "Local: %.6f, %.6f",
                rota.getNome(),
                desvio.getVeiculoUuid(), // ou buscar placa via repositório se necessário
                desvio.getDistanciaMetros(),
                desvio.getLatitudeDesvio(),
                desvio.getLongitudeDesvio());

        System.out.println(mensagem);
        // Aqui você pode integrar com websockets, email, etc
    }

    private void notificarRetorno(Rota rota) {
        String mensagem = String.format(
                "✅ VEÍCULO RETORNOU À ROTA!\n" +
                        "Rota: %s\n" +
                        "Veículo: %s",
                rota.getNome(),
                rota.getVeiculo() != null ? rota.getVeiculo().getPlaca() : "N/A");

        System.out.println(mensagem);
    }
}