package com.telemetria.integration.sefaz.cte;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CteHistoricoRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public CteHistoricoRepository(
            NamedParameterJdbcTemplate jdbc
    ) {
        this.jdbc = jdbc;
    }

    private static final String INSERT_SQL = """
        INSERT INTO telemetria.cte_processamento_historico (
            cte_id,
            chave,
            status_anterior,
            status_novo,
            etapa,
            codigo,
            mensagem,
            criado_em
        )
        SELECT
            id,
            chave,
            :status_anterior,
            :status_novo,
            :etapa,
            :codigo,
            :mensagem,
            CURRENT_TIMESTAMP
        FROM telemetria.cte_processamento
        WHERE chave = :chave
        """;

    public void registrar(
            String chave,
            CteStatus statusAnterior,
            CteStatus statusNovo,
            String etapa,
            String codigo,
            String mensagem
    ) {

        MapSqlParameterSource params =
                new MapSqlParameterSource()
                        .addValue("chave", chave)
                        .addValue(
                                "status_anterior",
                                statusAnterior != null
                                        ? statusAnterior.name()
                                        : null
                        )
                        .addValue(
                                "status_novo",
                                statusNovo.name()
                        )
                        .addValue("etapa", etapa)
                        .addValue("codigo", codigo)
                        .addValue("mensagem", mensagem);

        int linhas =
                jdbc.update(
                        INSERT_SQL,
                        params
                );

        if (linhas == 0) {

            throw new CteException(
                    "Não foi possível registrar histórico. " +
                    "CT-e não encontrado: " + chave
            );
        }
    }
}