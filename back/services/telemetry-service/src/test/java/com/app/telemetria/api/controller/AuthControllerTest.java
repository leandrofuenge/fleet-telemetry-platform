package com.app.telemetria.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.app.telemetria.api.dto.request.AuthRequest;
import com.app.telemetria.api.dto.request.RefreshTokenRequest;
import com.app.telemetria.api.dto.response.AuthResponse;
import com.app.telemetria.domain.entity.SessaoAtiva;
import com.app.telemetria.domain.entity.Usuario;
import com.app.telemetria.domain.enums.Perfil;
import com.app.telemetria.domain.exception.BusinessException;
import com.app.telemetria.domain.exception.ErrorCode;
import com.app.telemetria.domain.service.UsuarioService;
import com.app.telemetria.infrastructure.persistence.UsuarioRepository;
import com.app.telemetria.infrastructure.security.JwtService;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UsuarioService usuarioService;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthController authController;

    private Usuario usuario;
    private AuthRequest authRequest;
    private RefreshTokenRequest refreshTokenRequest;

    @BeforeEach
    void setup() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setLogin("leandro");
        usuario.setEmail("leandro@email.com");
        usuario.setCpf("52998224725");
        usuario.setSenha("senhaHash");
        usuario.setPerfil(Perfil.ADMIN);
        usuario.setMfaAtivado(true);
        usuario.setDataExpiracaoSenha(LocalDate.now().plusDays(30));
        usuario.setTentativasFalha(0);

        authRequest = new AuthRequest();
        authRequest.setLogin("leandro");
        authRequest.setSenha("Senha@123");

        refreshTokenRequest = new RefreshTokenRequest();
        refreshTokenRequest.setRefreshToken("refresh-token-123");
    }

    @Nested
    class Login {

        @Test
        @DisplayName("Deve realizar login com sucesso")
        void deveRealizarLoginComSucesso() {
            SessaoAtiva sessao = new SessaoAtiva();

            when(usuarioRepository.findByLogin("leandro")).thenReturn(java.util.Optional.of(usuario));
            when(passwordEncoder.matches("Senha@123", "senhaHash")).thenReturn(true);
            when(usuarioService.isMfaObrigatorio(usuario)).thenReturn(true);
            when(usuarioService.isMfaAtivado(usuario)).thenReturn(true);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(jwtService.generateAccessToken(usuario)).thenReturn("access-token");
            when(jwtService.generateRefreshToken(usuario)).thenReturn("refresh-token");
            when(usuarioService.criarSessao(usuario, "access-token")).thenReturn(sessao);

            ResponseEntity<AuthResponse> response = authController.login(authRequest);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals("access-token", response.getBody().getAccessToken());
            assertEquals("refresh-token", response.getBody().getRefreshToken());

            verify(usuarioService).verificarBloqueio("leandro");
            verify(usuarioService).verificarSenhaExpirada(usuario);
            verify(passwordEncoder).matches("Senha@123", "senhaHash");
            verify(usuarioService).resetarTentativasFalha("leandro");
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(usuarioService).criarSessao(usuario, "access-token");
            verify(usuarioService).atualizarUltimoAcesso(usuario);
        }

        @Test
        @DisplayName("Deve lançar INVALID_CREDENTIALS quando usuário não existir")
        void deveLancarExcecaoQuandoUsuarioNaoExistir() {
            when(usuarioRepository.findByLogin("leandro")).thenReturn(java.util.Optional.empty());

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> authController.login(authRequest)
            );

            assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
            verify(usuarioService).verificarBloqueio("leandro");
            verify(usuarioService, never()).resetarTentativasFalha(any());
        }

        @Test
        @DisplayName("Deve lançar INVALID_CREDENTIALS quando senha estiver incorreta")
        void deveLancarExcecaoQuandoSenhaEstiverIncorreta() {
            when(usuarioRepository.findByLogin("leandro")).thenReturn(java.util.Optional.of(usuario));
            when(passwordEncoder.matches("Senha@123", "senhaHash")).thenReturn(false);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> authController.login(authRequest)
            );

            assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
            verify(usuarioService).registrarTentativaFalha("leandro");
            verify(usuarioService, never()).resetarTentativasFalha(any());
            verify(authenticationManager, never()).authenticate(any());
        }

        @Test
        @DisplayName("Deve lançar INVALID_CREDENTIALS quando AuthenticationManager retornar BadCredentialsException")
        void deveLancarExcecaoQuandoAuthenticationManagerFalhar() {
            when(usuarioRepository.findByLogin("leandro")).thenReturn(java.util.Optional.of(usuario));
            when(passwordEncoder.matches("Senha@123", "senhaHash")).thenReturn(true);
            when(usuarioService.isMfaObrigatorio(usuario)).thenReturn(false);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Credenciais inválidas"));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> authController.login(authRequest)
            );

            assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
        }

        @Test
        @DisplayName("Deve lançar ACCOUNT_LOCKED quando conta estiver bloqueada pelo Spring Security")
        void deveLancarAccountLockedQuandoContaBloqueadaPeloSpringSecurity() {
            when(usuarioRepository.findByLogin("leandro")).thenReturn(java.util.Optional.of(usuario));
            when(passwordEncoder.matches("Senha@123", "senhaHash")).thenReturn(true);
            when(usuarioService.isMfaObrigatorio(usuario)).thenReturn(false);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new LockedException("Conta bloqueada"));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> authController.login(authRequest)
            );

            assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        }

        @Test
        @DisplayName("Deve lançar ACCOUNT_DISABLED quando conta estiver desabilitada")
        void deveLancarAccountDisabledQuandoContaDesabilitada() {
            when(usuarioRepository.findByLogin("leandro")).thenReturn(java.util.Optional.of(usuario));
            when(passwordEncoder.matches("Senha@123", "senhaHash")).thenReturn(true);
            when(usuarioService.isMfaObrigatorio(usuario)).thenReturn(false);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new DisabledException("Conta desabilitada"));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> authController.login(authRequest)
            );

            assertEquals(ErrorCode.ACCOUNT_DISABLED, ex.getErrorCode());
        }

        @Test
        @DisplayName("Deve lançar INVALID_CREDENTIALS quando verificar bloqueio falhar")
        void deveLancarInvalidCredentialsQuandoVerificarBloqueioFalhar() {
            doThrow(new BusinessException(ErrorCode.ACCOUNT_LOCKED))
                    .when(usuarioService).verificarBloqueio("leandro");

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> authController.login(authRequest)
            );

            assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
            verify(usuarioRepository, never()).findByLogin(any());
        }

        @Test
        @DisplayName("Deve lançar INVALID_CREDENTIALS quando senha estiver expirada")
        void deveLancarInvalidCredentialsQuandoSenhaExpirada() {
            when(usuarioRepository.findByLogin("leandro")).thenReturn(java.util.Optional.of(usuario));
            doThrow(new BusinessException(ErrorCode.PASSWORD_EXPIRED))
                    .when(usuarioService).verificarSenhaExpirada(usuario);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> authController.login(authRequest)
            );

            assertEquals(ErrorCode.INVALID_CREDENTIALS, ex.getErrorCode());
        }
    }

    @Nested
    class Logout {

        @Test
        @DisplayName("Deve realizar logout com refresh token")
        void deveRealizarLogoutComRefreshToken() {
            ResponseEntity<Void> response = authController.logout(refreshTokenRequest);

            assertEquals(200, response.getStatusCode().value());
            verify(usuarioService).removerSessao("refresh-token-123");
        }

        @Test
        @DisplayName("Deve realizar logout sem body")
        void deveRealizarLogoutSemBody() {
            ResponseEntity<Void> response = authController.logout(null);

            assertEquals(200, response.getStatusCode().value());
            verify(usuarioService, never()).removerSessao(any());
        }

        @Test
        @DisplayName("Deve realizar logout quando refresh token for nulo")
        void deveRealizarLogoutQuandoRefreshTokenForNulo() {
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken(null);

            ResponseEntity<Void> response = authController.logout(request);

            assertEquals(200, response.getStatusCode().value());
            verify(usuarioService, never()).removerSessao(any());
        }
    }

    @Nested
    class RefreshToken {

        @Test
        @DisplayName("Deve realizar refresh token com sucesso")
        void deveRealizarRefreshComSucesso() {
            when(jwtService.isTokenValid("refresh-token-123")).thenReturn(true);
            when(jwtService.getLogin("refresh-token-123")).thenReturn("leandro");
            when(usuarioRepository.findByLogin("leandro")).thenReturn(java.util.Optional.of(usuario));
            when(jwtService.generateAccessToken(usuario)).thenReturn("new-access-token");
            when(jwtService.generateRefreshToken(usuario)).thenReturn("new-refresh-token");
            when(usuarioService.criarSessao(usuario, "new-access-token")).thenReturn(new SessaoAtiva());

            ResponseEntity<AuthResponse> response = authController.refresh(refreshTokenRequest);

            assertNotNull(response);
            assertEquals(200, response.getStatusCode().value());
            assertNotNull(response.getBody());
            assertEquals("new-access-token", response.getBody().getAccessToken());
            assertEquals("new-refresh-token", response.getBody().getRefreshToken());

            verify(usuarioService).atualizarUltimoAcessoDaSessao("refresh-token-123");
            verify(usuarioService).criarSessao(usuario, "new-access-token");
            verify(usuarioService).removerSessao("refresh-token-123");
        }

        @Test
        @DisplayName("Deve lançar TOKEN_INVALID quando refresh token for nulo")
        void deveLancarTokenInvalidQuandoRefreshTokenForNulo() {
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken(null);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> authController.refresh(request)
            );

            assertEquals(ErrorCode.TOKEN_INVALID, ex.getErrorCode());
        }

        @Test
        @DisplayName("Deve lançar TOKEN_INVALID quando refresh token estiver vazio")
        void deveLancarTokenInvalidQuandoRefreshTokenEstiverVazio() {
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("");

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> authController.refresh(request)
            );

            assertEquals(ErrorCode.TOKEN_INVALID, ex.getErrorCode());
        }

        @Test
        @DisplayName("Deve lançar TOKEN_INVALID quando token for inválido")
        void deveLancarTokenInvalidQuandoTokenForInvalido() {
            when(jwtService.isTokenValid("refresh-token-123")).thenReturn(false);

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> authController.refresh(refreshTokenRequest)
            );

            assertEquals(ErrorCode.TOKEN_INVALID, ex.getErrorCode());
            verify(jwtService, never()).getLogin(any());
        }

        @Test
        @DisplayName("Deve lançar USER_NOT_FOUND quando usuário do refresh não existir")
        void deveLancarUserNotFoundQuandoUsuarioNaoExistir() {
            when(jwtService.isTokenValid("refresh-token-123")).thenReturn(true);
            when(jwtService.getLogin("refresh-token-123")).thenReturn("leandro");
            when(usuarioRepository.findByLogin("leandro")).thenReturn(java.util.Optional.empty());

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> authController.refresh(refreshTokenRequest)
            );

            assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("Deve lançar ACCOUNT_LOCKED quando usuário estiver bloqueado no refresh")
        void deveLancarAccountLockedQuandoUsuarioBloqueadoNoRefresh() {
            when(jwtService.isTokenValid("refresh-token-123")).thenReturn(true);
            when(jwtService.getLogin("refresh-token-123")).thenReturn("leandro");
            when(usuarioRepository.findByLogin("leandro")).thenReturn(java.util.Optional.of(usuario));

            doThrow(new BusinessException(ErrorCode.ACCOUNT_LOCKED))
                    .when(usuarioService).atualizarUltimoAcessoDaSessao("refresh-token-123");

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> authController.refresh(refreshTokenRequest)
            );

            assertEquals(ErrorCode.ACCOUNT_LOCKED, ex.getErrorCode());
        }

        @Test
        @DisplayName("Deve lançar TOKEN_INVALID quando ocorrer erro inesperado no refresh")
        void deveLancarTokenInvalidQuandoOcorrerErroInesperado() {
            when(jwtService.isTokenValid("refresh-token-123")).thenThrow(new RuntimeException("erro inesperado"));

            BusinessException ex = assertThrows(
                    BusinessException.class,
                    () -> authController.refresh(refreshTokenRequest)
            );

            assertEquals(ErrorCode.TOKEN_INVALID, ex.getErrorCode());
        }
    }
}