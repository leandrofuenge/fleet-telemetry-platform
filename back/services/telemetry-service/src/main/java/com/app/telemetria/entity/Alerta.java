package com.app.telemetria.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alertas")
public class Alerta {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "veiculo_id")
    private Veiculo veiculo;
    
    @ManyToOne
    @JoinColumn(name = "motorista_id")
    private Motorista motorista;
    
    @ManyToOne
    @JoinColumn(name = "viagem_id")
    private Viagem viagem;
    
    @Column(nullable = false)
    private String tipo; // EXCESSO_VELOCIDADE, PARADA_PROLONGADA, INICIO_VIAGEM, FIM_VIAGEM, etc
    
    @Column(nullable = false)
    private String gravidade; // BAIXA, MEDIA, ALTA, CRITICA
    
    private String mensagem;
    private Double latitude;
    private Double longitude;
    private Double velocidade;
    private Double odometro;
    
    @Column(name = "data_hora")
    private LocalDateTime dataHora;
    
    private Boolean lido = false;
    private Boolean resolvido = false;
    
    @Column(name = "data_hora_leitura")
    private LocalDateTime dataHoraLeitura;
    
    @Column(name = "data_hora_resolucao")
    private LocalDateTime dataHoraResolucao;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (dataHora == null) {
            dataHora = LocalDateTime.now();
        }
    }
    
    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Veiculo getVeiculo() { return veiculo; }
    public void setVeiculo(Veiculo veiculo) { this.veiculo = veiculo; }
    
    public Motorista getMotorista() { return motorista; }
    public void setMotorista(Motorista motorista) { this.motorista = motorista; }
    
    public Viagem getViagem() { return viagem; }
    public void setViagem(Viagem viagem) { this.viagem = viagem; }
    
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    
    public String getGravidade() { return gravidade; }
    public void setGravidade(String gravidade) { this.gravidade = gravidade; }
    
    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
    
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    
    public Double getVelocidade() { return velocidade; }
    public void setVelocidade(Double velocidade) { this.velocidade = velocidade; }
    
    public Double getOdometro() { return odometro; }
    public void setOdometro(Double odometro) { this.odometro = odometro; }
    
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    
    public Boolean getLido() { return lido; }
    public void setLido(Boolean lido) { this.lido = lido; }
    
    public Boolean getResolvido() { return resolvido; }
    public void setResolvido(Boolean resolvido) { this.resolvido = resolvido; }
    
    public LocalDateTime getDataHoraLeitura() { return dataHoraLeitura; }
    public void setDataHoraLeitura(LocalDateTime dataHoraLeitura) { this.dataHoraLeitura = dataHoraLeitura; }
    
    public LocalDateTime getDataHoraResolucao() { return dataHoraResolucao; }
    public void setDataHoraResolucao(LocalDateTime dataHoraResolucao) { this.dataHoraResolucao = dataHoraResolucao; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}