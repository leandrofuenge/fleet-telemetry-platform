package com.app.telemetria.service;

import com.app.telemetria.entity.Alerta;
import com.app.telemetria.entity.Viagem;
import com.app.telemetria.enums.SeveridadeAlerta;
import com.app.telemetria.enums.TipoAlerta;
import com.app.telemetria.exception.WeatherApiException;
import com.app.telemetria.repository.AlertaRepository;
import com.app.telemetria.repository.VeiculoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.DoublePredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class WeatherAlertService {

    private final WebClient webClient;
    private final AlertaRepository alertaRepository;
    private final VeiculoRepository veiculoRepository;
    private final Map<Long, LocalDateTime> ultimoAlertaPorVeiculo = new HashMap<>();

    @Value("${openweather.api.key:}")
    private String apiKey;

    @Value("${openweather.api.url:https://api.openweathermap.org/data/2.5}")
    private String apiBaseUrl;

    @Value("${openweather.api.units:metric}")
    private String units;

    @Value("${openweather.api.lang:pt_br}")
    private String lang;

    // ========== CONSTRUTOR ==========
    public WeatherAlertService(AlertaRepository alertaRepository, VeiculoRepository veiculoRepository) {
        this.alertaRepository = alertaRepository;
        this.veiculoRepository = veiculoRepository;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openweathermap.org/data/2.5")
                .build();

        System.out.println("🌦️ WeatherAlertService inicializado");
    }

    @PostConstruct
    public void init() {
        System.out.println("\n🌤️ ===== CONFIGURAÇÃO DO OPENWEATHER ===== 🌤️");

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("❌ API KEY DO OPENWEATHER NÃO CONFIGURADA!");
            System.err.println("📝 Configure em application.properties: openweather.api.key=SUA_CHAVE");
            System.err.println("🔑 Chave atual: '" + apiKey + "'");
        } else {
            System.out.println("✅ API Key configurada: " + apiKey.substring(0, Math.min(5, apiKey.length())) + "...");
            System.out.println("📊 Tamanho da chave: " + apiKey.length() + " caracteres");
            System.out.println("🌐 URL Base: " + apiBaseUrl);
            System.out.println("📏 Unidades: " + units);
            System.out.println("🗣️ Idioma: " + lang);

            if (apiKey.length() != 32) {
                System.err.println("⚠️ ATENÇÃO: A chave API parece ter tamanho diferente do esperado (32 caracteres)");
                System.err.println("⚠️ Tamanho atual: " + apiKey.length() + " caracteres");
            }
        }
        System.out.println("==========================================\n");
    }

    // ========== ENUMS ==========

    private enum CondicaoClimatica {
        CHUVA_FRACA(500, "Chuva fraca", "🌧️", SeveridadeAlerta.MEDIA),
        CHUVA_MODERADA(501, "Chuva moderada", "🌧️", SeveridadeAlerta.MEDIA),
        CHUVA_FORTE(502, "Chuva forte", "🌧️⚠️", SeveridadeAlerta.ALTA),
        CHUVA_MUITO_FORTE(503, "Chuva muito forte", "🌧️⚠️", SeveridadeAlerta.ALTA),
        CHUVA_EXTREMA(504, "Chuva extrema", "🌧️⛔", SeveridadeAlerta.ALTA), // ou CRITICA se existir

        NEVE_FRACA(600, "Neve fraca", "❄️", SeveridadeAlerta.MEDIA),
        NEVE_MODERADA(601, "Neve moderada", "❄️", SeveridadeAlerta.MEDIA),
        NEVE_FORTE(602, "Neve forte", "❄️⚠️", SeveridadeAlerta.ALTA),

        TEMPESTADE(200, "Tempestade", "⛈️", SeveridadeAlerta.ALTA),
        NEBLINA(741, "Nevoeiro", "🌫️", SeveridadeAlerta.MEDIA),
        FUMAÇA(711, "Fumaça", "🔥", SeveridadeAlerta.MEDIA),
        POEIRA(761, "Poeira", "💨", SeveridadeAlerta.MEDIA);

        final int codigo;
        final String descricao;
        final String icone;
        final SeveridadeAlerta gravidadePadrao;

        CondicaoClimatica(int codigo, String descricao, String icone, SeveridadeAlerta gravidade) {
            this.codigo = codigo;
            this.descricao = descricao;
            this.icone = icone;
            this.gravidadePadrao = gravidade;
        }

        static Optional<CondicaoClimatica> fromCodigo(int codigo) {
            return Arrays.stream(values())
                    .filter(c -> c.codigo == codigo)
                    .findFirst();
        }
    }

    private enum FaixaTemperatura {
        MUITO_QUENTE(t -> t > 35, "🔥 Temperatura muito alta!", SeveridadeAlerta.ALTA),
        QUENTE(t -> t > 30, "🌡️ Temperatura elevada", SeveridadeAlerta.BAIXA),
        FRIO(t -> t < 5, "❄️ Temperatura baixa", SeveridadeAlerta.MEDIA),
        CONGELANTE(t -> t < 0, "⛔ Temperatura congelante!", SeveridadeAlerta.ALTA);

        final DoublePredicate condicao;
        final String mensagem;
        final SeveridadeAlerta gravidade;

        FaixaTemperatura(DoublePredicate condicao, String mensagem, SeveridadeAlerta gravidade) {
            this.condicao = condicao;
            this.mensagem = mensagem;
            this.gravidade = gravidade;
        }

        static Optional<FaixaTemperatura> fromTemperatura(double temp) {
            return Arrays.stream(values())
                    .filter(f -> f.condicao.test(temp))
                    .findFirst();
        }
    }

    private enum FaixaVento {
        VENTO_FORTE(v -> v > 50, "💨 VENTO FORTE! Segure firme!", SeveridadeAlerta.ALTA),
        VENTO_MODERADO(v -> v > 30, "💨 Vento moderado", SeveridadeAlerta.MEDIA);

        final DoublePredicate condicao;
        final String mensagem;
        final SeveridadeAlerta gravidade;

        FaixaVento(DoublePredicate condicao, String mensagem, SeveridadeAlerta gravidade) {
            this.condicao = condicao;
            this.mensagem = mensagem;
            this.gravidade = gravidade;
        }

        static Optional<FaixaVento> fromVelocidade(double kmh) {
            return Arrays.stream(values())
                    .filter(f -> f.condicao.test(kmh))
                    .findFirst();
        }
    }

    // ========== MODELO DA RESPOSTA ==========

    public record WeatherResponse(
            Weather[] weather,
            Main main,
            Wind wind,
            Rain rain,
            Snow snow,
            Clouds clouds) {
        public record Weather(int id, String main, String description, String icon) {
        }

        public record Main(double temp, int humidity) {
        }

        public record Wind(double speed, double gust) {
        }

        public record Rain(double _1h, double _3h) {
        }

        public record Snow(double _1h, double _3h) {
        }

        public record Clouds(int all) {
        }
    }

    // ========== MÉTODO PRINCIPAL ==========

    public void verificarClimaParaVeiculo(Long veiculoId, Double latitude, Double longitude, Viagem viagem) {
        System.out.println(
                "🌤️ Verificando clima para veículo " + veiculoId + " em (" + latitude + ", " + longitude + ")");

        if (latitude == null || longitude == null) {
            System.out.println("⚠️ Coordenadas inválidas para veículo " + veiculoId);
            return;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("❌ API Key não configurada! Pulando verificação climática.");
            return;
        }

        if (ultimoAlertaPorVeiculo.getOrDefault(veiculoId, LocalDateTime.MIN)
                .plusHours(1).isAfter(LocalDateTime.now())) {
            System.out.println("⏰ Último alerta foi há menos de 1 hora para veículo " + veiculoId);
            return;
        }

        veiculoRepository.findById(veiculoId).ifPresent(veiculo -> {
            try {
                WeatherResponse weather = getWeatherWithRetry(latitude, longitude);

                if (weather != null && weather.weather() != null && weather.weather().length > 0) {
                    String mensagem = gerarMensagemClimatica(weather);
                    SeveridadeAlerta gravidade = determinarGravidade(weather);

                    criarAlertaClimatico(veiculoId, viagem, mensagem, gravidade);
                    ultimoAlertaPorVeiculo.put(veiculoId, LocalDateTime.now());

                    System.out.println("✅ Alerta climático gerado para veículo " + veiculoId);
                } else {
                    System.out.println("⚠️ Resposta da API sem dados climáticos válidos");
                }
            } catch (Exception e) {
                System.err.println("❌ Falha ao obter dados climáticos após retries: " + e.getMessage());
            }
        });
    }

    @Retryable(retryFor = { RestClientException.class, TimeoutException.class, WeatherApiException.class,
            WebClientResponseException.class }, maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 2.0, maxDelay = 30000), recover = "recoverWeatherApi")
    public WeatherResponse getWeatherWithRetry(double lat, double lon) {
        System.out.println("🔄 Consultando OpenWeatherMap para coordenadas: " + lat + ", " + lon);

        if (apiKey == null || apiKey.isEmpty()) {
            throw new WeatherApiException("API Key não configurada");
        }

        return getWeatherForLocation(lat, lon)
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(weather -> System.out.println("✅ Consulta à OpenWeatherMap bem-sucedida"))
                .doOnError(e -> {
                    if (e instanceof WebClientResponseException.Unauthorized) {
                        System.err.println("❌ Erro 401: API Key inválida ou não autorizada");
                        System.err.println(
                                "🔑 Chave atual: " + apiKey.substring(0, Math.min(5, apiKey.length())) + "...");
                    } else {
                        System.err.println("❌ Erro na consulta: " + e.getMessage());
                    }
                })
                .block();
    }

    @Recover
    public WeatherResponse recoverWeatherApi(Exception e, double lat, double lon) {
        System.err.println("⚠️ Todas as 5 tentativas falharam. Usando fallback para coordenadas: " + lat + ", " + lon);
        System.err.println("Erro: " + e.getClass().getSimpleName() + " - " + e.getMessage());

        if (e instanceof WebClientResponseException.Unauthorized) {
            System.err.println("🔑 PROVIDENCIE: Configure a API key correta no application.properties");
        }

        return criarRespostaFallback(lat, lon);
    }

    public Mono<WeatherResponse> getWeatherForLocation(double lat, double lon) {
        System.out.println("🔑 Usando API Key: "
                + (apiKey != null ? apiKey.substring(0, Math.min(5, apiKey.length())) + "..." : "NÃO CONFIGURADA"));

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/weather")
                        .queryParam("lat", lat)
                        .queryParam("lon", lon)
                        .queryParam("appid", apiKey != null ? apiKey.trim() : "")
                        .queryParam("units", units)
                        .queryParam("lang", lang)
                        .build())
                .retrieve()
                .bodyToMono(WeatherResponse.class)
                .onErrorResume(WebClientResponseException.Unauthorized.class, e -> {
                    System.err.println("❌ ERRO 401: API Key inválida!");
                    System.err.println("🔑 Sua chave: '" + apiKey + "'");
                    System.err.println("📝 Verifique se a chave está correta no application.properties");
                    return Mono.error(new WeatherApiException("API Key inválida: 401 Unauthorized"));
                });
    }

    private String gerarMensagemClimatica(WeatherResponse weather) {
        List<String> partes = new ArrayList<>();

        Optional.ofNullable(weather.weather())
                .filter(w -> w.length > 0)
                .map(w -> w[0])
                .ifPresent(w -> {
                    CondicaoClimatica.fromCodigo(w.id())
                            .ifPresentOrElse(
                                    c -> partes.add(String.format("%s %s", c.icone, c.descricao)),
                                    () -> partes.add(String.format("☁️ %s", w.description())));
                });

        Optional.ofNullable(weather.main())
                .ifPresent(m -> {
                    partes.add(String.format("🌡️ %.1f°C", m.temp()));
                    FaixaTemperatura.fromTemperatura(m.temp())
                            .ifPresent(f -> partes.add(f.mensagem));
                });

        Optional.ofNullable(weather.wind())
                .ifPresent(w -> {
                    double kmh = w.speed() * 3.6;
                    partes.add(String.format("💨 Vento: %.1f km/h", kmh));
                    FaixaVento.fromVelocidade(kmh)
                            .ifPresent(f -> partes.add(f.mensagem));
                    if (w.gust() > 0) {
                        partes.add(String.format("⚡ Rajadas: %.1f km/h", w.gust() * 3.6));
                    }
                });

        Optional.ofNullable(weather.rain())
                .ifPresent(r -> {
                    if (r._1h() > 0)
                        partes.add(String.format("🌧️ Chuva: %.1f mm/h", r._1h()));
                    if (r._3h() > 0)
                        partes.add(String.format("🌧️ Acumulado 3h: %.1f mm", r._3h()));
                    if (r._1h() > 10)
                        partes.add("⚠️ Risco de aquaplanagem!");
                });

        Optional.ofNullable(weather.snow())
                .ifPresent(s -> {
                    if (s._1h() > 0)
                        partes.add(String.format("❄️ Neve: %.1f mm/h", s._1h()));
                    if (s._3h() > 0)
                        partes.add(String.format("❄️ Acumulado 3h: %.1f mm", s._3h()));
                    if (s._1h() > 5)
                        partes.add("⚠️ Pista escorregadia!");
                });

        Optional.ofNullable(weather.clouds())
                .ifPresent(c -> {
                    String nivel = c.all() < 30 ? "☀️ Poucas nuvens" : c.all() < 70 ? "⛅ Nublado" : "☁️ Muitas nuvens";
                    partes.add(String.format("%s (%d%%)", nivel, c.all()));
                });

        return partes.stream().collect(Collectors.joining("\n"));
    }

    private SeveridadeAlerta determinarGravidade(WeatherResponse weather) {
        return Stream.of(
                Optional.ofNullable(weather.weather())
                        .filter(w -> w.length > 0)
                        .map(w -> CondicaoClimatica.fromCodigo(w[0].id())
                                .map(c -> c.gravidadePadrao)
                                .orElse(SeveridadeAlerta.BAIXA)),
                Optional.ofNullable(weather.wind())
                        .map(w -> FaixaVento.fromVelocidade(w.speed() * 3.6)
                                .map(f -> f.gravidade)
                                .orElse(SeveridadeAlerta.BAIXA)),
                Optional.ofNullable(weather.main())
                        .map(m -> FaixaTemperatura.fromTemperatura(m.temp())
                                .map(f -> f.gravidade)
                                .orElse(SeveridadeAlerta.BAIXA)),
                Optional.ofNullable(weather.rain())
                        .map(r -> {
                            if (r._1h() > 10)
                                return SeveridadeAlerta.ALTA;
                            if (r._1h() > 5)
                                return SeveridadeAlerta.MEDIA;
                            return SeveridadeAlerta.BAIXA;
                        }))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Comparator.comparingInt(this::gravidadeToInt))
                .orElse(SeveridadeAlerta.BAIXA);
    }

    private int gravidadeToInt(SeveridadeAlerta g) {
        return switch (g) {
            case BAIXA -> 1;
            case MEDIA -> 2;
            case ALTA -> 3;
            // Se tiver CRITICA, adicione aqui
            default -> 0;
        };
    }

    private void criarAlertaClimatico(Long veiculoId, Viagem viagem, String mensagem, SeveridadeAlerta gravidade) {
        System.out.println("📝 Criando alerta climático para veículo " + veiculoId + " - Gravidade: " + gravidade);

        Alerta alerta = new Alerta();
        alerta.setVeiculoId(veiculoId);
        if (viagem != null) {
            alerta.setMotoristaId(viagem.getMotoristaId());
            alerta.setViagemId(viagem.getId());
        }
        alerta.setTipo(TipoAlerta.CLIMA); // Certifique-se de que CLIMA existe no enum TipoAlerta
        alerta.setSeveridade(gravidade);
        alerta.setMensagem(mensagem);
        alerta.setDataHora(LocalDateTime.now());
        alerta.setLido(false);
        alerta.setResolvido(false);

        alertaRepository.save(alerta);
        System.out.println("🚨 [" + gravidade + "] " + mensagem);
        System.out.println("✅ Alerta climático salvo no banco de dados");
    }

    private WeatherResponse criarRespostaFallback(double lat, double lon) {
        System.out.println("🔄 Criando resposta fallback para coordenadas: " + lat + ", " + lon);

        String condicao;
        int codigo;
        String descricao;

        if (lat > -24.0 && lat < -23.0 && lon > -47.0 && lon < -46.0) {
            condicao = "Clouds";
            codigo = 801;
            descricao = "nublado";
        } else if (lon < -46.0 && lon > -48.0) {
            condicao = "Clear";
            codigo = 800;
            descricao = "céu limpo";
        } else {
            condicao = "Clear";
            codigo = 800;
            descricao = "céu limpo";
        }

        WeatherResponse.Weather weather = new WeatherResponse.Weather(codigo, condicao, descricao, "01d");
        WeatherResponse.Weather[] weathers = { weather };

        WeatherResponse.Main main = new WeatherResponse.Main(22.0, 70);
        WeatherResponse.Wind wind = new WeatherResponse.Wind(3.0, 0);
        WeatherResponse.Clouds clouds = new WeatherResponse.Clouds(20);

        return new WeatherResponse(weathers, main, wind, null, null, clouds);
    }

    public void testarApi() {
        System.out.println("\n🧪 TESTANDO CONEXÃO COM OPENWEATHERMAP");
        try {
            WeatherResponse response = getWeatherWithRetry(-23.5505, -46.6333);
            if (response != null) {
                System.out.println("✅ CONEXÃO BEM-SUCEDIDA!");
                System.out.println("Clima: " + response.weather()[0].description());
                System.out.println("Temperatura: " + response.main().temp() + "°C");
            }
        } catch (Exception e) {
            System.err.println("❌ FALHA NO TESTE: " + e.getMessage());
        }
        System.out.println("====================================\n");
    }
}