package com.app.routing.dto;

import com.fasterxml.jackson.databind.JsonNode;

public class RouteResponse {

    private double distanciaKm;
    private double duracaoMinutos;
    private JsonNode geometria;

    public RouteResponse(double distanciaKm,
                         double duracaoMinutos,
                         JsonNode geometria) {
        this.distanciaKm = distanciaKm;
        this.duracaoMinutos = duracaoMinutos;
        this.geometria = geometria;
    }

    public double getDistanciaKm() {
        return distanciaKm;
    }

    public double getDuracaoMinutos() {
        return duracaoMinutos;
    }

    public JsonNode getGeometria() {
        return geometria;
    }
}