package com.app.telemetria.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.telemetria.api.dto.request.AuthRequest;
import com.app.telemetria.api.dto.request.RefreshTokenRequest;
import com.app.telemetria.api.dto.response.AuthResponse;
import com.app.telemetria.domain.entity.SessaoAtiva;
import com.app.telemetria.domain.entity.Usuario;
import com.app.telemetria.domain.exception.BusinessException;
import com.app.telemetria.domain.exception.ErrorCode;
import com.app.telemetria.domain.service.UsuarioService;
import com.app.telemetria.infrastructure.persistence.UsuarioRepository;
import com.app.telemetria.infrastructure.security.JwtService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private UsuarioService usuarioService;
    
    @Autowired
    private HttpServletRequest httpRequest;

    // ================= LOGIN =================
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        log.info("🔐 Tentativa de login para usuário: {}", request.getLogin());
        
        try {
            // RN-USR-001: Verificar bloqueio por tentativas
            usuarioService.verificarBloqueio(request.getLogin());
            
            // Buscar usuário
            Usuario usuario = usuarioRepository.findByLogin(request.getLogin())
                    .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
            
            // RN-USR-001: Verificar senha expirada
            usuarioService.verificarSenhaExpirada(usuario);
            
            // Verificar credenciais
            if (!passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
                usuarioService.registrarTentativaFalha(request.getLogin());
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
            }
            
            // Resetar tentativas de falha
            usuarioService.resetarTentativasFalha(request.getLogin());
            
            // RN-USR-002: Verificar MFA se necessário
            if (usuarioService.isMfaObrigatorio(usuario) && !usuarioService.isMfaAtivado(usuario)) {
                log.warn("⚠️ MFA obrigatório não ativado para usuário: {}", usuario.getLogin());
                // Retornar status para ativar MFA (será implementado)
            }
            
            // Autenticação Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getLogin(), request.getSenha()));
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // Gerar tokens
            String accessToken = jwtService.generateAccessToken(usuario);
            String refreshToken = jwtService.generateRefreshToken(usuario);
            
            // RN-USR-002: Criar sessão ativa
            SessaoAtiva sessao = usuarioService.criarSessao(usuario, accessToken);
            
            // Atualizar último acesso
            usuarioService.atualizarUltimoAcesso(usuario);
            
            log.info("✅ Login bem-sucedido para: {}", request.getLogin());
            return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));

        } catch (LockedException e) {
            log.warn("⚠️ Conta bloqueada: {}", request.getLogin());
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        } catch (DisabledException e) {
            log.warn("⚠️ Conta desabilitada: {}", request.getLogin());
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        } catch (BadCredentialsException | BusinessException e) {
            log.warn("⚠️ Credenciais inválidas: {}", request.getLogin());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    // ================= LOGOUT =================
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        log.info("🚪 Logout solicitado");
        
        if (request != null && request.getRefreshToken() != null) {
            String token = request.getRefreshToken();
            usuarioService.removerSessao(token);
        }
        
        return ResponseEntity.ok().build();
    }

    // ================= REFRESH TOKEN =================
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        log.info("🔄 Requisição de refresh token");
        
        String refreshToken = request.getRefreshToken();

        try {
            if (refreshToken == null || refreshToken.isEmpty()) {
                throw new BusinessException(ErrorCode.TOKEN_INVALID);
            }

            if (!jwtService.isTokenValid(refreshToken)) {
                throw new BusinessException(ErrorCode.TOKEN_INVALID);
            }

            String login = jwtService.getLogin(refreshToken);
            Usuario usuario = usuarioRepository.findByLogin(login)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            // Verificar se usuário está bloqueado
            if (usuario.isBloqueado()) {
                throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
            }

            // Gerar novos tokens
            String newAccessToken = jwtService.generateAccessToken(usuario);
            String newRefreshToken = jwtService.generateRefreshToken(usuario);
            
            // Atualizar último acesso da sessão
            usuarioService.atualizarUltimoAcessoDaSessao(refreshToken);
            
            // Criar nova sessão
            usuarioService.criarSessao(usuario, newAccessToken);
            
            // Remover sessão antiga
            usuarioService.removerSessao(refreshToken);

            log.info("✅ Refresh token concluído para: {}", login);
            return ResponseEntity.ok(new AuthResponse(newAccessToken, newRefreshToken));

        } catch (io.jsonwebtoken.JwtException e) {
            log.error("❌ Erro JWT durante refresh: {}", e.getMessage());
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Erro inesperado durante refresh: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }
}