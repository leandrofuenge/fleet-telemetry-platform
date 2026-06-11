package com.telemetria.domain.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.Alerta;
import com.telemetria.domain.entity.Telemetria;
import com.telemetria.domain.enums.SeveridadeAlerta;
import com.telemetria.domain.enums.TipoAlerta;
import com.telemetria.infrastructure.persistence.AlertaRepository;

@Service
public class AlertaBaseService {

    private static final Logger log = LoggerFactory.getLogger(AlertaBaseService.class);

    private final AlertaRepository alertaRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // Injetamos apenas o que é estritamente necessário para persistir e notificar o alerta
    public AlertaBaseService(AlertaRepository alertaRepository, SimpMessagingTemplate messagingTemplate) {
        this.alertaRepository = alertaRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Mapeia com segurança os dados da telemetria e delega para a criação do alerta.
     */
    @Transactional
    public void salvarAlertaMapeado(Telemetria t, TipoAlerta tipo, SeveridadeAlerta sev, 
                                    String msg, Long motoristaId, Long viagemId) {
        
        Long tenantId = (t != null) ? t.getTenantId() : null;
        Long veiculoId = (t != null) ? t.getVeiculoId() : null;
        String veiculoUuid = (t != null && t.getVeiculo() != null) ? t.getVeiculo().getUuid() : null;
        Double latitude = (t != null) ? t.getLatitude() : null;
        Double longitude = (t != null) ? t.getLongitude() : null;
        Double velocidadeKmh = (t != null) ? t.getVelocidade() : 0.0;
        Double odometroKm = (t != null) ? t.getOdometro() : null;
        Long telemetriaId = (t != null) ? t.getId() : null;

        criarAlerta(
                tenantId,
                veiculoId,
                veiculoUuid,
                motoristaId,
                viagemId,
                telemetriaId,
                tipo,
                sev,
                msg,
                latitude,
                longitude,
                velocidadeKmh,
                odometroKm
        );
    }

    /**
     * Instancia, persiste no banco de dados e notifica via WebSocket em tempo real.
     */
    private void criarAlerta(Long tenantId, Long veiculoId, String veiculoUuid, Long motoristaId, 
                             Long viagemId, Long telemetriaId, TipoAlerta tipo, 
                             SeveridadeAlerta severidade, String mensagem, Double latitude, 
                             Double longitude, Double velocidadeKmh, Double odometroKm) {
        
        try {
            Alerta alerta = Alerta.builder()
                    .tenantId(tenantId)
                    .veiculoId(veiculoId)
                    .veiculoUuid(veiculoUuid)
                    .motoristaId(motoristaId)
                    .viagemId(viagemId)
                    .telemetriaId(telemetriaId)
                    .tipo(tipo)
                    .severidade(severidade)
                    .mensagem(mensagem)
                    .latitude(latitude)
                    .longitude(longitude)
                    .velocidadeKmh(velocidadeKmh)
                    .odometroKm(odometroKm)
                    .dataHora(LocalDateTime.now())
                    .lido(false)
                    .resolvido(false)
                    .notificacaoEnviada(false)
                    .build();

            // 1. Salva no banco de dados relacional
            Alerta alertaSalvo = alertaRepository.save(alerta);
            log.debug("💾 Alerta tipo {} salvo com sucesso para o veículo {}", tipo, veiculoId);

            // 2. Dispara a mensagem via WebSocket para atualizar o front-end/painel instantaneamente
            messagingTemplate.convertAndSend("/topic/alertas", alertaSalvo);
            
        } catch (Exception e) {
            log.error("❌ Erro ao tentar persistir/notificar alerta do tipo {} para o veículo {}", tipo, veiculoId, e);
        }
    }
}