package com.telemetria.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "mensagens_viagem", indexes = @Index(name = "idx_msg_viagem", columnList = "viagem_id,criado_em"))
public class MensagemViagem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "tenant_id", nullable = false) private Long tenantId;
    @Column(name = "viagem_id", nullable = false) private Long viagemId;
    @Column(name = "remetente_uuid", nullable = false) private String remetenteUuid;
    @Column(name = "tipo_remetente", nullable = false) private String tipoRemetente;
    @Column(nullable = false, columnDefinition = "TEXT") private String conteudo;
    @Column(name = "tipo_conteudo", nullable = false) private String tipoConteudo = "TEXTO";
    @Column(name = "transcricao", columnDefinition = "TEXT") private String transcricao;
    @Column(nullable = false) private Boolean lida = false;
    @Column(name = "criado_em", nullable = false) private LocalDateTime criadoEm = LocalDateTime.now();
    public String getTipoRemetente() { return tipoRemetente; }
    public void setTipoRemetente(String value) { tipoRemetente = value; }
    public String getTipoConteudo() { return tipoConteudo; }
    public void setTipoConteudo(String value) { tipoConteudo = value; }
}
