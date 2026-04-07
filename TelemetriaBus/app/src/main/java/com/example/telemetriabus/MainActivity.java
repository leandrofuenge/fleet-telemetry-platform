package com.example.telemetriabus;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.ScrollView;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "TelemetriaBus";
    private static final int REQUEST_LOCATION_PERMISSION = 1001;

    // Configurações do Servidor
    private static final String SERVER_URL = "http://100.110.239.112:8080/api/v1/telemetria";
    private static final String SERVER_HOST = "100.110.239.112";

    // IDs (sincronizados com o simulador Python)
    private static final long VEICULO_ID = 1L;
    private static final long TENANT_ID = 1L;
    private static final long MOTORISTA_ID = 1L;
    private static final long VIAGEM_ID = 1L;
    private static final String VEICULO_UUID = "e7469e2f-3096-11f1-993c-5e38365aa120";
    private static final String PLANO = "STARTER";

    // OTIMIZAÇÃO: Controle de frequência
    private static final long MIN_TELEMETRY_INTERVAL_MS = 5000; // 5 segundos entre envios
    private static final long UI_UPDATE_INTERVAL_MS = 1000; // 1 segundo entre atualizações de UI
    private static final long GPS_UPDATE_INTERVAL_MS = 3000; // 3 segundos entre updates de GPS

    private long lastTelemetrySentTime = 0;
    private long lastUIUpdateTime = 0;
    private AtomicBoolean isSendingTelemetry = new AtomicBoolean(false);

    // Device info
    private String deviceId;
    private String imeiDispositivo;

    // Sensores
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor rotationVectorSensor;
    private float[] rotationMatrix = new float[9];
    private float[] orientation = new float[3];

    // Dados REAIS dos sensores (volateis para thread safety)
    private volatile float accelX = 0f, accelY = 0f, accelZ = 0f;
    private volatile float direcao = 0f;
    private volatile float aceleracaoTotal = 0f;
    private volatile float inclinacao = 0f;

    // GPS
    private FusedLocationProviderClient fusedLocationClient;
    private Location lastLocation;
    private LocationCallback locationCallback;

    // UI
    private TextView debugTextView;
    private ScrollView scrollView;
    private Handler uiHandler;

    // Status
    private volatile String connectionStatus = "Iniciando...";
    private String permissionStatus = "Verificando...";
    private volatile String lastHttpStatus = "-";
    private volatile String lastError = "-";
    private volatile long sentCount = 0;

    // Debug
    private String lastRequestDetails = "-";
    private String lastResponseDetails = "-";
    private String networkInfo = "-";
    private String dnsInfo = "-";
    private String connectivityTest = "-";

    // SIMULAÇÃO de dados que o celular não tem (como um tacógrafo)
    private Random random = new Random();
    private double odometro = 10000.0;
    private double ultimaLat = 0, ultimaLon = 0;
    private long ultimoTimestamp = 0;
    private double ultimaVelocidade = 0;
    private double rpmSimulado = 1200;
    private double nivelCombustivel = 75.0;
    private double consumoAcumulado = 1250.0;
    private int tempoOcioso = 0;
    private int tempoMotorLigado = 0;
    private int numeroFrenagens = 0;
    private int numeroAceleracoesBruscas = 0;
    private boolean ignicao = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Criar layout com scroll
        scrollView = new ScrollView(this);
        debugTextView = new TextView(this);
        debugTextView.setTextSize(10f);
        debugTextView.setTextColor(Color.WHITE);
        debugTextView.setBackgroundColor(Color.BLACK);
        int padding = 32;
        debugTextView.setPadding(padding, padding, padding, padding);
        debugTextView.setText("Inicializando app - Tacógrafo Digital...\n");
        scrollView.addView(debugTextView);
        setContentView(scrollView);

        // Handler para UI
        uiHandler = new Handler(Looper.getMainLooper());

        Log.d(TAG, "🚛 INICIANDO TACÓGRAFO DIGITAL (OTIMIZADO)");

        // Gerar device ID único
        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (deviceId == null || deviceId.isEmpty()) {
            deviceId = "DEV-" + UUID.randomUUID().toString().substring(0, 8);
        }
        imeiDispositivo = deviceId;

        Log.d(TAG, "📱 Device ID: " + deviceId);

        // Testar conectividade
        testNetworkConnectivity();

        // Inicializar sensores com delay maior para economizar bateria
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            Log.d(TAG, "📱 Sensores: " + (accelerometer != null ? "OK" : "FALHA"));
        }

        // Inicializar GPS
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location newLocation = result.getLastLocation();
                if (newLocation != null) {
                    processNewLocation(newLocation);
                }
            }
        };

        requestLocationPermissionIfNeeded();
        schedulePeriodicUIUpdate();
    }

    private void processNewLocation(Location newLocation) {
        // Calcular odômetro baseado na distância real percorrida
        if (ultimaLat != 0 && ultimaLon != 0) {
            float[] results = new float[1];
            Location.distanceBetween(ultimaLat, ultimaLon,
                    newLocation.getLatitude(), newLocation.getLongitude(), results);
            double distanciaMeters = results[0];
            if (distanciaMeters > 0 && distanciaMeters < 500) {
                odometro += distanciaMeters / 1000.0;
            }
        }
        ultimaLat = newLocation.getLatitude();
        ultimaLon = newLocation.getLongitude();

        // Calcular aceleração/desaceleração
        long now = System.currentTimeMillis();
        if (ultimoTimestamp > 0) {
            double deltaTime = (now - ultimoTimestamp) / 1000.0;
            if (deltaTime > 0) {
                double aceleracaoCalc = (newLocation.getSpeed() - ultimaVelocidade) / deltaTime;
                if (aceleracaoCalc < -2.5) {
                    numeroFrenagens++;
                } else if (aceleracaoCalc > 2.0) {
                    numeroAceleracoesBruscas++;
                }
            }
        }
        ultimoTimestamp = now;
        ultimaVelocidade = newLocation.getSpeed();

        lastLocation = newLocation;
        connectionStatus = "GPS ativo";

        if (lastLocation.hasBearing()) {
            direcao = lastLocation.getBearing();
        }

        // Enviar telemetria apenas se passou tempo suficiente
        long timeSinceLastSend = System.currentTimeMillis() - lastTelemetrySentTime;
        if (timeSinceLastSend >= MIN_TELEMETRY_INTERVAL_MS && !isSendingTelemetry.get()) {
            sendTelemetry();
        }
    }

    private void schedulePeriodicUIUpdate() {
        uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateDebugText();
                uiHandler.postDelayed(this, UI_UPDATE_INTERVAL_MS);
            }
        }, UI_UPDATE_INTERVAL_MS);
    }

    private void testNetworkConnectivity() {
        new Thread(() -> {
            try {
                StringBuilder sb = new StringBuilder();
                Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface iface = interfaces.nextElement();
                    if (iface.isUp() && !iface.isLoopback()) {
                        Enumeration<InetAddress> addresses = iface.getInetAddresses();
                        while (addresses.hasMoreElements()) {
                            InetAddress addr = addresses.nextElement();
                            if (!addr.isLoopbackAddress()) {
                                sb.append(addr.getHostAddress()).append(" ");
                            }
                        }
                    }
                }
                networkInfo = sb.toString().isEmpty() ? "Nenhum IP" : sb.toString();
            } catch (Exception e) {
                networkInfo = "Erro: " + e.getMessage();
            }

            try {
                InetAddress address = InetAddress.getByName(SERVER_HOST);
                boolean reachable = address.isReachable(3000);
                connectivityTest = reachable ? "Host OK" : "Host INALCANÇÁVEL";
                dnsInfo = "DNS: " + address.getHostAddress();
            } catch (Exception e) {
                connectivityTest = "Erro: " + e.getMessage();
                dnsInfo = "DNS ERRO";
            }

            uiHandler.post(this::updateDebugText);
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // OTIMIZAÇÃO: Usar SENSOR_DELAY_UI ao invés de NORMAL (mais lento = menos CPU)
        if (sensorManager != null) {
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            }
            if (rotationVectorSensor != null) {
                sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI);
            }
        }
        if (hasLocationPermission()) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        stopLocationUpdates();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelX = event.values[0];
            accelY = event.values[1];
            accelZ = event.values[2];
            aceleracaoTotal = (float) Math.sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ);
            // NÃO atualizar UI aqui - deixar para o agendamento periódico
        } else if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientation);
            direcao = (float) Math.toDegrees(orientation[0]);
            if (direcao < 0) direcao += 360;
            inclinacao = (float) Math.toDegrees(orientation[1]);
            // NÃO atualizar UI aqui - deixar para o agendamento periódico
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Não fazer nada
    }

    private void requestLocationPermissionIfNeeded() {
        if (!hasLocationPermission()) {
            permissionStatus = "Solicitando...";
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            permissionStatus = "Concedida";
            startLocationUpdates();
        }
    }

    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocationUpdates() {
        if (!hasLocationPermission()) {
            permissionStatus = "Negada";
            return;
        }

        permissionStatus = "Concedida";
        // OTIMIZAÇÃO: GPS a cada 3 segundos ao invés de 1-2
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, GPS_UPDATE_INTERVAL_MS)
                .setMinUpdateIntervalMillis(GPS_UPDATE_INTERVAL_MS)
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            lastError = e.getMessage();
        }
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient == null) return;
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        } catch (SecurityException e) {
            lastError = e.getMessage();
        }
    }

    private JSONArray gerarPressaoPneus() {
        JSONArray pneus = new JSONArray();
        String[] posicoes = {"D1", "D2", "T1", "T2"};
        for (String pos : posicoes) {
            JSONObject pneu = new JSONObject();
            try {
                double pressao = 90 + random.nextDouble() * 20;
                pneu.put("pos", pos);
                pneu.put("psi", pressao);
            } catch (Exception e) {}
            pneus.put(pneu);
        }
        return pneus;
    }






    private void sendTelemetry() {
        // OTIMIZAÇÃO: Verificar se já está enviando
        if (!isSendingTelemetry.compareAndSet(false, true)) {
            Log.d(TAG, "⏭️ Envio já em andamento, pulando...");
            return;
        }

        try {
            if (lastLocation == null) {
                isSendingTelemetry.set(false);
                return;
            }

            lastTelemetrySentTime = System.currentTimeMillis();

            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            String dataHora = LocalDateTime.now().format(formatter);
            long timestamp = System.currentTimeMillis();

            // ATUALIZAR DADOS SIMULADOS (Tacógrafo)
            double velocidadeKmph = lastLocation.getSpeed() * 3.6;

            // Simular RPM baseado na velocidade
            rpmSimulado = 800 + (velocidadeKmph * 15);
            rpmSimulado = Math.min(4500, Math.max(800, rpmSimulado));

            // Simular consumo de combustível
            double consumoInstantaneo = 0.05 + (velocidadeKmph / 2000);
            nivelCombustivel -= consumoInstantaneo;
            if (nivelCombustivel < 0) nivelCombustivel = 0;
            consumoAcumulado += consumoInstantaneo;

            // Simular tempo de motor ligado
            tempoMotorLigado += (int)(MIN_TELEMETRY_INTERVAL_MS / 1000);
            if (lastLocation.getSpeed() < 0.5) {
                tempoOcioso += (int)(MIN_TELEMETRY_INTERVAL_MS / 1000);
            }

            // Simular variação de RPM
            rpmSimulado += random.nextDouble() * 100 - 50;

            // MONTAR MENSAGEM COMPLETA
            JSONObject msg = new JSONObject();

            // Identificação
            msg.put("tenant_id", TENANT_ID);
            msg.put("veiculo_id", VEICULO_ID);
            msg.put("veiculo_uuid", VEICULO_UUID);
            msg.put("motorista_id", MOTORISTA_ID);
            msg.put("viagem_id", VIAGEM_ID);
            msg.put("device_id", deviceId);
            msg.put("imei_dispositivo", imeiDispositivo);
            msg.put("impreciso", false);
            msg.put("preservar_dados", false);

            // GPS - DADOS REAIS
            msg.put("latitude", lastLocation.getLatitude());
            msg.put("longitude", lastLocation.getLongitude());
            msg.put("altitude", lastLocation.hasAltitude() ? lastLocation.getAltitude() : 800.0);
            msg.put("velocidade", lastLocation.getSpeed());
            msg.put("direcao", (double) direcao);
            msg.put("hdop", 1.2);
            msg.put("satelites", 8);
            msg.put("precisao_gps", (double) lastLocation.getAccuracy());
            msg.put("lat_snap", lastLocation.getLatitude());
            msg.put("lng_snap", lastLocation.getLongitude());
            msg.put("nome_via", "Via Sensor");

            // Motor / OBD-II (SIMULADO)
            msg.put("ignicao", ignicao);
            msg.put("rpm", rpmSimulado);
            msg.put("carga_motor", 45.5);
            msg.put("torque_motor", 320.0);
            msg.put("temperatura_motor", 85.0 + random.nextDouble() * 10);
            msg.put("pressao_oleo", 3.5);
            msg.put("tensao_bateria", 13.8);
            msg.put("odometro", odometro);
            msg.put("horas_motor", tempoMotorLigado / 3600.0);
            msg.put("aceleracao", aceleracaoTotal);
            msg.put("inclinacao", inclinacao);

            // Combustível
            msg.put("nivel_combustivel", nivelCombustivel);
            msg.put("consumo_combustivel", consumoInstantaneo);
            msg.put("consumo_acumulado", consumoAcumulado);
            msg.put("tempo_ocioso", tempoOcioso);
            msg.put("tempo_motor_ligado", tempoMotorLigado);

            // Comportamento
            boolean frenagemBrusca = aceleracaoTotal < -2.5;
            msg.put("frenagem_brusca", frenagemBrusca);
            msg.put("numero_frenagens", numeroFrenagens);
            msg.put("numero_aceleracoes_bruscas", numeroAceleracoesBruscas);
            msg.put("excesso_velocidade", velocidadeKmph > 100);
            msg.put("velocidade_limite_via", 110.0);
            msg.put("curva_brusca", Math.abs(inclinacao) > 15);
            msg.put("pontuacao_motorista", 850 - (numeroFrenagens * 5) - (numeroAceleracoesBruscas * 3));

            // Segurança
            msg.put("colisao_detectada", aceleracaoTotal > 5.0);
            msg.put("geofence_violada", false);
            msg.put("geofence_id", JSONObject.NULL);
            msg.put("cinto_seguranca", true);
            msg.put("porta_aberta", false);
            msg.put("botao_panico", false);
            msg.put("adulteracao_gps", false);

            // Carga
            msg.put("temperatura_carga", 22.5);
            msg.put("umidade_carga", 55.0);
            msg.put("peso_carga_kg", 1500.0);
            msg.put("porta_bau_aberta", false);
            msg.put("impacto_carga", aceleracaoTotal > 3.0);
            msg.put("g_force_impacto", aceleracaoTotal > 3.0 ? aceleracaoTotal : 0.0);

            // Pneus
            JSONArray pneus = gerarPressaoPneus();
            msg.put("pressao_pneus_json", pneus);
            msg.put("alerta_pneu", false);

            // DMS
            msg.put("fadiga_detectada", false);
            msg.put("distracao_detectada", false);
            msg.put("uso_celular_detectado", false);
            msg.put("cigarro_detectado", false);
            msg.put("ausencia_cinto_dms", false);
            msg.put("score_dms", 95);

            // Ambiente
            msg.put("temperatura_externa", 28.5);
            msg.put("umidade_externa", 65.0);
            msg.put("chuva_detectada", false);
            msg.put("condicao_pista", "SECA");

            // Conectividade
            msg.put("sinal_gsm", -65);
            msg.put("sinal_gps", lastLocation.getAccuracy() > 10 ? 60 : 85);
            msg.put("tecnologia_rede", "4G");
            msg.put("firmware_versao", "v2.1.0");
            msg.put("modo_offline", false);
            msg.put("delay_sincronizacao_s", 0);

            // Tacógrafo
            msg.put("tacografo_status", "NORMAL");
            msg.put("tacografo_velocidade", velocidadeKmph);
            msg.put("tacografo_distancia", odometro);
            msg.put("horas_direcao_acumuladas", tempoMotorLigado / 3600.0);

            // Manutenção
            msg.put("manutencao_pendente", odometro > 15000);
            msg.put("proxima_revisao", JSONObject.NULL);
            msg.put("desgaste_freio", 65.0);

            // DTC / Payload
            msg.put("dtc_codes", new JSONArray());
            JSONObject payload = new JSONObject();
            payload.put("android_version", Build.VERSION.RELEASE);
            payload.put("device_model", Build.MODEL);
            payload.put("sensor_accel", true);
            payload.put("sensor_gyro", rotationVectorSensor != null);
            msg.put("payload", payload);

            // Timestamp
            msg.put("timestamp", timestamp);
            msg.put("priority", "NORMAL");
            msg.put("plano", PLANO);

            String payloadStr = msg.toString();

            lastRequestDetails = String.format(Locale.US, "V=%d, %.4f,%.4f, %.0fkm/h, %.0fRPM",
                    VEICULO_ID, lastLocation.getLatitude(), lastLocation.getLongitude(),
                    velocidadeKmph, rpmSimulado);

            connectionStatus = "Enviando...";

            // OTIMIZAÇÃO: Enviar em thread separada
            new Thread(() -> {
                HttpURLConnection conn = null;
                try {
                    URL url = new URL(SERVER_URL);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);
                    conn.setDoOutput(true);

                    byte[] body = payloadStr.getBytes(StandardCharsets.UTF_8);
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(body);
                        os.flush();
                    }

                    int responseCode = conn.getResponseCode();
                    lastHttpStatus = String.valueOf(responseCode);
                    sentCount++;

                    // Ler resposta
                    InputStream inputStream;
                    if (responseCode >= 200 && responseCode < 300) {
                        inputStream = conn.getInputStream();
                        connectionStatus = "✅ OK";
                        lastError = "-";
                    } else {
                        inputStream = conn.getErrorStream();
                        connectionStatus = "❌ " + responseCode;
                        lastError = "HTTP " + responseCode;
                    }

                    if (inputStream != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        String responseBody = response.toString();
                        lastResponseDetails = responseBody.length() > 100 ? responseBody.substring(0, 100) + "..." : responseBody;
                    }

                } catch (SocketTimeoutException e) {
                    connectionStatus = "❌ Timeout";
                    lastError = "Timeout";
                } catch (Exception e) {
                    connectionStatus = "❌ Falha";
                    lastError = e.getClass().getSimpleName();
                } finally {
                    if (conn != null) {
                        conn.disconnect();
                    }
                    isSendingTelemetry.set(false);
                }
            }).start();

        } catch (Exception e) {
            lastError = e.getClass().getSimpleName();
            isSendingTelemetry.set(false);
        }
    }






    private void updateDebugText() {
        // OTIMIZAÇÃO: Construir string mais eficiente
        StringBuilder sb = new StringBuilder(1000);

        String latitudeStr = "-";
        String longitudeStr = "-";
        String velocidadeStr = "-";

        if (lastLocation != null) {
            latitudeStr = String.format(Locale.US, "%.6f", lastLocation.getLatitude());
            longitudeStr = String.format(Locale.US, "%.6f", lastLocation.getLongitude());
            velocidadeStr = String.format(Locale.US, "%.1f km/h", lastLocation.getSpeed() * 3.6);
        }

        sb.append("╔════════════════════════════════╗\n");
        sb.append("║  🚛 TACÓGRAFO DIGITAL 🚛       ║\n");
        sb.append("╚════════════════════════════════╝\n\n");

        sb.append("📡 CONEXÃO\n");
        sb.append("Status: ").append(connectionStatus).append("\n");
        sb.append("HTTP: ").append(lastHttpStatus).append(" | Envios: ").append(sentCount).append("\n");
        if (!lastError.equals("-")) {
            sb.append("Erro: ").append(lastError).append("\n");
        }
        sb.append("\n");

        sb.append("📍 GPS\n");
        sb.append("Lat: ").append(latitudeStr).append("\n");
        sb.append("Lon: ").append(longitudeStr).append("\n");
        sb.append("Vel: ").append(velocidadeStr).append("\n");
        sb.append("Dir: ").append(String.format(Locale.US, "%.0f°", direcao)).append("\n");
        sb.append("Permissão: ").append(permissionStatus).append("\n");
        sb.append("\n");

        sb.append("📊 TACÓGRAFO\n");
        sb.append("RPM: ").append(String.format(Locale.US, "%.0f", rpmSimulado)).append("\n");
        sb.append("Odo: ").append(String.format(Locale.US, "%.1f km", odometro)).append("\n");
        sb.append("Comb: ").append(String.format(Locale.US, "%.0f%%", nivelCombustivel)).append("\n");
        sb.append("Motor: ").append(tempoMotorLigado / 60).append(" min\n");
        sb.append("Frenad: ").append(numeroFrenagens).append(" | AcelBrusc: ").append(numeroAceleracoesBruscas).append("\n");
        sb.append("\n");

        sb.append("📱 SENSORES\n");
        sb.append("Acel: ").append(String.format(Locale.US, "%.2f m/s²", aceleracaoTotal)).append("\n");
        sb.append("Inclin: ").append(String.format(Locale.US, "%.1f°", inclinacao)).append("\n");
        sb.append("\n");

        sb.append("🌐 REDE\n");
        sb.append(connectivityTest).append("\n");
        sb.append("\n");

        sb.append("⏱️ INTERVALOS\n");
        sb.append("Telemetria: ").append(MIN_TELEMETRY_INTERVAL_MS/1000).append("s\n");
        sb.append("GPS: ").append(GPS_UPDATE_INTERVAL_MS/1000).append("s\n");
        sb.append("UI: ").append(UI_UPDATE_INTERVAL_MS/1000).append("s\n");

        debugTextView.setText(sb.toString());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            permissionStatus = "Concedida";
            startLocationUpdates();
        } else {
            permissionStatus = "Negada";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Limpar handler
        if (uiHandler != null) {
            uiHandler.removeCallbacksAndMessages(null);
        }
    }
}