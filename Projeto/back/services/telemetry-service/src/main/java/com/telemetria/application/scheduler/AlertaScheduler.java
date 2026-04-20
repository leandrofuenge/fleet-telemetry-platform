package com.telemetria.application.scheduler;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.telemetria.domain.entity.Motorista;
import com.telemetria.domain.entity.Veiculo;
import com.telemetria.domain.service.AlertaService;
import com.telemetria.domain.service.VeiculoService;
import com.telemetria.domain.service.ViagemService;
import com.telemetria.infrastructure.persistence.MotoristaRepository;
import com.telemetria.infrastructure.persistence.VeiculoRepository;

@Component
@EnableScheduling
public class AlertaScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlertaScheduler.class);
    
    private final VeiculoRepository veiculoRepository;
    private final MotoristaRepository motoristaRepository;
    private final AlertaService alertaService;

    public AlertaScheduler(
            AlertaService alertaService,
            VeiculoService veiculoService,
            ViagemService viagemService,
            VeiculoRepository veiculoRepository,
            MotoristaRepository motoristaRepository) {
        this.alertaService = alertaService;
        this.veiculoRepository = veiculoRepository;
        this.motoristaRepository = motoristaRepository;
    }

    @Scheduled(fixedDelay = 60000) // A cada 1 minuto
    public void verificarAlertasPeriodicos() {
        log.debug("🔄 Executando verificação periódica de alertas");
        // Verificar vencimentos de documentos
        verificarVencimentoTacografo();
        verificarVencimentoDocumentos();
    }

    /**
     * RN-VEI-002: Alertas 30d e 7d antes do vencimento do tacógrafo
     */
    @Scheduled(cron = "0 0 1 * * ?") // Executa diariamente à 1h
    public void verificarVencimentoTacografo() {
        log.info("📅 Executando verificação de vencimento de tacógrafos");
        List<Veiculo> veiculos = veiculoRepository.findAll();
        LocalDate hoje = LocalDate.now();
        
        for (Veiculo veiculo : veiculos) {
            if (veiculo.getTacografoObrigatorio() != null && 
                veiculo.getTacografoObrigatorio() && 
                veiculo.getDataVencimentoTacografo() != null) {
                
                LocalDate vencimento = veiculo.getDataVencimentoTacografo();
                long diasAteVencimento = ChronoUnit.DAYS.between(hoje, vencimento);
                
                if (diasAteVencimento == 30 || diasAteVencimento == 7) {
                    alertaService.criarAlertaVencimentoTacografo(veiculo, diasAteVencimento);
                    log.info("⚠️ Alerta de tacógrafo: veículo {} vence em {} dias", 
                            veiculo.getPlaca(), diasAteVencimento);
                }
                
                if (vencimento.isBefore(hoje)) {
                    alertaService.criarAlertaTacografoVencido(veiculo);
                    log.warn("❌ Tacógrafo vencido: veículo {}", veiculo.getPlaca());
                }
            }
        }
    }

    /**
     * RN-VEI-003: Alertas 30d e 7d antes do vencimento dos documentos
     * Monitora: CRLV, Seguro, DPVAT, RCF, Vistoria, RNTRC
     */
    @Scheduled(cron = "0 0 2 * * ?") // Executa diariamente à 2h
    public void verificarVencimentoDocumentos() {
        log.info("📄 Executando verificação de vencimento de documentos");
        List<Veiculo> veiculos = veiculoRepository.findAll();
        
        for (Veiculo veiculo : veiculos) {
            // CRLV
            verificarDocumento(veiculo, veiculo.getDataVencimentoCrlv(), "CRLV");
            // Seguro
            verificarDocumento(veiculo, veiculo.getDataVencimentoSeguro(), "Seguro");
            // DPVAT
            verificarDocumento(veiculo, veiculo.getDataVencimentoDpvat(), "DPVAT");
            // RCF
            verificarDocumento(veiculo, veiculo.getDataVencimentoRcf(), "RCF");
            // Vistoria
            verificarDocumento(veiculo, veiculo.getDataVencimentoVistoria(), "Vistoria");
            // RNTRC
            verificarDocumento(veiculo, veiculo.getDataVencimentoRntrc(), "RNTRC");
        }
    }

    private void verificarDocumento(Veiculo veiculo, LocalDate vencimento, String documento) {
        if (vencimento == null) return;
        
        LocalDate hoje = LocalDate.now();
        long diasAteVencimento = ChronoUnit.DAYS.between(hoje, vencimento);
        
        if (diasAteVencimento == 30 || diasAteVencimento == 7) {
            alertaService.criarAlertaVencimentoDocumento(veiculo, documento, diasAteVencimento);
            log.info("⚠️ Alerta de {}: veículo {} vence em {} dias", 
                    documento, veiculo.getPlaca(), diasAteVencimento);
        }
        
        if (vencimento.isBefore(hoje)) {
            alertaService.criarAlertaDocumentoVencido(veiculo, documento);
            log.warn("❌ {} vencido: veículo {}", documento, veiculo.getPlaca());
        }
    }
    
    /**
     * RN-MOT-002: Alertas 60d, 30d e 7d antes do vencimento da CNH
     */
    @Scheduled(cron = "0 0 3 * * ?") // Executa diariamente à 3h
    public void verificarVencimentoCnh() {
        log.info("📅 Executando verificação de vencimento de CNH");
        List<Motorista> motoristas = motoristaRepository.findAll();
        LocalDate hoje = LocalDate.now();
        
        for (Motorista motorista : motoristas) {
            if (motorista.getDataVencimentoCnh() != null) {
                LocalDate vencimento = motorista.getDataVencimentoCnh();
                long diasAteVencimento = ChronoUnit.DAYS.between(hoje, vencimento);
                
                if (diasAteVencimento == 60 || diasAteVencimento == 30 || diasAteVencimento == 7) {
                    alertaService.criarAlertaVencimentoCnh(motorista, diasAteVencimento);
                    log.info("⚠️ Alerta de CNH: motorista {} vence em {} dias", 
                            motorista.getNome(), diasAteVencimento);
                }
                
                if (vencimento.isBefore(hoje)) {
                    alertaService.criarAlertaCnhVencida(motorista);
                    log.warn("❌ CNH vencida: motorista {}", motorista.getNome());
                }
            }
        }
    }
}