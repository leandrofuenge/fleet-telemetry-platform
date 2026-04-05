package com.telemetria.application.service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BackpressureMonitorService {

    private static final Logger log = LoggerFactory.getLogger(BackpressureMonitorService.class);

    private final AtomicInteger mensagensRecebidas = new AtomicInteger(0);
    private final AtomicInteger mensagensProcessadas = new AtomicInteger(0);
    private final AtomicLong tempoTotalProcessamento = new AtomicLong(0);

    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;
    private final int numCores; // ✅ NOVO: Armazenar número de cores

    // Limiares altos para evitar backpressure em carga normal
    private int lagThreshold = 5000;      // antes 500
    private int cpuThreshold = 95;        // antes 80
    private int memoryThreshold = 95;     // antes 80
    private int pauseDurationMs = 1000;

    private volatile boolean backpressureAtivo = false;
    private long ultimoLogEstatisticas = 0;

    public BackpressureMonitorService() {
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.numCores = osBean.getAvailableProcessors(); // ✅ NOVO
        
        log.info("✅ BackpressureMonitorService inicializado (modo alta carga)");
        log.info("💻 Detectado {} cores de CPU", numCores); // ✅ NOVO
        log.debug("📊 Configurações - LagThreshold: {}, CPUThreshold: {}, MemoryThreshold: {}, PauseDuration: {}ms",
                lagThreshold, cpuThreshold, memoryThreshold, pauseDurationMs);
    }

    public synchronized void registrarRecebimento() {
        int total = mensagensRecebidas.incrementAndGet();
        if (total % 100 == 0) {
            log.debug("📥 Mensagens recebidas: {}", total);
        }
        log.trace("➕ Mensagem recebida - Total: {}", total);
    }

    public void registrarRecebimento(String prioridade) {
        registrarRecebimento();
        log.trace("📨 Mensagem {} recebida", prioridade);
    }

    public boolean deveDescartar(String prioridade) {
        if ("CRITICO".equalsIgnoreCase(prioridade)) {
            return false;
        }
        return precisaBackpressure();
    }

    public synchronized void registrarProcessamento(long tempoMs) {
        int processadas = mensagensProcessadas.incrementAndGet();
        long totalTempo = tempoTotalProcessamento.addAndGet(tempoMs);
        if (processadas % 100 == 0) {
            double tempoMedio = (double) totalTempo / processadas;
            log.debug("✅ Mensagens processadas: {}, Tempo médio: {:.2f}ms",
                    processadas, tempoMedio);
        }
        log.trace("✅ Mensagem processada em {}ms - Total processadas: {}", tempoMs, processadas);
    }

    public int calcularLag() {
        return mensagensRecebidas.get() - mensagensProcessadas.get();
    }

    public double calcularTaxaProcessamento() {
        long totalTempo = tempoTotalProcessamento.get();
        int processadas = mensagensProcessadas.get();
        if (totalTempo == 0 || processadas == 0) return 0;
        return (processadas * 1000.0) / totalTempo;
    }

    /**
     * ✅ CORRIGIDO: Calcula o uso real de CPU dividindo por número de cores
     */
    public double getCpuUsage() {
        try {
            // Tentar usar a implementação específica do JDK que retorna % direto
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                com.sun.management.OperatingSystemMXBean sunBean = 
                    (com.sun.management.OperatingSystemMXBean) osBean;
                double cpuLoad = sunBean.getProcessCpuLoad();
                
                // Se retornar valor válido (0-1), converter para percentual
                if (cpuLoad >= 0) {
                    return cpuLoad * 100.0;
                }
            }
            
            // Fallback: usar load average dividido por cores
            double load = osBean.getSystemLoadAverage();
            if (load < 0) return 0;
            
            // Calcular percentual baseado no número de cores
            double cpuPercent = (load / numCores) * 100.0;
            
            // Limitar a 100%
            return Math.min(cpuPercent, 100.0);
            
        } catch (Exception e) {
            log.error("Erro ao obter uso de CPU: {}", e.getMessage());
            return 0;
        }
    }

    public double getMemoryUsage() {
        try {
            long used = memoryBean.getHeapMemoryUsage().getUsed();
            long max = memoryBean.getHeapMemoryUsage().getMax();
            if (max == 0) return 0;
            return (used * 100.0) / max;
        } catch (Exception e) {
            log.error("Erro ao obter uso de memória: {}", e.getMessage());
            return 0;
        }
    }

    public synchronized boolean precisaBackpressure() {
        int lag = calcularLag();
        double cpu = getCpuUsage();
        double memory = getMemoryUsage();

        boolean lagExcedido = lag > lagThreshold;
        boolean cpuExcedido = cpu > cpuThreshold;
        boolean memoryExcedido = memory > memoryThreshold;

        long agora = System.currentTimeMillis();
        if (agora - ultimoLogEstatisticas > 10000) {
            // ✅ CORRIGIDO: Formatar valores antes de passar para log
            String cpuFormatted = String.format("%.1f", cpu);
            String memoryFormatted = String.format("%.1f", memory);
            
            log.info("📊 Monitoramento - Lag: {}/{}, CPU: {}%/{}%, Memória: {}%/{}%",
                    lag, lagThreshold, cpuFormatted, cpuThreshold, memoryFormatted, memoryThreshold);
            ultimoLogEstatisticas = agora;
        }

        if (lagExcedido || cpuExcedido || memoryExcedido) {
            if (!backpressureAtivo) {
                log.warn("🚨 BACKPRESSURE ATIVADO!");
                log.warn(" - Lag: {} mensagens (limite: {})", lag, lagThreshold);
                log.warn(" - CPU: {}% (limite: {}%)", String.format("%.1f", cpu), cpuThreshold);
                log.warn(" - Memória: {}% (limite: {}%)", String.format("%.1f", memory), memoryThreshold);
                backpressureAtivo = true;
            } else {
                // ✅ CORRIGIDO: Formatar valores
                log.debug("⚠️ Backpressure já ativo - Lag: {}, CPU: {}%, Memória: {}%", 
                    lag, String.format("%.1f", cpu), String.format("%.1f", memory));
            }
            return true;
        }

        if (backpressureAtivo) {
            // ✅ CORRIGIDO: Formatar valores
            log.info("✅ Backpressure desativado - sistema normalizado");
            log.info("📊 Status atual - Lag: {}, CPU: {}%, Memória: {}%", 
                lag, String.format("%.1f", cpu), String.format("%.1f", memory));
            backpressureAtivo = false;
        }
        return false;
    }

    public synchronized void aplicarBackpressure() throws InterruptedException {
        if (precisaBackpressure()) {
            log.warn("⏸️ Aplicando backpressure - pausa de {}ms", pauseDurationMs);
            Thread.sleep(pauseDurationMs);
        }
    }

    // Getters e setters (com synchronized para consistência)
    public synchronized int getMensagensRecebidas() { return mensagensRecebidas.get(); }
    public synchronized int getMensagensProcessadas() { return mensagensProcessadas.get(); }
    public synchronized long getTempoTotalProcessamento() { return tempoTotalProcessamento.get(); }
    public synchronized boolean isBackpressureAtivo() { return backpressureAtivo; }
    public synchronized int getLagThreshold() { return lagThreshold; }
    public synchronized void setLagThreshold(int lagThreshold) { this.lagThreshold = lagThreshold; }
    public synchronized int getCpuThreshold() { return cpuThreshold; }
    public synchronized void setCpuThreshold(int cpuThreshold) { this.cpuThreshold = cpuThreshold; }
    public synchronized int getMemoryThreshold() { return memoryThreshold; }
    public synchronized void setMemoryThreshold(int memoryThreshold) { this.memoryThreshold = memoryThreshold; }
    public synchronized int getPauseDurationMs() { return pauseDurationMs; }
    public synchronized void setPauseDurationMs(int pauseDurationMs) { this.pauseDurationMs = pauseDurationMs; }
    public synchronized int getLag() { return calcularLag(); }
    public int getNumCores() { return numCores; } // ✅ NOVO getter
}