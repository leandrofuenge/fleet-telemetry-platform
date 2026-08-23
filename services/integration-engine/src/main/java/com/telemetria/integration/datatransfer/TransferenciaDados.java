package com.telemetria.integration.datatransfer;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transferencias_dados")
public class TransferenciaDados {
    @Id
    private UUID id;

    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Column(name = "tipo_documento", nullable = false, length = 20)
    private String tipoDocumento;

    @Column(nullable = false, length = 30)
    private String operacao;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "conteudo_hash", nullable = false, length = 64)
    private String conteudoHash;

    @Column(name = "tamanho_original_bytes", nullable = false)
    private Integer tamanhoOriginalBytes;

    @Column(name = "tamanho_base64_chars", nullable = false)
    private Integer tamanhoBase64Chars;

    @Column(name = "compactado_gzip", nullable = false)
    private Boolean compactadoGzip;

    @Column(name = "soap_envelopado", nullable = false)
    private Boolean soapEnvelopado;

    @Column(length = 500)
    private String mensagem;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    protected TransferenciaDados() {
    }

    public static TransferenciaDados sucesso(String correlationId, Base64TransferRequest request,
            Base64TransferResponse response, String conteudoHash) {
        TransferenciaDados transferencia = base(correlationId, request, conteudoHash);
        transferencia.status = "SUCESSO";
        transferencia.tamanhoOriginalBytes = response.getTamanhoBytesOriginal();
        transferencia.tamanhoBase64Chars = response.getTamanhoBytesBase64();
        transferencia.mensagem = limitar(response.getMensagem());
        return transferencia;
    }

    public static TransferenciaDados falha(String correlationId, Base64TransferRequest request,
            String conteudoHash, Exception exception) {
        TransferenciaDados transferencia = base(correlationId, request, conteudoHash);
        transferencia.status = "FALHA";
        transferencia.mensagem = limitar(exception.getMessage());
        return transferencia;
    }

    private static TransferenciaDados base(String correlationId, Base64TransferRequest request, String conteudoHash) {
        TransferenciaDados transferencia = new TransferenciaDados();
        transferencia.id = UUID.randomUUID();
        transferencia.correlationId = correlationId;
        transferencia.tipoDocumento = normalizarTipo(request.getTipoDocumento());
        transferencia.operacao = "BASE64_TRANSFER";
        transferencia.conteudoHash = conteudoHash;
        transferencia.tamanhoOriginalBytes = 0;
        transferencia.tamanhoBase64Chars = 0;
        transferencia.compactadoGzip = request.isCompactarRespostaGzip();
        transferencia.soapEnvelopado = request.isEnveloparSoap();
        transferencia.criadoEm = LocalDateTime.now();
        return transferencia;
    }

    private static String normalizarTipo(String tipoDocumento) {
        return tipoDocumento == null || tipoDocumento.isBlank() ? "CTE" : tipoDocumento.toUpperCase();
    }

    private static String limitar(String texto) {
        if (texto == null) return null;
        return texto.length() <= 500 ? texto : texto.substring(0, 500);
    }
}
