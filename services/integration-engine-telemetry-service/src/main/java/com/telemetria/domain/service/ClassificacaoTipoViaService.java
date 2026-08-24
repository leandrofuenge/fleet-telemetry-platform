package com.telemetria.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.telemetria.domain.enums.TipoVia;
import com.telemetria.infrastructure.integration.geocoding.LocationClassifierService;
import com.telemetria.infrastructure.integration.routing.OSRMRoutingGateway;

/**
 * RN-ROT-002
 * Responsável por determinar o tipo de via associado a uma coordenada.
 */
@Service
public class ClassificacaoTipoViaService {

    private static final Logger log =
            LoggerFactory.getLogger(ClassificacaoTipoViaService.class);

    @Autowired
    private LocationClassifierService locationClassifierService;

    @Autowired
    private OSRMRoutingGateway osrmRoutingGateway;

    /**
     * Classifica o tipo de via para uma coordenada.
     */
    public TipoVia classificarTipoVia(
            double latitude,
            double longitude) {

        log.debug(
                "🔍 Classificando tipo de via para ({}, {})",
                latitude,
                longitude);

        try {

            // Primeira estratégia:
            // classificação geográfica baseada na localização.
            String classificacaoLocalizacao =
                    locationClassifierService.classify(
                            latitude,
                            longitude);

            if (classificacaoLocalizacao != null) {

                TipoVia tipoVia =
                        TipoVia.fromClassificacao(
                                classificacaoLocalizacao);

                log.debug(
                        "📍 Tipo identificado: {} ({}m)",
                        tipoVia.getDescricao(),
                        tipoVia.getToleranciaMetros());

                return tipoVia;
            }

            // Segunda estratégia:
            // classificação via OSRM/OSM.
            String tipoViaOSM =
                    osrmRoutingGateway.obterTipoVia(
                            latitude,
                            longitude);

            if (tipoViaOSM != null) {

                TipoVia tipoVia =
                        classificarPorTipoViaOSM(
                                tipoViaOSM);

                log.debug(
                        "📍 Tipo identificado via OSRM: {} (OSM={})",
                        tipoVia.getDescricao(),
                        tipoViaOSM);

                return tipoVia;
            }

        } catch (Exception e) {

            log.warn(
                    "⚠️ Erro durante classificação da via: {}",
                    e.getMessage());
        }

        log.debug(
                "📍 Utilizando classificação padrão: {}",
                TipoVia.RODOVIA.getDescricao());

        return TipoVia.RODOVIA;
    }

    /**
     * Converte o highway type do OpenStreetMap
     * para o enum de domínio.
     */
    private TipoVia classificarPorTipoViaOSM(
            String tipoViaOSM) {

        if (tipoViaOSM == null) {
            return TipoVia.RODOVIA;
        }

        String tipoNormalizado =
                tipoViaOSM.toLowerCase();

        // Área urbana
        if (tipoNormalizado.equals("residential")
                || tipoNormalizado.equals("living_street")
                || tipoNormalizado.equals("pedestrian")
                || tipoNormalizado.equals("service")
                || tipoNormalizado.equals("tertiary")
                || tipoNormalizado.equals("secondary")) {

            return TipoVia.URBANO;
        }

        // Rodovia
        if (tipoNormalizado.equals("motorway")
                || tipoNormalizado.equals("trunk")
                || tipoNormalizado.equals("primary")
                || tipoNormalizado.equals("motorway_link")
                || tipoNormalizado.equals("trunk_link")) {

            return TipoVia.RODOVIA;
        }

        // Zona industrial
        if (tipoNormalizado.equals("industrial")
                || tipoNormalizado.equals("commercial")) {

            return TipoVia.PORTO_INDUSTRIAL;
        }

        return TipoVia.RODOVIA;
    }

    /**
     * Obtém a tolerância configurada para o tipo de via.
     */
    public double obterTolerancia(
            TipoVia tipoVia) {

        return tipoVia.getToleranciaMetros();
    }

    /**
     * Verifica se a distância calculada
     * está dentro da tolerância permitida.
     */
    public boolean estaDentroDaTolerancia(
            TipoVia tipoVia,
            double distanciaMetros) {

        return distanciaMetros
                <= tipoVia.getToleranciaMetros();
    }
}