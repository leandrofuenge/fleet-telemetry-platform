package com.app.telemetria.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.telemetria.api.dto.response.RouteResponse;
import com.telemetria.domain.entity.Alerta;
import com.telemetria.domain.entity.DispositivoIot;
import com.telemetria.domain.entity.HistoricoOdometro;
import com.telemetria.domain.entity.HistoricoScoreMotorista;
import com.telemetria.domain.entity.Motorista;
import com.telemetria.domain.entity.Rota;
import com.telemetria.domain.entity.Telemetria;
import com.telemetria.domain.entity.Veiculo;
import com.telemetria.domain.entity.Viagem;
import com.telemetria.domain.enums.SeveridadeAlerta;
import com.telemetria.domain.enums.StatusDispositivo;
import com.telemetria.domain.enums.TipoAlerta;
import com.telemetria.domain.enums.TipoDispositivo;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.VeiculoNotFoundException;
import com.telemetria.domain.service.AlertaService;
import com.telemetria.infrastructure.integration.geocoding.LocationClassifierService;
import com.telemetria.infrastructure.integration.routing.RoutingClient;
import com.telemetria.infrastructure.persistence.AlertaRepository;
import com.telemetria.infrastructure.persistence.DispositivoIotRepository;
import com.telemetria.infrastructure.persistence.HistoricoOdometroRepository;
import com.telemetria.infrastructure.persistence.HistoricoScoreMotoristaRepository;
import com.telemetria.infrastructure.persistence.MotoristaRepository;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;
import com.telemetria.infrastructure.persistence.VeiculoRepository;
import com.telemetria.infrastructure.persistence.ViagemRepository;

@ExtendWith(MockitoExtension.class)
class AlertaServiceTest {

    @Mock
    private AlertaRepository alertaRepository;
    @Mock
    private ViagemRepository viagemRepository;
    @Mock
    private TelemetriaRepository telemetriaRepository;
    @Mock
    private LocationClassifierService locationClassifierService;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private RoutingClient routingClient;
    @Mock
    private DispositivoIotRepository dispositivoRepository;
    @Mock
    private VeiculoRepository veiculoRepository;
    @Mock
    private HistoricoOdometroRepository historicoOdometroRepository;
    @Mock
    private MotoristaRepository motoristaRepository;
    @Mock
    private HistoricoScoreMotoristaRepository historicoScoreRepository;

    @InjectMocks
    private AlertaService alertaService;

    private Telemetria telemetria;
    private Viagem viagem;
    private Veiculo veiculo;
    private Motorista motorista;
    private Rota rota;

    @BeforeEach
    void setup() {
        veiculo = new Veiculo();
        veiculo.setId(100L);
        veiculo.setTenantId(10L);
        veiculo.setPlaca("ABC1D23");
        veiculo.setModelo("Volvo FH");

        motorista = new Motorista();
        motorista.setId(300L);
        motorista.setTenantId(10L);
        motorista.setNome("Leandro");
        motorista.setCpf("12345678900");
        motorista.setCategoriaCnh("D");
        motorista.setScore(650);
        motorista.setDataVencimentoCnh(LocalDate.now().plusDays(30));

        rota = new Rota();
        rota.setOrigem("Cuiabá");
        rota.setDestino("Rondonópolis");
        rota.setLatitudeOrigem(-15.60);
        rota.setLongitudeOrigem(-56.10);
        rota.setLatitudeDestino(-16.47);
        rota.setLongitudeDestino(-54.64);

        viagem = new Viagem();
        viagem.setId(200L);
        viagem.setVeiculo(veiculo);
        viagem.setMotorista(motorista);
        viagem.setStatus("EM_ANDAMENTO");
        viagem.setDataSaida(LocalDateTime.now().minusHours(5));
        viagem.setDataChegadaPrevista(LocalDateTime.now().plusMinutes(20));
        viagem.setRota(rota);

        telemetria = new Telemetria();
        telemetria.setId(1L);
        telemetria.setTenantId(10L);
        telemetria.setVeiculoId(100L);
        telemetria.setLatitude(-15.6);
        telemetria.setLongitude(-56.1);
        telemetria.setVelocidade(120.0);
        telemetria.setOdometro(15000.0);
        telemetria.setNivelCombustivel(10.0);
        telemetria.setDataHora(LocalDateTime.now());
    }

    @Nested
    class Consultas {

        @Test
        @DisplayName("Deve listar todos os alertas paginados")
        void deveListarTodos() {
            PageRequest pageable = PageRequest.of(0, 10);
            Page<Alerta> page = new PageImpl<>(List.of(new Alerta(), new Alerta()));

            when(alertaRepository.findAll(pageable)).thenReturn(page);

            Page<Alerta> resultado = alertaService.listarTodos(pageable);

            assertNotNull(resultado);
            assertEquals(2, resultado.getTotalElements());
            verify(alertaRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Deve listar alertas ativos")
        void deveListarAtivos() {
            when(alertaRepository.findByResolvidoFalseOrderByDataHoraDesc())
                    .thenReturn(List.of(new Alerta(), new Alerta(), new Alerta()));

            List<Alerta> resultado = alertaService.listarAtivos();

            assertEquals(3, resultado.size());
            verify(alertaRepository).findByResolvidoFalseOrderByDataHoraDesc();
        }

        @Test
        @DisplayName("Deve listar alertas por veículo")
        void deveListarPorVeiculo() {
            when(alertaRepository.findByVeiculoIdOrderByDataHoraDesc(100L))
                    .thenReturn(List.of(new Alerta(), new Alerta()));

            List<Alerta> resultado = alertaService.listarPorVeiculo(100L);

            assertEquals(2, resultado.size());
            verify(alertaRepository).findByVeiculoIdOrderByDataHoraDesc(100L);
        }

        @Test
        @DisplayName("Deve listar alertas por motorista")
        void deveListarPorMotorista() {
            when(alertaRepository.findByMotoristaIdOrderByDataHoraDesc(300L))
                    .thenReturn(List.of(new Alerta()));

            List<Alerta> resultado = alertaService.listarPorMotorista(300L);

            assertEquals(1, resultado.size());
            verify(alertaRepository).findByMotoristaIdOrderByDataHoraDesc(300L);
        }

        @Test
        @DisplayName("Deve listar alertas por viagem")
        void deveListarPorViagem() {
            when(alertaRepository.findByViagemIdOrderByDataHoraDesc(200L))
                    .thenReturn(List.of(new Alerta()));

            List<Alerta> resultado = alertaService.listarPorViagem(200L);

            assertEquals(1, resultado.size());
            verify(alertaRepository).findByViagemIdOrderByDataHoraDesc(200L);
        }

        @Test
        @DisplayName("Deve listar alertas por período")
        void deveListarPorPeriodo() {
            LocalDateTime inicio = LocalDateTime.now().minusDays(1);
            LocalDateTime fim = LocalDateTime.now();

            when(alertaRepository.findByDataHoraBetweenOrderByDataHoraDesc(inicio, fim))
                    .thenReturn(List.of(new Alerta(), new Alerta()));

            List<Alerta> resultado = alertaService.listarPorPeriodo(inicio, fim);

            assertEquals(2, resultado.size());
            verify(alertaRepository).findByDataHoraBetweenOrderByDataHoraDesc(inicio, fim);
        }

        @Test
        @DisplayName("Deve montar dashboard corretamente")
        void deveMontarDashboard() {
            Alerta a1 = new Alerta();
            a1.setId(1L);
            a1.setTipo(TipoAlerta.EXCESSO_VELOCIDADE);
            a1.setSeveridade(SeveridadeAlerta.ALTO);
            a1.setMensagem("Excesso");
            a1.setLido(false);
            a1.setResolvido(false);
            a1.setDataHora(LocalDateTime.now());

            Alerta a2 = new Alerta();
            a2.setId(2L);
            a2.setTipo(TipoAlerta.NIVEL_COMBUSTIVEL_BAIXO);
            a2.setSeveridade(SeveridadeAlerta.MEDIO);
            a2.setMensagem("Combustível");
            a2.setLido(false);
            a2.setResolvido(false);
            a2.setDataHora(LocalDateTime.now());

            when(alertaRepository.findByResolvidoFalseOrderByDataHoraDesc())
                    .thenReturn(List.of(a1, a2));

            Map<String, Object> dashboard = alertaService.dashboard();

            assertEquals(2, dashboard.get("totalAtivos"));
            assertEquals(1L, dashboard.get("altaGravidade"));
            assertEquals(1L, dashboard.get("mediaGravidade"));
            assertEquals(0L, dashboard.get("baixaGravidade"));
            assertNotNull(dashboard.get("alertasPorTipo"));
            assertNotNull(dashboard.get("ultimosAlertas"));
        }
    }

    @Nested
    class AtualizacaoStatus {

        @Test
        @DisplayName("Deve marcar alerta como lido")
        void deveMarcarComoLido() {
            Alerta alerta = new Alerta();
            alerta.setId(1L);
            alerta.setLido(false);

            when(alertaRepository.findById(1L)).thenReturn(Optional.of(alerta));
            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

            Alerta resultado = alertaService.marcarComoLido(1L);

            assertTrue(resultado.getLido());
            assertNotNull(resultado.getDataHoraLeitura());
            verify(alertaRepository).save(alerta);
        }

        @Test
        @DisplayName("Deve resolver alerta")
        void deveResolverAlerta() {
            Alerta alerta = new Alerta();
            alerta.setId(2L);
            alerta.setResolvido(false);

            when(alertaRepository.findById(2L)).thenReturn(Optional.of(alerta));
            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

            Alerta resultado = alertaService.resolverAlerta(2L);

            assertTrue(resultado.getResolvido());
            assertNotNull(resultado.getDataHoraResolucao());
            verify(alertaRepository).save(alerta);
        }
    }

    @Nested
    class AlertasTelemetria {

        @Test
        @DisplayName("Deve criar alerta de excesso de velocidade quando acima do limite e sem alerta recente")
        void deveCriarAlertaExcessoVelocidade() {
            when(alertaRepository.findPrimeiroByVeiculoIdAndTipoOrderByDataHoraDesc(
                    telemetria.getVeiculoId(), TipoAlerta.EXCESSO_VELOCIDADE))
                    .thenReturn(Optional.empty());

            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

            alertaService.verificarExcessoVelocidade(telemetria);

            ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);
            verify(alertaRepository).save(captor.capture());

            Alerta salvo = captor.getValue();
            assertEquals(TipoAlerta.EXCESSO_VELOCIDADE, salvo.getTipo());
            assertEquals(SeveridadeAlerta.ALTO, salvo.getSeveridade());
            assertEquals(telemetria.getVeiculoId(), salvo.getVeiculoId());
            assertEquals(telemetria.getTenantId(), salvo.getTenantId());
            assertFalse(salvo.getResolvido());

            verify(messagingTemplate).convertAndSend(
                    eq("/topic/alertas/" + TipoAlerta.EXCESSO_VELOCIDADE),
                    (Object) isA(Alerta.class)
            );
        }
        

        @Test
        @DisplayName("Não deve criar alerta de excesso quando velocidade for normal")
        void naoDeveCriarAlertaExcessoQuandoVelocidadeNormal() {
            telemetria.setVelocidade(80.0);

            alertaService.verificarExcessoVelocidade(telemetria);

            verify(alertaRepository, never()).save(any());
            verify(messagingTemplate, never()).convertAndSend(
                    anyString(),
                    (Object) any()
            );
        }

        
        
        
        
        @Test
        @DisplayName("Não deve criar alerta de excesso quando velocidade for nula")
        void naoDeveCriarAlertaExcessoQuandoVelocidadeNula() {
            telemetria.setVelocidade(null);

            alertaService.verificarExcessoVelocidade(telemetria);

            verify(alertaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve criar alerta de combustível baixo")
        void deveCriarAlertaCombustivelBaixo() {
            telemetria.setNivelCombustivel(8.0);
            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

            alertaService.verificarNivelCombustivel(telemetria, viagem);

            ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);
            verify(alertaRepository).save(captor.capture());

            Alerta salvo = captor.getValue();
            assertEquals(TipoAlerta.NIVEL_COMBUSTIVEL_BAIXO, salvo.getTipo());
            assertEquals(SeveridadeAlerta.MEDIO, salvo.getSeveridade());
            assertEquals(viagem.getMotoristaId(), salvo.getMotoristaId());
            assertEquals(viagem.getId(), salvo.getViagemId());
        }

        @Test
        @DisplayName("Não deve criar alerta de combustível quando nível for normal")
        void naoDeveCriarAlertaCombustivelQuandoNormal() {
            telemetria.setNivelCombustivel(40.0);

            alertaService.verificarNivelCombustivel(telemetria, viagem);

            verify(alertaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve criar alerta de velocidade baixa fora de área urbana")
        void deveCriarAlertaVelocidadeBaixaForaAreaUrbana() {
            telemetria.setVelocidade(5.0);

            when(locationClassifierService.classify(anyDouble(), anyDouble())).thenReturn("RODOVIA");
            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

            alertaService.verificarVelocidadeBaixa(telemetria, viagem);

            ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);
            verify(alertaRepository).save(captor.capture());

            Alerta salvo = captor.getValue();
            assertEquals(TipoAlerta.VELOCIDADE_BAIXA, salvo.getTipo());
            assertEquals(SeveridadeAlerta.MEDIO, salvo.getSeveridade());
        }

        @Test
        @DisplayName("Não deve criar alerta de velocidade baixa em área urbana")
        void naoDeveCriarAlertaVelocidadeBaixaEmAreaUrbana() {
            telemetria.setVelocidade(5.0);
            when(locationClassifierService.classify(anyDouble(), anyDouble())).thenReturn("AREA_URBANA");

            alertaService.verificarVelocidadeBaixa(telemetria, viagem);

            verify(alertaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve criar alerta de parada prolongada")
        void deveCriarAlertaParadaProlongada() {
            LocalDateTime inicioParada = LocalDateTime.now().minusMinutes(40);

            when(alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalse(100L, TipoAlerta.PARADA_PROLONGADA))
                    .thenReturn(false);
            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));
            when(telemetriaRepository.findUltimaTelemetriaByVeiculoId(100L)).thenReturn(Optional.of(telemetria));

            alertaService.verificarParadaProlongada(100L, inicioParada);

            verify(alertaRepository).save(any(Alerta.class));
        }

        @Test
        @DisplayName("Deve criar alerta de GPS sem sinal")
        void deveCriarAlertaGpsSemSinal() {
            Telemetria ultima = new Telemetria();
            ultima.setDataHora(LocalDateTime.now().minusMinutes(20));
            ultima.setLatitude(-15.6);
            ultima.setLongitude(-56.1);
            ultima.setVelocidade(0.0);
            ultima.setOdometro(10000.0);

            when(alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalse(100L, TipoAlerta.GPS_SEM_SINAL))
                    .thenReturn(false);
            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));
            when(telemetriaRepository.findUltimaTelemetriaByVeiculoId(100L)).thenReturn(Optional.of(telemetria));

            alertaService.verificarGpsSemSinal(100L, ultima);

            verify(alertaRepository).save(any(Alerta.class));
        }

        @Test
        @DisplayName("Deve criar alerta de tempo de direção")
        void deveCriarAlertaTempoDirecao() {
            viagem.setDataSaida(LocalDateTime.now().minusMinutes(300));

            when(alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalse(
                    viagem.getVeiculoId(), TipoAlerta.TEMPO_DIRECAO))
                    .thenReturn(false);
            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));
            when(telemetriaRepository.findUltimaTelemetriaByVeiculoId(viagem.getVeiculoId()))
                    .thenReturn(Optional.of(telemetria));

            alertaService.verificarTempoDirecao(viagem, telemetria);

            verify(alertaRepository).save(any(Alerta.class));
        }

        @Test
        @DisplayName("Deve criar alerta de atraso de viagem")
        void deveCriarAlertaAtrasoViagem() {
            viagem.setDataChegadaPrevista(LocalDateTime.now().plusMinutes(10));

            RouteResponse routeResponse = new RouteResponse();
            routeResponse.setDuracaoMinutos(60.0);

            when(routingClient.calcular(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(routeResponse);
            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));
            when(telemetriaRepository.findUltimaTelemetriaByVeiculoId(viagem.getVeiculoId()))
                    .thenReturn(Optional.of(telemetria));

            alertaService.verificarAtrasoViagemInteligente(viagem, telemetria);

            verify(alertaRepository).save(any(Alerta.class));
        }

        @Test
        @DisplayName("Deve criar alerta de início de viagem")
        void deveCriarAlertaInicioViagem() {
            viagem.setStatus("EM_ANDAMENTO");

            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));
            when(telemetriaRepository.findUltimaTelemetriaByVeiculoId(viagem.getVeiculoId()))
                    .thenReturn(Optional.of(telemetria));

            alertaService.verificarInicioViagem(viagem);

            verify(alertaRepository).save(any(Alerta.class));
        }

        @Test
        @DisplayName("Deve criar alerta de fim de viagem")
        void deveCriarAlertaFimViagem() {
            viagem.setStatus("FINALIZADA");

            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));
            when(telemetriaRepository.findUltimaTelemetriaByVeiculoId(viagem.getVeiculoId()))
                    .thenReturn(Optional.of(telemetria));

            alertaService.verificarFimViagem(viagem);

            verify(alertaRepository).save(any(Alerta.class));
        }
    }

    @Nested
    class ProcessamentoAssincrono {

        @Test
        @DisplayName("Deve processar telemetria com sucesso")
        void deveProcessarTelemetriaComSucesso() {
            when(viagemRepository.findByVeiculoIdAndStatus(telemetria.getVeiculoId(), "EM_ANDAMENTO"))
                    .thenReturn(Optional.of(viagem));

            when(alertaRepository.findPrimeiroByVeiculoIdAndTipoOrderByDataHoraDesc(
                    anyLong(), any(TipoAlerta.class)))
                    .thenReturn(Optional.empty());

            when(alertaRepository.existsByVeiculoIdAndTipoAndResolvidoFalse(
                    anyLong(), any(TipoAlerta.class)))
                    .thenReturn(false);

            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

            RouteResponse routeResponse = new RouteResponse();
            routeResponse.setDuracaoMinutos(60.0);

            when(routingClient.calcular(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                    .thenReturn(routeResponse);

            CompletableFuture<String> future = alertaService.processarTelemetria(telemetria);

            assertDoesNotThrow(future::join);
            assertEquals("Alertas processados com sucesso", future.join());
        }

        @Test
        @DisplayName("Deve retornar mensagem para telemetria inválida")
        void deveRetornarMensagemQuandoTelemetriaInvalida() {
            Telemetria invalida = new Telemetria();
            invalida.setVeiculoId(null);

            CompletableFuture<String> future = alertaService.processarTelemetria(invalida);

            assertEquals("Telemetria inválida", future.join());
        }

        @Test
        @DisplayName("Deve processar múltiplas telemetrias")
        void deveProcessarMultiplasTelemetrias() {
            Telemetria t1 = new Telemetria();
            t1.setId(1L);
            t1.setVeiculoId(100L);
            t1.setTenantId(10L);
            t1.setVelocidade(80.0);
            t1.setNivelCombustivel(50.0);
            t1.setDataHora(LocalDateTime.now());

            Telemetria t2 = new Telemetria();
            t2.setId(2L);
            t2.setVeiculoId(101L);
            t2.setTenantId(10L);
            t2.setVelocidade(120.0);
            t2.setNivelCombustivel(10.0);
            t2.setLatitude(-15.0);
            t2.setLongitude(-56.0);
            t2.setDataHora(LocalDateTime.now());

            when(viagemRepository.findByVeiculoIdAndStatus(anyLong(), eq("EM_ANDAMENTO")))
                    .thenReturn(Optional.empty());
            when(alertaRepository.findPrimeiroByVeiculoIdAndTipoOrderByDataHoraDesc(anyLong(), any(TipoAlerta.class)))
                    .thenReturn(Optional.empty());
            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

            CompletableFuture<List<String>> future =
                    alertaService.processarMultiplasTelemetrias(List.of(t1, t2));

            List<String> resultado = future.join();

            assertEquals(2, resultado.size());
        }
    }

    @Nested
    class Dispositivos {

        @Test
        @DisplayName("Deve vincular dispositivo ao veículo")
        void deveVincularDispositivo() {
            when(veiculoRepository.findById(100L)).thenReturn(Optional.of(veiculo));
            when(dispositivoRepository.findByDeviceId("ESP32-001")).thenReturn(Optional.empty());

            alertaService.vincularDispositivo(100L, "ESP32-001");

            ArgumentCaptor<DispositivoIot> captor = ArgumentCaptor.forClass(DispositivoIot.class);
            verify(dispositivoRepository).save(captor.capture());

            DispositivoIot salvo = captor.getValue();
            assertEquals("ESP32-001", salvo.getDeviceId());
            assertEquals(100L, salvo.getVeiculoId());
            assertEquals(StatusDispositivo.ATIVO, salvo.getStatus());
            assertEquals(TipoDispositivo.PRINCIPAL, salvo.getTipo());
        }

        @Test
        @DisplayName("Deve lançar exceção ao vincular dispositivo já associado a outro veículo")
        void deveLancarExcecaoAoVincularDispositivoJaUsado() {
            DispositivoIot existente = new DispositivoIot();
            existente.setDeviceId("ESP32-001");
            existente.setVeiculoId(999L);

            when(veiculoRepository.findById(100L)).thenReturn(Optional.of(veiculo));
            when(dispositivoRepository.findByDeviceId("ESP32-001")).thenReturn(Optional.of(existente));

            assertThrows(BusinessException.class,
                    () -> alertaService.vincularDispositivo(100L, "ESP32-001"));
        }

        @Test
        @DisplayName("Deve adicionar dispositivo backup")
        void deveAdicionarDispositivoBackup() {
            DispositivoIot principal = new DispositivoIot();
            principal.setId(1L);
            principal.setDeviceId("PRINCIPAL-01");
            principal.setVeiculoId(100L);
            principal.setTipo(TipoDispositivo.PRINCIPAL);

            when(veiculoRepository.findById(100L)).thenReturn(Optional.of(veiculo));
            when(dispositivoRepository.countByVeiculoId(100L)).thenReturn(1L);
            when(dispositivoRepository.findByVeiculoIdAndTipo(100L, TipoDispositivo.PRINCIPAL))
                    .thenReturn(Optional.of(principal));

            alertaService.adicionarDispositivoBackup(100L, "BACKUP-01");

            verify(dispositivoRepository, times(2)).save(any(DispositivoIot.class));
        }

        @Test
        @DisplayName("Deve lançar exceção ao adicionar backup acima do limite")
        void deveLancarExcecaoQuandoJaPossuiDoisDispositivos() {
            when(veiculoRepository.findById(100L)).thenReturn(Optional.of(veiculo));
            when(dispositivoRepository.countByVeiculoId(100L)).thenReturn(2L);

            assertThrows(BusinessException.class,
                    () -> alertaService.adicionarDispositivoBackup(100L, "BACKUP-02"));
        }

        @Test
        @DisplayName("Deve trocar dispositivo e registrar histórico de odômetro")
        void deveTrocarDispositivoERegistrarHistorico() {
            DispositivoIot antigo = new DispositivoIot();
            antigo.setId(10L);
            antigo.setDeviceId("OLD-01");
            antigo.setVeiculoId(100L);
            antigo.setTipo(TipoDispositivo.PRINCIPAL);

            Telemetria ultima = new Telemetria();
            ultima.setOdometro(10000.0);

            when(veiculoRepository.findById(100L)).thenReturn(Optional.of(veiculo));
            when(dispositivoRepository.findByVeiculoIdAndTipo(100L, TipoDispositivo.PRINCIPAL))
                    .thenReturn(Optional.of(antigo));
            when(telemetriaRepository.findTopByVeiculoIdAndDeviceIdOrderByDataHoraDesc(100L, "OLD-01"))
                    .thenReturn(Optional.of(ultima));
            when(dispositivoRepository.findByDeviceId("NEW-01")).thenReturn(Optional.empty());

            DispositivoIot novo = new DispositivoIot();
            novo.setId(20L);
            novo.setDeviceId("NEW-01");
            when(dispositivoRepository.save(any(DispositivoIot.class))).thenReturn(novo);

            alertaService.trocarDispositivo(100L, "NEW-01", 10200.0, 999L);

            verify(historicoOdometroRepository).save(any(HistoricoOdometro.class));
        }

        @Test
        @DisplayName("Deve criar alerta de inconsistência de odômetro quando delta > 500")
        void deveCriarAlertaInconsistenciaOdometro() {
            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

            alertaService.criarAlertaInconsistenciaOdometro(veiculo, 700.0, 15000.0);

            verify(alertaRepository).save(any(Alerta.class));
        }

        @Test
        @DisplayName("Deve lançar exceção quando veículo não existir na troca de dispositivo")
        void deveLancarExcecaoQuandoVeiculoNaoExistirNaTroca() {
            when(veiculoRepository.findById(100L)).thenReturn(Optional.empty());

            assertThrows(VeiculoNotFoundException.class,
                    () -> alertaService.trocarDispositivo(100L, "NEW-01", 15000.0, 1L));
        }
    }

    @Nested
    class AlertasDocumentosEVeiculo {

        @Test
        @DisplayName("Deve criar alerta de vencimento de tacógrafo")
        void deveCriarAlertaVencimentoTacografo() {
            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

            alertaService.criarAlertaVencimentoTacografo(veiculo, 30);

            verify(alertaRepository).save(any(Alerta.class));
        }

        @Test
        @DisplayName("Deve criar alerta de tacógrafo vencido")
        void deveCriarAlertaTacografoVencido() {
            veiculo.setDataVencimentoTacografo(LocalDate.now().minusDays(1));
            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

            alertaService.criarAlertaTacografoVencido(veiculo);

            verify(alertaRepository).save(any(Alerta.class));
        }

        @Test
        @DisplayName("Deve criar alerta de vencimento de documento")
        void deveCriarAlertaVencimentoDocumento() {
            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

            alertaService.criarAlertaVencimentoDocumento(veiculo, "CRLV", 7);

            verify(alertaRepository).save(any(Alerta.class));
        }

        @Test
        @DisplayName("Deve criar alerta de documento vencido")
        void deveCriarAlertaDocumentoVencido() {
            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

            alertaService.criarAlertaDocumentoVencido(veiculo, "CRLV");

            verify(alertaRepository).save(any(Alerta.class));
        }
    }

    @Nested
    class AlertasMotorista {

        @Test
        @DisplayName("Deve criar alerta de vencimento de CNH")
        void deveCriarAlertaVencimentoCnh() {
            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

            alertaService.criarAlertaVencimentoCnh(motorista, 7);

            verify(alertaRepository).save(any(Alerta.class));
        }

        @Test
        @DisplayName("Deve criar alerta de CNH vencida")
        void deveCriarAlertaCnhVencida() {
            motorista.setDataVencimentoCnh(LocalDate.now().minusDays(1));
            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

            alertaService.criarAlertaCnhVencida(motorista);

            verify(alertaRepository).save(any(Alerta.class));
        }

        @Test
        @DisplayName("Deve criar alerta de score baixo")
        void deveCriarAlertaScoreBaixo() {
            motorista.setScore(550);
            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

            alertaService.criarAlertaScoreBaixo(motorista);

            verify(alertaRepository).save(any(Alerta.class));
        }

        @Test
        @DisplayName("Deve criar alerta de score crítico")
        void deveCriarAlertaScoreCritico() {
            motorista.setScore(350);
            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

            alertaService.criarAlertaScoreCritico(motorista);

            verify(alertaRepository).save(any(Alerta.class));
        }
    }

    @Nested
    class ScoreMotorista {

        @Test
        @DisplayName("Deve reduzir score e salvar histórico para excesso de velocidade")
        void deveAtualizarScoreMotorista() {
            when(motoristaRepository.findById(300L)).thenReturn(Optional.of(motorista));

            alertaService.atualizarScoreMotorista(300L, "EXCESSO_VELOCIDADE", 200L);

            assertEquals(645, motorista.getScore());
            verify(motoristaRepository).save(motorista);
            verify(historicoScoreRepository).save(any(HistoricoScoreMotorista.class));
        }

        @Test
        @DisplayName("Deve criar alerta de score baixo quando score cair abaixo de 600")
        void deveCriarAlertaDeScoreBaixoAoAtualizar() {
            motorista.setScore(602);

            when(motoristaRepository.findById(300L)).thenReturn(Optional.of(motorista));
            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

            alertaService.atualizarScoreMotorista(300L, "EXCESSO_VELOCIDADE", 200L);

            verify(alertaRepository).save(any(Alerta.class));
            assertEquals(597, motorista.getScore());
        }

        @Test
        @DisplayName("Deve criar alerta de score crítico quando score cair abaixo de 400")
        void deveCriarAlertaDeScoreCriticoAoAtualizar() {
            motorista.setScore(405);

            when(motoristaRepository.findById(300L)).thenReturn(Optional.of(motorista));
            when(alertaRepository.save(any(Alerta.class))).thenAnswer(inv -> inv.getArgument(0));

            alertaService.atualizarScoreMotorista(300L, "USO_CELULAR", 200L);

            assertEquals(395, motorista.getScore());
            verify(alertaRepository).save(any(Alerta.class));
        }

        @Test
        @DisplayName("Não deve fazer nada para evento que não impacta score")
        void naoDeveAtualizarScoreParaEventoInexistente() {
            when(motoristaRepository.findById(300L)).thenReturn(Optional.of(motorista));

            alertaService.atualizarScoreMotorista(300L, "EVENTO_DESCONHECIDO", 200L);

            verify(motoristaRepository, never()).save(any());
            verify(historicoScoreRepository, never()).save(any());
            verify(alertaRepository, never()).save(any());
        }
    }

    @Nested
    class AreaUrbana {

        @Test
        @DisplayName("Deve enviar aviso quando veículo entrar em área urbana")
        void deveEnviarAvisoQuandoEntrarEmAreaUrbana() {
            when(locationClassifierService.classify(anyDouble(), anyDouble())).thenReturn("AREA_URBANA");

            alertaService.verificarAreaUrbanaEAvisar(-15.6, -56.1, "ABC1D23");

            verify(messagingTemplate).convertAndSend("/topic/alertas", "Veículo ABC1D23 entrou em área urbana");
        }

        @Test
        @DisplayName("Não deve enviar aviso quando não estiver em área urbana")
        void naoDeveEnviarAvisoQuandoNaoEstiverEmAreaUrbana() {
            when(locationClassifierService.classify(anyDouble(), anyDouble())).thenReturn("RODOVIA");

            alertaService.verificarAreaUrbanaEAvisar(-15.6, -56.1, "ABC1D23");

            verify(messagingTemplate, never()).convertAndSend(
                    eq("/topic/alertas"),
                    (Object) any()
            );
        }
    }
}