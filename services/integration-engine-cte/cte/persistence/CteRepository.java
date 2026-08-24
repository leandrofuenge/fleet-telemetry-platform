package com.telemetria.integration.sefaz.cte.persistence;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.telemetria.integration.sefaz.cte.domain.CteContext;
import com.telemetria.integration.sefaz.cte.domain.CteMetadata;
import com.telemetria.integration.sefaz.cte.domain.CteProcessResult;
import com.telemetria.integration.sefaz.cte.domain.CteStatus;

@Repository
public class CteRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public CteRepository(
            NamedParameterJdbcTemplate jdbc
    ) {
        this.jdbc = jdbc;
    }

    private static final String UPSERT_SQL = """
        INSERT INTO telemetria.cte_processamento (
            chave,
            numero,
            serie,
            modelo,
            versao,
            status,
            xml_hash,
            xml_tamanho_bytes,
            xml_normalizado,
            tentativa,
            criado_em,
            atualizado_em
        )
        VALUES (
            :chave,
            :numero,
            :serie,
            :modelo,
            :versao,
            :status,
            :xmlHash,
            :xmlTamanho,
            :xmlNormalizado,
            1,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        )
        ON CONFLICT (chave)
        DO UPDATE SET
            numero = EXCLUDED.numero,
            serie = EXCLUDED.serie,
            modelo = EXCLUDED.modelo,
            versao = EXCLUDED.versao,
            xml_hash = EXCLUDED.xml_hash,
            xml_tamanho_bytes = EXCLUDED.xml_tamanho_bytes,
            xml_normalizado = EXCLUDED.xml_normalizado,
            tentativa =
                telemetria.cte_processamento.tentativa + 1,
            atualizado_em = CURRENT_TIMESTAMP
        RETURNING id, tentativa, status
        """;

    public CtePersistenceData salvarOuAtualizar(
            CteContext context
    ) {

        CteMetadata metadata =
                context.metadata();

        String xml =
                context.xmlNormalizado();

        int tamanho =
                xml.getBytes(
                        java.nio.charset.StandardCharsets.UTF_8
                ).length;

        MapSqlParameterSource params =
                new MapSqlParameterSource()
                        .addValue(
                                "chave",
                                metadata.chave()
                        )
                        .addValue(
                                "numero",
                                metadata.numero()
                        )
                        .addValue(
                                "serie",
                                metadata.serie()
                        )
                        .addValue(
                                "modelo",
                                metadata.modelo()
                        )
                        .addValue(
                                "versao",
                                metadata.versao()
                        )
                        .addValue(
                                "status",
                                context.status().name()
                        )
                        .addValue(
                                "xmlHash",
                                context.xmlHash()
                        )
                        .addValue(
                                "xmlTamanho",
                                tamanho
                        )
                        .addValue(
                                "xmlNormalizado",
                                xml
                        );

        return jdbc.queryForObject(
                UPSERT_SQL,
                params,
                (rs, rowNum) ->
                        new CtePersistenceData(
                                rs.getLong("id"),
                                rs.getInt("tentativa"),
                                rs.getString("status")
                        )
        );
    }

    public void atualizarStatus(
            String chave,
            CteStatus status
    ) {

        String sql = """
            UPDATE telemetria.cte_processamento
               SET status = :status,
                   atualizado_em = CURRENT_TIMESTAMP
             WHERE chave = :chave
            """;

        MapSqlParameterSource params =
                new MapSqlParameterSource()
                        .addValue("chave", chave)
                        .addValue("status", status.name());

        jdbc.update(sql, params);
    }

    public void registrarErro(
            String chave,
            CteStatus status,
            String codigo,
            String mensagem
    ) {

        String sql = """
            UPDATE telemetria.cte_processamento
               SET status = :status,
                   erro_codigo = :codigo,
                   erro_mensagem = :mensagem,
                   atualizado_em = CURRENT_TIMESTAMP
             WHERE chave = :chave
            """;

        MapSqlParameterSource params =
                new MapSqlParameterSource()
                        .addValue("chave", chave)
                        .addValue("status", status.name())
                        .addValue("codigo", codigo)
                        .addValue("mensagem", mensagem);

        jdbc.update(sql, params);
    }

    public void registrarRespostaSefaz(
            String chave,
            CteProcessResult result
    ) {

        String sql = """
            UPDATE telemetria.cte_processamento
               SET status = :status,
                   codigo_sefaz = :codigo,
                   motivo_sefaz = :mensagem,
                   protocolo = :protocolo,
                   processado_em = CURRENT_TIMESTAMP,
                   autorizado_em =
                       CASE
                           WHEN :status = 'AUTORIZADO'
                           THEN CURRENT_TIMESTAMP
                           ELSE autorizado_em
                       END,
                   atualizado_em = CURRENT_TIMESTAMP
             WHERE chave = :chave
            """;

        MapSqlParameterSource params =
                new MapSqlParameterSource()
                        .addValue(
                                "chave",
                                chave
                        )
                        .addValue(
                                "status",
                                result.status().name()
                        )
                        .addValue(
                                "codigo",
                                result.codigo()
                        )
                        .addValue(
                                "mensagem",
                                result.mensagem()
                        )
                        .addValue(
                                "protocolo",
                                result.protocolo()
                        );

        jdbc.update(sql, params);
    }

    public record CtePersistenceData(
            Long id,
            Integer tentativa,
            String status
    ) {
    }
}
