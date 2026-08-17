# Implementação RF22–RF31

- Fiscal: MDF-e, CT-e, CIOT e cache de RNTRC, com bloqueio de viagem por MDF-e/CT-e pendente e encerramento automático do MDF-e.
- Comunicação e ocorrências: histórico por viagem, restrição de mensagem digitada em movimento e geofence temporária para ocorrência localizada.
- Sinistros e multas: sinistro aberto automaticamente por colisão/pânico, preservando o evento de telemetria; registro auditável de multas.
- VRP: jobs assíncronos, seleção de solver por quantidade de pontos e plano sujeito a revisão operacional.
- API pública: cadastro de clientes, escopos e webhooks HTTPS com segredo para assinatura HMAC.

As estruturas são criadas pela migration `V6`; a ativação do Flyway permanece um procedimento controlado de implantação.
