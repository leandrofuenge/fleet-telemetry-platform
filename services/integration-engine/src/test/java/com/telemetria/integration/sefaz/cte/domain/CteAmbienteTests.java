package com.telemetria.integration.sefaz.cte.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.telemetria.integration.sefaz.cte.exception.CteException;

class CteAmbienteTests {
    @Test
    void converteHomologacaoParaTpAmbDois() {
        assertThat(CteAmbiente.from("homologacao").codigo()).isEqualTo("2");
        assertThat(CteAmbiente.from("homologação").codigo()).isEqualTo("2");
        assertThat(CteAmbiente.from("2")).isEqualTo(CteAmbiente.HOMOLOGACAO);
    }

    @Test
    void converteProducaoParaTpAmbUm() {
        assertThat(CteAmbiente.from("producao").codigo()).isEqualTo("1");
        assertThat(CteAmbiente.from("produção").codigo()).isEqualTo("1");
        assertThat(CteAmbiente.from("1")).isEqualTo(CteAmbiente.PRODUCAO);
    }

    @Test
    void rejeitaAmbienteDesconhecido() {
        assertThatThrownBy(() -> CteAmbiente.from("teste"))
                .isInstanceOf(CteException.class).hasMessageContaining("Ambiente CT-e inválido");
    }
}
