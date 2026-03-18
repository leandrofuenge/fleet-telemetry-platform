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
import com.app.telemetria.domain.entity.Usuario;
import com.app.telemetria.domain.exception.BusinessException;
import com.app.telemetria.domain.exception.ErrorCode;
import com.app.telemetria.infrastructure.persistence.UsuarioRepository;
import com.app.telemetria.infrastructure.security.JwtService;

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
    private PasswordEncoder passwordEncoder; // ADICIONE ESTA LINHA

    // ================= LOGIN =================
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        log.info("🔐 Tentativa de login para usuário: {}", request.getLogin());
        log.debug("📝 Request details - Login: {}, Senha: [PROTECTED]", request.getLogin());

        try {
            // Primeiro, vamos verificar se o usuário existe e qual é a senha no banco
            log.debug("🔍 Verificando usuário no banco...");
            Usuario usuario = usuarioRepository.findByLogin(request.getLogin())
                    .orElseThrow(() -> {
                        log.warn("⚠️ Usuário não encontrado: {}", request.getLogin());
                        return new BusinessException(ErrorCode.INVALID_CREDENTIALS);
                    });
            
            log.debug("👤 Usuário encontrado - ID: {}, Nome: {}, Perfil: {}", 
                     usuario.getId(), usuario.getNome(), usuario.getPerfil());
            
            // DEBUG - Verificar a senha (remova em produção!)
            log.debug("🔑 Senha fornecida: '{}'", request.getSenha());
            log.debug("🔑 Senha no banco (hash): '{}'", usuario.getSenha());
            
            // Verificar se a senha corresponde
            boolean senhaCorreta = passwordEncoder.matches(request.getSenha(), usuario.getSenha());
            log.debug("✅ Senha correta? {}", senhaCorreta);
            
            if (!senhaCorreta) {
                log.warn("⚠️ Senha incorreta para usuário: {}", request.getLogin());
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
            }

            // Autenticação do usuário via Spring Security
            log.debug("🔄 Iniciando autenticação Spring Security para: {}", request.getLogin());
            
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getLogin(),
                            request.getSenha()
                    )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            log.debug("✅ Autenticação Spring Security bem-sucedida para: {}", request.getLogin());

            // Gera tokens
            log.debug("🔑 Gerando tokens para usuário: {}", request.getLogin());
            String accessToken = jwtService.generateAccessToken(usuario);
            String refreshToken = jwtService.generateRefreshToken(usuario);
            
            log.debug("✅ Tokens gerados - Access Token length: {}, Refresh Token length: {}", 
                     accessToken.length(), refreshToken.length());

            log.info("✅ Login bem-sucedido para: {}", request.getLogin());
            return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));

        } catch (BusinessException e) {
            log.warn("⚠️ Erro de negócio: {}", e.getMessage());
            throw e;
        } catch (BadCredentialsException e) {
            log.warn("⚠️ Credenciais inválidas para usuário: {}", request.getLogin());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        } catch (DisabledException e) {
            log.warn("⚠️ Conta desabilitada para usuário: {}", request.getLogin());
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        } catch (LockedException e) {
            log.warn("⚠️ Conta bloqueada para usuário: {}", request.getLogin());
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        } catch (Exception e) {
            log.error("❌ Erro inesperado durante login: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    // ================= REFRESH TOKEN =================
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        log.info("🔄 Requisição de refresh token recebida");
        
        String refreshToken = request.getRefreshToken();
        log.debug("📝 Refresh token recebido: {}...", 
                 refreshToken != null ? refreshToken.substring(0, Math.min(20, refreshToken.length())) + "..." : "null");

        try {
            // Valida refresh token
            if (refreshToken == null || refreshToken.isEmpty()) {
                log.warn("⚠️ Refresh token vazio ou nulo");
                throw new BusinessException(ErrorCode.TOKEN_INVALID);
            }

            log.debug("🔍 Validando refresh token...");
            if (!jwtService.isTokenValid(refreshToken)) {
                log.warn("⚠️ Refresh token inválido ou expirado");
                throw new BusinessException(ErrorCode.TOKEN_INVALID);
            }
            log.debug("✅ Refresh token válido");

            String login = jwtService.getLogin(refreshToken);
            log.debug("👤 Login extraído do token: {}", login);

            Usuario usuario = usuarioRepository.findByLogin(login)
                    .orElseThrow(() -> {
                        log.error("❌ Usuário não encontrado para login: {}", login);
                        return new BusinessException(ErrorCode.USER_NOT_FOUND);
                    });

            log.debug("👤 Usuário encontrado - ID: {}, Nome: {}, Perfil: {}", 
                     usuario.getId(), usuario.getNome(), usuario.getPerfil());

            // Gera novos tokens
            log.debug("🔑 Gerando novos tokens para: {}", login);
            String newAccessToken = jwtService.generateAccessToken(usuario);
            String newRefreshToken = jwtService.generateRefreshToken(usuario);
            
            log.debug("✅ Novos tokens gerados - Access: {}..., Refresh: {}...", 
                     newAccessToken.substring(0, 20), 
                     newRefreshToken.substring(0, 20));

            log.info("✅ Refresh token concluído com sucesso para: {}", login);
            return ResponseEntity.ok(new AuthResponse(newAccessToken, newRefreshToken));

        } catch (io.jsonwebtoken.JwtException e) {
            log.error("❌ Erro JWT durante refresh: {}", e.getMessage());
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        } catch (BusinessException e) {
            log.error("❌ Erro de negócio durante refresh: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Erro inesperado durante refresh: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }
}