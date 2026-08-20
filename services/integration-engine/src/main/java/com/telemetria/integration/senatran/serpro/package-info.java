/**
 * Integração de consulta veicular SENATRAN/SERPRO via API RADAR da InfoSimples.
 *
 * <p>Os tipos são organizados por responsabilidade: {@code api} expõe o endpoint,
 * {@code application} coordena o caso de uso, {@code domain} contém os contratos e
 * regras compartilhadas, e {@code infrastructure} concentra os adaptadores técnicos.
 *
 * <p>Contrato de entrada: placa brasileira e RENAVAM com dígito verificador válido.
 * O token é exigido pelo fornecedor na query string e, por isso, a URI nunca deve ser
 * registrada em logs. O cliente aplica timeout, repetição apenas em HTTP 429, HTTP 5xx
 * ou falha de conexão, e publica a métrica {@code integration.serpro.request}.</p>
 * O endpoint exige {@code X-Integration-API-Key}, propaga {@code X-Correlation-ID}
 * e utiliza cache curto em memória com chave SHA-256. Em múltiplas réplicas, rate
 * limit, circuit breaker e cache permanecem isolados por instância.</p>
 *
 * <p>A decisão de aptidão bloqueia os estados configurados e, por segurança, estados
 * desconhecidos. Dados veiculares são pessoais/operacionais: não devem ser registrados
 * integralmente nem persistidos sem base legal e política explícita de retenção.</p>
 */
package com.telemetria.integration.senatran.serpro;
