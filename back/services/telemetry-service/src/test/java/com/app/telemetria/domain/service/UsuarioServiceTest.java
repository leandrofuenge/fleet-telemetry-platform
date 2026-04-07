package com.app.telemetria.domain.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import com.telemetria.domain.entity.HistoricoSenha;
import com.telemetria.domain.entity.SessaoAtiva;
import com.telemetria.domain.entity.Usuario;
import com.telemetria.domain.enums.Perfil;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.domain.service.UsuarioService;
import com.telemetria.infrastructure.persistence.HistoricoSenhaRepository;
import com.telemetria.infrastructure.persistence.SessaoAtivaRepository;
import com.telemetria.infrastructure.persistence.UsuarioRepository;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private HistoricoSenhaRepository historicoSenhaRepository;

    @Mock
    private SessaoAtivaRepository sessaoAtivaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;

    @BeforeEach
    void setup() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setLogin("leandro");
        usuario.setEmail("leandro@email.com");
        usuario.setCpf("52998224725");
        usuario.setSenha("senhaHashAtual");
        usuario.setPerfil(Perfil.ADMIN);
        usuario.setMfaAtivado(false);
        usuario.setTentativasFalha(0);
    }

    @Nested
    class CriacaoUsuario {

        @Test
        @DisplayName("Deve criar usuário com sucesso")
        void deveCriarUsuarioComSucesso() {
            when(usuarioRepository.existsByLogin("leandro")).thenReturn(false);
            when(usuarioRepository.existsByEmail("leandro@email.com")).thenReturn(false);
            when(usuarioRepository.existsByCpf("52998224725")).thenReturn(false);
            when(passwordEncoder.encode("Senha@123")).thenReturn("senhaHashNova");
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
            when(historicoSenhaRepository.save(any(HistoricoSenha.class))).thenAnswer(inv -> inv.getArgument(0));

            Usuario resultado = usuarioService.criar(usuario, "Senha@123");

            assertNotNull(resultado);
            assertEquals("senhaHashNova", resultado.getSenha());
            verify(usuarioRepository).save(usuario);
            verify(historicoSenhaRepository).save(any(HistoricoSenha.class));
        }

        @Test
        @DisplayName("Deve lançar exceção para senha fraca na criação")
        void deveLancarExcecaoParaSenhaFracaNaCriacao() {
            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> usuarioService.criar(usuario, "123")
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando login já existir")
        void deveLancarExcecaoQuandoLoginJaExistir() {
            when(usuarioRepository.existsByLogin("leandro")).thenReturn(true);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> usuarioService.criar(usuario, "Senha@123")
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando email já existir")
        void deveLancarExcecaoQuandoEmailJaExistir() {
            when(usuarioRepository.existsByLogin("leandro")).thenReturn(false);
            when(usuarioRepository.existsByEmail("leandro@email.com")).thenReturn(true);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> usuarioService.criar(usuario, "Senha@123")
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção quando CPF já existir")
        void deveLancarExcecaoQuandoCpfJaExistir() {
            when(usuarioRepository.existsByLogin("leandro")).thenReturn(false);
            when(usuarioRepository.existsByEmail("leandro@email.com")).thenReturn(false);
            when(usuarioRepository.existsByCpf("52998224725")).thenReturn(true);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> usuarioService.criar(usuario, "Senha@123")
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar exceção interna para erro de integridade")
        void deveLancarExcecaoInternaParaErroDeIntegridade() {
            when(usuarioRepository.existsByLogin("leandro")).thenReturn(false);
            when(usuarioRepository.existsByEmail("leandro@email.com")).thenReturn(false);
            when(usuarioRepository.existsByCpf("52998224725")).thenReturn(false);
            when(passwordEncoder.encode("Senha@123")).thenReturn("senhaHashNova");
            when(usuarioRepository.save(any(Usuario.class)))
                    .thenThrow(new DataIntegrityViolationException("erro"));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> usuarioService.criar(usuario, "Senha@123")
            );

            assertEquals(ErrorCode.INTERNAL_ERROR, ex.getErrorCode());
        }
    }

    @Nested
    class AlteracaoSenha {

        @Test
        @DisplayName("Deve alterar senha com sucesso")
        void deveAlterarSenhaComSucesso() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("SenhaAtual@123", "senhaHashAtual")).thenReturn(true);
            when(historicoSenhaRepository.findUltimasSenhasHash(1L)).thenReturn(List.of("hashAntigo1", "hashAntigo2"));
            when(passwordEncoder.matches(eq("NovaSenha@123"), any(String.class))).thenReturn(false);
            when(passwordEncoder.encode("NovaSenha@123")).thenReturn("novoHash");
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
            when(historicoSenhaRepository.save(any(HistoricoSenha.class))).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.alterarSenha(1L, "SenhaAtual@123", "NovaSenha@123");

            assertEquals("novoHash", usuario.getSenha());
            verify(usuarioRepository).save(usuario);
            verify(historicoSenhaRepository).save(any(HistoricoSenha.class));
            verify(sessaoAtivaRepository).deleteByUsuarioId(1L);
        }

        @Test
        @DisplayName("Deve lançar exceção quando usuário não existir ao alterar senha")
        void deveLancarExcecaoQuandoUsuarioNaoExistirAoAlterarSenha() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> usuarioService.alterarSenha(1L, "SenhaAtual@123", "NovaSenha@123")
            );

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("Deve lançar exceção quando senha atual estiver incorreta")
        void deveLancarExcecaoQuandoSenhaAtualEstiverIncorreta() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("SenhaErrada@123", "senhaHashAtual")).thenReturn(false);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> usuarioService.alterarSenha(1L, "SenhaErrada@123", "NovaSenha@123")
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        }

        @Test
        @DisplayName("Deve lançar exceção quando nova senha for fraca")
        void deveLancarExcecaoQuandoNovaSenhaForFraca() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("SenhaAtual@123", "senhaHashAtual")).thenReturn(true);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> usuarioService.alterarSenha(1L, "SenhaAtual@123", "123")
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        }

        @Test
        @DisplayName("Deve lançar exceção quando repetir senha do histórico")
        void deveLancarExcecaoQuandoRepetirSenhaDoHistorico() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(passwordEncoder.matches("SenhaAtual@123", "senhaHashAtual")).thenReturn(true);
            when(historicoSenhaRepository.findUltimasSenhasHash(1L)).thenReturn(List.of("hash1", "hash2"));
            when(passwordEncoder.matches("NovaSenha@123", "hash1")).thenReturn(true);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> usuarioService.alterarSenha(1L, "SenhaAtual@123", "NovaSenha@123")
            );

            assertEquals(ErrorCode.VALIDATION_ERROR, ex.getErrorCode());
        }
    }

    @Nested
    class TentativasFalhaEBloqueio {

        @Test
        @DisplayName("Deve registrar tentativa de falha")
        void deveRegistrarTentativaDeFalha() {
            when(usuarioRepository.findByLogin("leandro")).thenReturn(Optional.of(usuario));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.registrarTentativaFalha("leandro");

            verify(usuarioRepository).save(usuario);
        }

        @Test
        @DisplayName("Deve resetar tentativas de falha")
        void deveResetarTentativasDeFalha() {
            usuario.setTentativasFalha(3);

            when(usuarioRepository.findByLogin("leandro")).thenReturn(Optional.of(usuario));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.resetarTentativasFalha("leandro");

            verify(usuarioRepository).save(usuario);
        }

        @Test
        @DisplayName("Deve lançar exceção se conta estiver bloqueada")
        void deveLancarExcecaoSeContaEstiverBloqueada() {
            Usuario usuarioBloqueado = mock(Usuario.class);

            when(usuarioRepository.findByLogin("leandro")).thenReturn(Optional.of(usuarioBloqueado));
            when(usuarioBloqueado.isBloqueado()).thenReturn(true);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> usuarioService.verificarBloqueio("leandro")
            );

            assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        }

        @Test
        @DisplayName("Não deve lançar exceção se conta não estiver bloqueada")
        void naoDeveLancarExcecaoSeContaNaoEstiverBloqueada() {
            Usuario usuarioNaoBloqueado = mock(Usuario.class);

            when(usuarioRepository.findByLogin("leandro")).thenReturn(Optional.of(usuarioNaoBloqueado));
            when(usuarioNaoBloqueado.isBloqueado()).thenReturn(false);

            assertDoesNotThrow(() -> usuarioService.verificarBloqueio("leandro"));
        }
    }

    @Nested
    class SenhaExpirada {

        @Test
        @DisplayName("Deve lançar exceção quando senha estiver expirada")
        void deveLancarExcecaoQuandoSenhaEstiverExpirada() {
            usuario.setDataExpiracaoSenha(LocalDate.now().minusDays(1));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> usuarioService.verificarSenhaExpirada(usuario)
            );

            assertEquals(ErrorCode.PASSWORD_EXPIRED, ex.getErrorCode());
        }

        @Test
        @DisplayName("Não deve lançar exceção quando senha não estiver expirada")
        void naoDeveLancarExcecaoQuandoSenhaNaoEstiverExpirada() {
            usuario.setDataExpiracaoSenha(LocalDate.now().plusDays(30));

            assertDoesNotThrow(() -> usuarioService.verificarSenhaExpirada(usuario));
        }
    }

    @Nested
    class Sessoes {

        @Test
        @DisplayName("Deve criar sessão com sucesso")
        void deveCriarSessaoComSucesso() {
            when(sessaoAtivaRepository.countByUsuarioId(1L)).thenReturn(1L);
            when(request.getHeader("X-Forwarded-For")).thenReturn("127.0.0.1");
            when(request.getHeader("User-Agent")).thenReturn("JUnit");
            when(sessaoAtivaRepository.save(any(SessaoAtiva.class))).thenAnswer(inv -> inv.getArgument(0));

            SessaoAtiva sessao = usuarioService.criarSessao(usuario, "token-jwt");

            assertNotNull(sessao);
            assertEquals(usuario.getId(), sessao.getUsuarioId());
            assertEquals("token-jwt", sessao.getTokenJwt());
            verify(sessaoAtivaRepository).save(any(SessaoAtiva.class));
        }

        @Test
        @DisplayName("Deve remover sessão mais antiga quando atingir limite")
        void deveRemoverSessaoMaisAntigaQuandoAtingirLimite() {
            SessaoAtiva s1 = new SessaoAtiva(usuario.getId(), "t1", "ip", "ua", LocalDateTime.now().plusDays(1));
            SessaoAtiva s2 = new SessaoAtiva(usuario.getId(), "t2", "ip", "ua", LocalDateTime.now().plusDays(1));
            SessaoAtiva s3 = new SessaoAtiva(usuario.getId(), "t3", "ip", "ua", LocalDateTime.now().plusDays(1));

            when(sessaoAtivaRepository.countByUsuarioId(1L)).thenReturn(3L);
            when(sessaoAtivaRepository.findByUsuarioIdOrderByDataCriacaoDesc(1L)).thenReturn(List.of(s1, s2, s3));
            when(request.getHeader("X-Forwarded-For")).thenReturn(null);
            when(request.getRemoteAddr()).thenReturn("192.168.0.10");
            when(request.getHeader("User-Agent")).thenReturn("JUnit");
            when(sessaoAtivaRepository.save(any(SessaoAtiva.class))).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.criarSessao(usuario, "novo-token");

            verify(sessaoAtivaRepository).delete(s3);
            verify(sessaoAtivaRepository).save(any(SessaoAtiva.class));
        }

        @Test
        @DisplayName("Deve remover sessão por token")
        void deveRemoverSessaoPorToken() {
            SessaoAtiva sessao = new SessaoAtiva(usuario.getId(), "token-jwt", "ip", "ua", LocalDateTime.now().plusDays(1));
            when(sessaoAtivaRepository.findByTokenJwt("token-jwt")).thenReturn(Optional.of(sessao));

            usuarioService.removerSessao("token-jwt");

            verify(sessaoAtivaRepository).delete(sessao);
        }

        @Test
        @DisplayName("Deve remover todas as sessões do usuário")
        void deveRemoverTodasAsSessoesDoUsuario() {
            usuarioService.removerTodasSessoes(1L);

            verify(sessaoAtivaRepository).deleteByUsuarioId(1L);
        }

        @Test
        @DisplayName("Deve limpar sessões expiradas")
        void deveLimparSessoesExpiradas() {
            when(sessaoAtivaRepository.deleteAllExpiradas()).thenReturn(2);

            assertDoesNotThrow(() -> usuarioService.limparSessoesExpiradas());

            verify(sessaoAtivaRepository).deleteAllExpiradas();
        }

        @Test
        @DisplayName("Deve ignorar erro ao limpar sessões expiradas")
        void deveIgnorarErroAoLimparSessoesExpiradas() {
            when(sessaoAtivaRepository.deleteAllExpiradas()).thenThrow(new RuntimeException("erro"));

            assertDoesNotThrow(() -> usuarioService.limparSessoesExpiradas());

            verify(sessaoAtivaRepository).deleteAllExpiradas();
        }

        @Test
        @DisplayName("Deve verificar limite de sessões")
        void deveVerificarLimiteDeSessoes() {
            when(sessaoAtivaRepository.countByUsuarioId(1L)).thenReturn(3L);

            assertDoesNotThrow(() -> usuarioService.verificarLimiteSessoes(usuario));

            verify(sessaoAtivaRepository).countByUsuarioId(1L);
        }

        @Test
        @DisplayName("Deve atualizar último acesso do usuário")
        void deveAtualizarUltimoAcessoDoUsuario() {
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.atualizarUltimoAcesso(usuario);

            assertNotNull(usuario.getUltimoAcesso());
            verify(usuarioRepository).save(usuario);
        }

        @Test
        @DisplayName("Deve atualizar último acesso da sessão")
        void deveAtualizarUltimoAcessoDaSessao() {
            SessaoAtiva sessao = new SessaoAtiva(usuario.getId(), "token-jwt", "ip", "ua", LocalDateTime.now().plusDays(1));
            when(sessaoAtivaRepository.findByTokenJwt("token-jwt")).thenReturn(Optional.of(sessao));
            when(sessaoAtivaRepository.save(any(SessaoAtiva.class))).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.atualizarUltimoAcessoDaSessao("token-jwt");

            assertNotNull(sessao.getUltimoAcesso());
            verify(sessaoAtivaRepository).save(sessao);
        }
    }

    @Nested
    class Mfa {

        @Test
        @DisplayName("Deve ativar MFA")
        void deveAtivarMfa() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.ativarMfa(1L, "secret123");

            assertTrue(usuario.getMfaAtivado());
            assertEquals("secret123", usuario.getMfaSecret());
            verify(usuarioRepository).save(usuario);
        }

        @Test
        @DisplayName("Deve desativar MFA")
        void deveDesativarMfa() {
            usuario.setMfaAtivado(true);
            usuario.setMfaSecret("secret123");

            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            usuarioService.desativarMfa(1L);

            assertFalse(usuario.getMfaAtivado());
            assertNull(usuario.getMfaSecret());
            verify(usuarioRepository).save(usuario);
        }

        @Test
        @DisplayName("MFA deve ser obrigatório para ADMIN")
        void mfaDeveSerObrigatorioParaAdmin() {
            usuario.setPerfil(Perfil.ADMIN);

            assertTrue(usuarioService.isMfaObrigatorio(usuario));
        }

        @Test
        @DisplayName("MFA deve ser obrigatório para SUPER_ADMIN")
        void mfaDeveSerObrigatorioParaSuperAdmin() {
            usuario.setPerfil(Perfil.SUPER_ADMIN);

            assertTrue(usuarioService.isMfaObrigatorio(usuario));
        }

        @Test
        @DisplayName("MFA não deve ser obrigatório para perfil comum")
        void mfaNaoDeveSerObrigatorioParaPerfilComum() {
            usuario.setPerfil(null);

            assertFalse(usuarioService.isMfaObrigatorio(usuario));
        }

        @Test
        @DisplayName("Deve informar se MFA está ativado")
        void deveInformarSeMfaEstaAtivado() {
            usuario.setMfaAtivado(true);
            assertTrue(usuarioService.isMfaAtivado(usuario));

            usuario.setMfaAtivado(false);
            assertFalse(usuarioService.isMfaAtivado(usuario));

            usuario.setMfaAtivado(null);
            assertFalse(usuarioService.isMfaAtivado(usuario));
        }
    }
}