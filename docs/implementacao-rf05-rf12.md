# Implementação RF05–RF12

Esta entrega conclui o fluxo operacional de telemetria, posição, geofences, rotas, viagens, desvios, comprovante de entrega e alertas.

- RF05: ingestão Kafka com prioridades, validação GPS, snap OSRM persistido e compressão GSM/FIFO.
- RF06: UPSERT da posição atual, monitoramento de perda de sinal e retenção por plano (incluindo CUSTOM, limitada a cinco anos).
- RF07: cadastro, alteração, desativação e validação de cercas circulares e poligonais; cooldown de cinco minutos por veículo/cerca/evento.
- RF08: cálculo explícito em `POST /api/v1/rotas/{id}/calcular-osrm`, perfil caminhão, cache MD5 por sete dias e bloqueio de ativação sem pontos OSRM.
- RF09–RF11: validações de início de viagem, score/ETA, desvios com aprovação em 24 horas e POD no MinIO já estavam presentes e foram revisados.
- RF12: `regras_alerta` por tenant, operadores e cooldown, agrupamento acima de dez ocorrências em cinco minutos, quarentena acima de cem e escalonamento registrado conforme SLA.

## Operação

As migrações `V3` e `V4` existem, mas o ambiente atual está com Flyway desativado. Antes de produção, habilite e faça o baseline controlado das migrações; não aplique a alteração diretamente sobre uma base compartilhada sem esse procedimento.
