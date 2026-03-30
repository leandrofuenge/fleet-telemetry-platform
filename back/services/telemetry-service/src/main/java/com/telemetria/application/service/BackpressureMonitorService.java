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

    // Contadores para monitoramento
    private final AtomicInteger mensagensRecebidas = new AtomicInteger(0);
    private final AtomicInteger mensagensProcessadas = new AtomicInteger(0);
    private final AtomicLong tempoTotalProcessamento = new AtomicLong(0);

    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;

    private int lagThreshold = 500;
    private int cpuThreshold = 80;
    private int memoryThreshold = 80;
    private int pauseDurationMs = 1000;

    // Estado do backpressure
    private boolean backpressureAtivo = false;
    private long ultimoLogEstatisticas = 0;

    public BackpressureMonitorService() {
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        log.info("✅ BackpressureMonitorService inicializado");
        log.debug("📊 Configurações iniciais - LagThreshold: {}, CPUThreshold: {}, MemoryThreshold: {}, PauseDuration: {}ms",
                lagThreshold, cpuThreshold, memoryThreshold, pauseDurationMs);
    }

    /**
     * Registra recebimento de mensagem
     */
    public void registrarRecebimento() {
        int total = mensagensRecebidas.incrementAndGet();
        if (total % 100 == 0) {
            log.debug("📥 Mensagens recebidas: {}", total);
        }
        log.trace("➕ Mensagem recebida - Total: {}", total);
    }

    /**
     * Registra recebimento de mensagem com prioridade
     */
    public void registrarRecebimento(String prioridade) {
        registrarRecebimento(); // Chama o método padrão
        log.trace("📨 Mensagem {} recebida", prioridade);
    }

    /**
     * Verifica se deve descartar mensagem baseado na prioridade
     */
    public boolean deveDescartar(String prioridade) {
        if ("CRITICO".equalsIgnoreCase(prioridade)) {
            return false; // Nunca descarta crítico
        }
        return precisaBackpressure() && backpressureAtivo;
    }

    /**
     * Registra processamento de mensagem
     */
    public void registrarProcessamento(long tempoMs) {
        int processadas = mensagensProcessadas.incrementAndGet();
        long totalTempo = tempoTotalProcessamento.addAndGet(tempoMs);

        if (processadas % 100 == 0) {
            log.debug("✅ Mensagens processadas: {}, Tempo médio: {:.2f}ms",
                    processadas, (double) totalTempo / processadas);
        }
        log.trace("✅ Mensagem processada em {}ms - Total processadas: {}", tempoMs, processadas);
    }

    /**
     * Calcula o lag atual (mensagens não processadas)
     */
    public int calcularLag() {
        int lag = mensagensRecebidas.get() - mensagensProcessadas.get();
        log.trace("📊 Lag atual: {} mensagens", lag);
        return lag;
    }

    /**
     * Calcula a taxa média de processamento (msg/segundo)
     */
    public double calcularTaxaProcessamento() {
        long totalTempo = tempoTotalProcessamento.get();
        int processadas = mensagensProcessadas.get();

        if (totalTempo == 0 || processadas == 0) {
            return 0;
        }

        double taxa = (processadas * 1000.0) / totalTempo;
        log.trace("⚡ Taxa de processamento: {:.2f} msg/s", taxa);
        return taxa;
    }

    /**
     * Verifica uso de CPU
     */
    public double getCpuUsage() {
        try {
            double load = osBean.getSystemLoadAverage();
            double cpu = load * 100;

            if (cpu < 0) {
                log.warn("⚠️ Load average negativo: {}, retornando 0", load);
                return 0;
            }

            log.trace("💻 CPU Usage: {:.1f}%", cpu);
            return cpu;
        } catch (Exception e) {
            log.error("❌ Erro ao obter uso de CPU: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Verifica uso de memória
     */
    public double getMemoryUsage() {
        try {
            long used = memoryBean.getHeapMemoryUsage().getUsed();
            long max = memoryBean.getHeapMemoryUsage().getMax();

            if (max == 0) {
                log.warn("⚠️ Max memory é 0, retornando 0");
                return 0;
            }

            double memory = (used * 100.0) / max;
            log.trace("🧠 Memory Usage: {:.1f}% (Usado: {}MB, Max: {}MB)",
                    memory, used / (1024 * 1024), max / (1024 * 1024));
            return memory;
        } catch (Exception e) {
            log.error("❌ Erro ao obter uso de memória: {}", e.getMessage());
            return 0;
        }
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

        // Log detalhado a cada 10 segundos
        long agora = System.currentTimeMillis();
        if (agora - ultimoLogEstatisticas > 10000) {
            log.info("📊 Monitoramento - Lag: {}/{}, CPU: {:.1f}/{:.1f}%, Memória: {:.1f}/{:.1f}%",
                    lag, lagThreshold, cpu, (double) cpuThreshold, memory, (double) memoryThreshold);
            ultimoLogEstatisticas = agora;
        }

        if (lagExcedido || cpuExcedido || memoryExcedido) {
            if (!backpressureAtivo) {
                log.warn("🚨 BACKPRESSURE ATIVADO!");
                log.warn(" - Lag: {} mensagens (limite: {})", lag, lagThreshold);
                log.warn(" - CPU: {:.1f}% (limite: {}%)", cpu, cpuThreshold);
                log.warn(" - Memória: {:.1f}% (limite: {}%)", memory, memoryThreshold);
                backpressureAtivo = true;
            } else {
                log.debug("⚠️ Backpressure já ativo - Lag: {}, CPU: {:.1f}%, Memória: {:.1f}%",
                        lag, cpu, memory);
            }
            return true;
        }

        if (backpressureAtivo) {
            log.info("✅ Backpressure desativado - sistema normalizado");
            log.info("📊 Status atual - Lag: {}, CPU: {:.1f}%, Memória: {:.1f}%", lag, cpu, memory);
            backpressureAtivo = false;
        }

        return false;
    }

    /**
     * Aplica backpressure (pausa a execução)
     */
    public void aplicarBackpressure() throws InterruptedException {
        if (precisaBackpressure()) {
            log.warn("⏸️ Aplicando backpressure - pausa de {}ms", pauseDurationMs);
            log.debug("📊 Status antes da pausa - Recebidas: {}, Processadas: {}, Lag: {}",
                    mensagensRecebidas.get(), mensagensProcessadas.get(), calcularLag());

            Thread.sleep(pauseDurationMs);

            log.debug("✅ Pausa concluída - Recebidas: {}, Processadas: {}, Lag: {}",
                    mensagensRecebidas.get(), mensagensProcessadas.get(), calcularLag());
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
        int recebidas = mensagensRecebidas.get();
        int processadas = mensagensProcessadas.get();
        long tempoTotal = tempoTotalProcessamento.get();

        log.info("\n📊 ESTATÍSTICAS DE BACKPRESSURE");
        log.info("================================");
        log.info("📥 Mensagens recebidas: {}", recebidas);
        log.info("✅ Mensagens processadas: {}", processadas);
        log.info("⏳ Lag atual: {} mensagens", lag);
        log.info("⚡ Taxa processamento: {:.2f} msg/s", taxa);
        log.info("⏱️ Tempo médio: {:.2f}ms", processadas > 0 ? (double) tempoTotal / processadas : 0);
        log.info("💻 CPU: {:.1f}%", cpu);
        log.info("🧠 Memória: {:.1f}%", memory);
        log.info("🚦 Backpressure ativo: {}", backpressureAtivo ? "SIM" : "NÃO");

        if (lag > 0 && taxa > 0) {
            long tempoEstimado = (long) (lag / taxa * 1000);
            log.info("⏱️ Tempo estimado para recuperação: {}ms ({} segundos)",
                    tempoEstimado, tempoEstimado / 1000);
        }

        log.info("📊 Configurações atuais - Lag: {}, CPU: {}, Memória: {}, Pause: {}ms",
                lagThreshold, cpuThreshold, memoryThreshold, pauseDurationMs);
    }

    // ===== GETTERS E SETTERS COM DEBUG =====

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

    public int getLagThreshold() {
        return lagThreshold;
    }

    public void setLagThreshold(int lagThreshold) {
        log.info("⚙️ Alterando lagThreshold de {} para {}", this.lagThreshold, lagThreshold);
        this.lagThreshold = lagThreshold;
    }

    public int getCpuThreshold() {
        return cpuThreshold;
    }

    public void setCpuThreshold(int cpuThreshold) {
        log.info("⚙️ Alterando cpuThreshold de {} para {}", this.cpuThreshold, cpuThreshold);
        this.cpuThreshold = cpuThreshold;
    }

    public int getMemoryThreshold() {
        return memoryThreshold;
    }

    public void setMemoryThreshold(int memoryThreshold) {
        log.info("⚙️ Alterando memoryThreshold de {} para {}", this.memoryThreshold, memoryThreshold);
        this.memoryThreshold = memoryThreshold;
    }

    public int getPauseDurationMs() {
        return pauseDurationMs;
    }

    public void setPauseDurationMs(int pauseDurationMs) {
        log.info("⚙️ Alterando pauseDurationMs de {}ms para {}ms", this.pauseDurationMs, pauseDurationMs);
        this.pauseDurationMs = pauseDurationMs;
    }

    public void setQueueMaxSize(int queueMaxSize) {
        log.info("⚙️ Configurando queueMaxSize para {}", queueMaxSize);
        // Implementação se necessário
    }

    public int getLag() {
        return calcularLag();
    }
}