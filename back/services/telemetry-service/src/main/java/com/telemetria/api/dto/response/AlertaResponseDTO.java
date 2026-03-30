package com.telemetria.api.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

import org.slf4j.Logger;

import com.telemetria.domain.entity.Alerta;
import com.telemetria.domain.enums.SeveridadeAlerta;
import com.telemetria.domain.enums.TipoAlerta;

public class AlertaResponseDTO {
    
    private Long id;
    private String uuid;
    private Long tenantId;
    private Long veiculoId;
    private String veiculoUuid;
    private Long motoristaId;
    private Long viagemId;
    private TipoAlerta tipo;
    private SeveridadeAlerta severidade;
    private String categoria;
    private String mensagem;
    private Double latitude;
    private Double longitude;
    private Double velocidadeKmh;
    private Double odometroKm;
    private String nomeLocal;
    private LocalDateTime dataHora;
    private Boolean lido;
    private Boolean resolvido;
    private LocalDateTime criadoEm;
    private Map<String, Object> dadosContexto;
    
    // Dados adicionais
    private String veiculoPlaca;
    private String motoristaNome;
    private String viagemOrigem;
    private String viagemDestino;

    // Construtor vazio (necessário para o fallback)
    public AlertaResponseDTO() {}

    public AlertaResponseDTO(Alerta alerta) {
        if (alerta == null) return;
        
        this.id = alerta.getId();
        this.uuid = alerta.getUuid();
        this.tenantId = alerta.getTenantId();
        this.veiculoId = alerta.getVeiculoId();
        this.veiculoUuid = alerta.getVeiculoUuid();
        this.motoristaId = alerta.getMotoristaId();
        this.viagemId = alerta.getViagemId();
        this.tipo = alerta.getTipo();
        this.severidade = alerta.getSeveridade();
        this.categoria = alerta.getCategoria();
        this.mensagem = alerta.getMensagem();
        this.latitude = alerta.getLatitude();
        this.longitude = alerta.getLongitude();
        this.velocidadeKmh = alerta.getVelocidadeKmh();
        this.odometroKm = alerta.getOdometroKm();
        this.nomeLocal = alerta.getNomeLocal();
        this.dataHora = alerta.getDataHora();
        this.lido = alerta.getLido();
        this.resolvido = alerta.getResolvido();
        this.criadoEm = alerta.getCriadoEm();
        this.dadosContexto = alerta.getDadosContexto();
        
        // Carregar dados dos relacionamentos com try-catch para evitar LazyInitializationException
        try {
            if (alerta.getVeiculo() != null) {
                this.veiculoPlaca = alerta.getVeiculo().getPlaca();
            }
        } catch (Exception e) {
            log.warn("Não foi possível carregar dados do veículo para alerta {}", alerta.getId());
        }
        
        try {
            if (alerta.getMotorista() != null) {
                this.motoristaNome = alerta.getMotorista().getNome();
            }
        } catch (Exception e) {
            log.warn("Não foi possível carregar dados do motorista para alerta {}", alerta.getId());
        }
        
        try {
            if (alerta.getViagem() != null && alerta.getViagem().getRota() != null) {
                this.viagemOrigem = alerta.getViagem().getRota().getOrigem();
                this.viagemDestino = alerta.getViagem().getRota().getDestino();
            }
        } catch (Exception e) {
            log.warn("Não foi possível carregar dados da viagem para alerta {}", alerta.getId());
        }
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    
    public Long getVeiculoId() { return veiculoId; }
    public void setVeiculoId(Long veiculoId) { this.veiculoId = veiculoId; }
    
    public String getVeiculoUuid() { return veiculoUuid; }
    public void setVeiculoUuid(String veiculoUuid) { this.veiculoUuid = veiculoUuid; }
    
    public Long getMotoristaId() { return motoristaId; }
    public void setMotoristaId(Long motoristaId) { this.motoristaId = motoristaId; }
    
    public Long getViagemId() { return viagemId; }
    public void setViagemId(Long viagemId) { this.viagemId = viagemId; }
    
    public TipoAlerta getTipo() { return tipo; }
    public void setTipo(TipoAlerta tipo) { this.tipo = tipo; }
    
    public SeveridadeAlerta getSeveridade() { return severidade; }
    public void setSeveridade(SeveridadeAlerta severidade) { this.severidade = severidade; }
    
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    
    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
    
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    
    public Double getVelocidadeKmh() { return velocidadeKmh; }
    public void setVelocidadeKmh(Double velocidadeKmh) { this.velocidadeKmh = velocidadeKmh; }
    
    public Double getOdometroKm() { return odometroKm; }
    public void setOdometroKm(Double odometroKm) { this.odometroKm = odometroKm; }
    
    public String getNomeLocal() { return nomeLocal; }
    public void setNomeLocal(String nomeLocal) { this.nomeLocal = nomeLocal; }
    
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    
    public Boolean getLido() { return lido; }
    public void setLido(Boolean lido) { this.lido = lido; }
    
    public Boolean getResolvido() { return resolvido; }
    public void setResolvido(Boolean resolvido) { this.resolvido = resolvido; }
    
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    
    public Map<String, Object> getDadosContexto() { return dadosContexto; }
    public void setDadosContexto(Map<String, Object> dadosContexto) { this.dadosContexto = dadosContexto; }
    
    public String getVeiculoPlaca() { return veiculoPlaca; }
    public void setVeiculoPlaca(String veiculoPlaca) { this.veiculoPlaca = veiculoPlaca; }
    
    public String getMotoristaNome() { return motoristaNome; }
    public void setMotoristaNome(String motoristaNome) { this.motoristaNome = motoristaNome; }
    
    public String getViagemOrigem() { return viagemOrigem; }
    public void setViagemOrigem(String viagemOrigem) { this.viagemOrigem = viagemOrigem; }
    
    public String getViagemDestino() { return viagemDestino; }
    public void setViagemDestino(String viagemDestino) { this.viagemDestino = viagemDestino; }
    
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AlertaResponseDTO.class);
}