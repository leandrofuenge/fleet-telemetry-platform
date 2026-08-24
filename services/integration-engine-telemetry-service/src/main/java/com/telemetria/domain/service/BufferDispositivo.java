package com.telemetria.domain.service;

import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.telemetria.domain.entity.Telemetria;

public class BufferDispositivo {

    private final Queue<Telemetria> fila =
            new ConcurrentLinkedQueue<>();

    private volatile LocalDateTime ultimaAtualizacao =
            LocalDateTime.now();

    public Queue<Telemetria> getFila() {
        return fila;
    }

    public LocalDateTime getUltimaAtualizacao() {
        return ultimaAtualizacao;
    }

    public void atualizarTimestamp() {
        this.ultimaAtualizacao = LocalDateTime.now();
    }

    public int tamanho() {
        return fila.size();
    }

    public boolean estaVazio() {
        return fila.isEmpty();
    }
}