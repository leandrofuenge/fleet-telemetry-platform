package com.telemetria.integration.sefaz.cte.domain;

public record CteContext(

        Long databaseId,

        CteMetadata metadata,

        String xmlOriginal,

        String xmlNormalizado,

        String xmlAssinado,

        String xmlHash,

        CteStatus status,

        Integer tentativa

) {

    public CteContext comStatus(
            CteStatus novoStatus
    ) {

        return new CteContext(
                databaseId,
                metadata,
                xmlOriginal,
                xmlNormalizado,
                xmlAssinado,
                xmlHash,
                novoStatus,
                tentativa
        );
    }

    public CteContext comXmlAssinado(
            String novoXmlAssinado
    ) {

        return new CteContext(
                databaseId,
                metadata,
                xmlOriginal,
                xmlNormalizado,
                novoXmlAssinado,
                xmlHash,
                status,
                tentativa
        );
    }

    public CteContext comHash(
            String novoHash
    ) {

        return new CteContext(
                databaseId,
                metadata,
                xmlOriginal,
                xmlNormalizado,
                xmlAssinado,
                novoHash,
                status,
                tentativa
        );
    }

    public CteContext comTentativa(
            Integer novaTentativa
    ) {

        return new CteContext(
                databaseId,
                metadata,
                xmlOriginal,
                xmlNormalizado,
                xmlAssinado,
                xmlHash,
                status,
                novaTentativa
        );
    }
}
