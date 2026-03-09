package com.app.telemetria.entity;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "telemetria")
public class Telemetria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "veiculo_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Veiculo veiculo;

    private Double latitude;
    private Double longitude;
    private Double velocidade;
    private Double odometro;
    private Double altitude;
    private Double precisaoGps;
    private Integer satelites;
    private Double direcao;
    private Double aceleracao;
    private Double inclinacao;

    private Boolean ignicao;
    private Double nivelCombustivel;
    private Double temperaturaMotor;
    private Double pressaoOleo;
    private Double consumoCombustivel;
    private Double tensaoBateria;
    private Double cargaMotor;
    private Double torqueMotor;

    private Double rpm;
    private Boolean frenagemBrusca;
    private Integer numeroFrenagens;
    private Integer numeroAceleracoesBruscas;
    private Integer pontuacaoMotorista;
    private Integer tempoMotorLigado;
    private Integer tempoOcioso;

    private Boolean colisaoDetectada;
    private Boolean excessoVelocidade;
    private Boolean geofenceViolada;
    private Boolean cintoSeguranca;
    private Boolean portaAberta;

    private Double temperaturaExterna;
    private Double umidadeExterna;
    private Boolean chuvaDetectada;

    private Double sinalGsm;
    private Double sinalGps;
    private String firmwareVersao;
    private String imeiDispositivo;

    private Boolean manutencaoPendente;
    private LocalDateTime proximaRevisao;
    private Double horasMotor;
    private Double desgasteFreio;

    private LocalDateTime dataHora;

    public Telemetria() {}


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Veiculo getVeiculo() { return veiculo; }
    public void setVeiculo(Veiculo veiculo) { this.veiculo = veiculo; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getVelocidade() { return velocidade; }
    public void setVelocidade(Double velocidade) { this.velocidade = velocidade; }

    public Double getOdometro() { return odometro; }
    public void setOdometro(Double odometro) { this.odometro = odometro; }

    public Double getAltitude() { return altitude; }
    public void setAltitude(Double altitude) { this.altitude = altitude; }

    public Double getPrecisaoGps() { return precisaoGps; }
    public void setPrecisaoGps(Double precisaoGps) { this.precisaoGps = precisaoGps; }

    public Integer getSatelites() { return satelites; }
    public void setSatelites(Integer satelites) { this.satelites = satelites; }

    public Double getDirecao() { return direcao; }
    public void setDirecao(Double direcao) { this.direcao = direcao; }

    public Double getAceleracao() { return aceleracao; }
    public void setAceleracao(Double aceleracao) { this.aceleracao = aceleracao; }

    public Double getInclinacao() { return inclinacao; }
    public void setInclinacao(Double inclinacao) { this.inclinacao = inclinacao; }

    public Boolean getIgnicao() { return ignicao; }
    public void setIgnicao(Boolean ignicao) { this.ignicao = ignicao; }

    public Double getNivelCombustivel() { return nivelCombustivel; }
    public void setNivelCombustivel(Double nivelCombustivel) { this.nivelCombustivel = nivelCombustivel; }

    public Double getTemperaturaMotor() { return temperaturaMotor; }
    public void setTemperaturaMotor(Double temperaturaMotor) { this.temperaturaMotor = temperaturaMotor; }

    public Double getPressaoOleo() { return pressaoOleo; }
    public void setPressaoOleo(Double pressaoOleo) { this.pressaoOleo = pressaoOleo; }

    public Double getConsumoCombustivel() { return consumoCombustivel; }
    public void setConsumoCombustivel(Double consumoCombustivel) { this.consumoCombustivel = consumoCombustivel; }

    public Double getTensaoBateria() { return tensaoBateria; }
    public void setTensaoBateria(Double tensaoBateria) { this.tensaoBateria = tensaoBateria; }

    public Double getCargaMotor() { return cargaMotor; }
    public void setCargaMotor(Double cargaMotor) { this.cargaMotor = cargaMotor; }

    public Double getTorqueMotor() { return torqueMotor; }
    public void setTorqueMotor(Double torqueMotor) { this.torqueMotor = torqueMotor; }

    public Double getRpm() { return rpm; }
    public void setRpm(Double rpm) { this.rpm = rpm; }

    public Boolean getFrenagemBrusca() { return frenagemBrusca; }
    public void setFrenagemBrusca(Boolean frenagemBrusca) { this.frenagemBrusca = frenagemBrusca; }

    public Integer getNumeroFrenagens() { return numeroFrenagens; }
    public void setNumeroFrenagens(Integer numeroFrenagens) { this.numeroFrenagens = numeroFrenagens; }

    public Integer getNumeroAceleracoesBruscas() { return numeroAceleracoesBruscas; }
    public void setNumeroAceleracoesBruscas(Integer numeroAceleracoesBruscas) { this.numeroAceleracoesBruscas = numeroAceleracoesBruscas; }

    public Integer getPontuacaoMotorista() { return pontuacaoMotorista; }
    public void setPontuacaoMotorista(Integer pontuacaoMotorista) { this.pontuacaoMotorista = pontuacaoMotorista; }

    public Integer getTempoMotorLigado() { return tempoMotorLigado; }
    public void setTempoMotorLigado(Integer tempoMotorLigado) { this.tempoMotorLigado = tempoMotorLigado; }

    public Integer getTempoOcioso() { return tempoOcioso; }
    public void setTempoOcioso(Integer tempoOcioso) { this.tempoOcioso = tempoOcioso; }

    public Boolean getColisaoDetectada() { return colisaoDetectada; }
    public void setColisaoDetectada(Boolean colisaoDetectada) { this.colisaoDetectada = colisaoDetectada; }

    public Boolean getExcessoVelocidade() { return excessoVelocidade; }
    public void setExcessoVelocidade(Boolean excessoVelocidade) { this.excessoVelocidade = excessoVelocidade; }

    public Boolean getGeofenceViolada() { return geofenceViolada; }
    public void setGeofenceViolada(Boolean geofenceViolada) { this.geofenceViolada = geofenceViolada; }

    public Boolean getCintoSeguranca() { return cintoSeguranca; }
    public void setCintoSeguranca(Boolean cintoSeguranca) { this.cintoSeguranca = cintoSeguranca; }

    public Boolean getPortaAberta() { return portaAberta; }
    public void setPortaAberta(Boolean portaAberta) { this.portaAberta = portaAberta; }

    public Double getTemperaturaExterna() { return temperaturaExterna; }
    public void setTemperaturaExterna(Double temperaturaExterna) { this.temperaturaExterna = temperaturaExterna; }

    public Double getUmidadeExterna() { return umidadeExterna; }
    public void setUmidadeExterna(Double umidadeExterna) { this.umidadeExterna = umidadeExterna; }

    public Boolean getChuvaDetectada() { return chuvaDetectada; }
    public void setChuvaDetectada(Boolean chuvaDetectada) { this.chuvaDetectada = chuvaDetectada; }

    public Double getSinalGsm() { return sinalGsm; }
    public void setSinalGsm(Double sinalGsm) { this.sinalGsm = sinalGsm; }

    public Double getSinalGps() { return sinalGps; }
    public void setSinalGps(Double sinalGps) { this.sinalGps = sinalGps; }

    public String getFirmwareVersao() { return firmwareVersao; }
    public void setFirmwareVersao(String firmwareVersao) { this.firmwareVersao = firmwareVersao; }

    public String getImeiDispositivo() { return imeiDispositivo; }
    public void setImeiDispositivo(String imeiDispositivo) { this.imeiDispositivo = imeiDispositivo; }

    public Boolean getManutencaoPendente() { return manutencaoPendente; }
    public void setManutencaoPendente(Boolean manutencaoPendente) { this.manutencaoPendente = manutencaoPendente; }

    public LocalDateTime getProximaRevisao() { return proximaRevisao; }
    public void setProximaRevisao(LocalDateTime proximaRevisao) { this.proximaRevisao = proximaRevisao; }

    public Double getHorasMotor() { return horasMotor; }
    public void setHorasMotor(Double horasMotor) { this.horasMotor = horasMotor; }

    public Double getDesgasteFreio() { return desgasteFreio; }
    public void setDesgasteFreio(Double desgasteFreio) { this.desgasteFreio = desgasteFreio; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
}
