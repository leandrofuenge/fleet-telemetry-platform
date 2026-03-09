package com.app.telemetria.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.web.bind.annotation.*;

import com.app.telemetria.dto.*;
import com.app.telemetria.entity.Usuario;
import com.app.telemetria.repository.UsuarioRepository;
import com.app.telemetria.security.JwtService;
import com.app.telemetria.exception.ErrorCode;
import com.app.telemetria.exception.BusinessException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // ================= LOGIN =================
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        try {
            // Autenticação do usuário
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getLogin(),
                            request.getSenha()
                    )
            );

            Usuario usuario = usuarioRepository.findByLogin(request.getLogin())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            // Gera tokens
            String accessToken = jwtService.generateAccessToken(usuario);
            String refreshToken = jwtService.generateRefreshToken(usuario);

            return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));

        } catch (BadCredentialsException e) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        } catch (DisabledException e) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        } catch (LockedException e) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
    }

    // ================= REFRESH TOKEN =================
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        try {
            String refreshToken = request.getRefreshToken();

            // Valida refresh token
            if (refreshToken == null || refreshToken.isEmpty()) {
                throw new BusinessException(ErrorCode.TOKEN_INVALID);
            }

            if (!jwtService.isTokenValid(refreshToken)) {
                throw new BusinessException(ErrorCode.TOKEN_INVALID);
            }

            String login = jwtService.getLogin(refreshToken);
            Usuario usuario = usuarioRepository.findByLogin(login)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            // Gera novos tokens
            String newAccessToken = jwtService.generateAccessToken(usuario);
            String newRefreshToken = jwtService.generateRefreshToken(usuario);

            return ResponseEntity.ok(new AuthResponse(newAccessToken, newRefreshToken));

        } catch (io.jsonwebtoken.JwtException e) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }
}