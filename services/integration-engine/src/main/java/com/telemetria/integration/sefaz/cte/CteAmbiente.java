package com.telemetria.integration.sefaz.cte;

import java.text.Normalizer;
import java.util.Locale;

/** Representação única do tpAmb previsto nos leiautes fiscais CT-e. */
public enum CteAmbiente {
    PRODUCAO("1", "producao"),
    HOMOLOGACAO("2", "homologacao");

    private final String codigo;
    private final String nomeConfiguracao;

    CteAmbiente(String codigo, String nomeConfiguracao) {
        this.codigo = codigo;
        this.nomeConfiguracao = nomeConfiguracao;
    }

    public String codigo() { return codigo; }
    public String nomeConfiguracao() { return nomeConfiguracao; }

    public static CteAmbiente from(String value) {
        if (value == null || value.isBlank()) {
            throw new CteException("Ambiente CT-e não configurado. Use homologacao/2 ou producao/1.");
        }
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "1", "producao" -> PRODUCAO;
            case "2", "homologacao" -> HOMOLOGACAO;
            default -> throw new CteException(
                    "Ambiente CT-e inválido: '" + value + "'. Use homologacao/2 ou producao/1.");
        };
    }
}
 