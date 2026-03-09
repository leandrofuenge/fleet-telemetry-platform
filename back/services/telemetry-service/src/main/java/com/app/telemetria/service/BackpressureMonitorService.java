package com.app.telemetria.service;

import org.springframework.stereotype.Service;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class BackpressureMonitorService {

    // Contadores para monitoramento
    private final AtomicInteger mensagensRecebidas = new AtomicInteger(0);
    private final AtomicInteger mensagensProcessadas = new AtomicInteger(0);
    private final AtomicLong tempoTotalProcessamento = new AtomicLong(0);
    
    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;
    
    // Limiares configuráveis
    private int queueMaxSize = 1000;
    private int lagThreshold = 500;
    private int cpuThreshold = 80;
    private int memoryThreshold = 80;
    private int pauseDurationMs = 1000;
    
    // Estado do backpressure
    private boolean backpressureAtivo = false;
    private long ultimoPico = 0;

    public BackpressureMonitorService() {
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
    }

    /**
     * Registra recebimento de mensagem
     */
    public void registrarRecebimento() {
        mensagensRecebidas.incrementAndGet();
    }

    /**
     * Registra processamento de mensagem
     */
    public void registrarProcessamento(long tempoMs) {
        mensagensProcessadas.incrementAndGet();
        tempoTotalProcessamento.addAndGet(tempoMs);
    }

    /**
     * Calcula o lag atual (mensagens não processadas)
     */
    public int calcularLag() {
        return mensagensRecebidas.get() - mensagensProcessadas.get();
    }

    /**
     * Calcula a taxa média de processamento (msg/segundo)
     */
    public double calcularTaxaProcessamento() {
        long totalTempo = tempoTotalProcessamento.get();
        int processadas = mensagensProcessadas.get();
        
        if (totalTempo == 0 || processadas == 0) return 0;
        
        return (processadas * 1000.0) / totalTempo;
    }

    /**
     * Verifica uso de CPU
     */
    public double getCpuUsage() {
        return osBean.getSystemLoadAverage() * 100;
    }

    /**
     * Verifica uso de memória
     */
    public double getMemoryUsage() {
        long used = memoryBean.getHeapMemoryUsage().getUsed();
        long max = memoryBean.getHeapMemoryUsage().getMax();
        return (used * 100.0) / max;
    }

    /**
     * Verifica se precisa aplicar backpressure
     */
    public boolean precisaBackpressure() {
        int lag = calcularLag();
        double cpu = getCpuUsage();
        double memory = getMemoryUsage();
        
        boolean lagExcedido = lag > lagThreshold;
        boolean cpuExcedido = cpu > cpuThreshold;
        boolean memoryExcedido = memory > memoryThreshold;
        
        if (lagExcedido || cpuExcedido || memoryExcedido) {
            if (!backpressureAtivo) {
                System.out.println("🚨 BACKPRESSURE ATIVADO!");
                System.out.println("   - Lag: " + lag + " mensagens (limite: " + lagThreshold + ")");
                System.out.println("   - CPU: " + String.format("%.1f", cpu) + "% (limite: " + cpuThreshold + "%)");
                System.out.println("   - Memória: " + String.format("%.1f", memory) + "% (limite: " + memoryThreshold + "%)");
                backpressureAtivo = true;
                ultimoPico = System.currentTimeMillis();
            }
            return true;
        }
        
        if (backpressureAtivo) {
            System.out.println("✅ Backpressure desativado - sistema normalizado");
            backpressureAtivo = false;
        }
        
        return false;
    }

    /**
     * Aplica backpressure (pausa a execução)
     */
    public void aplicarBackpressure() throws InterruptedException {
        if (precisaBackpressure()) {
            System.out.println("⏸️ Aplicando backpressure - pausa de " + pauseDurationMs + "ms");
            Thread.sleep(pauseDurationMs);
        }
    }

    /**
     * Obtém estatísticas detalhadas
     */
    public void imprimirEstatisticas() {
        int lag = calcularLag();
        double taxa = calcularTaxaProcessamento();
        double cpu = getCpuUsage();
        double memory = getMemoryUsage();
        
        System.out.println("\n📊 ESTATÍSTICAS DE BACKPRESSURE");
        System.out.println("================================");
        System.out.println("📥 Mensagens recebidas: " + mensagensRecebidas.get());
        System.out.println("✅ Mensagens processadas: " + mensagensProcessadas.get());
        System.out.println("⏳ Lag atual: " + lag + " mensagens");
        System.out.println("⚡ Taxa processamento: " + String.format("%.2f", taxa) + " msg/s");
        System.out.println("💻 CPU: " + String.format("%.1f", cpu) + "%");
        System.out.println("🧠 Memória: " + String.format("%.1f", memory) + "%");
        System.out.println("🚦 Backpressure ativo: " + (backpressureAtivo ? "SIM" : "NÃO"));
        
        if (lag > 0) {
            long tempoEstimado = (long)(lag / taxa * 1000);
            System.out.println("⏱️ Tempo estimado para recuperação: " + tempoEstimado + "ms");
        }
    }

    // ===== NOVOS MÉTODOS ADICIONADOS =====
    
    /**
     * Retorna o total de mensagens recebidas
     */
    public int getMensagensRecebidas() {
        return mensagensRecebidas.get();
    }

    /**
     * Retorna o total de mensagens processadas
     */
    public int getMensagensProcessadas() {
        return mensagensProcessadas.get();
    }

    /**
     * Retorna o tempo total de processamento
     */
    public long getTempoTotalProcessamento() {
        return tempoTotalProcessamento.get();
    }

    /**
     * Verifica se o backpressure está ativo
     */
    public boolean isBackpressureAtivo() {
        return backpressureAtivo;
    }

    // Getters e Setters para configurações
    public void setQueueMaxSize(int queueMaxSize) { 
        this.queueMaxSize = queueMaxSize; 
    }
    
    public void setLagThreshold(int lagThreshold) { 
        this.lagThreshold = lagThreshold; 
    }
    
    public void setCpuThreshold(int cpuThreshold) { 
        this.cpuThreshold = cpuThreshold; 
    }
    
    public void setMemoryThreshold(int memoryThreshold) { 
        this.memoryThreshold = memoryThreshold; 
    }
    
    public void setPauseDurationMs(int pauseDurationMs) { 
        this.pauseDurationMs = pauseDurationMs; 
    }
    
    public int getLag() { 
        return calcularLag(); 
    }
}