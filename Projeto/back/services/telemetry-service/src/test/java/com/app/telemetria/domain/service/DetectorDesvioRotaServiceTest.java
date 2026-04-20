package com.app.telemetria.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.telemetria.domain.entity.Cliente;
import com.telemetria.domain.entity.DesvioRota;
import com.telemetria.domain.entity.PontoRota;
import com.telemetria.domain.entity.Rota;
import com.telemetria.domain.entity.Telemetria;
import com.telemetria.domain.entity.Veiculo;
import com.telemetria.domain.service.DetectorDesvioRotaService;
import com.telemetria.infrastructure.integration.geocoding.GeocodingService;
import com.telemetria.infrastructure.persistence.DesvioRotaRepository;
import com.telemetria.infrastructure.persistence.RotaRepository;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;
import com.telemetria.infrastructure.persistence.VeiculoRepository;

@ExtendWith(MockitoExtension.class)
class DetectorDesvioRotaServiceTest {

    @Mock
    private RotaRepository rotaRepository;

    @Mock
    private TelemetriaRepository telemetriaRepository;

    @Mock
    private DesvioRotaRepository desvioRotaRepository;

    @Mock
    private VeiculoRepository veiculoRepository;

    @Mock
    private GeocodingService geocodingService;

    @InjectMocks
    private DetectorDesvioRotaService detectorDesvioRotaService;

    private Rota rota;
    private Veiculo veiculo;
    private Telemetria telemetriaNaRota;
    private Telemetria telemetriaForaDaRota;
    private PontoRota ponto1;
    private PontoRota ponto2;

    @BeforeEach
    void setup() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);

        veiculo = new Veiculo();
        veiculo.setId(100L);
        veiculo.setPlaca("ABC1D23");
        veiculo.setCliente(cliente);

        ponto1 = new PontoRota();
        ponto1.setLatitude(-15.6000);
        ponto1.setLongitude(-56.1000);

        ponto2 = new PontoRota();
        ponto2.setLatitude(-15.6000);
        ponto2.setLongitude(-56.0000);

        rota = new Rota();
        rota.setId(200L);
        rota.setNome("Rota Cuiabá Centro");
        rota.setStatus("EM_ANDAMENTO");
        rota.setVeiculo(veiculo);
        rota.setPontosRota(List.of(ponto1, ponto2));

        telemetriaNaRota = new Telemetria();
        telemetriaNaRota.setId(1L);
        telemetriaNaRota.setVeiculoId(100L);
        telemetriaNaRota.setVeiculoUuid("uuid-100");
        telemetriaNaRota.setLatitude(-15.6000);
        telemetriaNaRota.setLongitude(-56.0500);
        telemetriaNaRota.setVelocidade(60.0);

        telemetriaForaDaRota = new Telemetria();
        telemetriaForaDaRota.setId(2L);
        telemetriaForaDaRota.setVeiculoId(100L);
        telemetriaForaDaRota.setVeiculoUuid("uuid-100");
        telemetriaForaDaRota.setLatitude(-15.6200);
        telemetriaForaDaRota.setLongitude(-56.0500);
        telemetriaForaDaRota.setVelocidade(55.0);
    }

    @Nested
    class VerificarDesviosAtivos {

        @Test
        @DisplayName("Deve verificar desvios das rotas ativas sem lançar exceção")
        void deveVerificarDesviosAtivos() {
            when(rotaRepository.findByStatus("EM_ANDAMENTO"))
                    .thenReturn(List.of(rota));
            when(telemetriaRepository.findUltimaTelemetriaByVeiculoId(100L))
                    .thenReturn(Optional.of(telemetriaNaRota));
            when(desvioRotaRepository.findByRotaIdAndResolvidoFalse(200L))
                    .thenReturn(Optional.empty());

            assertDoesNotThrow(() -> detectorDesvioRotaService.verificarDesviosAtivos());

            verify(rotaRepository).findByStatus("EM_ANDAMENTO");
            verify(telemetriaRepository).findUltimaTelemetriaByVeiculoId(100L);
        }

        @Test
        @DisplayName("Deve ignorar rota sem veículo")
        void deveIgnorarRotaSemVeiculo() {
            rota.setVeiculo(null);

            when(rotaRepository.findByStatus("EM_ANDAMENTO"))
                    .thenReturn(List.of(rota));

            detectorDesvioRotaService.verificarDesviosAtivos();

            verify(telemetriaRepository, never()).findUltimaTelemetriaByVeiculoId(any());
            verify(desvioRotaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve ignorar quando não houver telemetria")
        void deveIgnorarQuandoNaoHouverTelemetria() {
            when(rotaRepository.findByStatus("EM_ANDAMENTO"))
                    .thenReturn(List.of(rota));
            when(telemetriaRepository.findUltimaTelemetriaByVeiculoId(100L))
                    .thenReturn(Optional.empty());

            detectorDesvioRotaService.verificarDesviosAtivos();

            verify(desvioRotaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve ignorar rota com menos de 2 pontos")
        void deveIgnorarRotaComMenosDeDoisPontos() {
            rota.setPontosRota(List.of(ponto1));

            when(rotaRepository.findByStatus("EM_ANDAMENTO"))
                    .thenReturn(List.of(rota));
            when(telemetriaRepository.findUltimaTelemetriaByVeiculoId(100L))
                    .thenReturn(Optional.of(telemetriaNaRota));
            when(desvioRotaRepository.findByRotaIdAndResolvidoFalse(200L))
                    .thenReturn(Optional.empty());

            detectorDesvioRotaService.verificarDesviosAtivos();

            verify(desvioRotaRepository).save(any(DesvioRota.class));
        }
    }

    @Nested
    class RegistroDeDesvio {

        @Test
        @DisplayName("Deve registrar novo desvio quando veículo estiver fora da rota")
        void deveRegistrarNovoDesvio() {
            when(rotaRepository.findByStatus("EM_ANDAMENTO"))
                    .thenReturn(List.of(rota));
            when(telemetriaRepository.findUltimaTelemetriaByVeiculoId(100L))
                    .thenReturn(Optional.of(telemetriaForaDaRota));
            when(desvioRotaRepository.findByRotaIdAndResolvidoFalse(200L))
                    .thenReturn(Optional.empty());
            when(desvioRotaRepository.save(any(DesvioRota.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            detectorDesvioRotaService.verificarDesviosAtivos();

            ArgumentCaptor<DesvioRota> captor = ArgumentCaptor.forClass(DesvioRota.class);
            verify(desvioRotaRepository).save(captor.capture());

            DesvioRota salvo = captor.getValue();
            org.junit.jupiter.api.Assertions.assertEquals(rota.getId(), salvo.getRotaId());
            org.junit.jupiter.api.Assertions.assertEquals(telemetriaForaDaRota.getVeiculoId(), salvo.getVeiculoId());
            org.junit.jupiter.api.Assertions.assertEquals(telemetriaForaDaRota.getVeiculoUuid(), salvo.getVeiculoUuid());
            org.junit.jupiter.api.Assertions.assertFalse(salvo.getResolvido());
            org.junit.jupiter.api.Assertions.assertFalse(salvo.getAlertaEnviado());
            org.junit.jupiter.api.Assertions.assertNotNull(salvo.getDataHoraDesvio());
            org.junit.jupiter.api.Assertions.assertTrue(salvo.getDistanciaMetros() > 50.0);
        }

        @Test
        @DisplayName("Não deve registrar novo desvio quando já existir desvio ativo")
        void naoDeveRegistrarNovoDesvioQuandoJaExistirAtivo() {
            DesvioRota desvioExistente = DesvioRota.builder()
                    .id(999L)
                    .rotaId(200L)
                    .resolvido(false)
                    .build();

            when(rotaRepository.findByStatus("EM_ANDAMENTO"))
                    .thenReturn(List.of(rota));
            when(telemetriaRepository.findUltimaTelemetriaByVeiculoId(100L))
                    .thenReturn(Optional.of(telemetriaForaDaRota));
            when(desvioRotaRepository.findByRotaIdAndResolvidoFalse(200L))
                    .thenReturn(Optional.of(desvioExistente));

            detectorDesvioRotaService.verificarDesviosAtivos();

            verify(desvioRotaRepository, never()).save(any(DesvioRota.class));
        }
    }

    @Nested
    class RetornoParaRota {

        @Test
        @DisplayName("Deve resolver desvio quando veículo retornar para a rota")
        void deveResolverDesvioQuandoRetornarParaRota() {
            DesvioRota desvioAtivo = DesvioRota.builder()
                    .id(500L)
                    .rotaId(200L)
                    .veiculoId(100L)
                    .resolvido(false)
                    .build();

            when(rotaRepository.findByStatus("EM_ANDAMENTO"))
                    .thenReturn(List.of(rota));
            when(telemetriaRepository.findUltimaTelemetriaByVeiculoId(100L))
                    .thenReturn(Optional.of(telemetriaNaRota));
            when(desvioRotaRepository.findByRotaIdAndResolvidoFalse(200L))
                    .thenReturn(Optional.of(desvioAtivo));
            when(desvioRotaRepository.save(any(DesvioRota.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            detectorDesvioRotaService.verificarDesviosAtivos();

            ArgumentCaptor<DesvioRota> captor = ArgumentCaptor.forClass(DesvioRota.class);
            verify(desvioRotaRepository).save(captor.capture());

            DesvioRota salvo = captor.getValue();
            org.junit.jupiter.api.Assertions.assertTrue(salvo.getResolvido());
            org.junit.jupiter.api.Assertions.assertNotNull(salvo.getDataHoraRetorno());
        }

        @Test
        @DisplayName("Não deve resolver nada quando não existir desvio ativo")
        void naoDeveResolverNadaQuandoNaoExistirDesvioAtivo() {
            when(rotaRepository.findByStatus("EM_ANDAMENTO"))
                    .thenReturn(List.of(rota));
            when(telemetriaRepository.findUltimaTelemetriaByVeiculoId(100L))
                    .thenReturn(Optional.of(telemetriaNaRota));
            when(desvioRotaRepository.findByRotaIdAndResolvidoFalse(200L))
                    .thenReturn(Optional.empty());

            detectorDesvioRotaService.verificarDesviosAtivos();

            verify(desvioRotaRepository, never()).save(any(DesvioRota.class));
        }
    }

    @Nested
    class Robustez {

        @Test
        @DisplayName("Deve continuar processamento mesmo se uma rota gerar exceção")
        void deveContinuarProcessamentoMesmoSeUmaRotaGerarExcecao() {
            Rota rotaComErro = new Rota();
            rotaComErro.setId(300L);
            rotaComErro.setNome("Rota com erro");
            rotaComErro.setStatus("EM_ANDAMENTO");
            rotaComErro.setVeiculo(veiculo);
            rotaComErro.setPontosRota(null);

            when(rotaRepository.findByStatus("EM_ANDAMENTO"))
                    .thenReturn(List.of(rotaComErro, rota));

            when(telemetriaRepository.findUltimaTelemetriaByVeiculoId(100L))
                    .thenReturn(Optional.of(telemetriaNaRota));

            when(desvioRotaRepository.findByRotaIdAndResolvidoFalse(200L))
                    .thenReturn(Optional.empty());

            assertDoesNotThrow(() -> detectorDesvioRotaService.verificarDesviosAtivos());

            verify(rotaRepository, times(1)).findByStatus("EM_ANDAMENTO");
            verify(telemetriaRepository, times(2)).findUltimaTelemetriaByVeiculoId(100L);
        }
    }
}