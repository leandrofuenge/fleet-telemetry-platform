package com.app.telemetria.domain.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

import com.app.telemetria.api.dto.response.VeiculoDTO;
import com.app.telemetria.domain.entity.Cliente;
import com.app.telemetria.domain.entity.Veiculo;
import com.app.telemetria.domain.exception.BusinessException;
import com.app.telemetria.domain.exception.ErrorCode;
import com.app.telemetria.domain.exception.VeiculoNotFoundException;
import com.app.telemetria.infrastructure.persistence.VeiculoRepository;

@ExtendWith(MockitoExtension.class)
class VeiculoServiceTest {

    @Mock
    private VeiculoRepository repository;

    @InjectMocks
    private VeiculoService veiculoService;

    private Veiculo veiculo;

    @BeforeEach
    void setup() {
        veiculo = new Veiculo();
        veiculo.setId(1L);
        veiculo.setPlaca("ABC1D23");
        veiculo.setModelo("Volvo FH");
        veiculo.setMarca("Volvo");
        veiculo.setCapacidadeCarga(25000.0);
        veiculo.setAnoFabricacao(2022);
        veiculo.setTenantId(10L);
        veiculo.setClienteId(100L);
        veiculo.setAtivo(true);
        veiculo.setPbt(4000.0);
        veiculo.setTacografoObrigatorio(false);
    }

    @Nested
    class CrudSalvar {

        @Test
        @DisplayName("Deve salvar veículo com sucesso")
        void deveSalvarVeiculoComSucesso() {
            when(repository.existsByPlacaAndTenantId("ABC1D23", 10L)).thenReturn(false);
            when(repository.save(any(Veiculo.class))).thenAnswer(inv -> {
                Veiculo v = inv.getArgument(0);
                v.setId(1L);
                return v;
            });

            VeiculoDTO dto = veiculoService.salvar(veiculo);

            assertNotNull(dto);
            assertEquals(1L, dto.getId());
            assertEquals("ABC1D23", dto.getPlaca());
            assertEquals("Volvo FH", dto.getModelo());
            assertEquals(25000.0, dto.getCapacidadeCarga());
            verify(repository).save(veiculo);
        }

        @Test
        @DisplayName("Deve lançar exceção quando placa for nula")
        void deveLancarExcecaoQuandoPlacaForNula() {
            veiculo.setPlaca(null);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> veiculoService.salvar(veiculo)
            );

            assertTrue(ex.getMessage().contains("Placa é obrigatória"));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando formato da placa for inválido")
        void deveLancarExcecaoQuandoFormatoDaPlacaForInvalido() {
            veiculo.setPlaca("1234567");

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> veiculoService.salvar(veiculo)
            );

            assertTrue(ex.getMessage().contains("Formato de placa inválido"));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando placa já existir no tenant")
        void deveLancarExcecaoQuandoPlacaJaExistirNoTenant() {
            Veiculo existente = new Veiculo();
            existente.setId(99L);
            existente.setPlaca("ABC1D23");
            existente.setModelo("Scania R450");

            when(repository.existsByPlacaAndTenantId("ABC1D23", 10L)).thenReturn(true);
            when(repository.findByPlacaAndTenantId("ABC1D23", 10L)).thenReturn(Optional.of(existente));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> veiculoService.salvar(veiculo)
            );

            assertTrue(ex.getMessage().contains("Placa já cadastrada"));
            assertTrue(ex.getMessage().contains("Scania R450"));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve marcar tacógrafo como obrigatório quando PBT for maior que 4536")
        void deveMarcarTacografoComoObrigatorioQuandoPbtMaiorQue4536() {
            veiculo.setPbt(6000.0);
            veiculo.setDataVencimentoTacografo(LocalDate.now().plusDays(30));

            when(repository.existsByPlacaAndTenantId("ABC1D23", 10L)).thenReturn(false);
            when(repository.save(any(Veiculo.class))).thenAnswer(inv -> inv.getArgument(0));

            VeiculoDTO dto = veiculoService.salvar(veiculo);

            assertNotNull(dto);
            assertTrue(veiculo.getTacografoObrigatorio());
            verify(repository).save(veiculo);
        }

        @Test
        @DisplayName("Deve lançar exceção quando tacógrafo for obrigatório e data não for informada")
        void deveLancarExcecaoQuandoTacografoObrigatorioSemData() {
            veiculo.setPbt(7000.0);
            veiculo.setDataVencimentoTacografo(null);

            when(repository.existsByPlacaAndTenantId("ABC1D23", 10L)).thenReturn(false);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> veiculoService.salvar(veiculo)
            );

            assertTrue(ex.getMessage().contains("requer data de vencimento do tacógrafo"));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção interna para erro de integridade ao salvar")
        void deveLancarExcecaoInternaParaErroDeIntegridadeAoSalvar() {
            when(repository.existsByPlacaAndTenantId("ABC1D23", 10L)).thenReturn(false);
            when(repository.save(any(Veiculo.class)))
                    .thenThrow(new DataIntegrityViolationException("erro"));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> veiculoService.salvar(veiculo)
            );

            assertEquals(ErrorCode.INTERNAL_ERROR, ex.getErrorCode());
        }
    }

    @Nested
    class CrudListarEBuscar {

        @Test
        @DisplayName("Deve listar todos os veículos")
        void deveListarTodosOsVeiculos() {
            when(repository.findAll()).thenReturn(List.of(veiculo));

            List<VeiculoDTO> resultado = veiculoService.listarTodos();

            assertEquals(1, resultado.size());
            assertEquals("ABC1D23", resultado.get(0).getPlaca());
        }

        @Test
        @DisplayName("Deve buscar veículo por ID")
        void deveBuscarVeiculoPorId() {
            when(repository.findById(1L)).thenReturn(Optional.of(veiculo));

            VeiculoDTO dto = veiculoService.buscarPorId(1L);

            assertNotNull(dto);
            assertEquals(1L, dto.getId());
            assertEquals("ABC1D23", dto.getPlaca());
        }

        @Test
        @DisplayName("Deve lançar exceção ao buscar veículo por ID inexistente")
        void deveLancarExcecaoAoBuscarVeiculoPorIdInexistente() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(VeiculoNotFoundException.class, () -> veiculoService.buscarPorId(99L));
        }

        @Test
        @DisplayName("Deve buscar veículo por placa")
        void deveBuscarVeiculoPorPlaca() {
            when(repository.findByPlaca("ABC1D23")).thenReturn(Optional.of(veiculo));

            VeiculoDTO dto = veiculoService.buscarPorPlaca("ABC1D23");

            assertNotNull(dto);
            assertEquals("ABC1D23", dto.getPlaca());
        }

        @Test
        @DisplayName("Deve lançar exceção ao buscar veículo por placa inexistente")
        void deveLancarExcecaoAoBuscarVeiculoPorPlacaInexistente() {
            when(repository.findByPlaca("ZZZ9Z99")).thenReturn(Optional.empty());

            assertThrows(VeiculoNotFoundException.class, () -> veiculoService.buscarPorPlaca("ZZZ9Z99"));
        }
    }

    @Nested
    class CrudAtualizar {

        @Test
        @DisplayName("Deve atualizar veículo com sucesso")
        void deveAtualizarVeiculoComSucesso() {
            Veiculo dados = new Veiculo();
            dados.setModelo("Volvo FH 2024");
            dados.setMarca("VOLVO");
            dados.setCapacidadeCarga(28000.0);
            dados.setAnoFabricacao(2024);

            when(repository.findById(1L)).thenReturn(Optional.of(veiculo));
            when(repository.save(any(Veiculo.class))).thenAnswer(inv -> inv.getArgument(0));

            VeiculoDTO dto = veiculoService.atualizar(1L, dados);

            assertNotNull(dto);
            assertEquals("Volvo FH 2024", dto.getModelo());
            assertEquals(28000.0, dto.getCapacidadeCarga());
            verify(repository).save(veiculo);
        }

        @Test
        @DisplayName("Deve atualizar placa quando válida e não duplicada")
        void deveAtualizarPlacaQuandoValidaENaoDuplicada() {
            Veiculo dados = new Veiculo();
            dados.setPlaca("DEF-1234");

            when(repository.findById(1L)).thenReturn(Optional.of(veiculo));
            when(repository.existsByPlacaAndTenantId("DEF-1234", 10L)).thenReturn(false);
            when(repository.save(any(Veiculo.class))).thenAnswer(inv -> inv.getArgument(0));

            VeiculoDTO dto = veiculoService.atualizar(1L, dados);

            assertEquals("DEF-1234", dto.getPlaca());
            verify(repository).save(veiculo);
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar com placa inválida")
        void deveLancarExcecaoAoAtualizarComPlacaInvalida() {
            Veiculo dados = new Veiculo();
            dados.setPlaca("XXXX");

            when(repository.findById(1L)).thenReturn(Optional.of(veiculo));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> veiculoService.atualizar(1L, dados)
            );

            assertTrue(ex.getMessage().contains("Formato de placa inválido"));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar com placa duplicada no tenant")
        void deveLancarExcecaoAoAtualizarComPlacaDuplicadaNoTenant() {
            Veiculo dados = new Veiculo();
            dados.setPlaca("DEF1G23");

            Veiculo existente = new Veiculo();
            existente.setPlaca("DEF1G23");
            existente.setModelo("Mercedes Actros");

            when(repository.findById(1L)).thenReturn(Optional.of(veiculo));
            when(repository.existsByPlacaAndTenantId("DEF1G23", 10L)).thenReturn(true);
            when(repository.findByPlacaAndTenantId("DEF1G23", 10L)).thenReturn(Optional.of(existente));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> veiculoService.atualizar(1L, dados)
            );

            assertTrue(ex.getMessage().contains("Placa já cadastrada"));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando veículo não existir para atualização")
        void deveLancarExcecaoQuandoVeiculoNaoExistirParaAtualizacao() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(VeiculoNotFoundException.class, () -> veiculoService.atualizar(99L, new Veiculo()));
        }

        @Test
        @DisplayName("Deve atualizar PBT e marcar tacógrafo obrigatório")
        void deveAtualizarPbtEMarcarTacografoObrigatorio() {
            Veiculo dados = new Veiculo();
            dados.setPbt(8000.0);
            veiculo.setDataVencimentoTacografo(LocalDate.now().plusDays(15));

            when(repository.findById(1L)).thenReturn(Optional.of(veiculo));
            when(repository.save(any(Veiculo.class))).thenAnswer(inv -> inv.getArgument(0));

            VeiculoDTO dto = veiculoService.atualizar(1L, dados);

            assertNotNull(dto);
            assertTrue(veiculo.getTacografoObrigatorio());
            verify(repository).save(veiculo);
        }

        @Test
        @DisplayName("Deve lançar exceção ao atualizar PBT com tacógrafo obrigatório sem data")
        void deveLancarExcecaoAoAtualizarPbtComTacografoObrigatorioSemData() {
            Veiculo dados = new Veiculo();
            dados.setPbt(9000.0);
            veiculo.setDataVencimentoTacografo(null);

            when(repository.findById(1L)).thenReturn(Optional.of(veiculo));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> veiculoService.atualizar(1L, dados)
            );

            assertTrue(ex.getMessage().contains("requer data de vencimento do tacógrafo"));
            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("Deve atualizar cliente e tenant com base no cliente informado")
        void deveAtualizarClienteETenantComBaseNoClienteInformado() {
            Veiculo dados = new Veiculo();
            Cliente cliente = new Cliente();
            cliente.setId(55L);

            dados.setClienteId(55L);
            dados.setCliente(cliente);

            when(repository.findById(1L)).thenReturn(Optional.of(veiculo));
            when(repository.save(any(Veiculo.class))).thenAnswer(inv -> inv.getArgument(0));

            veiculoService.atualizar(1L, dados);

            assertEquals(55L, veiculo.getClienteId());
            assertEquals(55L, veiculo.getTenantId());
            verify(repository).save(veiculo);
        }

        @Test
        @DisplayName("Deve lançar exceção interna ao ocorrer erro de integridade na atualização")
        void deveLancarExcecaoInternaAoOcorrerErroDeIntegridadeNaAtualizacao() {
            Veiculo dados = new Veiculo();
            dados.setModelo("Novo Modelo");

            when(repository.findById(1L)).thenReturn(Optional.of(veiculo));
            when(repository.save(any(Veiculo.class)))
                    .thenThrow(new DataIntegrityViolationException("erro"));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> veiculoService.atualizar(1L, dados)
            );

            assertEquals(ErrorCode.INTERNAL_ERROR, ex.getErrorCode());
        }
    }

    @Nested
    class CrudDeletar {

        @Test
        @DisplayName("Deve deletar veículo com sucesso")
        void deveDeletarVeiculoComSucesso() {
            when(repository.existsById(1L)).thenReturn(true);

            assertDoesNotThrow(() -> veiculoService.deletar(1L));

            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("Deve lançar exceção ao deletar veículo inexistente")
        void deveLancarExcecaoAoDeletarVeiculoInexistente() {
            when(repository.existsById(99L)).thenReturn(false);

            assertThrows(VeiculoNotFoundException.class, () -> veiculoService.deletar(99L));

            verify(repository, never()).deleteById(any());
        }
    }

    @Nested
    class ValidacaoDocumentos {

        @Test
        @DisplayName("Deve validar documentos válidos para viagem")
        void deveValidarDocumentosValidosParaViagem() {
            veiculo.setDataVencimentoCrlv(LocalDate.now().plusDays(10));
            veiculo.setDataVencimentoSeguro(LocalDate.now().plusDays(10));
            veiculo.setDataVencimentoDpvat(LocalDate.now().plusDays(10));
            veiculo.setTacografoObrigatorio(false);

            assertDoesNotThrow(() -> veiculoService.validarDocumentosParaViagem(veiculo));
        }

        @Test
        @DisplayName("Deve lançar exceção para CRLV vencido")
        void deveLancarExcecaoParaCrlvVencido() {
            veiculo.setDataVencimentoCrlv(LocalDate.now().minusDays(1));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> veiculoService.validarDocumentosParaViagem(veiculo)
            );

            assertTrue(ex.getMessage().contains("CRLV vencido"));
        }

        @Test
        @DisplayName("Deve lançar exceção para seguro vencido")
        void deveLancarExcecaoParaSeguroVencido() {
            veiculo.setDataVencimentoSeguro(LocalDate.now().minusDays(1));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> veiculoService.validarDocumentosParaViagem(veiculo)
            );

            assertTrue(ex.getMessage().contains("Seguro vencido"));
        }

        @Test
        @DisplayName("Deve lançar exceção para DPVAT vencido")
        void deveLancarExcecaoParaDpvatVencido() {
            veiculo.setDataVencimentoDpvat(LocalDate.now().minusDays(1));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> veiculoService.validarDocumentosParaViagem(veiculo)
            );

            assertTrue(ex.getMessage().contains("DPVAT vencido"));
        }

        @Test
        @DisplayName("Deve lançar exceção para tacógrafo vencido quando obrigatório")
        void deveLancarExcecaoParaTacografoVencidoQuandoObrigatorio() {
            veiculo.setTacografoObrigatorio(true);
            veiculo.setDataVencimentoTacografo(LocalDate.now().minusDays(1));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> veiculoService.validarDocumentosParaViagem(veiculo)
            );

            assertTrue(ex.getMessage().contains("Tacógrafo vencido"));
        }

        @Test
        @DisplayName("Deve validar documentos por ID")
        void deveValidarDocumentosPorId() {
            veiculo.setDataVencimentoCrlv(LocalDate.now().plusDays(10));
            when(repository.findById(1L)).thenReturn(Optional.of(veiculo));

            assertDoesNotThrow(() -> veiculoService.validarDocumentosParaViagem(1L));
        }

        @Test
        @DisplayName("Deve lançar exceção quando veículo não existir ao validar documentos por ID")
        void deveLancarExcecaoQuandoVeiculoNaoExistirAoValidarDocumentosPorId() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(VeiculoNotFoundException.class, () -> veiculoService.validarDocumentosParaViagem(99L));
        }

        @Test
        @DisplayName("Deve retornar true quando documentos forem válidos")
        void deveRetornarTrueQuandoDocumentosForemValidos() {
            veiculo.setDataVencimentoCrlv(LocalDate.now().plusDays(10));
            when(repository.findById(1L)).thenReturn(Optional.of(veiculo));

            boolean resultado = veiculoService.isDocumentosValidos(1L);

            assertTrue(resultado);
        }

        @Test
        @DisplayName("Deve retornar false quando documentos forem inválidos")
        void deveRetornarFalseQuandoDocumentosForemInvalidos() {
            veiculo.setDataVencimentoCrlv(LocalDate.now().minusDays(1));
            when(repository.findById(1L)).thenReturn(Optional.of(veiculo));

            boolean resultado = veiculoService.isDocumentosValidos(1L);

            assertFalse(resultado);
        }

        @Test
        @DisplayName("Deve retornar lista de documentos vencidos")
        void deveRetornarListaDeDocumentosVencidos() {
            veiculo.setDataVencimentoCrlv(LocalDate.now().minusDays(1));
            veiculo.setDataVencimentoSeguro(LocalDate.now().minusDays(1));
            veiculo.setDataVencimentoDpvat(LocalDate.now().plusDays(10));
            veiculo.setDataVencimentoRcf(LocalDate.now().minusDays(1));
            veiculo.setTacografoObrigatorio(true);
            veiculo.setDataVencimentoTacografo(LocalDate.now().minusDays(1));

            when(repository.findById(1L)).thenReturn(Optional.of(veiculo));

            List<String> vencidos = veiculoService.getDocumentosVencidos(1L);

            assertEquals(4, vencidos.size());
            assertTrue(vencidos.contains("CRLV"));
            assertTrue(vencidos.contains("Seguro"));
            assertTrue(vencidos.contains("RCF"));
            assertTrue(vencidos.contains("Tacógrafo"));
        }

        @Test
        @DisplayName("Deve lançar exceção ao obter documentos vencidos de veículo inexistente")
        void deveLancarExcecaoAoObterDocumentosVencidosDeVeiculoInexistente() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(VeiculoNotFoundException.class, () -> veiculoService.getDocumentosVencidos(99L));
        }
    }

    @Nested
    class VinculoDispositivos {

        @Test
        @DisplayName("Deve validar existência do veículo ao vincular dispositivo")
        void deveValidarExistenciaDoVeiculoAoVincularDispositivo() {
            when(repository.findById(1L)).thenReturn(Optional.of(veiculo));

            assertDoesNotThrow(() -> veiculoService.vincularDispositivo(1L, "ESP32-001"));
        }

        @Test
        @DisplayName("Deve lançar exceção ao vincular dispositivo em veículo inexistente")
        void deveLancarExcecaoAoVincularDispositivoEmVeiculoInexistente() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(VeiculoNotFoundException.class, () -> veiculoService.vincularDispositivo(99L, "ESP32-001"));
        }

        @Test
        @DisplayName("Deve validar existência do veículo ao adicionar dispositivo backup")
        void deveValidarExistenciaDoVeiculoAoAdicionarDispositivoBackup() {
            when(repository.findById(1L)).thenReturn(Optional.of(veiculo));

            assertDoesNotThrow(() -> veiculoService.adicionarDispositivoBackup(1L, "BACKUP-01"));
        }

        @Test
        @DisplayName("Deve lançar exceção ao adicionar dispositivo backup em veículo inexistente")
        void deveLancarExcecaoAoAdicionarDispositivoBackupEmVeiculoInexistente() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(VeiculoNotFoundException.class, () -> veiculoService.adicionarDispositivoBackup(99L, "BACKUP-01"));
        }

        @Test
        @DisplayName("Deve lançar exceção quando trocar dispositivo sem informar odômetro")
        void deveLancarExcecaoQuandoTrocarDispositivoSemInformarOdometro() {
            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> veiculoService.trocarDispositivo(1L, "NOVO-01", null)
            );

            assertTrue(ex.getMessage().contains("Odômetro atual é obrigatório"));
        }

        @Test
        @DisplayName("Deve lançar exceção quando trocar dispositivo com odômetro negativo")
        void deveLancarExcecaoQuandoTrocarDispositivoComOdometroNegativo() {
            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> veiculoService.trocarDispositivo(1L, "NOVO-01", -10.0)
            );

            assertTrue(ex.getMessage().contains("Odômetro atual é obrigatório"));
        }

        @Test
        @DisplayName("Deve validar existência do veículo ao trocar dispositivo")
        void deveValidarExistenciaDoVeiculoAoTrocarDispositivo() {
            when(repository.findById(1L)).thenReturn(Optional.of(veiculo));

            assertDoesNotThrow(() -> veiculoService.trocarDispositivo(1L, "NOVO-01", 12345.0));
        }

        @Test
        @DisplayName("Deve lançar exceção ao trocar dispositivo de veículo inexistente")
        void deveLancarExcecaoAoTrocarDispositivoDeVeiculoInexistente() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(VeiculoNotFoundException.class, () -> veiculoService.trocarDispositivo(99L, "NOVO-01", 1000.0));
        }
    }

    @Nested
    class BuscasAdicionais {

        @Test
        @DisplayName("Deve buscar por modelo")
        void deveBuscarPorModelo() {
            when(repository.findByModeloContainingIgnoreCase("Volvo")).thenReturn(List.of(veiculo));

            List<VeiculoDTO> resultado = veiculoService.buscarPorModelo("Volvo");

            assertEquals(1, resultado.size());
            assertEquals("Volvo FH", resultado.get(0).getModelo());
        }

        @Test
        @DisplayName("Deve buscar por marca")
        void deveBuscarPorMarca() {
            when(repository.findByMarcaIgnoreCase("Volvo")).thenReturn(List.of(veiculo));

            List<VeiculoDTO> resultado = veiculoService.buscarPorMarca("Volvo");

            assertEquals(1, resultado.size());
            assertEquals("ABC1D23", resultado.get(0).getPlaca());
        }

        @Test
        @DisplayName("Deve buscar veículos ativos")
        void deveBuscarVeiculosAtivos() {
            when(repository.findByAtivoTrue()).thenReturn(List.of(veiculo));

            List<VeiculoDTO> resultado = veiculoService.buscarAtivos();

            assertEquals(1, resultado.size());
        }

        @Test
        @DisplayName("Deve buscar por cliente")
        void deveBuscarPorCliente() {
            when(repository.findByClienteId(100L)).thenReturn(List.of(veiculo));

            List<VeiculoDTO> resultado = veiculoService.buscarPorCliente(100L);

            assertEquals(1, resultado.size());
        }

        @Test
        @DisplayName("Deve buscar por tenant")
        void deveBuscarPorTenant() {
            when(repository.findByTenantId(10L)).thenReturn(List.of(veiculo));

            List<VeiculoDTO> resultado = veiculoService.buscarPorTenant(10L);

            assertEquals(1, resultado.size());
        }

        @Test
        @DisplayName("Deve buscar veículos com documentos vencidos")
        void deveBuscarVeiculosComDocumentosVencidos() {
            when(repository.findWithDocumentosVencidos(10L)).thenReturn(List.of(veiculo));

            List<VeiculoDTO> resultado = veiculoService.buscarComDocumentosVencidos(10L);

            assertEquals(1, resultado.size());
        }

        @Test
        @DisplayName("Deve buscar veículos com tacógrafo vencendo em dias")
        void deveBuscarVeiculosComTacografoVencendoEmDias() {
            when(repository.findTacografoVencendoEmDias(any(LocalDate.class))).thenReturn(List.of(veiculo));

            List<VeiculoDTO> resultado = veiculoService.buscarTacografoVencendoEmDias(30);

            assertEquals(1, resultado.size());
            verify(repository).findTacografoVencendoEmDias(any(LocalDate.class));
        }

        @Test
        @DisplayName("Deve buscar por placa optional")
        void deveBuscarPorPlacaOptional() {
            when(repository.findByPlaca("ABC1D23")).thenReturn(Optional.of(veiculo));

            Optional<VeiculoDTO> resultado = veiculoService.buscarPorPlacaOptional("ABC1D23");

            assertTrue(resultado.isPresent());
            assertEquals("ABC1D23", resultado.get().getPlaca());
        }

        @Test
        @DisplayName("Deve retornar vazio ao buscar por placa optional inexistente")
        void deveRetornarVazioAoBuscarPorPlacaOptionalInexistente() {
            when(repository.findByPlaca("ZZZ9Z99")).thenReturn(Optional.empty());

            Optional<VeiculoDTO> resultado = veiculoService.buscarPorPlacaOptional("ZZZ9Z99");

            assertTrue(resultado.isEmpty());
        }

        @Test
        @DisplayName("Deve buscar por placa e tenant optional")
        void deveBuscarPorPlacaETenantOptional() {
            when(repository.findByPlacaAndTenantId("ABC1D23", 10L)).thenReturn(Optional.of(veiculo));

            Optional<VeiculoDTO> resultado = veiculoService.buscarPorPlacaAndTenantOptional("ABC1D23", 10L);

            assertTrue(resultado.isPresent());
            assertEquals("ABC1D23", resultado.get().getPlaca());
        }

        @Test
        @DisplayName("Deve verificar existência de placa no tenant")
        void deveVerificarExistenciaDePlacaNoTenant() {
            when(repository.existsByPlacaAndTenantId("ABC1D23", 10L)).thenReturn(true);

            boolean resultado = veiculoService.placaExistsInTenant("ABC1D23", 10L);

            assertTrue(resultado);
        }

        @Test
        @DisplayName("Deve verificar existência global da placa")
        void deveVerificarExistenciaGlobalDaPlaca() {
            when(repository.existsByPlaca("ABC1D23")).thenReturn(true);

            boolean resultado = veiculoService.placaExists("ABC1D23");

            assertTrue(resultado);
        }
    }
}