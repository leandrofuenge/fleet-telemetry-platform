package com.app.telemetria.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.telemetria.domain.entity.Carga;
import com.telemetria.domain.entity.Motorista;
import com.telemetria.domain.entity.Veiculo;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.domain.service.AlertaService;
import com.telemetria.domain.service.MotoristaService;
import com.telemetria.infrastructure.persistence.MotoristaRepository;

@ExtendWith(MockitoExtension.class)
class MotoristaServiceTest {

    @Mock
    private MotoristaRepository motoristaRepository;

    @Mock
    private AlertaService alertaService;

    @InjectMocks
    private MotoristaService motoristaService;

    private Motorista motorista;
    private Veiculo veiculo;
    private Carga carga;

    @BeforeEach
    void setup() {
        motorista = new Motorista();
        motorista.setId(1L);
        motorista.setNome("Leandro");
        motorista.setCpf("52998224725");
        motorista.setCnh("123456789");
        motorista.setCategoriaCnh("D");
        motorista.setScore(700);
        motorista.setDataVencimentoCnh(LocalDate.now().plusDays(30));
        motorista.setDataVencimentoAso(LocalDate.now().plusDays(30));
        motorista.setMoppValido(true);

        veiculo = new Veiculo();
        veiculo.setId(10L);
        veiculo.setPlaca("ABC1D23");

        carga = new Carga();
        carga.setTipo("PERIGOSA");
    }

    @Nested
    class Crud {

        @Test
        @DisplayName("Deve salvar motorista com sucesso")
        void deveSalvarMotorista() {
            Motorista novo = new Motorista();
            novo.setNome("Leandro");
            novo.setCpf("52998224725");
            novo.setCnh("123456789");
            novo.setCategoriaCnh("D");

            Motorista salvo = new Motorista();
            salvo.setId(1L);
            salvo.setNome(novo.getNome());
            salvo.setCpf(novo.getCpf());
            salvo.setCnh(novo.getCnh());
            salvo.setCategoriaCnh(novo.getCategoriaCnh());

            when(motoristaRepository.save(novo)).thenReturn(salvo);

            Motorista resultado = motoristaService.salvar(novo);

            assertNotNull(resultado);
            assertEquals(1L, resultado.getId());
            verify(motoristaRepository).save(novo);
        }

        @Test
        @DisplayName("Deve lançar exceção para CPF inválido ao salvar")
        void deveLancarExcecaoParaCpfInvalidoAoSalvar() {
            motorista.setCpf("11111111111");

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> motoristaService.salvar(motorista)
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
            verify(motoristaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção para CPF duplicado")
        void deveLancarExcecaoParaCpfDuplicado() {
            DataIntegrityViolationException exception =
                    new DataIntegrityViolationException("erro", new RuntimeException("Duplicate entry for cpf"));

            when(motoristaRepository.save(motorista)).thenThrow(exception);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> motoristaService.salvar(motorista)
            );

            assertEquals(ErrorCode.MOTORISTA_DUPLICATE, ex.getErrorCode());
        }

        @Test
        @DisplayName("Deve lançar exceção para CNH duplicada")
        void deveLancarExcecaoParaCnhDuplicada() {
            DataIntegrityViolationException exception =
                    new DataIntegrityViolationException("erro", new RuntimeException("Duplicate entry for cnh"));

            when(motoristaRepository.save(motorista)).thenThrow(exception);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> motoristaService.salvar(motorista)
            );

            assertEquals(ErrorCode.MOTORISTA_DUPLICATE, ex.getErrorCode());
        }

        @Test
        @DisplayName("Deve listar motoristas")
        void deveListarMotoristas() {
            when(motoristaRepository.findAll()).thenReturn(List.of(motorista));

            List<Motorista> resultado = motoristaService.listar();

            assertEquals(1, resultado.size());
            verify(motoristaRepository).findAll();
        }

        @Test
        @DisplayName("Deve buscar motorista por ID")
        void deveBuscarPorId() {
            when(motoristaRepository.findById(1L)).thenReturn(Optional.of(motorista));

            Motorista resultado = motoristaService.buscarPorId(1L);

            assertNotNull(resultado);
            assertEquals(1L, resultado.getId());
            verify(motoristaRepository).findById(1L);
        }

        @Test
        @DisplayName("Deve lançar exceção ao buscar ID inexistente")
        void deveLancarExcecaoAoBuscarIdInexistente() {
            when(motoristaRepository.findById(99L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> motoristaService.buscarPorId(99L)
            );

            assertEquals(ErrorCode.MOTORISTA_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("Deve buscar motorista por CPF")
        void deveBuscarPorCpf() {
            when(motoristaRepository.findByCpf("52998224725")).thenReturn(Optional.of(motorista));

            Motorista resultado = motoristaService.buscarPorCpf("52998224725");

            assertNotNull(resultado);
            assertEquals("Leandro", resultado.getNome());
            verify(motoristaRepository).findByCpf("52998224725");
        }

        @Test
        @DisplayName("Deve lançar exceção ao buscar CPF inexistente")
        void deveLancarExcecaoAoBuscarCpfInexistente() {
            when(motoristaRepository.findByCpf("00000000000")).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> motoristaService.buscarPorCpf("00000000000")
            );

            assertEquals(ErrorCode.MOTORISTA_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("Deve atualizar motorista com sucesso")
        void deveAtualizarMotorista() {
            Motorista existente = new Motorista();
            existente.setId(1L);
            existente.setNome("Leandro");
            existente.setCpf("52998224725");
            existente.setCnh("123456789");
            existente.setCategoriaCnh("D");

            Motorista dados = new Motorista();
            dados.setNome("Leandro Silva");
            dados.setCpf("52998224725");
            dados.setCnh("987654321");
            dados.setCategoriaCnh("E");

            when(motoristaRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(motoristaRepository.save(any(Motorista.class))).thenAnswer(inv -> inv.getArgument(0));

            Motorista atualizado = motoristaService.atualizar(1L, dados);

            assertEquals("Leandro Silva", atualizado.getNome());
            assertEquals("987654321", atualizado.getCnh());
            assertEquals("E", atualizado.getCategoriaCnh());
            verify(motoristaRepository).save(existente);
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar com CPF inválido")
        void deveLancarExcecaoAoAtualizarComCpfInvalido() {
            Motorista existente = new Motorista();
            existente.setId(1L);
            existente.setNome("Leandro");
            existente.setCpf("52998224725");
            existente.setCnh("123456789");
            existente.setCategoriaCnh("D");

            Motorista dados = new Motorista();
            dados.setNome("Leandro");
            dados.setCpf("11111111111");
            dados.setCnh("123456789");
            dados.setCategoriaCnh("D");

            when(motoristaRepository.findById(1L)).thenReturn(Optional.of(existente));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> motoristaService.atualizar(1L, dados)
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
            verify(motoristaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve deletar motorista")
        void deveDeletarMotorista() {
            when(motoristaRepository.findById(1L)).thenReturn(Optional.of(motorista));

            motoristaService.deletar(1L);

            verify(motoristaRepository).delete(motorista);
        }
    }

    @Nested
    class ValidacoesCnh {

        @Test
        @DisplayName("Deve validar CNH válida para viagem")
        void deveValidarCnhValidaParaViagem() {
            assertDoesNotThrow(() -> motoristaService.validarCnhParaViagem(motorista));
        }

        @Test
        @DisplayName("Deve lançar exceção quando data de vencimento da CNH for nula")
        void deveLancarExcecaoQuandoDataCnhForNula() {
            motorista.setDataVencimentoCnh(null);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> motoristaService.validarCnhParaViagem(motorista)
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        }

        @Test
        @DisplayName("Deve lançar exceção quando CNH estiver vencida")
        void deveLancarExcecaoQuandoCnhVencida() {
            motorista.setDataVencimentoCnh(LocalDate.now().minusDays(1));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> motoristaService.validarCnhParaViagem(motorista)
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        }

        @Test
        @DisplayName("Deve validar categoria compatível")
        void deveValidarCategoriaCompativel() {
            motorista.setCategoriaCnh("E");

            assertDoesNotThrow(() -> motoristaService.validarCategoriaCnhParaVeiculo(motorista, veiculo));
        }

        @Test
        @DisplayName("Deve lançar exceção para categoria incompatível")
        void deveLancarExcecaoParaCategoriaIncompativel() {
            motorista.setCategoriaCnh("B");

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> motoristaService.validarCategoriaCnhParaVeiculo(motorista, veiculo)
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        }
    }

    @Nested
    class ValidacoesAsoEMopp {

        @Test
        @DisplayName("Deve validar ASO válido")
        void deveValidarAsoValido() {
            assertDoesNotThrow(() -> motoristaService.validarAsoParaViagem(motorista));
        }

        @Test
        @DisplayName("Deve lançar exceção quando data do ASO for nula")
        void deveLancarExcecaoQuandoDataAsoForNula() {
            motorista.setDataVencimentoAso(null);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> motoristaService.validarAsoParaViagem(motorista)
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        }

        @Test
        @DisplayName("Deve lançar exceção quando ASO estiver vencido")
        void deveLancarExcecaoQuandoAsoVencido() {
            motorista.setDataVencimentoAso(LocalDate.now().minusDays(1));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> motoristaService.validarAsoParaViagem(motorista)
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        }

        @Test
        @DisplayName("Deve validar MOPP para carga perigosa")
        void deveValidarMoppParaCargaPerigosa() {
            motorista.setMoppValido(true);
            carga.setTipo("PERIGOSA");

            assertDoesNotThrow(() -> motoristaService.validarMoppingParaCargaPerigosa(motorista, carga));
        }

        @Test
        @DisplayName("Deve lançar exceção quando carga perigosa e motorista sem MOPP")
        void deveLancarExcecaoQuandoSemMopp() {
            motorista.setMoppValido(false);
            carga.setTipo("PERIGOSA");

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> motoristaService.validarMoppingParaCargaPerigosa(motorista, carga)
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        }

        @Test
        @DisplayName("Não deve validar MOPP para carga não perigosa")
        void naoDeveExigirMoppParaCargaNaoPerigosa() {
            motorista.setMoppValido(false);
            carga.setTipo("NORMAL");

            assertDoesNotThrow(() -> motoristaService.validarMoppingParaCargaPerigosa(motorista, carga));
        }
    }

    @Nested
    class ValidacaoScore {

        @Test
        @DisplayName("Deve atribuir score padrão quando nulo")
        void deveAtribuirScorePadraoQuandoNulo() {
            motorista.setScore(null);
            when(motoristaRepository.save(any(Motorista.class))).thenAnswer(inv -> inv.getArgument(0));

            assertDoesNotThrow(() -> motoristaService.validarScoreParaViagem(motorista));

            assertEquals(1000, motorista.getScore());
            verify(motoristaRepository).save(motorista);
        }

        @Test
        @DisplayName("Deve lançar exceção quando score for menor que 400")
        void deveLancarExcecaoQuandoScoreMenorQue400() {
            motorista.setScore(350);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> motoristaService.validarScoreParaViagem(motorista)
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        }

        @Test
        @DisplayName("Deve criar alerta quando score for menor que 600")
        void deveCriarAlertaQuandoScoreMenorQue600() {
            motorista.setScore(550);

            assertDoesNotThrow(() -> motoristaService.validarScoreParaViagem(motorista));

            verify(alertaService).criarAlertaScoreBaixo(motorista);
        }

        @Test
        @DisplayName("Não deve criar alerta quando score for maior ou igual a 600")
        void naoDeveCriarAlertaQuandoScoreMaiorIgual600() {
            motorista.setScore(700);

            assertDoesNotThrow(() -> motoristaService.validarScoreParaViagem(motorista));

            verify(alertaService, never()).criarAlertaScoreBaixo(any());
        }
    }

    @Nested
    class ValidacaoCpf {

        @Test
        @DisplayName("Deve validar CPF válido")
        void deveValidarCpfValido() {
            assertTrue(MotoristaService.validarCpf("52998224725"));
        }

        @Test
        @DisplayName("Deve validar CPF válido com máscara")
        void deveValidarCpfValidoComMascara() {
            assertTrue(MotoristaService.validarCpf("529.982.247-25"));
        }

        @Test
        @DisplayName("Deve rejeitar CPF com todos os dígitos iguais")
        void deveRejeitarCpfComDigitosIguais() {
            assertFalse(MotoristaService.validarCpf("11111111111"));
        }

        @Test
        @DisplayName("Deve rejeitar CPF com tamanho inválido")
        void deveRejeitarCpfComTamanhoInvalido() {
            assertFalse(MotoristaService.validarCpf("123"));
        }

        @Test
        @DisplayName("Deve rejeitar CPF com dígitos verificadores inválidos")
        void deveRejeitarCpfInvalido() {
            assertFalse(MotoristaService.validarCpf("52998224724"));
        }
    }
}