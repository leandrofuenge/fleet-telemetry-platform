package com.telemetria.infrastructure.integration.storage;

import java.io.InputStream;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * RF11 — Serviço de armazenamento de arquivos (Mock/MinIO)
 * 
 * Regras:
 * - Arquivos armazenados em storage externo (S3/MinIO)
 * - Nunca BLOB no banco de dados
 * - Caminhos salvos no banco (assinatura_path, foto_entrega_path)
 * 
 * NOTA: Implementação atual simula o storage. 
 * Para produção, adicionar dependência 'io.minio:minio' no pom.xml
 */
@Service
public class MinioStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    @Value("${minio.bucket:entregas}")
    private String bucketName;

    @Value("${minio.enabled:false}")
    private boolean minioEnabled;

    public MinioStorageService() {
        log.info("📦 MinioStorageService iniciado (enabled={})", minioEnabled);
    }

    /**
     * Upload de arquivo para o storage
     * 
     * @param inputStream Stream do arquivo
     * @param fileName    Nome original do arquivo
     * @param contentType MIME type (ex: image/jpeg, image/png)
     * @param tenantId    ID do tenant para organização
     * @param viagemId    ID da viagem
     * @return Path do arquivo no storage (para salvar no banco)
     */
    public String uploadArquivo(InputStream inputStream, String fileName, 
                                String contentType, Long tenantId, Long viagemId) {
        // Simulação: apenas gera um path válido
        String extensao = extrairExtensao(fileName);
        String objectName = gerarObjectName(tenantId, viagemId, extensao);
        
        log.info("📤 Arquivo 'uploadado' (simulação): {}/{} (tipo: {})", 
                bucketName, objectName, contentType);
        
        // TODO: Implementar upload real para MinIO quando adicionar a dependência
        // minioClient.putObject(PutObjectArgs.builder()...)
        
        return objectName;
    }

    /**
     * Remove arquivo do storage
     */
    public void removerArquivo(String objectName) {
        log.info("🗑️ Arquivo 'removido' (simulação): {}/{}", bucketName, objectName);
        // TODO: Implementar remoção real quando adicionar a dependência
    }

    /**
     * Verifica se arquivo existe no storage
     * Na simulação: sempre retorna true se o path não for nulo/vazio
     */
    public boolean arquivoExiste(String objectName) {
        if (objectName == null || objectName.trim().isEmpty()) {
            return false;
        }
        
        // Simulação: assume que o arquivo existe
        // TODO: Implementar verificação real com minioClient.statObject()
        return true;
    }

    /**
     * Gera URL pré-assinada para acesso temporário
     */
    public String gerarUrlPresignada(String objectName, int expiracaoSegundos) {
        // TODO: Implementar quando adicionar dependência do MinIO
        return null;
    }

    /**
     * Gera nome único para o objeto no storage
     */
    private String gerarObjectName(Long tenantId, Long viagemId, String extensao) {
        return String.format("tenant-%d/viagem-%d/%s.%s",
                tenantId, viagemId, UUID.randomUUID().toString(), extensao);
    }

    /**
     * Extrai extensão do arquivo
     */
    private String extrairExtensao(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "jpg";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Valida tipo de arquivo permitido
     */
    public boolean isTipoArquivoPermitido(String contentType) {
        if (contentType == null) return false;

        return contentType.equals("image/jpeg") ||
               contentType.equals("image/png") ||
               contentType.equals("image/webp") ||
               contentType.equals("application/pdf");
    }
}
